# HTTP API 操作手册

[返回总览](../README.md) · [配置限制](CONFIGURATION.md) · [验收清理](TESTING.md)

## 1. 通用规则

基础地址：`http://127.0.0.1:8081/api`。本章命令在应用电脑执行；先设置：

~~~bash
export API_BASE='http://127.0.0.1:8081/api'
export PRODUCT_ID='替换为创建商品响应的id'
export USER_ID='manual-user'
~~~

PRODUCT_ID 必须在创建成功后替换。JSON 请求发送 Content-Type: application/json。时间为 ISO-8601，实际时间和 ID 由响应返回。

当前接口没有接入用户登录认证。路径或 JSON 中的 userId 只是业务演示数据，不能代表可信身份。没有统一 ResponseTemplete 包装；直接返回 DTO、布尔、数字或 Qdrant JSON。

## 2. 端点总表

| 方法 | 路径 | 成功响应 |
|---|---|---|
| GET | /info | 应用配置信息 |
| POST | /products | Product |
| GET | /products/{id} | Product |
| GET | /products | ProductPage |
| PUT | /products/{id}?version=N | 更新后的 Product |
| PUT | /users/{userId}/favorites/{productId} | 空响应体 |
| DELETE | /users/{userId}/favorites/{productId} | 空响应体 |
| GET | /users/{userId}/favorites | Favorite 数组 |
| POST | /purchases | Purchase |
| PUT | /cache/{key} | boolean |
| GET | /cache/{key} | text/plain |
| DELETE | /cache/{key} | 删除数量 |
| POST | /messages/delayed | Message |
| GET | /messages/{id} | Mongo 回执文档 |
| PUT | /vectors/{uuid} | Qdrant 原始 JSON |
| POST | /vectors/query | Qdrant 原始 JSON |

控制器没有显式配置创建为 201 或空返回为 204，当前成功通常为 200；调用方以实际 HTTP 状态和响应内容为准。

## 3. 查看配置

~~~bash
curl -i "$API_BASE/info"
~~~

~~~json
{"application":"lambda-example-app","greeting":"Hello from Nacos","replicaSet":"rs0"}
~~~

replicaSet 来自配置，不是实时 rs.status；该接口也不会逐一探测所有中间件。

## 4. 创建商品

~~~bash
curl -i -X POST "$API_BASE/products" \
  -H 'Content-Type: application/json' \
  -d '{"name":"MongoDB 设计指南","category":"manual-book","price":99.00,"stock":10}'
~~~

| 字段 | 类型 | 规则 |
|---|---|---|
| name | string | 非空，最多 120 字符 |
| category | string | 非空，最多 64 字符 |
| price | number | >=0，整数最多 12 位，小数最多 2 位 |
| stock | integer | >=0 |

结构示意：

~~~json
{
  "id":"服务端生成的UUID",
  "name":"MongoDB 设计指南",
  "category":"manual-book",
  "price":99.00,
  "stock":10,
  "version":0,
  "createdAt":"服务端生成的UTC时间"
}
~~~

不要依赖 JSON 数字保留末尾零；金额按数值解析。记录返回的 id 和 version，之后更新使用实际 version。

创建没有 requestId 去重；同样请求重复提交会创建不同商品。

## 5. 查询单个商品

~~~bash
curl -i "$API_BASE/products/$PRODUCT_ID"
~~~

不存在返回 404。没有商品删除接口；验收数据通过专用 Mongo 数据库清理。

## 6. 商品游标分页

~~~bash
curl -G "$API_BASE/products" \
  --data-urlencode 'category=manual-book' \
  --data-urlencode 'limit=2'
~~~

| 参数 | 必需 | 规则 |
|---|---|---|
| category | 是 | 非空字符串 |
| limit | 否 | 默认 20，1..100 |
| beforeTime | 否 | ISO-8601 Instant；与 beforeId 成对 |
| beforeId | 否 | 上一页 nextBeforeId；与 beforeTime 成对 |

