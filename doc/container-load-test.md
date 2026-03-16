# FlashMall 容器化、负载均衡与缓存验证

## 1. 启动容器环境

```bash
docker compose up -d --build
```

启动后可访问：

- Nginx 首页：`http://localhost/`
- 商品详情轮询入口：`http://localhost/api/goods/detail/1`
- 最少连接入口：`http://localhost/api/lb/least/goods/detail/1`
- `ip_hash` 入口：`http://localhost/api/lb/iphash/goods/detail/1`
- 一致性哈希入口：`http://localhost/api/lb/urihash/goods/detail/1`
- Gateway：`http://localhost:9080`
- Nacos：`http://localhost:8848/nacos`

## 2. 验证负载均衡

连续请求商品详情：

```bash
curl -i http://localhost/api/goods/detail/1
curl -i http://localhost/api/goods/detail/1
curl -i http://localhost/api/goods/detail/1
```

重点看响应头：

- `X-Backend-Instance`：具体命中的商品服务实例
- `X-Nginx-Upstream`：Nginx 实际转发到的上游地址

查看两个商品实例日志：

```bash
docker logs flashmall-goods-1 --tail 50
docker logs flashmall-goods-2 --tail 50
```

`handledRequestCount` 会持续递增。轮询和最少连接场景下，两边计数应大致接近。

## 3. JMeter 压测

压测静态首页：

```bash
jmeter -n -t jmeter/static-page.jmx -l jmeter/static-page.jtl
```

压测商品详情轮询接口：

```bash
jmeter -n -t jmeter/goods-detail-lb.jmx -Jpath=/api/goods/detail/1 -l jmeter/goods-detail-lb.jtl
```

压测最少连接：

```bash
jmeter -n -t jmeter/goods-detail-lb.jmx -Jpath=/api/lb/least/goods/detail/1 -l jmeter/goods-least.jtl
```

压测 `ip_hash`：

```bash
jmeter -n -t jmeter/goods-detail-lb.jmx -Jpath=/api/lb/iphash/goods/detail/1 -l jmeter/goods-iphash.jtl
```

压测一致性哈希：

```bash
jmeter -n -t jmeter/goods-detail-lb.jmx -Jpath=/api/lb/urihash/goods/detail/1 -l jmeter/goods-urihash.jtl
```

建议对比：

- 静态文件平均响应时间通常应明显低于动态接口
- 开启 Redis 后，商品详情接口第二轮开始平均响应时间会下降
- 轮询和最少连接下，两台商品服务的处理请求数应接近
- `ip_hash` 下，同一压测源 IP 往往会更集中地命中某一个实例

## 4. 当前缓存策略

商品详情接口已实现 Cache Aside，并处理了三类问题：

- 缓存穿透：不存在的商品写入短 TTL 空值
- 缓存击穿：热点 Key 回源时使用 Redisson 分布式锁互斥重建
- 缓存雪崩：正常缓存 TTL 加随机抖动，避免同一时刻大面积失效
