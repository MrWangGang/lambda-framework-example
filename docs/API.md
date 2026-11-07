# HTTP 接口

基础地址 `http://127.0.0.1:8081/api`。JSON 请求使用 `Content-Type: application/json`。以下 ID 由实际接口响应取得。

| 方法与路径 | 用途 |
|---|---|
| `GET /info` | 应用名、Nacos greeting、副本集名称 |
| `POST /products` | 创建商品 |
| `GET /products/{id}` | 单个商品 |
| `GET /products?category=book&limit=20` | 指定分类的游标分页 |
| `PUT /products/{id}?version=0` | 按版本更新商品 |
| `PUT /users/{userId}/favorites/{productId}` | 幂等收藏 |
| `DELETE /users/{userId}/favorites/{productId}` | 取消收藏 |
| `GET /users/{userId}/favorites` | 最近 50 条收藏 |
| `POST /purchases` | 事务扣库存并创建订单 |
| `PUT /cache/{key}` | 设置缓存与 TTL |
| `GET /cache/{key}` | 读取缓存（纯文本） |
| `DELETE /cache/{key}` | 删除缓存 |
| `POST /messages/delayed` | 发布延迟通知 |
| `GET /messages/{id}` | 查看消费回执；尚未消费时 404 |
| `PUT /vectors/{uuid}` | 写入 3 维向量 |
| `POST /vectors/query` | 相似度查询 |

## 商品与订单

```bash
curl -X POST http://127.0.0.1:8081/api/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"MongoDB 设计指南","category":"book","price":99.00,"stock":10}'
```

返回 `id`、`version` 等字段。更新时提供完整 name/category/price/stock 和已读取的 version；版本冲突返回 409。错误入参返回 400，不存在的商品返回 404。

分页响应包含 `items`、`nextBeforeTime`、`nextBeforeId`。下一页同时传入 `beforeTime` 和 `beforeId`（URL 编码时间），直到下一页字段为空；limit 为 1..100。

```bash
curl -X POST http://127.0.0.1:8081/api/purchases \
  -H 'Content-Type: application/json' \
  -d '{"requestId":"order-001","userId":"u-001","productId":"替换为商品ID","quantity":2}'
```

相同请求号和参数重复调用不会重复扣库存；同一请求号用于不同参数返回 409。库存不足返回 409，事务不会留下半条订单。

## 缓存与延迟消息

```bash
curl -X PUT http://127.0.0.1:8081/api/cache/welcome \
  -H 'Content-Type: application/json' -d '{"value":"你好","ttlSeconds":60}'
curl http://127.0.0.1:8081/api/cache/welcome
curl -X DELETE http://127.0.0.1:8081/api/cache/welcome
curl -X POST http://127.0.0.1:8081/api/messages/delayed \
  -H 'Content-Type: application/json' -d '{"content":"延迟通知","delayMillis":2000}'
```

收到消息 id 后，约 2 秒再访问 `/messages/{id}`。TTL 范围 1..86400 秒；延迟范围 0..60000 毫秒。

## 向量

```bash
curl -X PUT http://127.0.0.1:8081/api/vectors/8a1a2bb5-7d8f-4ed7-b818-170f4ec0f722 \
  -H 'Content-Type: application/json' -d '{"vector":[1,0,0],"category":"book"}'
curl -X POST http://127.0.0.1:8081/api/vectors/query \
  -H 'Content-Type: application/json' -d '{"vector":[1,0,0],"limit":5}'
```

向量 ID 使用 UUID，维度必须为 3、数值有限且不能全部为零，limit 为 1..100。返回 Qdrant 的 JSON 查询结果。
