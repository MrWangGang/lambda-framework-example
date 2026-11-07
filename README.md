# Lambda Framework · 示例与验证

这个仓库展示如何使用 [Lambda Framework](https://github.com/MrWangGang/lambda-framework)，并提供连接真实 Redis、MongoDB、RabbitMQ 的集成测试。基线为 **JDK 27**，与框架的 `1.0.0-SNAPSHOT` 本地构件配套使用。

## 目录

```text
lambda-framework-example/
├── ace-microservices-fxq/       # RPC 调用方与接口示例
├── ace-microservices-service/   # 业务服务与接口示例
├── integration-tests/          # 无需 Nacos 的基础设施验证
├── scripts/mvn.sh              # 复用 IDEA 内置 Maven
└── docs/                       # 配置与验证说明
```

两个业务项目均拆分为 `facade` 和 `server`。Facade 只定义接口和模型，两个 facade 不相互依赖；跨服务调用依赖由 server 声明。根 POM 统一编排构建，避免手工调整模块顺序。

## 三步验证

### 1. 构建框架

建议将两个仓库放在同一个目录：

```text
workspace/
├── lambda-framework/
└── lambda-framework-example/
```

```bash
cd ../lambda-framework
./scripts/mvn.sh clean install
```

只安装到本机 Maven 缓存，不上传构件。不需要额外安装 Maven；`scripts/mvn.sh` 会使用已有 IDEA 内置 Maven，也支持 `MAVEN_BIN` 指定路径。

### 2. 构建示例并运行本地回归

```bash
cd ../lambda-framework-example
./scripts/mvn.sh clean verify
```

该命令编译全部示例模块并运行 JSON 回归测试，不连接外部数据库。原有两个 `RunTest` 没有测试方法，不能据其构建成功推断完整微服务启动成功。

### 3. 运行真实服务联调

先在目标服务器准备 MongoDB、Redis 和 RabbitMQ。Redis 可使用 ACL 用户；RabbitMQ 必须启用 `rabbitmq_delayed_message_exchange`。

```bash
export SERVICE_HOST="你的服务器IP"
export SERVICE_USER="root"
# 避免密码进入 shell 历史：在 bash / zsh 中交互读取
read -rs SERVICE_PASSWORD
export SERVICE_PASSWORD
./scripts/mvn.sh -pl integration-tests -Pintegration verify
unset SERVICE_PASSWORD
```

| 测试 | 验证内容 | 清理方式 |
|---|---|---|
| Redis | 框架配置、ACL 认证、写入及读取 | 删除随机测试键并确认不存在；键另带 60 秒 TTL |
| MongoDB | 框架单机配置、CRUD、Boot 上下文启动与模板读写 | 删除本次随机测试数据库并确认不存在 |
| RabbitMQ | 框架发送器、发布确认、延迟投递 | 删除本次随机队列与交换机 |

测试资源统一使用 `lambda_it_` 加随机 UUID。清理放在 `finally` 中；连接失败时不执行全库清空，也不会删除业务资源。失败日志在 `integration-tests/target/failsafe-reports/`，本地回归结果在 `target/surefire-reports/`。

## 运行完整微服务示例

完整示例依赖 Nacos 中的业务配置，还可能需要 MySQL、Kafka、ZooKeeper 等服务。它与上述独立基础设施测试的运行前提不同。

- `NACOS_SERVER_ADDR`：Nacos 地址，默认 `127.0.0.1:8848`。
- `NACOS_NAMESPACE`：命名空间 ID，默认公共命名空间。
- `NACOS_GROUP`：配置分组，默认 `ace-microservices`。
- `NACOS_USERNAME` / `NACOS_PASSWORD`：Nacos 认证信息。

配置通过 `application.properties` 中的 `spring.config.import` 加载，不再使用旧 bootstrap 机制。应用名称对应配置 Data ID：`ace-microservices-fxq.properties` 与 `ace-microservices-service.properties`。业务数据源、RSocket 端口等参数需按实际环境配置。

## 遇到问题

- **找不到框架依赖**：先在框架仓库执行 `install`，确认两个项目使用同一个本地 Maven 缓存。
- **Redis 认证失败**：确认 ACL 用户名存在；只填写密码会尝试 default 用户。
- **连接超时或握手中断**：检查云防火墙、主机端口、来源 IP 和容器日志。
- **延迟交换机不支持**：检查插件启用状态及 RabbitMQ / 插件版本兼容性。
- **MongoDB 事务失败**：单机不支持多文档事务；本测试验证的是单文档 CRUD。

本仓库不包含服务器密码、云密钥或私有 Maven 仓库凭据。不要将本地 `.env` 与测试结果中的敏感配置提交到仓库。

本次结果和复现范围见[测试记录](docs/TESTING.md)。