~~~json
{
  "items":[{"id":"某商品ID","name":"示例","category":"manual-book","price":99,"stock":10,"version":0,"createdAt":"时间"}],
  "nextBeforeTime":"还有下一页时的游标时间",
  "nextBeforeId":"还有下一页时的游标ID"
}
~~~

上面是结构示意。下一页：

~~~bash
export BEFORE_TIME='替换为nextBeforeTime'
export BEFORE_ID='替换为nextBeforeId'
curl -G "$API_BASE/products" \
  --data-urlencode 'category=manual-book' \
  --data-urlencode 'limit=2' \
  --data-urlencode "beforeTime=$BEFORE_TIME" \
  --data-urlencode "beforeId=$BEFORE_ID"
~~~

next 字段为 null 时停止，不要把字符串 null 当作游标传回。只传一个游标、limit=0/101 或 category 为空返回 400。

排序固定 createdAt 和 id 倒序，当前没有 sort/direction 参数，不返回 total/pages。`/products/page` 尚未实现；框架页码分页缺陷见 [扩展说明](EXTENDING.md)。

## 7. 按版本更新商品

~~~bash
export PRODUCT_VERSION='替换为当前版本号'
curl -i -X PUT "$API_BASE/products/$PRODUCT_ID?version=$PRODUCT_VERSION" \
  -H 'Content-Type: application/json' \
  -d '{"name":"MongoDB 设计指南（更新）","category":"manual-book","price":109.00,"stock":10}'
~~~

提交完整四个字段，不是 PATCH。成功 version 加一。请求 version 已过期或商品不存在时返回 409；当前实现不在该分支区分这两种情况。

购买操作也会递增商品 version，所以刚读出的版本在并发下可能已失效。冲突后重新读取、合并用户修改，再决定是否重试。

## 8. 添加、列出和取消收藏

~~~bash
curl -i -X PUT "$API_BASE/users/$USER_ID/favorites/$PRODUCT_ID"
curl -i "$API_BASE/users/$USER_ID/favorites"
curl -i -X DELETE "$API_BASE/users/$USER_ID/favorites/$PRODUCT_ID"
~~~

- 添加前验证商品存在，不存在为 404。
- 同用户同商品受唯一索引约束，重复添加不新增关系。
- 取消不存在的收藏仍正常结束。
- 列表返回最近最多 50 条 Favorite，按 createdAt、id 倒序。
- Favorite 包含 id、userId、productId、createdAt，**不会自动附带完整 Product**。
- 当前无收藏分页、总数或反向粉丝列表接口。
- 当前没有自动验证该 userId 对应真实用户。

## 9. 事务购买与幂等

~~~bash
curl -i -X POST "$API_BASE/purchases" \
  -H 'Content-Type: application/json' \
  --data-binary @- <<JSON
{"requestId":"manual-order-001","userId":"$USER_ID","productId":"$PRODUCT_ID","quantity":2}
JSON
~~~

| 字段 | 规则 |
|---|---|
| requestId | 1..80 位字母数字、下划线、连字符 |
| userId | 非空，最多 80 字符 |
| productId | 非空，实际存在且库存充足 |
| quantity | 1..1000 |

Purchase 返回 id（就是 requestId）、userId、productId、quantity、unitPrice、createdAt。unitPrice 是购买时价格快照。

重复相同请求号：

- 参数一致：返回已有订单，不再扣库存。
- userId/productId/quantity 任一变化：409。
- 库存不足或商品不存在：409。
- 入参不合法：400。
- 事务失败：不留下仅扣库存或仅有订单的中间状态。

未知结果或网络超时后应使用同一 requestId 重试，不能立即换号再买。当前没有独立订单查询、取消、退款接口，也不涉及真实支付。

## 10. Redis 缓存

~~~bash
curl -i -X PUT "$API_BASE/cache/manual_welcome" \
  -H 'Content-Type: application/json' \
  -d '{"value":"你好","ttlSeconds":60}'
