# Lambda Framework Example

使用 **JDK 27 + Spring WebFlux** 的可运行后端示例，基于 [Lambda Framework](https://github.com/MrWangGang/lambda-framework)。一个应用连接 MongoDB 副本集、Redis、RabbitMQ、Nacos 和 Qdrant，展示从请求校验到数据持久化的完整流程。

## 能做什么

| 场景 | 实现要点 |
|---|---|
| 商品管理 | Decimal128 金额、复合索引、游标分页、版本号防止覆盖更新 |
| 用户收藏 | 多对多关系集合、唯一索引、幂等添加与删除 |
| 下单扣库存 | MongoDB 多文档事务、库存条件更新、请求号幂等、瞬时事务冲突有限重试 |
| 缓存 | Redis ACL 登录、键前缀、TTL、读取与删除 |
| 延迟通知 | RabbitMQ 延迟插件、持久消息、发布确认、落库后 ACK、死信队列 |
| 向量检索 | Qdrant API Key、向量写入和相似度查询；演示使用 3 维向量 |
| 配置与发现 | Nacos 远程配置、动态刷新、实例注册与注销 |

## 快速启动

1. 将两个仓库放在同级目录，使用 JDK 27。IDEA 的 Project SDK 与 Maven Runner JRE 都设置为 27。
2. 构建框架到本地 Maven 缓存：

```bash
cd ../lambda-framework
./scripts/mvn.sh clean install
cd ../lambda-framework-example
cp .env.example .env
```

3. 编辑 `.env`，填写已有服务器地址、账号、密码和本机 JDK 路径。脚本使用 IDEA 自带 Maven，亦可通过 `MAVEN_BIN` 指定已有 Maven；不会安装 Maven。
4. 在 Nacos 公共命名空间创建下面的配置。服务器须已安装 RabbitMQ 延迟插件，MongoDB 须已配置 `rs0` 副本集。

```properties
# Data ID: lambda-example-app.properties
# Group: LAMBDA_EXAMPLE
# 配置类型: Properties
example.greeting=Hello from Nacos
```

5. 启动：

```bash
./scripts/run.sh
curl http://127.0.0.1:8081/api/info
```

将 Nacos 中的 `example.greeting` 改为其他值并发布，再请求 `/api/info` 可以看到更新。远程配置是启动必需项；连接错误会直接报错。

应用默认监听 `127.0.0.1:8081`。需要局域网访问时设置 `APP_BIND_ADDRESS=0.0.0.0`，并将 `APP_REGISTER_IP` 设置为调用方可达地址。示例接口尚未接入业务用户认证，适合作为内部开发起点。

## 目录

```text
lambda-example-app/src/main/
├── java/org/lambda/example/app/
│   ├── Application.java
│   ├── config/                 # 框架基础设施配置与事务
│   ├── catalog/                # 商品、收藏、订单
│   ├── cache/                  # Redis
│   ├── messaging/              # 延迟通知与消费回执
│   ├── vector/                 # Qdrant
│   └── web/                    # HTTP 接口与入参校验
└── resources/application.properties
scripts/                        # 复用 Maven 与启动脚本
docs/                           # 接口说明、配置与验收记录
```

已将原有零散演示和临时测试工程整理为上述业务模块。仓库不保留测试源码、测试数据、构建产物或真实凭据。

## 使用与维护

- [接口与调用示例](docs/API.md)：创建商品、游标分页、收藏、下单、延迟消息和向量检索。
- [配置说明](docs/CONFIGURATION.md)：环境变量、服务端口与资源命名。
- [验收记录](docs/TESTING.md)：真实服务验证范围、结果和清理。
- 修改后运行 `./scripts/mvn.sh clean package`；这是编译打包，不代表自动执行外部验收。需要清除构建产物时执行 `./scripts/mvn.sh clean`。

MongoDB 是业务事实来源。当前示例中缓存、消息与向量接口各自独立，尚未实现 MongoDB 与外部系统的跨库原子提交。业务需要可靠同步时应补充事务 Outbox 与幂等消费，详见配置说明。
