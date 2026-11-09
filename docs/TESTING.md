# 验收记录与手工复核操作手册

以下 JUnit 与 HTTP 数字记录历史验收。当前示例通过独立网关访问，业务服务监听内网 RSocket TCP。

日期：2026-11-07。使用已有 JDK 27.0.1.0.3 与 IntelliJ IDEA 内置 Maven 3.9.16；未安装额外 Maven，未发布远程 Maven 构件。

## 2026-11-07 旧版业务验收结果

本节记录旧版路由的历史基线。当前接口采用 POST 动作路径，见 README。

- 框架 `clean install`：15 个 reactor 项目全部成功。
- 示例 `clean package`：2 个 reactor 项目全部成功。
- 仓库外 JUnit 验收：**11 项通过，0 失败，0 错误，0 跳过**。
- 正式应用 HTTP 端到端验收：**22 项通过**，使用真实 MongoDB、Redis、RabbitMQ、Nacos 和 Qdrant。
- 交付的 `scripts/run.sh` 和本地 `.env` 实际启动通过，`/api/system/info` 读取远程 greeting 与 `rs0`。

## 独立测试

| 分类 | 项数 | 核验内容 |
|---|---:|---|
| JSON | 2 | 中文 record、时间字段往返、未知字段兼容 |
| 基础设施 | 4 | Redis ACL 与读写清理；Mongo CRUD；Boot Mongo 配置；Rabbit 延迟发布与确认 |
| MongoDB 副本集 | 2 | rs0 主节点发现；Java Driver 与 Spring Reactive 事务提交、回滚及跨集合一致性 |
| Nacos | 3 | 配置发布、读取、变更监听、删除；实例注册、发现、注销；Boot 远程配置导入和实际 HTTP 端口注册 |

Nacos 删除配置后，客户端可能短暂返回缓存内容；清理断言使用带认证的服务端查询，确认 HTTP 404、业务码 20004，避免把本地快照误认为服务端残留。

## 正式业务接口的 22 项检查

1. 远程 Nacos 配置加载与 MongoDB rs0 连接。
2. Nacos 注册实际应用端口。
3. 运行中动态刷新 greeting。
4. 创建及查询商品，保留金额精度。
5. 非法商品入参返回 400。
6. 分类复合游标分页，无重复商品。
7. 带版本更新成功，旧版本返回 409。
8. 收藏唯一约束、幂等添加及删除。
9. 订单与扣库存同事务提交。
10. 请求号幂等，不重复扣库存；参数变化返回 409。
11. 库存不足返回 409，不改变库存。
12. 失败事务不留下订单，同一请求号可在条件满足后成功。
13. 两个并发事务保持库存一致。
14. 并发相同请求号只创建一单、只扣一次库存。
15. 并发购买有限库存时不超卖。
16. Redis 写入、读取、删除。
17. Redis TTL 到期。
18. 延迟消息发布确认、到期消费、Mongo 回执及 ACK；设置 2 秒，本次观察约 2.50 秒。
19. 重复投递保留原始幂等回执。
20. 无效消息进入死信队列并清理。
21. Qdrant 向量写入及最近邻查询。
22. 非法 UUID、向量维度、全零向量返回 400。

延迟观察值包含网络与轮询时间；两个并发请求的检查用于验证一致性，不是吞吐或压力测试。

## 清理与代码目录

临时资源使用随机 `lambda_it_` 名称；验收结束后删除对应 Mongo 数据库、Redis 键、Rabbit 队列/交换机、Qdrant 集合、Nacos 活动配置及服务实例/空服务定义，并从服务端独立核验。正式示例仅保留启动配置和空数据结构。审计日志与配置变更历史按产品自身策略保留。

临时测试工程在仓库外执行，验收后删除；仓库仅保留 `src/main` 业务代码、配置模板、运行脚本和文档。执行 `mvn package` 只构建应用，不会重跑已移除的验收程序。验证记录与日志另行交付。

## 运行边界

服务版本：MongoDB 9.0.4、Redis 8.10.3、RabbitMQ 4.2.9 + 延迟插件 4.2.0、Qdrant 1.20.0、Nacos 3.3.0。RabbitMQ 版本受原版延迟插件兼容性限制。

