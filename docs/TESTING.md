# 测试与验证记录

日期：2026-11-07。使用已有 JDK 27.0.1.0.3 与 IntelliJ IDEA 内置 Maven 3.9.16；未安装额外 Maven，也未向远程 Maven 仓库发布构件。

## 结果

| 验证项 | 结果 |
|---|---|
| 框架 `clean install` | 15 个 reactor 项目全部成功 |
| 示例 `-Pintegration clean verify` | 8 个 reactor 项目全部成功 |
| JSON 回归 | 2 项通过：中文 record/时间往返、未知字段兼容 |
| Redis | ACL 认证、框架写入/读取、删除并确认不存在 |
| MongoDB | 单机认证、插入/查询/更新/删除、临时数据库清理 |
| MongoDB Spring Boot 配置 | 不配置 replica 属性，启动上下文并通过 ReactiveMongoTemplate 读写、清理 |
| RabbitMQ | 框架 Sender 发布确认、立即不可取、延迟 2 秒后收取、删除队列与交换机 |

最终 JUnit 结果：**6 项通过、0 失败、0 错误、0 跳过**（Surefire 2 + Failsafe 4）。外部服务版本为 MongoDB 9.0.4、Redis 8.10.3、RabbitMQ 4.2.9，延迟插件 4.2.0。

## 复现

先在框架仓库运行 `./scripts/mvn.sh clean install`，再在示例根目录执行：

```bash
export JAVA_HOME="你的 JDK 27 路径"
export SERVICE_HOST="服务主机"
export SERVICE_USER="服务账号"
read -rs SERVICE_PASSWORD
export SERVICE_PASSWORD
./scripts/mvn.sh -Pintegration clean verify
unset SERVICE_PASSWORD
```

不需要外部服务时，执行 `./scripts/mvn.sh clean verify`：编译全部模块并仅运行 2 项 JSON 测试。脚本复用 IDEA 内置 Maven，也可通过 `MAVEN_BIN` 指定已有 Maven。

Surefire 报告位于 `integration-tests/target/surefire-reports/`；Failsafe 报告位于 `integration-tests/target/failsafe-reports/`。macOS 测试自动选择对应架构的 Netty DNS 原生库；版本与 Boot BOM 的 Netty 版本一致。

## 数据清理

每次执行使用随机 `lambda_it_` 名称。Redis 测试键额外设置 60 秒 TTL；Mongo 测试数据库、Rabbit 测试队列和交换机均由 `finally` 清理。本次还通过服务端独立检查，确认测试资源剩余数量为 0。若进程被强制终止，应根据日志和本次 UUID 定位残留，不要使用全库清空命令。

## 范围

本结果覆盖构建、JSON 与上述真实基础设施路径。未验证完整 Nacos 微服务启动、跨服务 RPC、MySQL、Kafka、Elasticsearch、ZooKeeper、Redis 集群/哨兵、Mongo 副本集事务、性能压测或故障恢复。原有两个 `RunTest` 没有测试方法，不能算作启动测试。

RabbitMQ 选择 4.2.9 是延迟插件兼容性要求；没有宣称使用 RabbitMQ 最新主版本。框架保留 Jackson 2 公共 API 兼容线，Spring 生态传递版本由 Boot / Cloud BOM 管理。
