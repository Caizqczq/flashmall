# FlashMall 闪购商城 - 系统设计文档

## 1. 系统概述

FlashMall 是一个基于 Spring Cloud 微服务架构的**商品库存与秒杀系统**，采用渐进式开发策略，从基础 CRUD 逐步演进到高并发、高可用、高性能架构。

## 2. 系统架构图

```
                        ┌────────────────────┐
                        │     Client/Web     │
                        └─────────┬──────────┘
                                  │
                        ┌─────────▼──────────┐
                        │   Nginx (Phase 6)  │
                        └─────────┬──────────┘
                                  │
                ┌─────────────────▼─────────────────┐
                │       Spring Cloud Gateway        │
                │           (Port: 9080)            │
                │  ┌─────────┐  ┌────────────────┐  │
                │  │  路由转发 │  │ 全局认证过滤器  │  │
                │  └─────────┘  └────────────────┘  │
                └─────────────────┬─────────────────┘
                                  │
          ┌───────────┬───────────┼───────────┬───────────┐
          │           │           │           │           │
    ┌─────▼─────┐ ┌───▼───┐ ┌────▼────┐ ┌────▼────┐     │
    │ User Svc  │ │ Goods │ │ Order   │ │ Stock   │     │
    │  (9081)   │ │ Svc   │ │ Svc     │ │ Svc     │     │
    │           │ │(9082) │ │ (9083)  │ │ (9084)  │     │
    └─────┬─────┘ └───┬───┘ └────┬────┘ └────┬────┘     │
          │           │           │           │           │
    ┌─────▼───────────▼───────────▼───────────▼─────┐     │
    │                    MySQL                      │     │
    │                (flashmall DB)                 │     │
    └───────────────────────────────────────────────┘     │
                                                          │
    ┌─────────────────────────────────────────────────────┘
    │
    │   ┌─────────────────┐     ┌─────────────────┐
    ├──►│   Nacos 注册中心  │     │  Redis (Phase2) │
    │   └─────────────────┘     └─────────────────┘
    │
    │   ┌─────────────────┐     ┌──────────────────┐
    └──►│ RabbitMQ(Phase3)│     │ Sentinel(Phase4) │
        └─────────────────┘     └──────────────────┘
```

## 3. 服务拆分说明

| 服务 | 模块名 | 端口 | 职责 |
|------|--------|------|------|
| API 网关 | flashmall-gateway | 9080 | 路由转发、认证鉴权、限流（Phase4）、跨域处理 |
| 用户服务 | flashmall-user | 9081 | 用户注册、登录、JWT 认证、用户信息管理 |
| 商品服务 | flashmall-goods | 9082 | 商品 CRUD、上下架管理、秒杀商品管理 |
| 订单服务 | flashmall-order | 9083 | 普通下单、秒杀下单（Phase3）、订单查询、取消 |
| 库存服务 | flashmall-inventory | 9084 | 库存管理、库存扣减（乐观锁/Lua脚本）、库存回补 |
| 公共模块 | flashmall-common | - | 统一响应体、全局异常处理、JWT 工具、分页参数 |

## 4. 技术栈选型

| 类别 | 选型 | 版本 | 选型理由 |
|------|------|------|----------|
| JDK | OpenJDK | 17 | Spring Boot 3.x 最低要求，长期支持版本 |
| 框架 | Spring Boot | 3.2.5 | 成熟稳定，生态丰富 |
| 微服务 | Spring Cloud | 2023.0.1 | 与 Spring Boot 3.2.x 对应 |
| 注册中心 | Nacos | 2.3.x | 兼具注册中心和配置中心，运维简单 |
| 网关 | Spring Cloud Gateway | - | 响应式网关，性能优于 Zuul |
| ORM | MyBatis-Plus | 3.5.5 | 增强 MyBatis，减少样板代码 |
| 数据库 | MySQL | 8.0 | 关系型数据库首选，成熟稳定 |
| 缓存 | Redis | 7.x | 高性能缓存，支持 Lua 脚本（Phase2+） |
| 消息队列 | RabbitMQ | 3.12+ | 可靠消息传递，支持死信队列（Phase3） |
| 熔断降级 | Sentinel | 1.8.7 | 阿里开源，与 Spring Cloud Alibaba 深度集成（Phase4） |
| 分布式锁 | Redisson | 3.25+ | 功能完善的 Redis 客户端（Phase3） |
| 认证 | JWT | 0.12.5 | 无状态认证，适合微服务架构 |
| API 文档 | Knife4j | 4.4.0 | 基于 OpenAPI 3，UI 友好 |
| 构建工具 | Maven | 3.9.x | 依赖管理成熟，多模块支持好 |

