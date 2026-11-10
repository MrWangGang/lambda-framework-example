# 扩展开发路线与现有能力边界

[返回总览](../README.md) · [API](API.md) · [建模](DATA-MODELING.md)

本章帮助在当前应用上继续开发。所有标为“待实现”的项目当前没有对应端点或完成的集成，示意类不是已提交源码。

## 1. 新业务放在哪里

Service A 的 `app` 模块分为 `controller`、`service`、`repository`、`po`、`contract` 和 `facade` 实现。RPC 接口与 record 位于独立的 `service-a/facade` 模块。Service B 依赖这个 Facade JAR。

Controller 只绑定请求和调用 Service。`*ServiceImpl` 用 `@Resource` 注入依赖，在 Service 完成校验、业务规则和响应转换。Repository 负责 Mongo 查询或 Qdrant 集合访问。返回 Mono 或 Flux，把持久化操作串联返回。

## 2. 用户体系

业务登录核验成功后调用 `PrincipalFactory.setPrincipal`。网关通过 Redis 校验令牌，框架透传当前身份。注入 `PrincipalFactory`，调用 `currentUserId()` 获取 ID，或调用 `currentUser(LoginUser.class)` 读取快照。按会话和资源归属授权。

[框架用户体系手册](https://github.com/MrWangGang/lambda-framework/blob/main/docs/USER-AND-SECURITY.md)

## 3. 框架页码分页

POST /api/product/page 使用 ProductContract.PageRequest，ProductRepository 通过 queryPage 组合 Mongo count、查询和稳定排序。返回 Paged<ProductContract.Detail>。大列表采用 /api/product/cursor。

## 4. RSocket：独立服务的接入

需要明确服务端 TCP 端口、客户端目标、Nacos 注册 IP、数据/元数据 MIME、路由、认证边界及超时。

框架的连接缓存、实例 key、发现异常恢复、端口注册等存在待验证/待修问题。[完整分析](https://github.com/MrWangGang/lambda-framework/blob/main/docs/RSOCKET-AND-VIRTUAL-THREADS.md)

分别验证 HTTP 经网关转发和原生 Fory Facade 调用。服务多实例发布需覆盖实例重启与替换。

## 5. 虚拟线程：保留 Reactor

目标是把适合阻塞等待的任务在底层调度到虚拟线程，保留业务 Mono/Flux。

Boot 的 spring.threads.virtual.enabled 可以衔接 Reactor boundedElastic，但原有模块配置注册和启动时序还需整理。Netty 的事件循环、数据库驱动线程和 CPU 计算线程不会自动全部改变。

未来 COS / SMS 同步 SDK 调用应封装在 Mono.fromCallable 中，使用合适的调度器和并发上限；身份继续通过 Reactor Context 传递。没有完成执行位置与性能验证前，不宣传“全项目已虚拟线程化”。

## 6. 商品到推荐系统

当前 Qdrant 只演示 3 维向量相似度检索。实际推荐链可逐步加入：

1. 商品文本/图片生成 embedding，确定模型与维度。
2. Mongo 保存商品事实和 embedding 版本。
3. 用 Outbox 推送增量事件。
4. Qdrant 幂等 upsert，保存业务 ID 与版本。
5. 查询时限定租户、可见性、上架状态等范围。
6. 召回后回查 Mongo，过滤失效、无权限、无库存对象。
7. 再做业务排序、去重、探索和曝光记录。

这些是扩展设计，当前没有自动 embedding、分类过滤或排序模型。

## 7. 消息可靠性

当前已有发布确认、持久消息、手动 ACK、幂等回执、死信。仍不等于 MongoDB 与 RabbitMQ 跨系统原子提交。

需要保证订单事件可靠发出时：

- 订单和 Outbox 在同一 Mongo 事务写入。
- 发布任务持有事件 ID，确认后更新发送状态。
- 消费端处理重复与乱序。
- 超时结果不明时先查状态，再有限重试。
- 配置死信监控与可审查的补偿流程。

当前购买接口不会自动发消息。不要在文档或前端流程中假设“购买成功必然产生通知”。

## 8. API 与模型演进

- 新字段先考虑旧数据缺省值和兼容读取。
- 不改变已持久化金额字段类型而不迁移。
- 修改向量维度新建集合并设计重建/切换。
- 修改 Rabbit 拓扑参数需要迁移或新版本名称。
- 修改唯一索引先清理/合并历史冲突。
- 外部 API 接口增加版本或兼容策略。
- 数据库 schemaVersion 与事件 version 各自有明确语义。

## 9. 完成定义

每项扩展要同时有代码、配置、调用示例、成功与失败用例、清理方式以及真实测试记录。只有方案文档或构建通过时，不标记为“接入完成”。

测试代码按当前交付约定放在仓库外；正式示例保留可读的 src/main 业务实现。验收后清理测试数据，保留必要的日志证据。
