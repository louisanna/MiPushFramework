package top.trumeet.mipushframework.main

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.xiaomi.xmsf.R
import top.trumeet.mipush.provider.entities.RegisteredApplication

object RegistrationStateStyle {

    val ErrorColor = Color(0xFFF41804)
    val GreenColor = Color(0xff4caf50)
    val YellowColor = Color(0xffff9800)

    /**
     * 状态文案与颜色。参考 MiPush manager 的配色：
     * 绿=已注册、橙=未注册/异常、红=服务缺失或应用已卸载。
     */
    fun contentOf(app: RegisteredApplication, context: Context): Pair<String, Color> {
        if (!app.installed) {
            return Pair(shortLabel(app, context), ErrorColor)
        }
        val prefix =
            if (!app.existServices) context.getString(R.string.mipush_services_not_found) + " - "
            else ""
        val color = colorOf(app)
        return when (app.registeredType) {
            RegisteredApplication.RegisteredType.Registered -> {
                Pair(prefix + context.getString(R.string.app_registered), color)
            }

            RegisteredApplication.RegisteredType.Unregistered -> {
                Pair(prefix + context.getString(R.string.app_registered_error), color)
            }

//      RegisteredApplication.RegisteredType.NotRegistered
            else -> {
                Pair(prefix + context.getString(R.string.status_app_not_registered), color)
            }
        }
    }

    /**
     * 列表第二行使用的短状态（不带「服务缺失」前缀）。
     */
    fun shortLabel(app: RegisteredApplication, context: Context): String {
        if (!app.installed) {
            return context.getString(R.string.app_uninstalled)
        }
        val prefix =
            if (!app.existServices) context.getString(R.string.mipush_services_not_found) + " - "
            else ""
        return when (app.registeredType) {
            RegisteredApplication.RegisteredType.Registered ->
                prefix + context.getString(R.string.app_registered)

            RegisteredApplication.RegisteredType.Unregistered ->
                prefix + context.getString(R.string.app_registered_error)

            else -> prefix + context.getString(R.string.status_app_not_registered)
        }
    }

    fun colorOf(app: RegisteredApplication): Color {
        if (!app.installed) {
            return ErrorColor
        }
        return if (!app.existServices) ErrorColor
        else when (app.registeredType) {
            RegisteredApplication.RegisteredType.Registered -> {
                GreenColor
            }

            RegisteredApplication.RegisteredType.Unregistered -> {
                YellowColor
            }

//      RegisteredApplication.RegisteredType.NotRegistered
            else -> {
                Color.Unspecified
            }
        }
    }
}