## 5. API 接口定义

### 5.1 用户服务（/api/user）

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /api/user/register | 用户注册 | 否 |
| POST | /api/user/login | 用户登录，返回 JWT Token | 否 |
| GET | /api/user/info | 获取当前用户信息 | 是 |
| GET | /api/user/inner/{id} | 根据 ID 获取用户（内部调用） | 否 |

### 5.2 商品服务（/api/goods）

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/goods/list | 商品列表（分页） | 否 |
| GET | /api/goods/detail/{id} | 商品详情 | 否 |
| POST | /api/goods | 新增商品 | 是 |
| PUT | /api/goods/{id} | 修改商品 | 是 |
| PUT | /api/goods/{id}/on-shelf | 上架商品 | 是 |
| PUT | /api/goods/{id}/off-shelf | 下架商品 | 是 |
| GET | /api/goods/inner/{id} | 根据 ID 获取商品（内部调用） | 否 |

### 5.3 订单服务（/api/order）

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /api/order | 创建订单 | 是 |
| GET | /api/order/list | 我的订单列表（分页） | 是 |
| GET | /api/order/{id} | 订单详情 | 是 |
| PUT | /api/order/{id}/cancel | 取消订单 | 是 |

### 5.4 库存服务（/api/stock）

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/stock/{goodsId} | 查询商品库存 | 是 |
| POST | /api/stock/init | 初始化库存 | 是 |
| PUT | /api/stock | 更新库存 | 是 |
| POST | /api/stock/deduct | 扣减库存（内部调用） | 否 |
| POST | /api/stock/add | 增加库存（内部调用） | 否 |

## 6. 数据库 ER 图

```
┌─────────────────┐       ┌────────────────────┐
│     t_user      │       │     t_goods        │
├─────────────────┤       ├────────────────────┤
│ id (PK)         │       │ id (PK)            │
│ username (UK)   │       │ goods_name         │
│ password        │       │ goods_img          │
│ nickname        │       │ goods_detail       │
│ phone           │       │ goods_price        │
│ email           │       │ status             │
│ status          │       │ create_time        │
│ create_time     │       │ update_time        │
│ update_time     │       │ deleted            │
│ deleted         │       └──────┬─────────────┘
└────────┬────────┘              │
         │                       │ 1:1
         │              ┌────────▼─────────┐
         │              │    t_stock       │
         │              ├──────────────────┤
         │              │ id (PK)          │
         │              │ goods_id (UK)    │──── 关联 t_goods.id
         │              │ stock            │
         │              │ version          │
         │              │ create_time      │
         │              │ update_time      │
         │              └──────────────────┘
         │
         │                       │ 1:N
         │              ┌────────▼──────────────┐
         │              │  t_seckill_goods      │
         │              ├───────────────────────┤
         │              │ id (PK)               │
         │              │ goods_id (FK)         │──── 关联 t_goods.id
         │              │ seckill_price         │
         │              │ stock_count           │
         │              │ start_time            │
         │              │ end_time              │
         │              │ status                │
         │              │ create_time           │
         │              │ update_time           │
         │              │ deleted               │
         │              └───────────────────────┘
         │
         │ 1:N
    ┌────▼────────────────┐
    │     t_order         │
    ├─────────────────────┤
    │ id (PK)             │
    │ order_no (UK)       │
    │ user_id (IDX)       │──── 关联 t_user.id
    │ goods_id            │──── 关联 t_goods.id
    │ goods_name          │
    │ goods_price         │
    │ quantity            │
    │ total_amount        │
    │ status              │
    │ pay_time            │
    │ create_time         │
    │ update_time         │
    │ deleted             │
    └─────────────────────┘

    ┌──────────────────────────┐
    │   t_seckill_order        │
    ├──────────────────────────┤
    │ id (PK)                  │
    │ user_id                  │──┐
    │ goods_id                 │──┤ UK(user_id, goods_id) 防重
    │ order_id                 │──── 关联 t_order.id
    │ create_time              │
    └──────────────────────────┘
```

## 7. 渐进式演进路线图

### Phase 1: 基础骨架（当前阶段）
- [x] Maven 多模块项目搭建
- [x] 4 个微服务 + 1 网关 + 1 公共模块
- [x] 各服务基础 CRUD
- [x] MySQL 单库单表
- [x] Nacos 注册中心 + Gateway 路由
- [x] OpenFeign 服务间调用
- [x] JWT 认证 + 网关全局过滤器
- [x] Knife4j API 文档