单节点 MongoDB 副本集与 Nacos standalone 没有高可用。本次未进行生产规模压力、故障切换、备份恢复演练，未部署 MySQL、Kafka、Elasticsearch、ZooKeeper，未实机验证跨服务 RPC。腾讯云 COS/SMS 仅完成对接文档，未调用真实云 API 或发送短信。

JDK 27 下，Lombok、Nacos 的部分传递依赖仍打印 Unsafe／final field 兼容性提示；上述测试全部通过。本次还观察到 SDK 多客户端关闭阶段的断连回调日志，不应把关闭日志当作业务功能失败，也不代表这些第三方提示已被修复。


## 2026-11-09 回归结果

Channel 直接检查 13 项通过，TCP 检查 8 项通过，通用分页 12 项通过。两个微服务、网关和示例共 26 项实机检查通过。验收包括 Mongo ObjectId 类型、枚举、单聊文字、群聊表情和媒体引用、HTTP NDJSON 双向交换、Redis、RabbitMQ 延迟回执、Qdrant、Nacos 配置读取，以及 PNG 和 MP4 分块字节传输。按名称清理了 Mongo 测试库、Redis 键、Qdrant 集合与 RabbitMQ 资源，并停止 JVM。

## 手工复核前的资源隔离

每次演练使用新的标识，例如 lambda_manual_20261109_a。下述名字只是示例，执行前换成本次唯一值。

在私有 .env 中同时修改：

~~~bash
MONGO_DATABASE='lambda_manual_20261109_a'
EXAMPLE_RESOURCE_PREFIX='lambda_manual_20261109_a'
APP_NAME='lambda-manual-20261109-a'
NACOS_GROUP='LAMBDA_MANUAL'
~~~

