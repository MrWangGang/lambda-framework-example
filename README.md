# Lambda Framework Example

三个可运行应用展示统一网关、自动配置的数据组件，以及带身份的 Fory 双向 RPC。

~~~text
service-a/
  lambda-framework-example-service-a-server/
  lambda-framework-example-service-a-facade/
  lambda-framework-example-service-a-enum/
  lambda-framework-example-service-a-docker/
service-b/
  lambda-framework-example-service-b-server/
  lambda-framework-example-service-b-enum/
  lambda-framework-example-service-b-docker/
gateway/
  lambda-framework-example-gateway-server/
  lambda-framework-example-gateway-docker/
~~~

每个业务微服务在自己的目录中维护 `*-enum/`。该模块只放实现 `ExceptionEnumFunction` 的业务异常枚举，由本服务的 `*-server/` 引入。新业务错误使用 `B` 加七位数字。框架错误使用 `S` 加七位数字。

~~~java
return products.findById(id)
        .switchIfEmpty(Mono.error(() ->
                new BusinessException(ServiceAExceptionEnum.PRODUCT_NOT_FOUND)));
~~~

示例 A 按业务使用 `B01` 至 `B08`，示例 B 使用 `B09`。前端与 RPC 调用方按 `serviceCode` 处理，展示 `serviceMessage`。 [A 的异常码](service-a/lambda-framework-example-service-a-enum/src/main/java/com/lambda/example/servicea/enums/ServiceAExceptionEnum.java) · [B 的异常码](service-b/lambda-framework-example-service-b-enum/src/main/java/com/lambda/example/serviceb/enums/ServiceBExceptionEnum.java)

## 启动

