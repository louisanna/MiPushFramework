# L2 模块化方案（渐进式）

目标：把"推送连接/收包/通知入口"从 `:push` 中拆出编译期边界，仍打包进同一个 `com.xiaomi.xmsf` APK。

## 目标结构

```
:app                  （新）com.android.application，只做壳 + 资源合并 + 签名
:push                 （改）com.android.library，保留 UI/业务/配置
:connector            （新）com.android.library，XMPushService、收包、通知入口
:common               （不变）
:condom               （不变）
:mipush_hook          （不变，后续处理织入）
```

APK 仍是单包；只是构建单元拆分。

## 依赖方向（强约束）

```
:app        -> :push, :connector
:push       -> :connector-api, :common, :condom, :mipush_hook
:connector  -> :connector-api, :common, :condom
:connector-api -> 无（纯 Kotlin/Java 接口 + 数据类）
```

规则：

- `:connector` 不允许依赖 `:push`（禁止反向依赖）
- UI/Activity 只能在 `:push`/`:app`
- 通知构造通过 `:connector-api` 的接口回调给 `:push` 实现，或直接由 `:connector` 用 NotificationManager 发出（同一 app 身份，二选一）

## 落地步骤（每步可编译、可回滚）

1. **建 `:connector-api`**：只放接口（`PushMessageSink`、`ConnectorConfig`），空实现，`settings.gradle` 注册，构建通过。
2. **建 `:connector` 空壳**：一个空 Service + `AndroidManifest` 占位，依赖 `:connector-api`，构建通过。
3. **迁移 `XMPushService` 等类**：从 `:push` 移到 `:connector`，`push` 改为依赖 `connector-api`；用 `grep` 校验无反向引用。
4. **迁移通知入口**：`MyMIPushNotificationHelper` 的调用改走 `PushMessageSink` 实现（在 `:push` 或 `:connector`）。
5. **`push` 由 application 改 library**：新增 `:app` 壳 module，manifest 合并、applicationId、签名移到 `:app`。
6. **删除兼容层**：`connector-api` 里只留稳定接口，`push` 不再直接引用 connector 内部类。

## 验收标准

- 构建产物仍是单个 APK，包名 `com.xiaomi.xmsf`
- `./gradlew :app:assembleNormalRelease` 成功
- 杀 UI 进程后 `:connector` 存活（保持 L1 行为）
- 推送到达、通知正常
- `:connector` 的依赖里不出现 `:push`

## 风险

- 循环依赖：`XMPushService` 引用 `push` 的 ConfigCenter/DatabaseUtils → 需要先把这些下沉到 `:common` 或通过接口注入
- 资源：`connector` 无资源，通知文案/图标通过接口传入
- 织入：`mipush_hook` 的 aspect 依赖 `XMPushService` 等类，迁移后需同步更新织入目标
