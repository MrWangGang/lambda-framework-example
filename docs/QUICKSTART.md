# 运行三个示例应用

1. 使用 JDK 27 和本机 Maven，在框架仓库运行 `./scripts/mvn.sh clean install`。
2. 复制示例仓库的 `.env.example` 并按 [配置](CONFIGURATION.md) 设置环境变量。
3. 分别运行 `./scripts/run-service-a.sh`、`./scripts/run-service-b.sh` 和 `./scripts/run-gateway.sh`。
4. 按 [README](../README.md) 获取令牌，从网关访问普通接口和 WSS。
5. 在各自终端按 Ctrl+C 退出应用。
