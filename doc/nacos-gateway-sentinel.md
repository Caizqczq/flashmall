# Nacos、Gateway 与 Sentinel 验证说明

## 1. 当前项目改造点

- 服务注册发现：`flashmall-user/goods/order/stock/gateway` 均接入 Nacos Discovery。
- 配置中心：各服务通过 `spring.config.import` 导入 `flashmall-common.yaml` 与自身服务配置。
- 服务网关：Gateway 通过 `lb://flashmall-*` 从 Nacos 动态发现实例，并保留 `/api/**` 统一入口。
- 动态配置验证：商品服务新增 `/goods/config/probe`，可读取并动态刷新 Nacos 属性。
- 动态路由验证：商品服务新增 `/goods/route/probe`，返回当前命中的实例信息。
- 流量治理：Gateway 使用 Sentinel 网关限流，商品服务使用 Sentinel 对探针接口做限流、熔断和降级。

## 2. 启动环境

```bash
docker compose up -d --build
```

发布本仓库中的 Nacos 示例配置：

```bash
bash scripts/nacos/publish-config.sh
```

Nacos 控制台地址：

```text
http://localhost:8848/nacos
```

本项目在 `docker-compose.yml` 中关闭了 Nacos 鉴权，控制台如提示登录可使用默认账号 `nacos/nacos`。

## 3. 验证服务注册发现与网关路由

查看服务是否注册：

```bash
curl "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=flashmall-goods"
```

通过网关访问商品服务：

```bash
curl -i http://localhost:9080/api/goods/route/probe
```

连续请求可以观察响应体中的 `port` 与响应头 `X-Backend-Instance`：

```bash
for i in {1..8}; do curl -s -i http://localhost:9080/api/goods/route/probe | grep -E "X-Backend-Instance|port"; done
```

停止一个商品实例后再次请求，Gateway 会从 Nacos 获取健康实例并继续转发：

```bash
docker stop flashmall-goods-2
curl -i http://localhost:9080/api/goods/route/probe
docker start flashmall-goods-2
```

## 4. 验证 Nacos 动态配置

读取当前配置：

```bash
curl http://localhost:9080/api/goods/config/probe
```

在 Nacos 控制台或通过接口修改 `flashmall-goods.yaml`：

```bash
curl -X POST "http://localhost:8848/nacos/v1/cs/configs" \
  --data-urlencode "dataId=flashmall-goods.yaml" \
  --data-urlencode "group=DEFAULT_GROUP" \
  --data-urlencode "type=yaml" \
  --data-urlencode 'content=flashmall:
  config:
    goods-message: "goods config updated from Nacos"
    version: "goods-v2"
  traffic:
    goods:
      probe-qps: 10
      slow-rt-ms: 200
      slow-ratio-threshold: 0.5
      slow-min-request-amount: 5
      slow-stat-interval-ms: 10000
      slow-time-window-seconds: 5
      exception-ratio-threshold: 0.5
      exception-min-request-amount: 5
      exception-stat-interval-ms: 10000
      exception-time-window-seconds: 5'
```

等待 1 到 3 秒后再次请求，无需重启服务即可看到 `goodsMessage` 和 `configVersion` 更新：

```bash
curl http://localhost:9080/api/goods/config/probe
```

## 5. 验证 Sentinel 流量治理

正常访问：

```bash
curl http://localhost:9080/api/goods/traffic/probe
```

服务限流默认由 `flashmall.traffic.goods.probe-qps` 控制，网关限流默认由 `flashmall.traffic.gateway.goods-qps` 控制。两者都可在 Nacos 中动态调整。

快速触发服务慢调用熔断：

```bash
for i in {1..20}; do curl -s "http://localhost:9080/api/goods/traffic/probe?sleepMs=300"; echo; done
```

快速触发异常比例熔断和业务降级：

```bash
for i in {1..20}; do curl -s "http://localhost:9080/api/goods/traffic/probe?fail=true"; echo; done
```

预期现象：

- 网关限流：HTTP 状态码为 `429`，响应体提示 `网关限流`。
- 服务限流：响应体 `code=429`，`status=BLOCKED`。
- 服务熔断：响应体 `code=429`，`blockType=DegradeException`。
- 业务降级：响应体 `code=503`，`status=FALLBACK`。

## 6. JMeter 压测

网关 + 服务限流压测：

```bash
jmeter -n -t jmeter/traffic-governance.jmx \
  -Jhost=localhost -Jport=9080 \
  -Jpath=/api/goods/traffic/probe \
  -Jthreads=80 -Jloops=30 -JrampUp=5 \
  -l jmeter/traffic-flow.jtl
```

慢调用熔断压测：

```bash
jmeter -n -t jmeter/traffic-governance.jmx \
  -Jhost=localhost -Jport=9080 \
  -Jpath="/api/goods/traffic/probe?sleepMs=300" \
  -Jthreads=20 -Jloops=30 -JrampUp=3 \
  -l jmeter/traffic-slow-degrade.jtl
```

异常降级压测：

```bash
jmeter -n -t jmeter/traffic-governance.jmx \
  -Jhost=localhost -Jport=9080 \
  -Jpath="/api/goods/traffic/probe?fail=true" \
  -Jthreads=20 -Jloops=30 -JrampUp=3 \
  -l jmeter/traffic-exception-degrade.jtl
```

建议记录以下指标：

- `traffic-flow.jtl` 中 HTTP `429` 的占比，用于说明网关限流效果。
- 慢调用压测中 `DegradeException` 出现时间，用于说明熔断窗口生效。
- 异常压测中 `FALLBACK` 与 `DegradeException` 的比例，用于说明降级和熔断链路生效。
