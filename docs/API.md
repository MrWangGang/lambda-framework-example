# 示例 API

[完整入口与请求示例](../README.md#普通请求)。所有业务请求发到网关，使用 POST 与 JSON 请求体，ID 放请求 record 字段。

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


## 单向流与可选组件

所有请求带上登录令牌。

| POST 路径 | JSON 请求体 | 返回 |
|---|---|---|
| `/api/purchase/summary` | `{"userId":"507f1f77bcf86cd799439011"}` | 订单按商品分类的聚合数组，ObjectId 直接绑定 Mongo 聚合参数 |
| `/api/product/export` | `{"category":"book","limit":"20"}` | NDJSON，每行一个业务响应外壳 |
| `/api/catalog/maintenance/rebuild` | `{"category":"book","lockProvider":"REDIS"}` | 雪花任务 ID 与缓存中的商品计数。启用 ZooKeeper 时也可指定 `ZOOKEEPER` |
| `/api/catalog/event/publish` | `{"productId":"507f1f77bcf86cd799439011"}` | Kafka 发布确认后返回事件 ID |
| `/api/catalog/event/find` | `{"eventId":"事件ID"}` | 消费成功后读取 MongoDB 回执 |
| `/api/partner/status` | `{}` | 外部服务状态 |