提前在同 namespace / group 创建 lambda-manual-20261109-a.properties，内容只放测试 greeting。按启动手册运行独立网关。网关从同一份环境变量读取 APP_NAME 并将 /api/** 转发到该服务。

记录：

- 两仓库提交号及 Java / Maven。
- Mongo 数据库、资源前缀、Nacos namespace/group/dataId。
- HTTP 地址、开始时间、创建的商品/订单/消息/向量 ID。
- 每项结果、异常、清理结果。

## 按顺序复核业务

使用 [API 手册](API.md) 的网关地址。业务成功看 serviceCode 与 data，参数、库存和版本类错误看 serviceCode 与 serviceMessage。

| 顺序 | 操作 | 通过条件 |
|---:|---|---|
| 1 | /api/system/info、Nacos 服务列表 | 应用名和 RSocket TCP 注册端口正确 |
| 2 | 发布不同 greeting 再查询 | 运行中读取新值 |
| 3 | 同分类创建至少 5 个商品 | 金额、库存、ID 正确 |
| 4 | page limit=2 连续游标翻页 | 5 条全部出现一次，末页游标空 |
| 5 | 空分类、limit=0/101、单边游标 | 返回参数校验错误 |
| 6 | 正确 version 更新，再用旧 version 更新 | 前者成功、后者返回版本冲突 |
| 7 | 同用户重复收藏，再取消两次 | 不重复关系，取消幂等 |
| 8 | 库存 3 的商品购买 2 | 库存剩 1、订单只一条 |
| 9 | 重复同 requestId 同参数 | 库存不再减少 |
| 10 | 同 requestId 改 quantity | 返回请求号冲突 |
| 11 | 库存不足购买 | 返回库存不足，无新订单，无负库存 |
| 12 | 两个并发请求争抢最后库存 | 不超卖，失败请求不留订单 |
| 13 | Redis set/get/delete 与短 TTL | 值一致、删除数量正确、到期后返回缓存不存在 |
| 14 | 延迟消息 2000ms | 发布确认后到期出现唯一回执 |
| 15 | 重复相同 messageId（专门工具投递） | 回执不被重复覆盖 |
| 16 | 无效消息投到专用队列 | 死信增加，消费者仍工作 |
| 17 | 合法向量写入与 query | 命中该 UUID |
| 18 | 非法 UUID、2 维、全零、非法 limit | 返回参数校验错误 |

并发库存验证是正确性测试，不是压测。消息重复投递需要复用同一消息 ID；重复调用现有 HTTP POST 会生成新 ID，不能用来证明消费幂等。

## 清理顺序

先停止测试应用，等待消费者和 Nacos 注册退出，再删除测试资源。这样不会边清理边被运行中的应用重新创建。

### A. MongoDB 专用库

使用 mongosh 登录后，先选择本次测试库，查看名称和集合。示意：

~~~javascript
use lambda_manual_20261109_a
db.getName()
db.getCollectionNames()
db.products.countDocuments({})
db.purchases.countDocuments({})
~~~

确认是专用测试库，再执行带名称保护的删除：

~~~javascript
const targetName = db.getName();
if (!targetName.startsWith("lambda_manual_")) {
  throw new Error("Refuse to delete a non-test database");
}
db.dropDatabase();
~~~

删除后重新列库确认。不要对 lambda_example、admin、config、local 或未知库执行本段操作。

### B. Redis

API 的 DELETE 可以删除已记录的测试缓存键；停止应用后也可以登录 Redis，限定本次前缀扫描：

~~~text
SCAN 0 MATCH lambda_manual_20261109_a:cache:* COUNT 100
~~~

继续扫描直到游标回到 0。对明确列出的键执行 UNLINK，例如：

~~~text
UNLINK lambda_manual_20261109_a:cache:manual_welcome
~~~

确认前缀键数归零。不执行 KEYS *、FLUSHDB 或 FLUSHALL 清理共享环境。TTL 到期可以辅助清理，但要核验，不只等待后宣称已清空。

### C. RabbitMQ

在管理控制台筛选完整测试前缀，先停止消费者，再删除两条测试队列及两个测试交换机：

~~~text
lambda_manual_20261109_a.notifications
lambda_manual_20261109_a.notifications.dead
lambda_manual_20261109_a.delayed
lambda_manual_20261109_a.delayed.dead
~~~

延迟消息可能尚在延迟交换机中，不能只 purge 业务队列就认为完成。删除仅限此次测试创建的拓扑，保留其他应用资源。

### D. Qdrant

在已注入 SERVICE_HOST / QDRANT_API_KEY 的终端，使用精确测试集合名：

~~~bash
export TEST_COLLECTION='lambda_manual_20261109_a_vectors'
case "$TEST_COLLECTION" in
  lambda_manual_*_vectors) ;;
  *) printf '%s\n' 'Refuse to delete a non-test collection' >&2; exit 1 ;;
esac
curl -fsS -X DELETE \
  -H "api-key: $QDRANT_API_KEY" \
  "http://$SERVICE_HOST:6333/collections/$TEST_COLLECTION"
~~~

再次 GET 指定集合应不存在，列表中不再出现。不要删除默认 lambda_example_vectors 或其他业务集合。

### E. Nacos

- 确认测试实例已注销。
- 删除精确 namespace/group/dataId 的测试配置。
- 若留下无实例的专用服务定义，核对名称后删除。
- 通过服务端确认，不以 SDK 本地缓存作为删除依据。
- 保留正式 lambda-example-app.properties。
- 配置历史与审计记录按产品策略保留，不要求清除审计痕迹。

## 清理验收表

| 对象 | 判定 |
|---|---|
| 测试应用进程 | 已停止 |
| Mongo 专用库 | 不存在 |
| Redis 测试前缀 | 0 个键 |
| Rabbit 测试队列/交换机 | 不存在 |
| Qdrant 测试集合 | 不存在 |
| Nacos 活动配置、实例、空服务定义 | 无本次测试资源 |
| 默认业务资源 | 未被误删 |
| 本地构建产物 | 按需 mvn clean |
| 临时验收源码 | 不提交仓库 |
| 日志证据 | 保留必要结果并去除凭据 |

## 报告模板

~~~text
验收时间与时区：
框架 / 示例提交号：
服务与客户端版本：
资源隔离名称：
实际执行用例：
通过 / 失败 / 跳过：
失败原因与复现步骤：
已知未覆盖内容：
资源清理结果：
日志位置：
~~~

只在重新执行后填写新的通过数量。文档修订、编译成功和服务 running 分别是不同层面的证据。
