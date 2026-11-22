# 第三方代码 / Third-party

## Shizuku API

- 文件：`aidl/moe/shizuku/server/*.aidl`（仅接口定义，来自 RikkaApps/Shizuku-API）
- 用途：以 shell 身份执行 `pm grant`，为 App 补上 WRITE_SECURE_SETTINGS
- 许可：Apache License 2.0，版权归 RikkaW / Shizuku 贡献者
- 上游：https://github.com/RikkaApps/Shizuku-API

构建时还会从 Maven Central 拉取 `dev.rikka.shizuku:api` 与 `:provider`（13.1.5）的 classes.jar，
同样遵循 Apache-2.0，仅作为编译/运行依赖，不修改。
