# 示例 API

[完整接口清单和请求示例](../README.md#api)。所有业务请求发到网关，使用 POST 与 JSON 请求体，ID 放请求 record 字段。

| 输入 | 约定 |
|---|---|
| MongoDB id、userId、productId | 24 位十六进制 ObjectId 字符串 |
| price、stock、quantity、version | 字符串。price 支持最多两位小数，数量与版本在业务边界转换 |
| status | active 或 inactive |
| page、size、limit | 正整数字符串。页码从 1 开始，默认 1/10，页大小不超过 100 |
| beforeTime、beforeId | 同时传入，使用前一页返回的 nextBeforeTime、nextBeforeId |
| requestId | 每次购买操作的稳定幂等键 |
| ttlSeconds | 1 至 86400 的整数字符串 |
| delayMillis | 0 至 60000 的整数字符串 |
| vector | 三个有限数值字符串，向量至少一维非零 |
| 向量 id | UUID |

~~~bash
base=http://127.0.0.1:8081/api
curl -fsS -X POST "$base/product/find" -H 'Content-Type: application/json' \
  -d '{"id":"507f1f77bcf86cd799439011"}'
curl -fsS -X POST "$base/product/cursor" -H 'Content-Type: application/json' \
  -d '{"category":"book","limit":"20"}'
curl -fsS -X POST "$base/cache/put" -H 'Content-Type: application/json' \
  -d '{"key":"smoke","value":"ready","ttlSeconds":"60"}'
curl -fsS -X POST "$base/message/sendDelayed" -H 'Content-Type: application/json' \
  -d '{"content":"hello","delayMillis":"2000"}'
~~~

查询示例中的 id 使用本次实际创建的商品 ID。网关的业务响应使用 JSON 外壳。成功时 `serviceCode` 为 `E00000000`，缓存查询的 `data` 是 JSON 字符串。输入校验、资源不存在和库存冲突等后端错误通过 `serviceCode` 和 `serviceMessage` 返回，业务客户端按这两个字段判断。收藏列表由普通 Flux 聚合成 `data` 数组，空列表返回 `data: []`。部分删除接口返回空数据。需要验收数据时使用独立测试库和资源前缀，验收后清理。