### Phase 2: 引入缓存
- [ ] 商品详情 Redis 缓存（Cache Aside 模式）
- [ ] 用户 Token 存储到 Redis（支持主动失效）
- [ ] 热点数据本地缓存（Caffeine 二级缓存）
- [ ] 缓存穿透防护（布隆过滤器/空值缓存）
- [ ] 缓存击穿防护（互斥锁/逻辑过期）
- [ ] 缓存雪崩防护（随机过期时间）

### Phase 3: 秒杀核心
- [ ] Redis Lua 脚本实现库存原子扣减
- [ ] RabbitMQ 异步下单（流量削峰填谷）
- [ ] 秒杀商品库存预热（MySQL → Redis）
- [ ] 秒杀结果轮询机制（Redis 标记位）
- [ ] 数据库乐观锁兜底
- [ ] 唯一索引防止重复秒杀

### Phase 4: 限流熔断
- [ ] Gateway 全局限流（令牌桶/滑动窗口）
- [ ] Sentinel 熔断降级规则配置
- [ ] 接口级别限流注解（自定义 AOP）
- [ ] 降级策略（返回友好提示）
- [ ] Sentinel Dashboard 可视化监控

### Phase 5: 高可用
- [ ] MySQL 主从复制 + 读写分离
- [ ] Redis Cluster / Sentinel 哨兵
- [ ] RabbitMQ 镜像队列
- [ ] Nacos 集群部署
- [ ] 分库分表（ShardingSphere）

### Phase 6: 性能优化
- [ ] JMeter 压力测试基准
- [ ] 慢 SQL 分析优化
- [ ] 连接池调优（HikariCP、Lettuce）
- [ ] JVM 参数调优
- [ ] Nginx 负载均衡 + 动静分离

## 8. 秒杀核心流程（Phase 3 目标架构）

```
用户请求 ──► Gateway 限流
               │
               ▼
         Token 校验
               │
               ▼
        秒杀服务入口
               │
     ┌─────────▼─────────┐
     │ Redis Lua 原子扣减  │◄── 库存预热(MySQL→Redis)
     │ (预扣库存)          │
     └─────────┬─────────┘
               │ 扣减成功
               ▼
     ┌─────────────────────┐
     │  发送 MQ 消息        │
     │  (异步下单)          │
     └─────────┬───────────┘
               │
               ▼
     ┌─────────────────────┐
     │  MQ Consumer        │
     │  ├─ 校验重复下单     │◄── 唯一索引兜底
     │  ├─ MySQL 扣减库存   │◄── 乐观锁 WHERE stock >= qty
     │  └─ 创建订单记录     │
     └─────────┬───────────┘
               │
               ▼
     ┌─────────────────────┐
     │  Redis 标记秒杀结果   │
     └─────────────────────┘
               │
               ▼
         客户端轮询结果
```

## 9. 项目启动指南

### 9.1 环境准备

1. **JDK 17**
2. **MySQL 8.0**：创建数据库并执行 `sql/schema.sql` 和 `sql/data.sql`
3. **Nacos 2.3.x**：本地启动或 Docker 启动
   ```bash
   # Docker 启动 Nacos（单机模式）
   docker run -d --name nacos \
     -e MODE=standalone \
     -p 8848:8848 -p 9848:9848 \
     nacos/nacos-server:v2.3.1
   ```

### 9.2 编译项目

```bash
mvn clean compile
```

### 9.3 启动顺序

1. 启动 Nacos
2. 启动 flashmall-gateway（9080）
3. 启动 flashmall-user（9081）
4. 启动 flashmall-goods（9082）
5. 启动 flashmall-order（9083）
6. 启动 flashmall-inventory（9084）

### 9.4 验证服务

- Nacos 控制台：http://127.0.0.1:8848/nacos （用户名/密码: nacos/nacos）
- 用户服务文档：http://127.0.0.1:9081/doc.html
- 商品服务文档：http://127.0.0.1:9082/doc.html
- 订单服务文档：http://127.0.0.1:9083/doc.html
- 库存服务文档：http://127.0.0.1:9084/doc.html

### 9.5 快速测试

```bash
# 1. 用户注册
curl -X POST http://localhost:9080/api/user/register \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"123456","nickname":"演示用户"}'

# 2. 用户登录（获取 Token）
curl -X POST http://localhost:9080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"123456"}'

# 3. 查看商品列表（无需认证）
curl http://localhost:9080/api/goods/list

# 4. 查看库存（需要 Token）
curl http://localhost:9080/api/stock/1 \
  -H "Authorization: Bearer <YOUR_TOKEN>"

# 5. 创建订单（需要 Token）
curl -X POST http://localhost:9080/api/order \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <YOUR_TOKEN>" \
  -d '{"goodsId":1,"quantity":1}'
```
