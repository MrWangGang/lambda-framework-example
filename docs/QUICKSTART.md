# 首次运行：从本地工程到真实服务

[返回总览](../README.md) · [配置](CONFIGURATION.md) · [API](API.md) · [排错](TROUBLESHOOTING.md)

## 1. 先确认手上有什么

需要已有：

- JDK 27；不使用 JRE 或旧 JDK 编译。
- IntelliJ IDEA 内置 Maven，或另一个已安装 Maven。
- 同级目录的 lambda-framework 和 lambda-framework-example。
- 远程 MongoDB、Redis、RabbitMQ、Nacos、Qdrant 的连接信息。
- Nacos 业务账号能够读取配置、注册和注销实例。

服务器详情与实际凭据在本地私有《开发规范.md》。本手册用占位符说明操作，不包含私钥或生产口令。

## 2. 安装框架到本机 Maven 缓存

~~~bash
cd ../lambda-framework
export JAVA_HOME='/实际的/JDK27/Contents/Home'
export PATH="$JAVA_HOME/bin:$PATH"
./scripts/mvn.sh -version
./scripts/mvn.sh -B clean install
cd ../lambda-framework-example
~~~

预期 Maven 显示 Java 27，框架所有模块 SUCCESS。install 不是上传到远程 Maven 仓库。

若提示找不到 Maven：

~~~bash
export MAVEN_BIN='/实际的/IntelliJ IDEA.app/Contents/plugins/maven-plugin/lib/maven3/bin/mvn'
./scripts/mvn.sh -version
~~~

脚本不会安装 Maven。

## 3. 编辑 .env

首次创建；已有文件时直接编辑，不覆盖现有凭据：

~~~bash
cp .env.example .env
chmod 600 .env
~~~

内容示意：

~~~bash
JAVA_HOME='/实际的/JDK27/Contents/Home'
SERVICE_HOST='中间件服务器地址'
SERVICE_USER='root'
SERVICE_PASSWORD='实际服务密码'
NACOS_NAMESPACE='public'
NACOS_GROUP='LAMBDA_EXAMPLE'
MONGODB_REPLICA_SET='rs0'
MONGO_DATABASE='lambda_example'
EXAMPLE_RESOURCE_PREFIX='lambda_example'
APP_NAME='lambda-example-app'
APP_BIND_ADDRESS='127.0.0.1'
APP_REGISTER_IP='127.0.0.1'
APP_PORT='8081'
~~~

密码含空格、逗号、斜杠等时保留正确的 shell 引号；不要对独立 password 属性做 URL 编码。密码含单引号时应按 shell 规则处理，不能直接复制到上述单引号中。

.env 会被 shell source，只应包含可信的变量赋值，不是普通 JSON 配置。不要放入命令替换、下载或执行语句。

## 4. 检查服务原生端口

在应用电脑设置并执行：

~~~bash
export SERVICE_HOST='中间件服务器地址'
nc -vz "$SERVICE_HOST" 27017
nc -vz "$SERVICE_HOST" 6379
nc -vz "$SERVICE_HOST" 5672
nc -vz "$SERVICE_HOST" 6333
nc -vz "$SERVICE_HOST" 8848
nc -vz "$SERVICE_HOST" 9848
~~~

TCP 可达只说明网络通路，不能证明账号、插件和副本集正确。工具不存在时可用系统已有的网络检查方式，不要求额外安装。

服务端应确认：

- MongoDB rs0 存在 PRIMARY，通告的成员地址可从本机访问。
- Redis 的 root ACL 可登录，default 用户状态符合部署配置。
- RabbitMQ 延迟插件已启用。
- Qdrant 携带 api-key 能访问集合接口。
- Nacos 8848 和 9848 均可达；8080 仅用于控制台。

## 5. 创建 Nacos 启动配置

打开部署环境的 Nacos 控制台，例如 `http://NACOS_HOST:8080`。业务账号在授权范围内创建配置：

| 项目 | 默认值 |
|---|---|
| namespace | public |
| group | LAMBDA_EXAMPLE |
| Data ID | lambda-example-app.properties |
| 配置类型 | Properties |

~~~properties
example.greeting=Hello from Nacos
~~~

如果 APP_NAME 改为 catalog-demo，则 Data ID 也改为 catalog-demo.properties。namespace 使用 ID；自定义 namespace 的显示名与 ID 可能不同。

当前 spring.config.import 没有 optional 前缀，远程配置是启动必需项。不要为了让应用“先启动”而静默跳过必需配置。

## 6. 启动与确认

在示例根目录运行：

~~~bash
./scripts/run.sh
~~~

脚本顺序：

1. 切到仓库根目录。
2. 读取 .env，导出其中变量。
3. 检查 JAVA_HOME、SERVICE_HOST、SERVICE_PASSWORD 非空。
4. 使用已有 Maven 构建 app 及上游模块。
5. 用 JAVA_HOME 下的 java 运行 JAR。

默认 JAR：
`lambda-example-app/target/lambda-example-app-1.0.0-SNAPSHOT.jar`。

启动会创建 Mongo 索引、Rabbit 交换机/队列与 Qdrant 集合，并开始消费消息。这些步骤各有等待上限，基础设施不可用会导致启动失败或异常。

另开终端：

~~~bash
curl -i http://127.0.0.1:8081/api/info
~~~

预期 JSON：

~~~json
{
  "application":"lambda-example-app",
  "greeting":"Hello from Nacos",
  "replicaSet":"rs0"
}
~~~

这里只能证明 HTTP 与配置读取正常。下一步按 API 创建商品，并验证 Redis、消息和向量接口。

## 7. 动态刷新验证

1. 在 Nacos 修改 example.greeting 并发布。
2. 等待客户端收到配置变更。
3. 再次请求 /api/info。
4. 确认 greeting 变更，application 和 replicaSet 不应因该操作变化。

不是所有配置都支持运行中重建连接。数据库地址、凭据、资源前缀等变更通常需要有计划地重启应用并处理旧资源。

## 8. IDEA 直接启动

- 导入根 pom.xml。
- 设置 Project SDK、Maven Runner、Importer 使用 JDK 27。
- 主类为 `org.lambda.example.app.Application`。
- Run Configuration 填写对应环境变量。
- 不把 .env 原样贴入 Program arguments；IDEA 默认不会自动 source 它。
- VM options 可以按脚本设置 `--enable-native-access=ALL-UNNAMED`。

启动结果以实际日志和业务请求为准。

## 9. 对外访问与 Nacos 注册

默认仅本机访问。需要其他机器访问时：

~~~bash
APP_BIND_ADDRESS='0.0.0.0'
APP_REGISTER_IP='调用方可到达的应用主机IP'
APP_PORT='8081'
~~~

server.address 决定绑定地址；注册 IP 告诉其他客户端去哪里连接。不要注册 0.0.0.0，也不要在远程部署时让调用方误连自己的 127.0.0.1。

示例没有业务认证，开放 HTTP 前先明确访问范围。应用端口 8081 和 Nacos 控制台 8080 是两个不同服务。

## 10. 停止与清理

前台运行用 Ctrl+C，等待关闭完成。确认 Nacos 实例注销、MQ 消费停止以及进程退出。

停止不会删除 Mongo 文档、Redis 键、Rabbit 消息或 Qdrant 向量。验收完成按 [TESTING](TESTING.md) 清理专用资源；不要删除共享数据卷。

清理编译产物：

~~~bash
./scripts/mvn.sh clean
~~~

再次启动时 run.sh 会重新构建。
