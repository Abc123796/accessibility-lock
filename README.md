# 无障碍锁 · Accessibility Lock

[![build](https://github.com/USER/REPO/actions/workflows/build.yml/badge.svg)](https://github.com/USER/REPO/actions/workflows/build.yml)
[![license](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

> 国产 ROM 会在后台把无障碍服务悄悄关掉。这个 App 每 3 秒检查一次，一关就拨回来。

- 免 root，只需要一次 `adb` 授权
- **零网络权限**：不联网、不上传任何东西
- 40 KB 左右，没有第三方依赖（不用 AndroidX，纯平台 API）
- 最低 Android 7.0（API 24）

## 它解决什么

无障碍服务被系统杀掉或者被用户/系统关掉后，很多自动化、朗读、辅助类 App 就直接失效，
而且经常是「开关看着还开着，其实服务已经死了」。

本 App 常驻一个前台服务，周期性检查两件事：

1. `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` 里还有没有你选中的服务；
2. `Settings.Secure.ACCESSIBILITY_ENABLED` 总开关是不是被关了。

缺了就把开关写回去；写完 3 秒后再复核一次系统是否真的把服务绑上了，没绑上会明确告诉你
（通常意味着被 ROM 的「受限设置」拦了，光写设置没用）。

```
每 3s ──► 读 Settings.Secure ──► 和选中列表比对
                                   │ 缺/总开关被关
                                   ▼
                             写回设置（需 WRITE_SECURE_SETTINGS）
                                   │
                                   ▼
                        3s 后复核：系统真的绑上了吗？
```

## 安装

1. 下载 `app-release.apk` 安装（允许「未知来源」）。
2. 打开 App，点一下要守护的服务所在那行（出现 ✓ 即选中）。
3. 点「启动守护」。
4. 首次会提示授权，用下面任一方式执行一次：

```bash
adb shell pm grant cn.dsh.alaslock android.permission.WRITE_SECURE_SETTINGS
```

没有电脑的话，用 [Shizuku](https://shizuku.rikka.app/) 的终端执行同一行（去掉开头的 `adb shell`）。

5. 点「申请电池白名单」，并在系统设置里再给本 App 开自启动 / 后台无限制。

> 第 4 步是必须的：Android 里没这个权限，任何普通 App 都改不了无障碍总开关。

## 使用

| 位置 | 说明 |
| --- | --- |
| 状态胶囊 | 灰=已停止，绿=一切正常，黄=刚修过一次 |
| 服务列表 | 点整行切换是否守护 |
| 强制保护 | 默认开。手动在设置里关掉无障碍也会被拨回；关掉后只在服务被移出列表时补 |
| 强制重连 | 专治「开关开着但服务死了」：把服务摘下来 1.5 秒再挂回去，逼系统重新绑定 |
| 恢复次数 | 累计自动拨回多少次 |

通知栏常驻一条低优先级通知，上面有「暂停」可以直接停掉守护。

## 构建

```bash
export ANDROID_HOME=~/Android/Sdk        # 需含 platforms/android-34 与 build-tools
export ANDROID_JAR=$ANDROID_HOME/platforms/android-34/android.jar
export BUILD_TOOLS=$ANDROID_HOME/build-tools/34.0.0

./build.sh                                # 产物在 build/alock.apk
```

`build.sh` 会按顺序做：`aapt2 compile` → `aapt2 link` → `javac` → `d8` → `zipalign` → `apksigner`。
没有 Gradle，构建在几秒内完成，方便在低配机器上改。

签名：仓库不带签名库。本地首次构建会生成一个开发用 `ks.jks`（已在 `.gitignore` 里），
**发布用的 APK 请换成你自己的 key**，否则没法覆盖安装官方包。

### 单元测试

核心的开关字符串解析/合并逻辑（`LockLogic`）是纯函数，可以脱离 Android 直接跑：

```bash
javac -encoding UTF-8 --release 11 -d build/test \
  app/java/cn/dsh/alaslock/LockLogic.java test/LockLogicTest.java
java -cp build/test cn.dsh.alaslock.LockLogicTest
```

## 目录

```
app/AndroidManifest.xml              权限与组件声明
app/java/cn/dsh/alaslock/
  MainActivity.java                  界面
  GuardianService.java               前台守护、轮询、写回、复核
  GuardianJobService.java            15 分钟兜底自检
  BootReceiver.java                  开机自启
  StopReceiver.java                  通知栏「暂停」
  LockLogic.java                     无障碍开关串的纯函数逻辑（有单测）
  Prefs.java                         偏好与调度
  Ui.java                            颜色/控件工厂
app/res/                             图标与文案
test/LockLogicTest.java              23 条断言
build.sh                             无 Gradle 的构建脚本
.github/workflows/build.yml          CI：构建 + 跑单测
```

## 已知限制

- 必须一次性授权 `WRITE_SECURE_SETTINGS`，没有绕过去的路（除非 root 或 Shizuku 常驻）。
- 部分 ROM 的「受限设置 / 应用管控」会忽略外部写入，此时 App 会在通知里提示「写回了但系统没认」。
- 如果目标 App 被系统「强行停止」，有些 ROM 会把它标记成不可自启，锁只能把开关拨回去，
  仍需手动打开一次目标 App。
- 只在少数机型上验证过，欢迎提交 ROM 兼容性反馈（见 Issues 模板）。

## 为什么不做「自己点设置页」

有的工具用自家无障碍服务去模拟点击系统设置。本项目不做：那会要求本 App 拿到无障碍权限、
能读到整个屏幕内容，代价和风险都比写一个设置项大得多。写设置更小、更可审计。

## 贡献

- 报 Bug 请附：ROM 版本、目标服务、掉线时开关的实际状态（关掉 / 看着开着）、通知里的提示文案。
- 代码保持：无第三方依赖、无网络权限、核心逻辑可单测。

## License

[MIT](LICENSE)
