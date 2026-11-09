# 配置字典与资源命名

[返回总览](../README.md) · [首次启动](QUICKSTART.md) · [运维](OPERATIONS.md)

业务服务与网关分别加载各自的 `application.properties`。两个启动脚本读取根目录 `.env`。以下默认值是源码默认，不代表可以省略服务准备。

## 1. 脚本环境变量

| 变量 | 默认 / 必填 | 用途 |
|---|---|---|
| JAVA_HOME | run.sh 必填 | JDK 27 的 Home |
| MAVEN_BIN | 可选 | 已有 Maven 可执行文件 |
| SERVICE_HOST | 配置默认 127.0.0.1，但 run.sh 要求非空 | 多项基础服务共用地址 |
| SERVICE_USER | root | Mongo / Redis / Rabbit 默认用户名 |
| SERVICE_PASSWORD | run.sh 必填 | 多项服务默认密码，Qdrant 默认 API Key 来源 |
| APP_NAME | lambda-example-app | 应用名、Nacos 服务名和 Data ID 前缀 |
| APP_BIND_ADDRESS | 127.0.0.1 | 业务 RSocket TCP 监听地址 |
| APP_RSOCKET_PORT | 7000 | 业务 TCP 端口 |
| GATEWAY_NAME | lambda-example-gateway | 网关名与 Nacos Data ID 前缀 |
| GATEWAY_BIND_ADDRESS | 127.0.0.1 | 网关 HTTP 监听地址 |
| APP_PORT | 8081 | 网关 HTTP 端口 |
| APP_REGISTER_IP | 127.0.0.1 | Nacos 对外公布的应用 IP |
| MONGO_DATABASE | lambda_example | Mongo 业务数据库 |
| MONGODB_REPLICA_SET | rs0 | 副本集名称 |
| EXAMPLE_RESOURCE_PREFIX | lambda_example | Redis / Rabbit / Qdrant 资源前缀 |
| NACOS_SERVER_ADDR | SERVICE_HOST:8848 | 配置与发现 API 地址 |
| NACOS_USERNAME | SERVICE_USER | Nacos 账号，可单独覆盖 |
| NACOS_PASSWORD | SERVICE_PASSWORD | Nacos 密码，可单独覆盖 |
| NACOS_NAMESPACE | public | 命名空间 ID |
| NACOS_GROUP | LAMBDA_EXAMPLE | 配置与发现 group |
| QDRANT_API_KEY | SERVICE_PASSWORD | Qdrant API Key，可单独覆盖 |

改 APP_NAME 后，Nacos 对应配置必须提前存在。改资源前缀不会自动迁移或删除旧资源。

## 2. Spring 属性映射

