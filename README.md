# Lambda Framework Example 操作手册

使用 **JDK 27 + Spring WebFlux** 的可运行后端示例，基于 [Lambda Framework](https://github.com/MrWangGang/lambda-framework)。一个应用连接 MongoDB 副本集、Redis、RabbitMQ、Nacos 和 Qdrant，演示请求校验、持久化、事务、消息确认及向量查询。

文档更新：**2026-11-09**。本仓库只有一个应用模块 `lambda-example-app`。本次扩充文档，没有新增登录、页码分页或 RPC 业务实现。

## 1. 操作手册导航

| 任务 | 文档 |
|---|---|
| 从零导入 IDEA、配置并启动 | [首次运行](docs/QUICKSTART.md) |
| 查询每个接口的参数、返回、错误与 curl | [完整 API 手册](docs/API.md) |
| 修改环境变量、地址、账号与资源前缀 | [配置字典](docs/CONFIGURATION.md) |
| MongoDB 一对多、多对一、多对多与性能 | [数据建模规范](docs/DATA-MODELING.md) |
| Docker、端口、防火墙、账号用途、备份与清理 | [部署运维](docs/OPERATIONS.md) |
| 对接腾讯云 COS / 短信 | [云接口接入设计](docs/CLOUD-INTEGRATION.md) |
| 复核历史测试、执行手工验收并清数据 | [验收与清理](docs/TESTING.md) |
| 启动失败、认证失败、版本冲突 | [排错手册](docs/TROUBLESHOOTING.md) |
| 接入用户体系、框架分页、RPC、虚拟线程 | [扩展开发](docs/EXTENDING.md) |
| GitHub SSH、构建和推送 | [框架维护手册](https://github.com/MrWangGang/lambda-framework/blob/main/docs/OPERATIONS.md) |

## 2. 已实现的业务

| 场景 | 实现 |
|---|---|
| 商品 | Decimal128 金额、复合索引、游标分页、版本条件更新 |
| 收藏 | 多对多关系集合、唯一索引、幂等添加与删除 |
| 购买 | MongoDB 多文档事务、条件扣库存、requestId 幂等、有限冲突重试 |
| 缓存 | Redis ACL、键前缀、TTL、查询和删除 |
| 延迟通知 | 插件交换机、持久消息、发布确认、落库后 ACK、死信队列 |
| 向量检索 | Qdrant API Key、3 维 Cosine 向量写入与查询 |
| 配置和发现 | Nacos 配置导入、动态 greeting、应用注册与注销 |

示例的用户 ID 来自请求输入，尚无登录权限保护。默认只监听本机 `127.0.0.1:8081`，适合作为内部开发起点。

## 3. 快速启动

前提：JDK 27、已有 IDEA Maven、可达的五项中间件。MongoDB 是 rs0 副本集，RabbitMQ 已启用兼容延迟插件。

~~~bash
# 两个仓库放在同级目录
cd ../lambda-framework
export JAVA_HOME='/实际的/JDK27/Contents/Home'
./scripts/mvn.sh -B clean install
cd ../lambda-framework-example
cp .env.example .env
chmod 600 .env
# 编辑 .env，填写自己的环境
~~~

在 Nacos 创建：

| 字段 | 值 |
|---|---|
| Namespace ID | `public` |
| Group | `LAMBDA_EXAMPLE` |
| Data ID | `lambda-example-app.properties` |
| 格式 | Properties |

内容：

~~~properties
example.greeting=Hello from Nacos
~~~

~~~bash
./scripts/run.sh
~~~

另开终端验证：

~~~bash
curl -i http://127.0.0.1:8081/api/info
~~~

预期为 200，JSON 包含 application、greeting、replicaSet。`replicaSet` 是配置值，本接口不是数据库实时健康检查；完整验证还需执行业务请求。

## 4. 代码目录

~~~text
lambda-example-app/src/main/
├── java/org/lambda/example/app/
│   ├── Application.java
│   ├── config/InfrastructureConfiguration.java
│   ├── catalog/Models.java
│   ├── catalog/CatalogService.java
│   ├── cache/CacheService.java
│   ├── messaging/DelayedMessageService.java
│   ├── vector/VectorService.java
│   └── web/ExampleController.java
└── resources/application.properties
scripts/
├── mvn.sh
└── run.sh
docs/
.env.example
~~~

不保留临时测试源码、真实凭据和构建产物。`.env` 被 Git 忽略；`scripts/run.sh` 会构建 JAR 后启动，不会安装 Maven。

## 5. 状态与限制

- 2026-11-07：框架 15 个 reactor 项目、示例 2 个 reactor 项目构建通过；仓库外 JUnit 11 项、HTTP 业务验收 22 项通过。
- 示例分页是 `items / nextBeforeTime / nextBeforeId` 复合游标。
- 框架另有 `Paging/Paged` 页码分页，已发现 3 类问题，外部回归 12 项中 8 项失败，未修复；示例没有 `/api/products/page`。
- 用户体系、跨服务 RPC、整体虚拟线程改造尚未在本示例完成接入与验收。
- 缓存、消息、向量接口独立演示，不会随商品变更自动同步，也不提供跨库原子提交。
- 收藏列表最多返回 50 条；当前没有收藏游标接口。
- 当前没有商品删除、订单查询/取消、支付、COS 上传或短信发送端点。
- 单节点 MongoDB 副本集、Nacos standalone 不具备高可用；没有生产规模压测结论。

## 6. 推荐阅读路径

先照 QUICKSTART 启动并创建一个商品，再按 API 手册完成分页、更新、收藏、购买、缓存、消息和向量操作。每次验收使用独立数据库及资源前缀，最后按 TESTING 清理。

业务增长时先读 DATA-MODELING，再扩展用户认证和查询索引。不要把文档中的未来设计当成已经发布的接口。

## 7. 维护命令

~~~bash
./scripts/mvn.sh -B clean package
git diff --check
./scripts/mvn.sh clean
~~~

package 是编译打包；历史外部测试程序已移除，不能把这条命令视为重跑全部验收。运行记录和新增发现见 TESTING。
