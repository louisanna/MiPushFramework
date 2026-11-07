# L2 迁移计划


## 进展（2026-11-07）

已迁移到 :connector（均构建+装机验证）：
- 接收器：MiPushPing、NetworkStatus、PkgUninstall、Boot、NotificationEvent、KeepAlive、AccountChanged、MiuiPushMessage、MiuiPushActivateService
- 组件：XMAccountManager、ConnectorService、ConnectorForeground、ConnectorPreferences、ConnectorLogger/Environment/PushServiceLauncher

## 当前阻塞：bridge XMPushService

bridge 依赖链：
XMPushService → Configurations / IconConfigurations / ConfigurationsLoader / PackageConfig
             → com.nihility.Global（配置中心单例，push 内部）
             → R.string（资源）、ConvertUtils（SDK thrift）

因此 bridge 迁移前必须先建立 **:config 模块**（或把 Global+Configurations 系列下沉到 :connector），否则会形成反向依赖。

下一步建议：
1. 新建 :config（或 :common 扩展）承载 Configurations 系列 + Singleton
2. Global 拆分：配置部分下沉，hook 部分留在 :push
3. bridge 迁移到 :connector 并注入配置接口
4. Step 4：去掉 :push → :connector 临时依赖，改为 :app 壳统一依赖