| 属性 | 当前来源 / 行为 |
|---|---|
| spring.application.name | 业务服务用 APP_NAME，网关用 GATEWAY_NAME |
| spring.main.web-application-type | 业务服务 none，网关 reactive |
| spring.rsocket.server.address / port | APP_BIND_ADDRESS / APP_RSOCKET_PORT |
| server.address / server.port | 网关使用 GATEWAY_BIND_ADDRESS / APP_PORT |
| spring.config.import | 业务服务必需导入，网关可选导入，Data ID 为各自应用名.properties |
| spring.cloud.gateway.server.webflux.routes | 默认将 /api/** 转发到 rb://APP_NAME |
| spring.cloud.nacos.config.server-addr | NACOS_SERVER_ADDR |
| spring.cloud.nacos.discovery.server-addr | NACOS_SERVER_ADDR |
| spring.cloud.nacos.config.namespace | NACOS_NAMESPACE |
| spring.cloud.nacos.discovery.namespace | NACOS_NAMESPACE |
| spring.cloud.nacos.discovery.group | NACOS_GROUP |
| spring.cloud.nacos.discovery.ip | APP_REGISTER_IP |
| spring.cloud.nacos.username/password | 专用 Nacos 变量或共用凭据 |
| lambda.repository.mongo.host | SERVICE_HOST:27017 |
| lambda.repository.mongo.user/password | 共用凭据 |
| lambda.repository.mongo.auth-database | admin |
| lambda.repository.mongo.database | MONGO_DATABASE |
| lambda.repository.mongo.deploy | 固定 cluster |
| lambda.repository.mongo.replica | MONGODB_REPLICA_SET |
| lambda.repository.redis.host | SERVICE_HOST:6379 |
| lambda.repository.redis.username/password | 共用凭据 |
| lambda.repository.redis.database | 0 |
| lambda.repository.redis.deploy | single |
| lambda.mq.rabbitmq.host | SERVICE_HOST:5672 |
| lambda.mq.rabbitmq.user/password | 共用凭据 |
| example.resource-prefix | EXAMPLE_RESOURCE_PREFIX |
| example.qdrant.url | http://SERVICE_HOST:6333 |
| example.qdrant.api-key | QDRANT_API_KEY 或共用密码 |
| example.greeting | 本地 local-default；Nacos 远程内容用于实际示例 |

特别注意：现有代码没有读取 MONGO_PASSWORD、REDIS_PASSWORD、RABBITMQ_PASSWORD 等自定义变量。自行新增这些名字不会自动生效。

## 3. 服务使用不同地址或账号

可以通过 Spring 属性覆盖现有占位表达式。非敏感地址可用命令行参数，例如：

~~~bash
./scripts/run.sh \
  --lambda.repository.mongo.host=mongo.internal:27017 \
  --lambda.repository.redis.host=redis.internal:6379 \
  --lambda.mq.rabbitmq.host=rabbit.internal:5672 \
  --example.qdrant.url=http://qdrant.internal:6333
~~~

上面反斜杠表示 shell 续行。凭据建议用外部私有配置文件或受控运行环境注入，不写进提交文件，也避免放到可见的进程参数。

外部 Properties 文件中的属性示意：

~~~properties
lambda.repository.mongo.user=YOUR_MONGO_USER
lambda.repository.mongo.password=YOUR_MONGO_PASSWORD
lambda.repository.redis.username=YOUR_REDIS_USER
lambda.repository.redis.password=YOUR_REDIS_PASSWORD
lambda.mq.rabbitmq.user=YOUR_RABBIT_USER
lambda.mq.rabbitmq.password=YOUR_RABBIT_PASSWORD
~~~

外部文件路径和权限由部署环境管理。使用 spring.config.additional-location 指向它时，确认文件确实存在，仍保留必需的 Nacos 配置导入。

## 4. 自动创建的资源

| 系统 | 默认资源 |
|---|---|
| MongoDB | 数据库 lambda_example |
| MongoDB 集合 | products、favorites、purchases、delivery_receipts |
| Redis | lambda_example:cache:<key> |
| Rabbit 延迟交换机 | lambda_example.delayed |
| Rabbit 业务队列 | lambda_example.notifications |
| Rabbit 死信交换机 | lambda_example.delayed.dead |
| Rabbit 死信队列 | lambda_example.notifications.dead |
| Rabbit routing key | notify；死信为 dead |
| Qdrant | lambda_example_vectors |
| Nacos | lambda-example-app.properties / LAMBDA_EXAMPLE / public |

Mongo 数据库由 MONGO_DATABASE 隔离，**不会因 EXAMPLE_RESOURCE_PREFIX 改变**。集合和索引可能在启动时创建，部分集合在首次写入时创建。

测试隔离必须同时改 Mongo 数据库和资源前缀。需要隔离 Nacos 时再改 APP_NAME / GROUP，并提前创建对应配置。

## 5. 索引

| 集合 | 索引名 | 字段 |
|---|---|---|
| products | category_recent | category ASC, createdAt DESC, _id DESC |
| favorites | unique_user_product | userId ASC, productId ASC，唯一 |
| favorites | user_recent | userId ASC, createdAt DESC, _id DESC |
| favorites | product_recent | productId ASC, createdAt DESC |
| purchases | 默认 _id | requestId 作为订单主键 |
| delivery_receipts | 默认 _id | message.id 作为回执主键 |

已有历史重复收藏会导致唯一索引创建失败；不能直接删除生产数据解决。先查重、确定合并规则，再迁移。

## 6. 业务限制

| 项目 | 当前限制 |
|---|---|
| 商品 name | 非空，最长 120 |
| 商品 category | 非空，最长 64（写入入参） |
| price | 非负，最多 12 位整数、2 位小数 |
| stock | 非负整数 |
| 商品分页 limit | 1..100，默认 20 |
| 收藏列表 | 固定最多 50 |
| quantity | 1..1000 |
| requestId | 1..80 位字母数字、下划线、连字符 |
| 缓存 key | 同上 |
| 缓存 value | 非 null，最多 10000 字符 |
| 缓存 TTL | 1..86400 秒 |
| 消息 content | 非空，最多 10000 字符 |
| 延迟 | 0..60000 毫秒 |
| 向量 | 固定 3 维，有限数值，不能全零 |
| 向量 limit | 1..100，无默认值需传入 |

商品查询 category 只检查非空，和商品写入的长度约束并非完全相同。手册记录当前行为，不隐含不存在的统一校验。

## 7. 可靠性约定

购买请求用 requestId 幂等，只在 userId/productId/quantity 完全一致时返回旧订单。库存扣减和新订单写入在同一 MongoDB 事务。只对 TransientTransactionError 标签做最多 3 次重试，不对任意错误无限重试。

Rabbit 消费 qos=32，flatMap 并发=8；先幂等保存 Mongo 回执，再 ACK。单条处理失败 nack(false) 进入死信；消费流整体错误最多退避重试 10 次。死信队列不会自动重新投递。

Qdrant REST 写入使用 wait=true；读写请求有 10 秒超时。集合只在不存在时创建，当前代码不会自动修复已有集合的维度或距离配置。

## 8. 配置变更流程

1. 记录原配置、应用版本和资源名。
2. 确认修改项是否可以动态刷新。
3. greeting 可以运行中更新；数据库凭据、资源前缀等按重启变更处理。
4. 修改后验证连接、业务读写和资源隔离。
5. 失败时回到已记录配置，避免两组应用同时消费错误队列。
6. 同步文档和环境模板；实际密码仅在私有环境保存。
