# SDK 37 升级分支（feat/android-17-target）

从 `fixMiPushFramework@96ea744` 拉出，目标：compileSdk/targetSdk 37。

## 目标版本组合（对齐 magisk317/MiPushFramework）

| 组件 | 目标 |
|---|---|
| compileSdk | 37 |
| targetSdk | 37 |
| AGP | 9.5.0（aliyun google 镜像已确认可拉取） |
| Gradle | 9.9 |
| Kotlin | 2.5.0-Beta2 |
| buildTools | 37.0.0 |
| Java | 27 |

当前：AGP 7.4.2 / Gradle 7.6 / Kotlin 1.8.0 / compileSdk 33 / targetSdk 30。

## 已知阻塞

1. **本机拿不到 Gradle 9.9 发行包**
   - services.gradle.org、阿里云/华为云/清华/腾讯镜像的 `gradle-9.9-bin.zip` 全部超时或 404；
   - 应对：用 GitHub Actions（runner 有网）做编译 spike，把日志提交到 `build-artifacts` 分支再取回。
2. **AspectJX 3.3.2** 不兼容 AGP 9 / Gradle 9，需要替换织入方案。
3. **GreenDAO 3.3.0** 与 Kotlin 2.x 存疑，可能迁 Room。
4. Compose BOM 需要升级到支持 Kotlin 2.5 的版本，Material3 API 有 breaking change。
5. 本机 JDK 需要 27（当前是 11/17 混用）。

## 执行顺序

1. 在 CI 上跑一次最小构建：Gradle 9.9 + AGP 9.5 + Kotlin 2.5 + compileSdk 37，只编 `:common`；
2. 逐个模块加回（`:condom` → `:mipush_hook` → `:push`），记录每个失败点；
3. 替换 AspectJX（ASM transform 或运行时 hook）；
4. 行为适配：FGS 类型、通知权限、edge-to-edge、16KB page（.so 已按 16KB 编）；
5. 全量构建通过后合回主线。

## 下一步（待办）

- [ ] 新增 `.github/workflows/sdk37-spike.yml`：用 Gradle 9.9 + AGP 9.5 编译分支，日志提交到 `build-artifacts`
- [ ] 首次运行并收集错误清单
- [ ] 按错误清单逐项修复
