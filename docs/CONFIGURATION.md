# 配置示例应用

复制 `.env.example`，填入本机部署值。网关和两个服务使用同一 Nacos namespace 和 group。应用名与 Data ID 分别为 `service-a.properties`、`service-b.properties` 和 `gateway.properties`。

| 环境变量 | 用途 |
|---|---|
| `JAVA_HOME`、`MAVEN_BIN` | JDK 27 与已有 Maven |
| `NACOS_SERVER_ADDR`、`NACOS_NAMESPACE`、`NACOS_GROUP`、`NACOS_USERNAME`、`NACOS_PASSWORD` | Nacos 引导与发现 |
| `MONGODB_URI`、`MONGODB_MODE` | 带数据库和 replicaSet 的连接 URI |
| `REDIS_NODES`、`REDIS_MODE`、`REDIS_USERNAME`、`REDIS_PASSWORD` | 缓存和会话存储 |
| `QDRANT_ENDPOINTS`、`QDRANT_MODE`、`QDRANT_API_KEY` | Qdrant gRPC |
| `RABBITMQ_ADDRESSES`、`RABBITMQ_MODE`、`RABBITMQ_USERNAME`、`RABBITMQ_PASSWORD` | RabbitMQ，服务端启用延迟插件 |
| `EXAMPLE_VECTOR_REPLICATION_FACTOR` | 创建 Qdrant 集合时的副本数 |
| `LAMBDA_MQ_KAFKA_BOOTSTRAPSERVERS`、`LAMBDA_MQ_KAFKA_MODE` | 可选 Kafka 引导地址与模式 |
| `LAMBDA_LOCK_ZOOKEEPER_CONNECTSTRING`、`LAMBDA_LOCK_ZOOKEEPER_MODE` | 可选 ZooKeeper 锁地址与模式 |
| `EXAMPLE_DEMO_USER`、`EXAMPLE_DEMO_PASSWORD` | 示例登录账号与口令 |
| `EXAMPLE_RESOURCE_PREFIX` | 缓存键、队列、向量集合前缀 |

多个 Redis、RabbitMQ 地址使用逗号分隔的 `host:port`。多个 Qdrant 地址使用逗号分隔的 `grpc[s]://host:port`。MongoDB 单机使用单节点副本集，Redis 集群使用数据库 0。外网访问通过网关，RSocket TCP 端口开放给获准的内网调用方。

复制模板时保留单机配置，切换某个组件时启用对应的集群块。MongoDB URI 的账号口令做 URL 编码。RabbitMQ 集群每个接入节点加载延迟插件，示例会创建 quorum 队列。切换已有队列类型时使用新资源前缀迁移。已创建的向量集合也需要单独迁移副本配置。

Kafka 和 ZooKeeper 是可选接入，配置端点后框架会启动连接检查。示例的延迟通知使用 RabbitMQ。Redisson 沿用 Redis 连接。ZooKeeper 只绑定服务器回环地址时，开发机通过 SSH 隧道访问。 [框架组件配置](https://github.com/MrWangGang/lambda-framework/blob/main/docs/MODULES-AND-CONFIGURATION.md)