curl -i "$API_BASE/cache/manual_welcome"
curl -i -X DELETE "$API_BASE/cache/manual_welcome"
~~~

key 只允许字母数字、下划线、连字符，长 1..80。value 非 null，最长 10000；ttlSeconds 1..86400。

PUT 返回布尔结果；GET 返回纯文本；不存在或 TTL 到期时 GET 返回 404；DELETE 返回删除数量。实际 Redis key 会加资源前缀，不能把 HTTP key 当完整物理 key。

缓存接口不会自动缓存商品，也不会因商品更新自动失效。

## 11. 延迟通知与回执

~~~bash
curl -i -X POST "$API_BASE/messages/delayed" \
  -H 'Content-Type: application/json' \
  -d '{"content":"手册演练通知","delayMillis":2000}'
~~~

返回 Message：

~~~json
{"id":"消息UUID","content":"手册演练通知","createdAt":"创建时间"}
~~~

保存 id，等待约 2 秒后：

~~~bash
export MESSAGE_ID='替换为消息id'
curl -i "$API_BASE/messages/$MESSAGE_ID"
~~~

尚未落库为 404。回执包含 _id、content、createdAt、receivedAt；具体日期序列化按当前 Mongo Document 输出。

时间包含发布、延迟、网络、消费和轮询，不是精确到毫秒的 SLA。每次 POST 生成新消息 ID，HTTP 重试不会自动复用旧 ID。

消费者先幂等写回执再 ACK；失败进入死信队列。发布成功不等于消费者已成功，也不等于未来永不丢失。没有回执时应核查队列和消费者日志，而非无限重发。

## 12. Qdrant 向量写入与查询

~~~bash
export VECTOR_ID='8a1a2bb5-7d8f-4ed7-b818-170f4ec0f722'
curl -i -X PUT "$API_BASE/vectors/$VECTOR_ID" \
  -H 'Content-Type: application/json' \
  -d '{"vector":[1,0,0],"category":"manual-book"}'
curl -i -X POST "$API_BASE/vectors/query" \
  -H 'Content-Type: application/json' \
  -d '{"vector":[1,0,0],"limit":5}'
~~~

| 字段 | 规则 |
|---|---|
| 路径 id | 可解析的 UUID |
| vector | 恰好 3 个非 null 有限数，不能全为 0 |
| category | 写入必需，非空且最长 64 |
| limit | 查询必需，1..100 |

返回 Qdrant 原始 JSON；查询结果通常在 result.points 中，包含 id、score、payload。以实际 Qdrant 响应结构为准，不把 HTTP 成功当作一定查到结果。

写入同一 UUID 是 upsert，会更新该点。当前 query 不支持按 category 过滤；category 只是写入 payload。向量 ID 与商品 ID 也没有自动校验关系。

本演示是 3 维 Cosine 检索，不是完整推荐算法。接真实 embedding 时应新建匹配维度的集合，并补召回过滤、业务回查及排序。当前没有向量删除端点，验收清理通过 Qdrant 管理 API。

## 13. 错误处理与客户端约定

| 状态 / 现象 | 常见原因 | 操作 |
|---|---|---|
| 400 | 参数格式、范围、UUID、向量不合法 | 修正输入，不盲目重试 |
| 404 | 商品/缓存/回执不存在 | 区分未创建、已过期、尚未消费 |
| 409 | 版本、库存、requestId 冲突 | 重新读取状态并按幂等规则处理 |
| 503 | Broker 未确认消息等 | 查询发布与队列状态 |
| 5xx / 超时 | 依赖故障、未映射异常 | 用请求号排查服务端，避免重复副作用 |

错误体由当前 Spring Boot / WebFlux 默认处理和异常类型决定，尚未约定稳定的自定义错误 DTO。客户端不要依赖所有异常都含相同 message 字段。

本手册未创建测试数据。实际演练后按 TESTING 清理，尤其是商品、订单、回执和向量这些没有 HTTP 删除接口的数据。
