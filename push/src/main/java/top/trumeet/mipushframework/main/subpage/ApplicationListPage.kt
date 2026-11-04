package top.trumeet.mipushframework.main.subpage

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.elvishew.xlog.XLog
import com.xiaomi.xmsf.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.trumeet.common.utils.Utils
import top.trumeet.mipush.provider.db.EventDb
import top.trumeet.mipush.provider.db.RegisteredApplicationDb
import top.trumeet.mipush.provider.entities.RegisteredApplication
import top.trumeet.mipushframework.component.AppIcon
import top.trumeet.mipushframework.component.RefreshableLazyColumn
import top.trumeet.mipushframework.component.iconCache
import top.trumeet.mipushframework.main.RegistrationStateStyle
import top.trumeet.mipushframework.utils.ParseUtils
import top.trumeet.mipushframework.utils.ZygiskHelper

data class AppInfoForDisplay(
    val registrationState: Pair<String, Color>,
    val lastReceiveTime: String,
    val isZygisk: Boolean = false,
)

private var g_itemsInfo by mutableStateOf(emptyMap<String, AppInfoForDisplay>())
private var g_items by mutableStateOf(ApplicationPageOperation.MiPushApplications())

@Composable
fun ApplicationList(query: String) {
    val context = LocalContext.current
    ApplicationList(query) {
        val miPushApplications =
            ApplicationPageOperation.getMiPushApplicationsThatQueryMatched(query)
        ApplicationPageOperation.updateRegisteredApplicationDb(
            context,
            miPushApplications.res
        )
        miPushApplications
    }
}

@Composable
fun ApplicationList(
    query: String = "",
    getMiPushApplications: () -> ApplicationPageOperation.MiPushApplications
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    if (isPreview) g_items = getMiPushApplications()
    var isNeedRefresh by rememberSaveable(query) { mutableStateOf(true) }
    var pendingDelete by remember { mutableStateOf<RegisteredApplication?>(null) }

    val refreshScope = rememberCoroutineScope { Dispatchers.IO }
    val onRefresh: (onRefreshed: () -> Unit) -> Unit = { onRefreshed ->
        refreshScope.launch {
            val applications = getMiPushApplications()
            updateInfos(applications, context)
            withContext(Dispatchers.Main) {
                g_items = applications
                isNeedRefresh = false
                onRefreshed()
            }
            applications.res.forEach {
                iconCache.cache(it.packageName)
            }
        }
    }

    Page {
        pendingDelete?.let { app ->
            DeleteRegistrationDialog(
                app = app,
                onDismiss = { pendingDelete = null },
                onConfirm = {
                    pendingDelete = null
                    deleteRegistration(app)
                    onRefresh {}
                }
            )
        }
        RefreshableLazyColumn(onRefresh, { false }, onRefresh, isNeedRefresh) {
            item { StatsHeader(g_items) }
            items(g_items.res, { it.packageName }) { app ->
                ApplicationItem(app) {
                    if (app.installed) {
                        EventListPageUtils.startManagePermissions(
                            context,
                            app.packageName,
                            true
                        )
                    } else {
                        pendingDelete = app
                    }
                }
            }
            item {
                val notUseMiPushCount by remember { derivedStateOf { g_items.totalPkg - g_items.res.size } }
                Footer(notUseMiPushCount)
            }
        }
    }
}

/**
 * 删除「已卸载但仍注册」的应用留下的本地记录：
 * 数据库条目、事件历史、注册密钥、最近推送时间。
 * 服务器侧 regId 无法从这里注销（见 README），但之后框架不再为它保留任何信息。
 */
private fun deleteRegistration(app: RegisteredApplication) {
    RegisteredApplicationDb.delete(app.packageName)
    EventDb.deleteByPackage(app.packageName)
    Utils.setRegSec(app.packageName, "")
    Utils.setLastReceiveTime(app.packageName, 0L)
}

