# Lambda Framework Example

三个可运行应用展示统一网关、自动配置的数据组件，以及带身份的 Fory 双向 RPC。

~~~text
service-a/
  app/       业务应用，提供商品、缓存、延迟消息、向量和登录
  facade/    A 的 Java RPC 契约 JAR
service-b/   使用 A 的 Facade 处理聊天双向流
gateway/     前端 HTTP、WSS 与认证入口
~~~

## 启动

先在框架仓库执行 `./scripts/mvn.sh clean install`。在示例根目录复制 `.env.example` 为 `.env`，按 [配置手册](docs/CONFIGURATION.md) 提供连接值与凭据。模板默认使用单机，附各组件的集群配置。Mongo 单机也使用单节点副本集。三个应用使用同一 Nacos namespace 和 group。

分别在终端启动。

~~~bash
./scripts/run-service-a.sh
./scripts/run-service-b.sh
./scripts/run-gateway.sh
~~~

默认网关为 `http://127.0.0.1:8081`，A 和 B 的内网 RSocket 端口分别为 7000 和 7001。Nacos 服务名是 `service-a`、`service-b`，Data ID 是 `service-a.properties`、`service-b.properties` 和 `gateway.properties`。完成后在各终端按 Ctrl+C。

## 普通请求

用配置的演示用户和口令调用 `POST /api/session/login`，读取 `data.token`。其余 HTTP 请求使用 `Authorization: Bearer <token>`。

~~~bash
curl -fsS --json '{"username":"demo","password":"填入本机配置的演示口令"}'   http://127.0.0.1:8081/api/session/login

curl -fsS -H "Authorization: Bearer ${TOKEN}" --json '{}'   http://127.0.0.1:8081/api/session/whoami
~~~

| 路径前缀 | 用途 |
|---|---|
| `/api/product/` | 商品、分页和游标 |
| `/api/favorite/` | 收藏关系 |
| `/api/purchase/` | MongoDB 事务扣库存 |
| `/api/cache/` | Redis 缓存 |
| `/api/message/` | RabbitMQ 延迟消息和 Mongo 回执 |
| `/api/vector/` | Qdrant 向量写入和查询 |

HTTP 路由 `rb://service-a` 覆盖 `/api/**`。前端发送 ObjectId 的 24 位十六进制字符串，Java 业务拿到 `ObjectId`。响应读取 `serviceCode`、`serviceMessage` 和 `data`。 [请求参数](docs/API.md)

## 数据层示例

- [ProductRepository](service-a/app/src/main/java/com/lambda/example/servicea/repository/ProductRepository.java) 用 MongoDB 查询与分页。
- [VectorRepository](service-a/app/src/main/java/com/lambda/example/servicea/repository/VectorRepository.java) 绑定 Qdrant 集合，响应式写入与相似度查询。
- [CacheService](service-a/app/src/main/java/com/lambda/example/servicea/service/CacheService.java) 用 Redis 管理缓存。
- [DelayedMessageService](service-a/app/src/main/java/com/lambda/example/servicea/service/DelayedMessageService.java) 用 RabbitMQ 延迟投递、手动确认和 MongoDB 回执。
- [SessionController](service-a/app/src/main/java/com/lambda/example/servicea/controller/SessionController.java) 展示登录、读取身份和注销。

## WSS 与双向 Fory RPC

小程序连接 `wss://网关域名/ws/api/chat/exchange`，在握手时传 Bearer 令牌。本机 WebSocket 客户端使用 `ws://127.0.0.1:8081/ws/api/chat/exchange`。网关的 `ws://service-b` 路由剥掉 `/ws` 前缀，B 把业务流直接传给 A。

~~~java
@RSocketRpc
private ChatFacade chat;

@MessageMapping("/api/chat/exchange")
public BiFlux<ChatFacade.Receipt> exchange(Flux<ChatFacade.Message> messages) {
    return chat.exchange(messages);
}
~~~

A 在 `service-a/facade` 声明 `service-a.chatFacade.exchange`，在 `service-a/app` 用 `@RSocketFacade` 实现。前端每条文本消息发送一个 JSON 值，例如 `{"messageId":"m-1","text":"你好"}`。每条回执独立返回，`data` 为数组。回执中的 `principal` 是网关验证后透传的用户 ID。Java RPC 的业务数据与身份元数据都用 Fory。

<details>
<summary>展开开发规范与智能体技能入口</summary>

[开发规范技能](https://github.com/MrWangGang/lambda-framework/blob/main/skills/lambda-java-backend/SKILL.md) · [架构与开发规范](https://github.com/MrWangGang/lambda-framework/blob/main/docs/Java后端开发与架构规范.md)

</details>
