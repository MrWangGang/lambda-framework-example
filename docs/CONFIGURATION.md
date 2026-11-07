# 配置与运行约定

## 环境变量

| 变量 | 默认值／含义 |
|---|---|
| `JAVA_HOME` | 必填，JDK 27 路径 |
| `MAVEN_BIN` | 可选，已有 Maven 可执行文件；未设置时优先 IDEA 内置 Maven |
| `SERVICE_HOST` | 服务主机，默认 `127.0.0.1` |
| `SERVICE_USER` / `SERVICE_PASSWORD` | 用户名默认 `root`；密码必填 |
| `MONGODB_REPLICA_SET` | `rs0`，必须与服务端一致 |
| `MONGO_DATABASE` | `lambda_example` |
| `EXAMPLE_RESOURCE_PREFIX` | `lambda_example`，用于 Redis、RabbitMQ、Qdrant 隔离 |
| `NACOS_SERVER_ADDR` | `${SERVICE_HOST}:8848` |
| `NACOS_USERNAME` / `NACOS_PASSWORD` | 默认使用公共服务账号密码 |
| `NACOS_NAMESPACE` / `NACOS_GROUP` | `public` / `LAMBDA_EXAMPLE` |
| `APP_NAME` | `lambda-example-app`；配置 Data ID 为 `${APP_NAME}.properties` |
| `APP_PORT` / `APP_BIND_ADDRESS` | `8081` / `127.0.0.1` |
| `APP_REGISTER_IP` | `127.0.0.1`，部署时换成调用方可达地址 |
| `QDRANT_API_KEY` | 默认使用 `SERVICE_PASSWORD` |

`application.properties` 中还可覆盖 `lambda.repository.*`、`lambda.mq.*` 和 `example.qdrant.url`。不要将真实凭据提交到仓库。`.env` 会由 `scripts/run.sh` 作为 shell 配置读取，只填写可信的变量赋值。

## 服务前提

- MongoDB：27017，认证库 `admin`。事务要求副本集；单节点副本集支持事务但不具备高可用。副本集成员公布的主机名/IP 必须可从应用访问。
- Redis：6379，支持 ACL 用户名与密码。
- RabbitMQ：5672；管理界面 15672。启用 `rabbitmq_delayed_message_exchange`，插件版本须与 RabbitMQ 匹配。
- Nacos 3：8848 为 API，9848 为客户端 gRPC，8080 为控制台。仅开 8848 不足以支持 Java 客户端。
- Qdrant：6333 为 REST，6334 为 gRPC；本示例使用 REST 和 API Key。

所有服务使用各自原始端口，不做端口转换。

## 数据与可靠性

商品列表要求 category，采用 `(createdAt, _id)` 倒序游标；不要改成大偏移 `skip`。商品金额使用 Decimal128；更新必须携带当前 version。收藏使用独立关系集合与 `(userId, productId)` 唯一索引，避免用户文档中出现无限增长的商品 ID 数组。

下单使用 requestId 作为订单主键；重复请求只有参数完全一致才返回已有订单，否则返回 409。扣库存与写订单使用同一个 MongoDB 事务。瞬时事务冲突最多重试 3 次，超过上限返回错误，由调用方使用同一 requestId 查询或重试。

缓存操作独立演示 TTL。延迟消息在消费者持久化 MongoDB 回执后 ACK；重复消费按消息 ID 幂等写入。消费失败进入死信队列，须由运维或业务补偿流程处理。死信队列不是自动重试队列。

演示向量集合固定为 3 维 Cosine，便于手工调用；接入真实 embedding 模型时，按模型维度新建集合并迁移数据。向量查询结果应回查 MongoDB 过滤已删除、无权限和过期业务对象。商品更新不会自动同步到 Qdrant。

生产系统如需“订单落库后一定发出消息”，应在同一 MongoDB 事务内写入订单和 Outbox，再由可靠任务投递，依赖发布确认与幂等消费者收敛状态。
