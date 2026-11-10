# 通过 Nacos 配置示例

复制 `.env.example` 为 `.env`。本地填写 JDK 和 Nacos 的地址、namespace、group、用户名与凭据。三个应用启动时从 Nacos 读取必需的配置。

| 应用 | Nacos Data ID | 内容 |
|---|---|---|
| Service A | `service-a.properties` | RSocket、MongoDB、Redis、Qdrant、RabbitMQ、可选 Kafka、可选 ZooKeeper、雪花 ID、业务与外部服务 |
| Service B | `service-b.properties` | RSocket 与服务发现 |
| Gateway | `gateway.properties` | 网关地址、HTTP 和 WSS 路由、Redis 会话认证 |

在 Nacos 控制台选择和 `.env` 一致的 namespace 与 group，以 properties 格式发布 [三个模板](../nacos)。填入该环境实际地址与凭据。网关和 Service A 的 Redis 会话配置要一致。服务的 RSocket 绑定地址与 Nacos 注册地址要能被网关和其他可信服务访问。

模板默认单机。切换集群时替换对应组件的单机属性并填入所有节点。MongoDB URI 必须带数据库和 `replicaSet`，口令做 URL 编码。Redis Cluster 使用 DB 0。RabbitMQ 延迟插件在接入节点启用，示例集群模式创建 quorum 队列。切换已有队列类型时使用新资源前缀迁移。Qdrant 新集合副本数使用 `example.vector.replication-factor`。

| 可选配置 | 启用行为 |
|---|---|
| `lambda.mq.kafka.bootstrap-servers`、`example.kafka.topic`、`example.kafka.group` | Kafka 商品事件发布和消费。先创建 topic，再启动服务 |
| `lambda.lock.zookeeper.connect-string` | ZooKeeper 锁。回环部署可用 SSH 隧道连接 |
| `example.partner.base-url` | 外部服务状态查询，对接服务提供 `GET /status` |

省略可选连接属性时，相应客户端不会启动。维护接口默认可用 Redis 锁，ZooKeeper 只在配置后提供第二种锁后端。集群模式的 Kafka、ZooKeeper 和雪花 worker 也在模板中。连接、端口或拓扑变化发布后重启相关应用。
