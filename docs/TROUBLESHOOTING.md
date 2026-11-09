# 故障定位手册

[返回总览](../README.md) · [首次启动](QUICKSTART.md) · [运维](OPERATIONS.md)

## 1. 先判断故障阶段

| 阶段 | 典型证据 | 优先检查 |
|---|---|---|
| 编译前 | Maven / parent POM 错误 | JDK、同级目录、框架 install |
| 启动配置 | Nacos import 错误 | Data ID、namespace、账号、8848/9848 |
| 初始化资源 | ApplicationRunner 失败 | Mongo 索引、Rabbit 插件、Qdrant 集合 |
| HTTP 业务 | 400/404/409/5xx | 请求参数、版本、库存、日志 |
| 后台消费 | 队列堆积、无回执 | 消费者数、Mongo 可写、死信 |
| 停止阶段 | Nacos 断连回调 | 区分退出清理日志与运行中故障 |

保留第一个异常 cause，后续“Bean 创建失败”往往只是结果。

## 2. JDK 与 Maven

**release version 27 not supported**

执行 `./scripts/mvn.sh -version`，检查 Maven 实际 Java。不要只检查 PATH 中 java。IDEA Runner / Importer 都需匹配。

**找不到框架构件**

先在框架根目录 clean install，再构建示例；确认两个 Maven 使用同一 localRepository。只修改框架源码但没有 install，示例可能继续使用缓存中的旧 JAR。

**Lombok 提示或编译问题**

区分注解处理未开启、IDE 插件识别与 JDK 兼容提示。历史 JDK 27 构建出现部分 Unsafe / final field 提示但完成构建，不代表所有提示已修复。

## 3. Nacos

**控制台能登录，应用连不上**

控制台端口 8080，应用 server-addr 是 8848，客户端还需要 9848。检查安全组 / 防火墙和容器监听。

**配置不存在**

核对：

- APP_NAME 对应的 Data ID：APP_NAME.properties。
- group 默认 LAMBDA_EXAMPLE。
- namespace 默认 public，若自定义则使用实际 ID。
- 配置格式 Properties。
- 业务账号有对应资源权限。

**配置删除后客户端还能读到**

SDK 可能存在本地快照。清理确认应查询带认证的服务端；历史验收通过 HTTP 404 / 业务码 20004 确认删除，不能仅看 SDK 返回值。

**远程调用找错地址**

检查 APP_REGISTER_IP。127.0.0.1 对每台调用机器含义不同，0.0.0.0 也不是可被调用的注册地址。RSocket 端口登记与 HTTP 登记另有差别。

## 4. MongoDB

**Authentication failed**

区分 user/password 与 auth-database；当前部署账号在 admin，业务库是另一个库。分离属性无需 URL 编码，URI 中特殊字符才需要编码。

**等待 PRIMARY 超时**

检查 rs.status、rs.conf、副本集名称和成员通告地址。应用必须能连接成员真实地址，仅种子端口可达不够。

**Transaction numbers are only allowed...**

服务仍是 standalone 或连到了错误实例。事务需要副本集 / 分片部署；改客户端 replicaSet 参数不会替服务端完成初始化。

**索引创建失败**

检查历史重复数据和同名索引的定义。不要清空生产库解决唯一索引冲突；先建立迁移方案。

**409 更新冲突**

重新查询商品并使用最新 version。购买也会增加 version。当前更新接口对“商品缺失”和“版本冲突”统一返回 409。

## 5. Redis

**WRONGPASS 或用户禁用**

SERVICE_USER 对应 ACL 用户。服务器 default 关闭时，不能只传密码。

业务缓存通过不代表用户体系通过：框架 Security 专用 Redis 尚缺 username 接入，详见[用户体系手册](https://github.com/MrWangGang/lambda-framework/blob/main/docs/USER-AND-SECURITY.md)。

**找不到键**

HTTP key 会增加 EXAMPLE_RESOURCE_PREFIX:cache: 前缀。检查数据库编号和 TTL；CLI 看到 JSON 引号是序列化方式，不一定是重复转义。

## 6. RabbitMQ

**unknown exchange type x-delayed-message**

插件未启用或版本不兼容。查看 rabbitmq-plugins list -e；不能用管理后台 15672 连接 AMQP。

**PRECONDITION_FAILED inequivalent arg**

相同名称交换机 / 队列已存在，但 durable/type/arguments 不同。为测试使用新前缀；生产环境需要迁移拓扑，不能直接删除有消息的队列。

**有发布确认但没有回执**

发布确认不等于消费成功。检查队列 ready/unacknowledged、消费者是否停止、Mongo 是否可写、死信是否增加。当前单条处理失败直接 nack(false)，不自动重试该条消息。

**消费者最终停止**

整体消费流最多退避重试 10 次。查看停止日志并恢复依赖；不能仅以 HTTP 仍可用推断后台消费者正常。

## 7. Qdrant

**401/403**

确认 api-key 变量，Qdrant 无 root 用户登录模式。

**维度不匹配**

示例集合固定 3 维 Cosine，已有集合只检查是否存在，不自动检查/迁移维度。换独立集合或按计划重建，不能直接删除正在使用的业务索引。

**查询没有 category 过滤**

当前接口只接受 vector/limit；category 是写入 payload，未实现过滤。修改请求多加 category 不会自动生效。

## 8. HTTP 与业务

| 现象 | 说明 |
|---|---|
| 404 /api/products/page | 尚未实现页码分页；现有为 /products 游标 |
| 收藏列表只有 50 条 | 当前固定上限，无游标 |
| 同 requestId 返回旧订单 | 参数相同的幂等结果 |
| 同 requestId 返回 409 | 参数被改动 |
| 商品更新后向量没变 | 当前没有自动同步 |
| 缓存删了商品还存在 | 缓存演示与商品业务独立 |
| 查询回执立即 404 | 延迟未到或消费尚未完成 |
| 默认 8081 连不上 | 应用没启动、绑定本机、端口改动或防火墙 |

## 9. 诊断资料模板

~~~text
日期与时区：
框架提交：
示例提交：
Java / Maven：
APP_NAME / 数据库 / 资源前缀：
失败阶段：
请求方法与路径：
HTTP 状态 / 请求号：
第一条异常 cause：
基础服务状态：
已采取操作与结果：
~~~

记录非敏感配置。不要提交 .env、完整 token、验证码、数据库密码、SSH 私钥或含签名参数的文件 URL。

## 10. 恢复后怎么确认

恢复依赖后重新执行与故障有关的最小业务用例，再核验没有多扣库存、重复订单、未处理死信或孤立向量。不要只观察“日志不报错”。

需要更大改动时更新测试记录，明确与历史验收的区别。
