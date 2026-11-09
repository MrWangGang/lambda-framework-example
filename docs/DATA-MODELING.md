# MongoDB 关系设计与业务增长规范

[返回总览](../README.md) · [接口](API.md) · [配置与索引](CONFIGURATION.md)

本章先说明当前示例，再给出应用扩展设计。用户、地址、商家、文章和多商品订单的结构是设计示例，尚未全部实现为本仓库代码。

## 1. 先按访问方式决定结构

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

内嵌和引用取决于访问及增长方式，模型可以随业务迭代。[MongoDB 建模原则](https://www.mongodb.com/docs/manual/data-modeling/best-practices/)

## 2. 当前示例的真实模型

| 集合 | 事实与关系 |
|---|---|
| products | 商品现价、库存、版本 |
| favorites | userId 与 productId 的多对多关系 |
| purchases | 每条购买仅一种商品，保存数量与成交单价 |
| delivery_receipts | 消息消费结果，以消息 ID 幂等 |

当前没有 users 集合和身份验证；userId 是输入标识。当前 purchases 也不是完整订单中心，未实现支付、退款、履约、订单行列表。

## 3. 一对多：用户与少量地址

设计示意：

~~~json
{
  "_id":"u1001",
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
  { _id: "u1001", version: 1, "addresses.id": "a1" },
  { $set: { "addresses.$.city": "深圳" }, $inc: { version: 1 } }
)
~~~

要检查 matchedCount；0 可能是版本过期、用户或地址不存在。增加地址还应在服务端约束数量和地址 ID 唯一，不能仅靠前端限制。

如果地址变成独立配送实体，有大量审计记录或高频独立查询，可拆为 addresses(userId, ...)，而不是继续扩张用户文档。

## 4. 一对无限增长的多：作者与文章

~~~json
{"_id":"post1001","authorId":"u1001","status":"published","title":"示例","createdAt":"时间"}
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

## 5. 多对一：商品与商家

~~~json
{"_id":"p1001","merchantId":"m1001","name":"示例商品","price":99.00,"status":"active"}
~~~

商家独立存在 merchants 集合，商品保存 merchantId。商家改联系电话时只更新商家文档。

若商品列表必须显示店铺名，可以保存 merchantNameSnapshot；必须明确这个字段允许暂时陈旧，商家改名由消息任务更新或查询时覆盖。订单中商家名称属于历史快照时，不应跟随当前商家名更新。

常见索引建议：

- 商家后台：merchantId + status + createdAt + _id。
- 商家唯一业务编号：单独唯一索引，约束范围需包含 tenantId（若有租户）。

当前示例 Product 没有 merchantId，上面是扩展方案。

## 6. 多对多：用户与收藏商品

当前实现：

~~~json
{"_id":"关系ID","userId":"u1001","productId":"p1001","createdAt":"时间"}
~~~

真实索引：

- unique(userId, productId)：同一用户不能重复收藏同一商品。
- (userId, createdAt DESC, _id DESC)：用户的收藏时间线。
- (productId, createdAt DESC)：按商品查看关系的索引基础。

添加使用 upsert + setOnInsert，配合唯一索引处理并发重复；取消按 userId/productId 删除。

不推荐把全部 productIds 放进 users，也不把全部 userIds 放进 products。两边都会随增长变成大数组和热点文档，分页与单条更新更难控制。

当前 GET favorites 最多取 50 条，并未实现游标。扩展时添加完整游标和 _id tie-breaker，反向商品时间线也应补齐稳定排序对应的索引。

商品被删除后如何处理收藏，应按业务选择隐藏、异步清理或保留失效提示；当前没有自动级联删除。

## 7. 订单与订单明细

当前示例一条 Purchase 只购买一个商品，unitPrice 是成交时价格，商品改价不修改历史记录。

多商品订单可以采用有上限的内嵌明细：

~~~json
{
  "_id":"order1001",
  "userId":"u1001",
  "status":"created",
  "items":[
    {"productId":"p1001","nameSnapshot":"成交商品名","quantity":2,"unitPrice":"99.00"}
  ],
  "totalAmount":"198.00",
  "currency":"CNY",
  "schemaVersion":1
}
~~~

为便于展示，JSON 金额写成字符串；Java 实体仍应定义 BigDecimal 并映射 Decimal128。不要在实际数据中混用字符串、Double 和 Decimal128 表示同一金额字段。

设计约束：

- 明细数量有上限。
- 商品 ID 保留引用，名称、单价保留快照。
- 收货地址也保存成交快照，不实时跟随用户地址修改。
- 支付、退款、履约记录持续增长时分集合。
- requestId 的唯一范围应包含业务或租户，不能无意跨租户撞号。

## 8. 查询性能

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

观察是否使用预期索引、是否发生大量扫描/内存排序、totalDocsExamined 与返回数量的关系。不要仅凭有 IXSCAN 就认定查询已经足够快。

避免：

- 先取全部结果再 Java 分页。
- 无边界的正则扫描和多字段任意排序。
- 逐条关联查询形成 N+1。
- 高频页面每次都做全量精确 count。
- 为了“可能会用”建立大量索引，放大写入成本。

## 9. 分页与时间

深翻页优先用索引范围游标。按时间分页必须加入唯一字段作为稳定次序，示例用 createdAt + _id。

不要假设 MongoDB 时间和 Java Instant 保留完全相同的精度；游标应来自实际持久化后返回的值。列表接口已有独立真实验收，但变更日期类型后应重新验证同毫秒多记录和边界翻页。

后台页码分页可使用 skip/limit，但要限制最大翻页深度并评估 count。框架 Paging 的当前缺陷见[扩展手册](EXTENDING.md)。[MongoDB 分页说明](https://www.mongodb.com/docs/manual/reference/method/cursor.skip/)

## 10. 更新性能与并发

库存：
~~~javascript
db.products.updateOne(
  { _id: "p1001", stock: { $gte: 2 } },
  { $inc: { stock: -2, version: 1 } }
)
~~~

这只是单文档语义示意；本示例实际通过 Java findAndModify 与订单事务完成。

原则：

- 原子更新用 $inc / $set 等操作符，避免覆盖整文档。
- 版本条件用于防止旧数据覆盖新数据。
- 热门商品库存可能成为写热点，需要业务排队、分桶或预留策略；缓存锁不自动解决所有一致性问题。
- 单文档原子性足够时不引入跨集合事务。
- 跨文档事务只包必要数据库操作，短事务比包含外部等待更可控。

MongoDB 会在单文档更新时重新判断条件；预期版本或库存条件用于避免冲突覆盖。[原子更新说明](https://www.mongodb.com/docs/manual/core/write-operations-atomicity/)

## 11. 增长与扩容路线

| 阶段 | 先做什么 | 依据 |
|---|---|---|
| 小规模 | 正确索引、请求上限、备份 | 慢查询、数据大小 |
| 增长中 | 游标、投影、冷热分离、缓存 | P95、扫描量、工作集、IO |
| 热点明显 | 原子条件更新、排队、限流、拆分热点 | 锁等待、冲突率、单键压力 |
| 单机资源逼近上限 | 副本集高可用、容量规划、读策略 | 恢复目标、延迟与资源利用率 |
| 需要水平分片 | 评估 shard key 与迁移计划 | 查询局部性、写热点、基数 |

不按固定“多少万条”决定分片。集合数量、文档大小、索引工作集和访问分布比单一行数更重要。分片键改变成本较高，先验证是否导致跨分片查询或热点。

## 12. 缓存、向量与消息的一致性

MongoDB 保存业务事实。Redis 可以保存可重建缓存；Qdrant 保存可重建召回索引；RabbitMQ 负责事件传递。

当前示例这几条链独立，没有自动同步。扩展建议：

1. 同一 Mongo 事务写业务数据与 outbox 事件。
2. 独立发布任务发送，记录重试和确认。
3. 消费端按 eventId + 版本幂等处理。
4. 对乱序事件拒绝旧版本覆盖新版本。
5. 定期对账并重建丢失的缓存 / 向量。

这套 Outbox 流程是设计建议，尚未实现。不要把消息发送放进事务方法就认为已经具有跨系统原子性。
