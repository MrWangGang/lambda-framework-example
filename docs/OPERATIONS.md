# Docker 部署、端口、账号与运维手册

[返回总览](../README.md) · [配置](CONFIGURATION.md) · [测试清理](TESTING.md) · [排错](TROUBLESHOOTING.md)

## 1. 部署基线与资料位置

以下是 2026-11-07 已部署并验收的环境记录，不是本次文档更新重新检测出的实时状态。

| 项目 | 记录 |
|---|---|
| 操作系统 | Debian 13 |
| 资源 | 4 CPU、约 7.5 GB 内存 |
| 服务管理目录 | /opt/app-services |
| Compose 配置 | /opt/app-services/compose.yaml |
| 凭据 | 服务器 .env、Redis ACL 文件、Mongo keyfile 等私有文件 |
| 本机项目配置 | 示例根目录 .env，Git 忽略 |
| 实际服务器地址、口令、SSH 私钥 | 本地私有《第三方配置.md》 |

公开操作命令用 SERVICE_HOST 占位。Docker Compose 文件在服务器，**不在此仓库**；本文不是完整的新服务器一键安装脚本。

## 2. 版本与国内来源

| 软件 | 历史验收版本 | 镜像 / 来源 |
|---|---|---|
| Docker CE | 29.9.3 | 腾讯国内 Docker CE 源 |
| Compose | 5.7.0 | 腾讯国内 Docker CE 源 |
| MongoDB | 9.0.4 | docker.m.daocloud.io/library/mongo:9.0 |
| Redis | 8.10.3 | docker.m.daocloud.io/library/redis:8.10.3 |
| RabbitMQ | 4.2.9 | docker.m.daocloud.io/library/rabbitmq:4.2.9-management |
| Rabbit 延迟插件 | 4.2.0 | 官方发布资产 |
| Qdrant | 1.20.0 | docker.m.daocloud.io/qdrant/qdrant:v1.20.0 |
| Nacos | 3.3.0 | docker.m.daocloud.io/nacos/nacos-server:v3.3.0 |

Docker 安装源：`https://mirrors.tencent.com/docker-ce/linux/debian`。镜像使用国内 DaoCloud 路径。镜像可用性会变化，维护时核对实际镜像及 digest。

RabbitMQ 保留与原版延迟插件兼容的版本；“更新所有软件”不能跳过插件兼容性。Mongo 的 9.0 tag 是可移动标签，重建前记录实际 digest，不假设后续 pull 仍是 9.0.4。

## 3. 账号用途

| 服务 | 用户名 / 鉴权方式 | 用途 |
|---|---|---|
| SSH | root | 服务器管理 |
| MongoDB | root，authSource=admin | 已部署管理员；应用正式化时另设权限范围 |
| Redis | ACL root | default 已关闭，必须指定用户名 |
| RabbitMQ | root，vhost / | AMQP 与管理台 |
| Qdrant | 无传统用户名，api-key 请求头 | REST / gRPC 认证 |
| Nacos | root | LAMBDA_DEVELOPER 业务角色，非全局管理员 |
| Nacos | nacos | 原生全局管理员，管理账号与权限 |

实际密码与完整密钥只在本地私有资料和受控环境中保存。云 SecretId/SecretKey 是另一套凭据，不是中间件 root 密码。

修改 .env 不一定修改已经存在的数据库用户：Mongo / Rabbit 初始化变量主要用于空卷首次初始化；已有账号应通过服务自身管理接口改密，并同步应用。

## 4. 腾讯云防火墙端口清单

全部是 **TCP 原生端口，主机端口与服务端口一致**。

| 端口 | 服务 | 使用方 |
|---:|---|---|
| 22 | SSH | 管理电脑 |
| 27017 | MongoDB rs0 | Java / 数据库客户端 |
| 6379 | Redis | 应用 |
| 5672 | RabbitMQ AMQP | 发布者 / 消费者 |
| 15672 | RabbitMQ 管理后台 | 运维浏览器 |
| 6333 | Qdrant REST | 当前示例 |
| 6334 | Qdrant gRPC | gRPC 客户端，当前示例未使用 |
| 8080 | Nacos 3 控制台 | 浏览器 |
| 8848 | Nacos API / 配置 | 应用与管理 API |
| 9848 | Nacos 客户端 gRPC | Java SDK，不能漏开 |
| 8081 | 示例 HTTP | 仅把示例部署到服务器并对外监听时另行配置 |

前 10 个端口在历史部署验收中已验证可达。8081 是应用端口，不是已经随五项服务部署的必开端口。

腾讯云轻量服务器使用实例防火墙；CVM 还要检查对应安全组。规则之外同时检查主机防火墙、监听地址和 Compose ports。规则放通不代表容器正在监听。

Nacos 控制台不是 8848 页面，Qdrant 6334 也不能代替 REST 6333。集群扩容还需按产品文档规划节点间端口，本表只描述当前单机部署。

## 5. 日常巡检

登录后，在服务器执行：

~~~bash
cd /opt/app-services
docker version
docker compose version
docker compose ps
docker compose images
docker compose logs --tail=100 mongodb
docker compose logs --tail=100 redis
docker compose logs --tail=100 rabbitmq
docker compose logs --tail=100 qdrant
docker compose logs --tail=100 nacos
docker stats --no-stream
df -h
~~~

预期容器运行，日志没有持续认证失败 / 磁盘不足 / 连接循环。容器 running 不等于服务业务正常，仍需使用实际账号做最小读写或状态查询。