@Composable
private fun DeleteRegistrationDialog(
    app: RegisteredApplication,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(app.appName) },
        text = { Text(stringResource(R.string.app_uninstalled_desc, app.packageName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.action_delete_record))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
private fun StatsHeader(applications: ApplicationPageOperation.MiPushApplications) {
    val registered = applications.res.count {
        it.installed && it.registeredType == RegisteredApplication.RegisteredType.Registered
    }
    val uninstalled = applications.res.count { !it.installed }
    val notRegistered = applications.res.count { it.installed } - registered
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(registered.toString(), stringResource(R.string.app_registered), RegistrationStateStyle.GreenColor, Modifier.weight(1f))
        StatCard(notRegistered.toString(), stringResource(R.string.status_app_not_registered), RegistrationStateStyle.YellowColor, Modifier.weight(1f))
        StatCard(uninstalled.toString(), stringResource(R.string.app_uninstalled), RegistrationStateStyle.ErrorColor, Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        )
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                color = color
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

private fun updateInfos(
    applications: ApplicationPageOperation.MiPushApplications,
    context: Context
) {
    val zygiskPackages = try {
        ZygiskHelper.packages(context)
    } catch (t: Throwable) {
        emptySet<String>()
    }
    val infoMap = emptyMap<String, AppInfoForDisplay>().toMutableMap()
    applications.res.forEach {
        infoMap[it.packageName] = AppInfoForDisplay(
            registrationState = RegistrationStateStyle.contentOf(it, context),
            lastReceiveTime = if (it.lastReceiveTime.time == 0L) ""
            else context.getString(R.string.last_receive) + ParseUtils.getFriendlyDateString(
                it.lastReceiveTime,
                Utils.getUTC(),
                context
            ),
            isZygisk = zygiskPackages.contains(it.packageName),
        )
    }
    g_itemsInfo = infoMap
}

@Composable
private fun Footer(notUseMiPushCount: Int) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painterResource(R.drawable.ic_info_outline_black_24dp),
            null,
            tint = Color(0xFF757575),
            modifier = Modifier.padding(10.dp)
        )
        Text(
            ApplicationPageOperation.getNotSupportHint(
                context,
                notUseMiPushCount
            )
        )
    }
}

@Composable
private fun ApplicationItem(item: RegisteredApplication, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        )
    ) {
        Row(
            Modifier
                .clickable(onClick = onClick)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(item.packageName, item.appName, Modifier.size(44.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                AppInfo(item)
                Subtitle(item)
            }
            Spacer(Modifier.width(10.dp))
            val info = g_itemsInfo[item.packageName]
            if (info != null) {
                if (info.isZygisk) {
                    ZygiskBadge()
                    Spacer(Modifier.width(6.dp))
                }
                StatusPill(info.registrationState.first, info.registrationState.second)
            }
        }
    }
}

@Composable
private fun ZygiskBadge() {
    Box(
        Modifier
            .background(RegistrationStateStyle.GreenColor.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            stringResource(R.string.zygisk_badge),
            style = MaterialTheme.typography.labelSmall,
            color = RegistrationStateStyle.GreenColor,
            maxLines = 1,
        )
    }
}

@Composable
private fun Subtitle(item: RegisteredApplication) {
    val info = g_itemsInfo[item.packageName]!!
    val text = when {
        !item.installed -> item.packageName + " · " + stringResource(R.string.app_uninstalled_hint)
        info.lastReceiveTime.isNotEmpty() -> info.lastReceiveTime
        else -> item.packageName
    }
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun AppInfo(item: RegisteredApplication) {
    Text(
        item.appName,
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * 状态胶囊，配色沿用 MiPush manager：绿=已注册、橙=未注册、红=异常/已卸载。
 */
@Composable
private fun StatusPill(text: String, color: Color) {
    val contentColor =
        if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else color
    Box(
        Modifier
            .background(contentColor.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            maxLines = 1,
        )
    }
}


@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
    showSystemUi = true,
)
@Composable
fun ApplicationListPreview() {
    XLog.init()

    ApplicationList {
        val miPushApplications = ApplicationPageOperation.MiPushApplications()
        miPushApplications.res = listOf(
            registeredApplication(
                RegisteredApplication.RegisteredType.NotRegistered,
                "123"
            ),
            registeredApplication(
                RegisteredApplication.RegisteredType.Registered,
                "qwe"
            ),
            registeredApplication(
                RegisteredApplication.RegisteredType.Registered,
                "asd"
            ),
            registeredApplication(
                RegisteredApplication.RegisteredType.Unregistered,
                "zxc"
            ),
            registeredApplication(
                RegisteredApplication.RegisteredType.Unregistered,
                "456",
                false
            ),
        ) + ('a'..'z').map {
            registeredApplication(
                RegisteredApplication.RegisteredType.NotRegistered,
                it.toString()
            )
        }

        miPushApplications
    }
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
    showSystemUi = true,
)
@Composable
fun OneApplicationWithNonMiPushAppPreview() {
    XLog.init()

    ApplicationList {
        val miPushApplications = ApplicationPageOperation.MiPushApplications()
        miPushApplications.res = listOf(
            registeredApplication(
                RegisteredApplication.RegisteredType.NotRegistered,
                "123"
            )
        )
        miPushApplications.totalPkg = 100
        miPushApplications
    }
}

private fun registeredApplication(
    registeredType: Int,
    appName: String,
    existServices: Boolean = true
): RegisteredApplication {
    val registeredApplication =
        RegisteredApplication(
            null,
            appName,
            RegisteredApplication.Type.ASK,
            true,
            registeredType,
            appName
        )
    registeredApplication.existServices = existServices
    return registeredApplication
}