先按 [框架构建命令](https://github.com/MrWangGang/lambda-framework#开始使用) 用 JDK 27 和 IDEA 自带 Maven 安装框架。将 `.env.example` 复制为 `.env`，填写本机 JDK 和 Nacos 引导信息，再按 [Nacos 配置](#nacos-配置) 发布配置。

在示例仓库根目录构建三个应用。

~~~bash
export JAVA_HOME="$(/usr/libexec/java_home -v 27)"
MAVEN_BIN="$HOME/Applications/IntelliJ IDEA.app/Contents/plugins/maven-plugin/lib/maven3/bin/mvn"
"$MAVEN_BIN" -B clean install
~~~

在三个终端各自进入示例仓库根目录，先把本地配置导入环境。

~~~bash
set -a
. ./.env
set +a
~~~

在三个终端分别执行以下各一条启动命令。

~~~bash
"$JAVA_HOME/bin/java" --enable-native-access=ALL-UNNAMED -jar service-a/lambda-framework-example-service-a-server/target/lambda-framework-example-service-a-server-1.0.0-SNAPSHOT.jar
"$JAVA_HOME/bin/java" --enable-native-access=ALL-UNNAMED -jar service-b/lambda-framework-example-service-b-server/target/lambda-framework-example-service-b-server-1.0.0-SNAPSHOT.jar
"$JAVA_HOME/bin/java" --enable-native-access=ALL-UNNAMED -jar gateway/lambda-framework-example-gateway-server/target/lambda-framework-example-gateway-server-1.0.0-SNAPSHOT.jar
~~~

默认网关为 `http://127.0.0.1:8081`，A 和 B 的内网 RSocket 端口分别为 7000 和 7001。Nacos 服务名是 `service-a`、`service-b`，Data ID 是 `service-a.properties`、`service-b.properties` 和 `gateway.properties`。完成后在各终端按 Ctrl+C。

## Docker 部署

在示例根目录执行 Maven `clean install` 后，A、B 和网关各自的 `*-docker/` 中会有 `app.jar`。此文件来自本次编译生成的 Spring Boot 可执行 JAR。将需要部署的整个 Docker 目录上传到 Linux 服务器即可。

部署机准备私有的环境文件，至少填入可访问的 `JAVA_RUNTIME_IMAGE` 和 `NACOS_SERVER_ADDR`。运行时镜像使用 Java 27。按环境填写 `NACOS_NAMESPACE`、`NACOS_GROUP`、`NACOS_USERNAME` 和 `NACOS_PASSWORD`。凭据由部署机提供。

~~~bash
cd /部署目录/lambda-framework-example-service-a-docker
docker compose --env-file /部署配置/runtime.env up -d --build
docker compose --env-file /部署配置/runtime.env ps
docker compose --env-file /部署配置/runtime.env logs --tail=100
~~~

三个 Docker 目录分别执行同样的命令。示例 Compose 使用 Linux host 网络，应用直接监听 Nacos 配置中的原生端口。同机数据组件可继续使用回环地址。分主机部署时，服务监听和注册到 Nacos 的地址必须对网关及调用方可达。发布新版本时重新执行 Maven `install`，上传新的目录，再执行 `up -d --build`。

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

HTTP 路由 `rb://service-a` 覆盖 `/api/**`。前端发送 ObjectId 的 24 位十六进制字符串，Java 业务拿到 `ObjectId`。响应读取 `serviceCode`、`serviceMessage` 和 `data`。 [接口参数与调用](#接口参数与调用)

## 数据层示例

商品、订单、收藏和消息回执都继承 `UnifyPO<ObjectId>`，统一 ID、创建时间、更新时间和版本。已有 MongoDB 集合升级前按 [迁移手册](https://github.com/MrWangGang/lambda-framework#旧集合迁移) 回填缺失字段。

- [ProductRepository](service-a/lambda-framework-example-service-a-server/src/main/java/com/lambda/example/servicea/repository/ProductRepository.java) 用 MongoDB 查询与分页。
- [VectorRepository](service-a/lambda-framework-example-service-a-server/src/main/java/com/lambda/example/servicea/repository/VectorRepository.java) 绑定 Qdrant 集合，响应式写入与相似度查询。
- [CacheServiceImpl](service-a/lambda-framework-example-service-a-server/src/main/java/com/lambda/example/servicea/service/CacheServiceImpl.java) 用 Redis 管理缓存。
- [DelayedMessageServiceImpl](service-a/lambda-framework-example-service-a-server/src/main/java/com/lambda/example/servicea/service/DelayedMessageServiceImpl.java) 用 RabbitMQ 延迟投递、手动确认和 MongoDB 回执。
- [SessionController](service-a/lambda-framework-example-service-a-server/src/main/java/com/lambda/example/servicea/controller/SessionController.java) 展示登录、读取身份和注销。

| 业务入口 | 对应组件 |
|---|---|
| `POST /api/product/export` | `UiFlux` 单向 NDJSON，发送商品游标请求 |
| `POST /api/catalog/maintenance/rebuild` | Redis 或 ZooKeeper 锁、Redis 计数缓存、雪花任务 ID |
| `POST /api/catalog/event/publish`、`/api/catalog/event/find` | Kafka 商品事件、雪花事件 ID 和 MongoDB 回执 |
| `POST /api/partner/status` | 框架 HTTP 客户端访问 Nacos 配置的外部服务 |

可选组件的使用方式和请求示例见 [接口参数与调用](#接口参数与调用)。

## WSS 与双向 Fory RPC

小程序连接 `wss://网关域名/ws/api/chat/exchange`，在握手时传 Bearer 令牌。本机 WebSocket 客户端使用 `ws://127.0.0.1:8081/ws/api/chat/exchange`。网关的 `ws://service-b` 路由剥掉 `/ws` 前缀，B 由 `ChatRelayServiceImpl` 使用 `@RSocketRpc("service-a")` 把业务流直接传给 A。

~~~java
@Resource
private ChatRelayServiceImpl chat;

@MessageMapping("/api/chat/exchange")
public BiFlux<ChatFacade.Receipt> exchange(Flux<ChatFacade.Message> messages) {
    return chat.exchange(messages);
}
~~~

A 在 `service-a/lambda-framework-example-service-a-facade` 声明 `ChatFacade.exchange`，在 `service-a/lambda-framework-example-service-a-server` 用 `@RSocketFacade` 实现。框架生成 `service-a.chatFacade.exchange`，重复路由会导致启动失败。前端每条文本消息发送一个 JSON 值，例如 `{"messageId":"m-1","text":"你好"}`。每条回执独立返回，`data` 为数组。回执中的 `principal` 是网关验证后透传的用户 ID。Java RPC 的业务数据与身份元数据都用 Fory。

<details>
<summary>展开开发规范与智能体技能入口</summary>

[开发规范技能](https://github.com/MrWangGang/lambda-framework/blob/main/skills/lambda-java-backend/SKILL.md)

</details>

## Nacos 配置

复制 `.env.example` 为 `.env`。本地填写 JDK 和 Nacos 的地址、namespace、group、用户名与凭据。三个应用启动时从 Nacos 读取必需的配置。

| 应用 | Nacos Data ID | 内容 |
|---|---|---|
| Service A | `service-a.properties` | RSocket、MongoDB、Redis、Qdrant、RabbitMQ、可选 Kafka、可选 ZooKeeper、雪花 ID、业务与外部服务 |
| Service B | `service-b.properties` | RSocket 与服务发现 |
| Gateway | `gateway.properties` | 网关地址、HTTP 和 WSS 路由、Redis 会话认证 |

在 Nacos 控制台选择和 `.env` 一致的 namespace 与 group，以 properties 格式发布 [三个模板](nacos)。填入该环境实际地址与凭据。网关和 Service A 的 Redis 会话配置要一致。服务的 RSocket 绑定地址与 Nacos 注册地址要能被网关和其他可信服务访问。

模板默认单机。切换集群时替换对应组件的单机属性并填入所有节点。MongoDB URI 必须带数据库和 `replicaSet`，口令做 URL 编码。Redis Cluster 使用 DB 0。RabbitMQ 延迟插件在接入节点启用，示例集群模式创建 quorum 队列。切换已有队列类型时使用新资源前缀迁移。Qdrant 新集合副本数使用 `example.vector.replication-factor`。

| 可选配置 | 启用行为 |
|---|---|
| `lambda.mq.kafka.bootstrap-servers`、`example.kafka.topic`、`example.kafka.group` | Kafka 商品事件发布和消费。先创建 topic，再启动服务 |
| `lambda.lock.zookeeper.connect-string` | ZooKeeper 锁。回环部署可用 SSH 隧道连接 |
| `example.partner.base-url` | 外部服务状态查询，对接服务提供 `GET /status` |

省略可选连接属性时，相应客户端不会启动。维护接口默认可用 Redis 锁，ZooKeeper 只在配置后提供第二种锁后端。集群模式的 Kafka、ZooKeeper 和雪花 worker 也在模板中。连接、端口或拓扑变化发布后重启相关应用。

## 接口参数与调用

[完整入口与请求示例](#普通请求)。所有业务请求发到网关，使用 POST 与 JSON 请求体，ID 放请求 record 字段。

| 输入 | 约定 |
|---|---|
| MongoDB id、userId、productId | 24 位十六进制 ObjectId 字符串 |
| price、stock、quantity、version | 字符串。price 支持最多两位小数，数量与版本在业务边界转换 |
| status | 在售或下架 |
| page、size、limit | 正整数字符串。页码从 1 开始，默认 1/10，页大小不超过 100 |
| beforeTime、beforeId | 同时传入，使用前一页返回的 nextBeforeTime、nextBeforeId |
| requestId | 每次购买操作的稳定幂等键 |
| ttlSeconds | 1 至 86400 的整数字符串 |
| delayMillis | 0 至 60000 的整数字符串 |
| vector | 三个有限的 JSON 数值或数字字符串，向量至少一维非零 |
| 向量 id | UUID |

先登录获取 `data.token`。在终端中设置 `TOKEN` 后执行以下调用。会话信息、快照与注销分别使用 `/session/whoami`、`/session/profile` 和 `/session/logout`，请求体为 `{}`。

~~~bash
base=http://127.0.0.1:8081/api
curl -fsS -X POST "$base/product/find" -H "Authorization: Bearer ${TOKEN}" -H 'Content-Type: application/json' \
  -d '{"id":"507f1f77bcf86cd799439011"}'
curl -fsS -X POST "$base/product/cursor" -H "Authorization: Bearer ${TOKEN}" -H 'Content-Type: application/json' \
  -d '{"category":"book","limit":"20"}'
curl -fsS -X POST "$base/cache/put" -H "Authorization: Bearer ${TOKEN}" -H 'Content-Type: application/json' \
  -d '{"key":"smoke","value":"ready","ttlSeconds":"60"}'
curl -fsS -X POST "$base/message/sendDelayed" -H "Authorization: Bearer ${TOKEN}" -H 'Content-Type: application/json' \
  -d '{"content":"hello","delayMillis":"2000"}'
~~~

查询示例中的 id 使用本次实际创建的商品 ID。网关的业务响应使用 JSON 外壳。成功时 `serviceCode` 为 `00000000`，框架错误为 `S` 加七位数字，业务错误为 `B` 加七位数字，缓存查询的 `data` 是 JSON 字符串。输入校验、资源不存在和库存冲突等后端错误通过 `serviceCode` 和 `serviceMessage` 返回，业务客户端按这两个字段判断。收藏列表由普通 Flux 聚合成 `data` 数组，空列表返回 `data: []`。部分删除接口返回空数据。向量写入的 `data` 为布尔值，查询的 `data` 为包含 `id`、`score`、`category` 的数组。


### 单向流与可选组件

所有请求带上登录令牌。

| POST 路径 | JSON 请求体 | 返回 |
|---|---|---|
| `/api/purchase/summary` | `{"userId":"507f1f77bcf86cd799439011"}` | 订单按商品分类的聚合数组，ObjectId 直接绑定 Mongo 聚合参数 |
| `/api/product/export` | `{"category":"book","limit":"20"}` | NDJSON，每行一个业务响应外壳 |
| `/api/catalog/maintenance/rebuild` | `{"category":"book","lockProvider":"REDIS"}` | 雪花任务 ID 与缓存中的商品计数。启用 ZooKeeper 时也可指定 `ZOOKEEPER` |
| `/api/catalog/event/publish` | `{"productId":"507f1f77bcf86cd799439011"}` | Kafka 发布确认后返回事件 ID |
| `/api/catalog/event/find` | `{"eventId":"事件ID"}` | 消费成功后读取 MongoDB 回执 |
| `/api/partner/status` | `{}` | 外部服务状态 |

## MongoDB 建模

本章先说明当前示例，再给出应用扩展设计。用户、地址、商家、文章和多商品订单的结构是设计示例，尚未全部实现为本仓库代码。

### 1. 先按访问方式决定结构

MongoDB 的集合和文档不需要照搬关系数据库的每一张表。先回答：

1. 哪些数据每次一起读取？
2. 子项数量是否有明确上限？
3. 子项是否独立查询、修改或授权？
4. 父信息变化时，能接受多长时间的冗余不同步？
5. 哪些更新必须原子完成？
6. 预计单实体大小、热点程度和查询量如何增长？

| 关系 | 建议起点 | 原因 |
|---|---|---|
| 用户 → 少量收货地址 | 有上限的内嵌数组 | 一次读取与单文档更新 |
| 作者 → 持续增长的文章 | 独立 articles，保存 authorId | 独立分页、更新与索引 |
| 商品 → 商家 | products 保存 merchantId | 避免每次商家变更更新所有商品 |
| 用户 ↔ 收藏商品 | favorites 关系集合 | 双向查询与唯一约束 |
| 订单 → 有界明细 | 内嵌成交快照 + productId | 保留历史，不随现价改变 |

多对多必须使用中间表或独立关系集合。一对多、多对一直接用内嵌或在多端持有 ObjectId 引用。把关键接口的读写比例、子项上限、独立分页、热点和更新放大写入建模说明。内嵌适合经常一起读取且有界的数据，独立更新频繁或持续增长的数据分集合。查询时再按需求用批量查询或 `$lookup`。[MongoDB 建模原则](https://www.mongodb.com/docs/manual/data-modeling/best-practices/)

### 2. 当前示例模型

| 集合 | 实体 | ID 与关键查询 |
|---|---|---|
| products | ProductEntity | ObjectId、分类与时间复合索引、乐观锁、String 金额 |
| favorites | FavoriteEntity | ObjectId 主键与两端 ObjectId，唯一约束 userId + productId |
| purchases | PurchaseEntity | ObjectId 主键，requestId 独立唯一索引 |
| delivery_receipts | DeliveryReceiptEntity | ObjectId，消息重投按主键幂等更新 |

历史字符串主键、Decimal128 字段和旧采购幂等键需要迁移。使用新库验证时保留业务库原状。

### 3. 一对多：用户与少量地址

设计示意：

~~~javascript
{
  "_id":ObjectId("65a000000000000000000001"),
  "nickname":"小王",
  "addresses":[
    {"id":"a1","receiver":"示例收件人","city":"广州","detail":"示例地址","default":true}
  ],
  "version":1
}
~~~

适用前提：地址数量设业务上限，例如 20；这是本设计建议，不是框架自动限制。

查询：

- 用户资料页按 _id 一次取回。
- 不需要在每个地址上创建不使用的索引。
- 展示列表时投影字段，按权限隐藏完整地址。

更新示意，在已选中的业务数据库内执行：

~~~javascript
db.users.updateOne(
  { _id: ObjectId("65a000000000000000000001"), version: 1, "addresses.id": "a1" },
  { $set: { "addresses.$.city": "深圳" }, $inc: { version: 1 } }
)
~~~

要检查 matchedCount；0 可能是版本过期、用户或地址不存在。增加地址还应在服务端约束数量和地址 ID 唯一，不能仅靠前端限制。

如果地址变成独立配送实体，有大量审计记录或高频独立查询，可拆为 addresses(userId, ...)，而不是继续扩张用户文档。

### 4. 一对无限增长的多：作者与文章

~~~javascript
{_id:ObjectId("65a000000000000000000002"), authorId:ObjectId("65a000000000000000000001"), status:"published", title:"示例", createdAt:ISODate("2026-11-09T08:00:00Z")}
~~~

不在 user 文档中保存所有 articleIds。文章索引设计：

~~~javascript
db.articles.createIndex({authorId:1, status:1, createdAt:-1, _id:-1})
~~~

查某作者已发布文章时使用 authorId + status 过滤，按 createdAt/_id 游标翻页。单篇文章更新只写 articles，不同时修改整个作者文档。

作者名称需要出现在文章列表时可选：

- 查询一页后收集 authorId 批量查询并组装，避免每篇查询一次。
- 少量冗余 authorName，定义异步更新和补偿策略。
- 聚合关联需限制范围并检查 explain，避免先做巨量 join 再分页。

软删除和可见性状态必须进入查询条件。索引随真实查询调整，不是给所有字段建立索引。

### 5. 多对一：商品与商家

~~~javascript
{_id:ObjectId("65a000000000000000000003"), merchantId:ObjectId("65a000000000000000000004"), name:"示例商品", price:"99.00", status:"在售"}
~~~

商家独立存在 merchants 集合，商品保存 merchantId。商家改联系电话时只更新商家文档。

若商品列表必须显示店铺名，可以保存 merchantNameSnapshot；必须明确这个字段允许暂时陈旧，商家改名由消息任务更新或查询时覆盖。订单中商家名称属于历史快照时，不应跟随当前商家名更新。

常见索引建议：

- 商家后台：merchantId + status + createdAt + _id。
- 商家唯一业务编号：单独唯一索引，约束范围需包含 tenantId（若有租户）。

当前示例 Product 没有 merchantId，上面是扩展方案。

### 6. 多对多：用户与收藏商品

当前实现：

~~~javascript
{_id:ObjectId("65a000000000000000000005"), userId:ObjectId("65a000000000000000000001"), productId:ObjectId("65a000000000000000000003"), createdAt:ISODate("2026-11-09T08:00:00Z")}
~~~

真实索引：

- unique(userId, productId)：同一用户不能重复收藏同一商品。
- (userId, createdAt DESC, _id DESC)：用户的收藏时间线。
- (productId, createdAt DESC)：按商品查看关系的索引基础。

添加使用 upsert + setOnInsert，配合唯一索引处理并发重复；取消按 userId/productId 删除。

不推荐把全部 productIds 放进 users，也不把全部 userIds 放进 products。两边都会随增长变成大数组和热点文档，分页与单条更新更难控制。

当前 `POST /api/favorite/list` 最多取 50 条，并未实现游标。扩展时添加完整游标和 _id tie-breaker，反向商品时间线也应补齐稳定排序对应的索引。

商品被删除后如何处理收藏，应按业务选择隐藏、异步清理或保留失效提示；当前没有自动级联删除。

### 7. 订单与订单明细

当前示例一条 Purchase 只购买一个商品，unitPrice 是成交时价格，商品改价不修改历史记录。

多商品订单可以采用有上限的内嵌明细：

~~~javascript
{
  "_id":ObjectId("65a000000000000000000006"),
  "userId":ObjectId("65a000000000000000000001"),
  "status":"created",
  "items":[
    {"productId":ObjectId("65a000000000000000000003"),"nameSnapshot":"成交商品名","quantity":2,"unitPrice":"99.00"}
  ],
  "totalAmount":"198.00",
  "currency":"CNY",
  "schemaVersion":1
}
~~~

金额在 API、Java 实体和数据库中统一为字符串。需要按数值排序或计算时定义明确的换算和索引策略，避免按字符串排序代替数值排序。

设计约束：

- 明细数量有上限。
- 商品 ID 保留引用，名称、单价保留快照。
- 收货地址也保存成交快照，不实时跟随用户地址修改。
- 支付、退款、履约记录持续增长时分集合。
- requestId 的唯一范围应包含业务或租户，不能无意跨租户撞号。

### 8. 查询性能

按接口列出过滤、排序、投影和返回上限，再设计索引。示例商品列表索引为：

~~~javascript
db.products.createIndex(
  {category:1, createdAt:-1, _id:-1},
  {name:"category_recent"}
)
~~~

验证执行计划：

~~~javascript
db.products.find({category:"book"})
  .sort({createdAt:-1, _id:-1})
  .limit(20)
  .explain("executionStats")
~~~

观察是否使用预期索引、是否发生大量扫描/内存排序、totalDocsExamined 与返回数量的关系。不要仅凭有 IXSCAN 就认定查询已经足够快。 聚合 Repository 直接绑定 ObjectId，关联两端都保存 BSON ObjectId。[复杂聚合与参数绑定示例](https://github.com/MrWangGang/lambda-framework/blob/main/skills/lambda-java-backend/SKILL.md#aggregation-中的-objectid)

避免：

- 先取全部结果再 Java 分页。
- 无边界的正则扫描和多字段任意排序。
- 逐条关联查询形成 N+1。
- 高频页面每次都做全量精确 count。
- 为了“可能会用”建立大量索引，放大写入成本。

### 9. 分页与时间

深翻页优先用索引范围游标。按时间分页必须加入唯一字段作为稳定次序，示例用 createdAt + _id。

不要假设 MongoDB 时间和 Java Instant 保留完全相同的精度；游标应来自实际持久化后返回的值。变更日期类型时要覆盖同毫秒多记录和边界翻页。

后台页码分页可使用 skip/limit，但要限制最大翻页深度并评估 count。框架分页入口见 [数据访问与分页](https://github.com/MrWangGang/lambda-framework#数据访问与分页)。[MongoDB 分页说明](https://www.mongodb.com/docs/manual/reference/method/cursor.skip/)

### 10. 更新性能与并发

库存：
~~~javascript
db.products.updateOne(
  { _id: ObjectId("65a000000000000000000003"), stock: { $gte: 2 } },
  { $inc: { stock: -2, version: 1 } }
)
~~~

这段展示单文档库存条件更新。本示例通过 Java `findAndModify` 扣减并在事务内创建订单。业务 SKU 还需匹配可售状态，管理端库存调整独立于普通资料编辑。完整规则见 [乐观锁、SKU 库存和金额原子更新](https://github.com/MrWangGang/lambda-framework/blob/main/skills/lambda-java-backend/SKILL.md#乐观锁sku-库存和金额原子更新)。

原则：

- 原子更新用 $inc / $set 等操作符，避免覆盖整文档。
- 所有集合实体继承 `UnifyPO<ObjectId>`，包括关系记录和回执。统一存储 `_id`、`createdAt`、`updatedAt`、`version`。有覆盖语义的编辑校验旧版本，库存更新在一次原子写入中推进版本。
- 余额保持规范字符串，按旧金额和旧版本匹配并原子替换。金额与流水联动时使用事务与唯一操作键。
- 热门商品库存可能成为写热点，需要业务排队、分桶或预留策略；缓存锁不自动解决所有一致性问题。
- 单文档原子性足够时不引入跨集合事务。
- 跨文档事务只包必要数据库操作，短事务比包含外部等待更可控。

MongoDB 会在单文档更新时重新判断条件；预期版本或库存条件用于避免冲突覆盖。[原子更新说明](https://www.mongodb.com/docs/manual/core/write-operations-atomicity/)

### 11. 增长与扩容路线

| 阶段 | 先做什么 | 依据 |
|---|---|---|
| 小规模 | 正确索引、请求上限、备份 | 慢查询、数据大小 |
| 增长中 | 游标、投影、冷热分离、缓存 | P95、扫描量、工作集、IO |
| 热点明显 | 原子条件更新、排队、限流、拆分热点 | 锁等待、冲突率、单键压力 |
| 单机资源逼近上限 | 副本集高可用、容量规划、读策略 | 恢复目标、延迟与资源利用率 |
| 需要水平分片 | 评估 shard key 与迁移计划 | 查询局部性、写热点、基数 |

不按固定“多少万条”决定分片。集合数量、文档大小、索引工作集和访问分布比单一行数更重要。分片键改变成本较高，先验证是否导致跨分片查询或热点。

### 12. 缓存、向量与消息的一致性

MongoDB 保存业务事实。Redis 可以保存可重建缓存；Qdrant 保存可重建召回索引；RabbitMQ 负责事件传递。

当前示例这几条链独立，没有自动同步。扩展建议：

1. 同一 Mongo 事务写业务数据与 outbox 事件。
2. 独立发布任务发送，记录重试和确认。
3. 消费端按 eventId + 版本幂等处理。
4. 对乱序事件拒绝旧版本覆盖新版本。
5. 定期对账并重建丢失的缓存 / 向量。

这套 Outbox 流程是设计建议，尚未实现。不要把消息发送放进事务方法就认为已经具有跨系统原子性。

## 部署端口与排查

云防火墙、安全组和主机防火墙按应用实际监听端口和访问来源配置。下表是示例默认端口。业务 RSocket 和数据组件使用内网访问规则。

| TCP 端口 | 用途 |
|---|---|
| 22 | SSH 管理 |
| 27017 | MongoDB 副本集 |
| 6379 | Redis |
| 5672、15672 | RabbitMQ AMQP 和管理界面 |
| 6333、6334 | Qdrant REST 和 Java gRPC |
| 8080、8848、9848 | Nacos 控制台、配置 API 和客户端 gRPC |
| 2181、9092 | 启用时用于 ZooKeeper 和 Kafka |
| 7000、7001 | Service A、Service B 的 RSocket TCP |
| 8081 | 示例 HTTP 和 WebSocket 网关 |

MongoDB 单机需要实际副本集名称，Mongo URI 要与副本集公布的可达地址匹配。Redis ACL 用户配置要与客户端一致。RabbitMQ 延迟功能依赖延迟消息插件。Nacos Data ID、namespace 和 group 必须与本机引导配置一致。通过 `docker compose ps` 和 `docker compose logs --tail=100 <服务名>` 查看部署状态。Docker Compose 和真实凭据保存在各自的部署环境。

GitHub SSH 的账号检查、推送地址和构建顺序见 [框架 README](https://github.com/MrWangGang/lambda-framework#github-ssh)。示例的推送地址为 `git@github.com:MrWangGang/lambda-framework-example.git`。

## 腾讯云 COS 与短信

本节描述后续 COS 文件和短信对接方式。

### 1. 凭据与配置准备

腾讯云 SecretId/SecretKey 与中间件用户名密码不同。前端不能获得永久云密钥。

建议的未来环境变量：

| 变量 | 用途 |
|---|---|
| TENCENT_SECRET_ID / TENCENT_SECRET_KEY | 服务端 CAM 身份 |
| COS_REGION | 存储桶实际地域 |
| COS_BUCKET | 完整 BucketName-APPID |
| SMS_REGION | 短信 API 支持的地域 |
| SMS_SDK_APP_ID | 短信应用 ID |
| SMS_SIGN_NAME | 已审核签名内容 |
| SMS_TEMPLATE_ID | 已审核模板 ID |

**这些变量目前尚未被示例读取**。添加到 .env 不会自动产生云功能，需要应用配置类和客户端适配层。

Maven 坐标：

- COS：com.qcloud:cos_api
- SMS：com.tencentcloudapi:tencentcloud-sdk-java-sms
- STS：com.tencentcloudapi:tencentcloud-sdk-java-sts

### 2. COS 准备步骤

1. 在 COS 控制台选择或创建桶，记录完整桶名和地域。
2. 明确私有文件与公开素材的访问策略。
3. 给后端 CAM 身份授予需要的对象路径与操作权限。
4. 浏览器直传时配置对应 Origin、Method 和 Header 的 CORS。
5. 定义对象 Key 规则、大小/类型限制与业务归属。

Java SDK 的主要客户端为 COSClient，提供对象写入、读取、删除和预签名等能力；客户端配置和凭据按官方 SDK 创建。[COS Java 入门](https://cloud.tencent.com/document/product/436/10199)

### 3. 推荐的文件业务流程

以下接口名称是设计方案，不是当前可调用端点。

~~~text
已认证用户申请上传
  → 后端检查用户、用途、文件类型/大小
  → 生成业务文件记录和唯一 objectKey
  → 返回限定用途的上传授权
  → 客户端上传 COS
  → 后端核验对象存在及元数据
  → 文件记录由 pending 改为 ready
  → 业务对象关联 fileId
~~~

Key 设计建议：
`tenant/<tenantId>/users/<userId>/<yyyyMM>/<随机ID>.<后缀>`。

不要使用客户端任意提交的完整路径，也不要用原文件名作为全局唯一 Key。数据库建议记录：

~~~json
{
  "_id":"fileId",
  "ownerId":"userId",
  "bucket":"BucketName-APPID",
  "region":"实际地域",
  "objectKey":"服务端生成的Key",
  "originalName":"原始文件名",
  "contentType":"image/png",
  "size":12345,
  "status":"pending",
  "createdAt":"时间"
}
~~~

下载前校验业务权限，生成短期授权。数据库保存 objectKey 而非即将过期的签名 URL。删除采用状态记录和补偿任务，避免数据库删了但对象残留、或对象删了但业务仍引用。

### 4. STS 与预签名的选择

| 方式 | 适合情况 | 需要控制 |
|---|---|---|
| 服务端代理上传 | 文件较小、后端需处理内容 | 请求体大小、流量、超时 |
| 单对象预签名 URL | 指定 Key 的短时直接上传/下载 | Key、HTTP 方法、有效期 |
| STS 临时凭据 | 客户端需要 SDK 多步骤操作 | resource/action 范围、期限 |

STS 返回临时 ID、临时 Key 和 Token，权限必须限定到业务需要的路径和操作。[STS 指引](https://cloud.tencent.com/document/product/436/14048)

预签名 URL 的持有者在有效期内可以执行已授权操作，不应写入公开日志。大文件和分块上传按 SDK 能力设计，不能把简单上传示例当成所有文件场景。[预签名上传](https://cloud.tencent.com/document/product/436/14114)

### 5. COS 的失败与补偿

| 情况 | 处理建议 |
|---|---|
| 申请授权后没有上传 | pending 超时清理 |
| 上传成功但确认请求丢失 | 通过 fileId 查询并幂等确认 |
| 同 Key 重复上传 | 明确是否覆盖；默认采用唯一 Key |
| 下载签名过期 | 重新鉴权并签发 |
| 删除对象失败 | 记录待清理任务并有限重试 |
| 内容类型伪装 | 服务端校验内容，不只信扩展名 |
| 业务记录存在、对象不存在 | 告警和补偿，不返回虚假成功 |

文件生命周期与 MongoDB 事务不能直接形成跨系统原子提交，需要状态机和幂等补偿。

### 6. 短信准备步骤

1. 开通短信产品，创建应用，记录 SmsSdkAppId。
2. 申请签名和正文模板并等待审核。
3. 明确模板变量数量、顺序和用途。
4. 准备具有短信调用权限的 CAM 身份。
5. 国内短信确认套餐/余额以及目标号码范围。

官方 Java SDK 包含发送、回执和统计接口，建议交给 SDK 完成云 API 签名。[短信 Java SDK](https://cloud.tencent.com/document/product/382/43194)

### 7. SendSms 参数

接口域名 `sms.tencentcloudapi.com`，Action 为 `SendSms`，API Version 为 `2021-01-11`，Region 使用产品支持值。

| 参数 | 业务含义 |
|---|---|
| PhoneNumberSet | 接收号码数组，建议 E.164 |
| SmsSdkAppId | 短信应用 ID |
| TemplateId | 已审核模板 ID |
| SignName | 签名内容；国内短信必填，不是签名 ID |
| TemplateParamSet | 按模板顺序填变量 |
| SessionContext | 可选业务关联信息 |

单次最多 200 个号码，境内/境外不能混发。逐项检查 SendStatusSet，记录 RequestId 和可用的发送标识；接口受理与最终送达不同。[SendSms API](https://cloud.tencent.com/document/product/382/55981)

业务请求结构示意，无法直接作为完整的签名 HTTP 请求执行：

~~~json
{
  "PhoneNumberSet":["替换为已授权的E.164号码"],
  "SmsSdkAppId":"实际应用ID",
  "SignName":"已审核签名内容",
  "TemplateId":"已审核模板ID",
  "TemplateParamSet":["动态验证码","有效分钟数"]
}
~~~

### 8. 验证码业务设计

这是应用层设计，不是腾讯云接口自动实现的能力：

1. 按手机号、IP、用户/设备及业务场景做发送频控。
2. 使用安全随机数生成验证码。
3. Redis 保存验证码摘要、场景、有效期和尝试次数；不在日志打印验证码。
4. 发送结果为失败时按错误类型更新状态，避免直接触发无上限重试。
5. 验证时原子检查、计数和消费，成功后一次性失效。
6. 登录验证码和改密码验证码使用不同场景 key。
7. 超时但结果不明时，避免立即重新发送产生重复收费和骚扰。
8. 查询送达回执，用业务请求号关联；回执不能当作用户已完成身份验证。

具体有效期和重发间隔由产品确定并配置，不写死在控制器里。

### 9. Reactor / 虚拟线程接法

COS / SMS 同步 SDK 调用需要在框架适配层隔离，保留 Mono / Flux 接口：

~~~java
return Mono.fromCallable(() -> smsClient.SendSms(request))
    .subscribeOn(Schedulers.boundedElastic());
~~~

示意依赖已有 smsClient 和 request；当前代码未添加该客户端。开启经过验证的虚拟线程配置后，boundedElastic 可以使用虚拟线程。SDK 自身连接超时、请求超时和连接池仍需要设置。

超时取消不保证云端没有执行，因此不能给所有发送操作无条件 retry。客户端按 SDK 约定复用和关闭，不为每次请求创建一整套连接池。

### 10. 网络与端口

调用 COS / SMS 通常需要应用访问外部 HTTPS 443 和正常 DNS，不需要为发短信额外开放数据库入站端口。若设计云回调入口，再单独部署 HTTPS、认证和幂等处理。