避免把 docker compose config 的完整输出直接公开，变量展开后可能带凭据。只查看服务名可用 `docker compose config --services`。

## 6. 启停与重建

~~~bash
cd /opt/app-services
docker compose restart nacos
docker compose logs --tail=100 nacos
docker compose ps
~~~

restart 只重启现有容器，不应用镜像和部分配置变更。确认变更后重新创建单个服务：

~~~bash
docker compose up -d nacos
~~~

所有服务设置 unless-stopped，Docker 已启用开机启动。历史验收没有执行整机重启测试。

日常不使用 `docker compose down -v`，该选项会删除命名卷。停止应用、停止容器、删除容器、删除数据卷是四种不同操作。

## 7. MongoDB 副本集检查

~~~bash
docker compose exec mongodb mongosh \
  --username root --authenticationDatabase admin --password
~~~

交互输入密码后：

~~~javascript
db.adminCommand({ping:1})
db.hello()
rs.status()
rs.conf()
~~~

确认 setName / _id 为 rs0、有 PRIMARY、成员地址从应用可达。当前单节点副本集支持事务，没有高可用。

部署记录：内部认证 keyfile 位于 /opt/app-services/mongodb/keyfile，只读挂载，权限 400。因使用 IP 作为成员地址，配置了 disableSplitHorizonIPCheck=true；没有因此关闭客户端认证。

有稳定 DNS 和内网拓扑后，应规划主机名、TLS、多个投票数据节点和恢复流程。不要把 directConnection=true 当作解决所有副本集地址问题的通用办法。

## 8. Redis / RabbitMQ / Qdrant / Nacos 检查

Redis：

~~~bash
docker compose exec redis redis-cli --user root --askpass PING
~~~

预期 PONG。用同一账号检查测试键，删除前限定资源前缀，不执行 FLUSHALL。

RabbitMQ：

~~~bash
docker compose exec rabbitmq rabbitmq-diagnostics ping
docker compose exec rabbitmq rabbitmq-plugins list -e
docker compose exec rabbitmq rabbitmqctl list_queues name messages_ready messages_unacknowledged consumers
~~~

确认 rabbitmq_delayed_message_exchange 已加载、队列消费者正常。消息堆积要区分 ready 与 unacknowledged；死信需人工或业务补偿，不是自动重试。

Qdrant 在应用电脑：

~~~bash
curl -fsS -H "api-key: $QDRANT_API_KEY" \
  "http://$SERVICE_HOST:6333/collections"
~~~

环境变量由私有配置注入。Nacos 用控制台检查应用配置和服务列表，确认 root 业务账号的权限。SDK 连接需要 9848，不能只测试 8848 登录。

## 9. 持久化与备份

命名卷记录：

- app-services_mongodb_data
- app-services_redis_data
- app-services_rabbitmq_data
- app-services_qdrant_data
- app-services_nacos_data
- app-services_nacos_logs

卷只提供持久化，不等于备份。备份应转移到独立故障域，记录校验值、版本、时间和恢复步骤。

| 服务 | 备份要点 | 恢复验证 |
|---|---|---|
| MongoDB | 使用兼容工具做一致备份；原始卷复制需停写/停库保证一致 | 独立环境恢复、索引、事务和数据抽查 |
| Redis | 按持久化配置保存 RDB/AOF，并包含 ACL | 启动、认证、关键键/TTL |
| RabbitMQ | definitions 是拓扑/账号，不等于消息备份 | 插件、拓扑、消息恢复策略分别验证 |
| Qdrant | 按集合/存储方式使用官方 snapshot | 维度、距离、点数量和查询 |
| Nacos | Derby 数据和必要配置；原始文件复制要保证一致 | 登录、配置、权限、服务注册 |

Mongo 从单机转 rs0 前的历史备份：
`/opt/app-services/backups/mongodb-before-rs0-20261107-230125/data-volume.tgz`。

该文件是转换前快照，不是实时备份，也不是已经完成恢复演练的证明。

## 10. 升级操作顺序

1. 记录当前镜像 digest、版本、卷和配置。
2. 检查 Rabbit 插件、Nacos 客户端、Mongo 驱动等兼容性。
3. 完成可恢复备份，先在独立环境演练。
4. 暂停受影响业务入口或安排维护窗口。
5. 拉取指定国内镜像并更新单个服务。
6. 查看日志、认证、读写和应用端到端行为。
7. 观察稳定后再升级下一项。
8. 回滚前确认新版本数据格式能被旧版本读取。

不使用没有固定版本的 latest 来代替可追溯升级，不把此次历史版本表宣称为日后始终最新。

## 11. 部署示例应用

当前服务器已部署的是中间件，示例此前在开发电脑运行验收。部署应用时还需准备 JDK 27 运行环境、JAR、私有配置、进程管理及业务入口。

建议步骤：

1. 框架 install，示例 package。
2. 保存提交号与 JAR 校验值。
3. 将 JAR 和私有配置交给部署目录。
4. 设置绑定地址、可达的 Nacos 注册 IP。
5. 通过进程管理器启动和停止，保存日志。
6. 验证 /api/system/info 以及真实业务请求。
7. 保留上一版本制品用于回退。

用户认证尚未接入，不能仅把绑定地址改为 0.0.0.0 就视为上线准备完成。

## 12. 数据清理

测试按独立数据库和前缀隔离，按 [TESTING](TESTING.md) 列清资源并删除。不要删除正式默认配置、共享账号、全部数据库或全部 Docker 卷。

产品审计日志和 Nacos 配置历史按自身保留策略处理，业务测试数据清理不等于删除审计记录。
