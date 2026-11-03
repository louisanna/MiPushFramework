package top.trumeet.mipushframework.main.subpage

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nihility.InternalMessenger
import com.xiaomi.push.service.XMPushServiceMessenger
import com.xiaomi.xmsf.R
import com.xiaomi.xmsf.SettingUtils
import com.xiaomi.xmsf.utils.ConfigCenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.trumeet.common.utils.Utils
import top.trumeet.mipushframework.MainPageOperation
import top.trumeet.mipushframework.component.SettingsGroup
import top.trumeet.mipushframework.component.SettingsItem
import top.trumeet.mipushframework.main.AdvancedSettingsPage
import top.trumeet.mipushframework.main.HelpPage
import top.trumeet.mipushframework.utils.ZygiskHelper
import top.trumeet.ui.theme.Theme

@Composable
fun Settings() {
    Theme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            color = MaterialTheme.colorScheme.background
        ) {
            SettingsScreen()
        }
    }
}


@Composable
private fun SettingsScreen() {
    Column {
        ServiceConfigurationBlock()
        MiPushBlock()
        DebugBlock()
        AboutBlock()
    }
}

@Composable
private fun MiPushBlock() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var snapshot by remember { mutableStateOf<ZygiskHelper.Snapshot?>(null) }
    var showProfileDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        snapshot = withContext(Dispatchers.IO) { ZygiskHelper.snapshot(true) }
    }

    val usable = snapshot?.usable == true

    SettingsGroup(title = stringResource(R.string.settings_mipush)) {
        SettingsItem(
            title = stringResource(R.string.settings_drop_uninstalled),
            summary = stringResource(R.string.settings_drop_uninstalled_summary),
            key = "DropMessagesForUninstalledPackages",
            defaultValue = true
        ) {}

        SettingsItem(
            title = stringResource(R.string.settings_zygisk),
            summary = if (usable) {
                stringResource(R.string.settings_zygisk_summary_ready, snapshot!!.packages.size)
            } else {
                stringResource(R.string.settings_zygisk_summary_unavailable)
            },
            key = "ZygiskForceRegister",
            defaultValue = true,
            enabled = usable
        ) {}

        SettingsItem(
            title = stringResource(R.string.settings_spoof_profile),
            summary = stringResource(
                R.string.settings_spoof_profile_summary,
                snapshot?.profile ?: "-"
            ),
            enabled = usable
        ) { showProfileDialog = true }
    }

    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text(stringResource(R.string.settings_spoof_profile_dialog)) },
            text = {
                Column {
                    ZygiskHelper.PROFILES.forEach { profile ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            ZygiskHelper.setProfile(context, profile)
                                        }
                                        snapshot = withContext(Dispatchers.IO) {
                                            ZygiskHelper.snapshot(true)
                                        }
                                        showProfileDialog = false
                                    }
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = snapshot?.profile == profile,
                                onClick = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(profile)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun ServiceConfigurationBlock() {
    val context = LocalContext.current

    SettingsGroup(title = stringResource(R.string.settings_service_setting)) {
        SettingsItem(
            title = stringResource(R.string.settings_service_advance_setting),
            summary = stringResource(R.string.settings_summary_service_advance_setting)
        ) {
            context.startActivity(Intent(context, AdvancedSettingsPage::class.java))
        }

        SetConfigurationsDirectory()
        SetXMPPServer(context)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SetXMPPServer(context: Context) {
    var currentXMPPServer by remember { mutableStateOf("") }
    object : InternalMessenger(context) {
        init {
            register(IntentFilter(XMPushServiceMessenger.IntentSetConnectionStatus))
            addListener { intent: Intent ->
                val host = intent.getStringExtra("host")
                if (host.isNullOrEmpty()) {
                    return@addListener
                }
                currentXMPPServer = host
            }

            send(Intent(XMPushServiceMessenger.IntentGetConnectionStatus))
        }
    }
    var text by remember { mutableStateOf(SettingUtils.getXMPPServer(context) ?: "") }
    SettingsItem(title = stringResource(R.string.settings_XMPP_server),
        summary = stringResource(R.string.settings_XMPP_server_summary) +
                "\nSet: [${SettingUtils.getXMPPServer(context) ?: ""}]" +
                "\nCurrent: [$currentXMPPServer]",
        confirmButton = { dismiss: () -> Unit ->
            TextButton(onClick = {
                SettingUtils.setXMPPServer(context, text)
                SettingUtils.sendXMPPReconnectRequest(context)
                currentXMPPServer = text
                dismiss()
            }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        onDismiss = {
            text = ""
        },
        content = {
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(SettingUtils.getXMPPServerHint()) },
                singleLine = true
            )
        })
}

@Composable
private fun SetConfigurationsDirectory() {
    val context = LocalContext.current
    var selectedDirectoryUri by remember {
        mutableStateOf(
            SettingUtils.getConfigurationDirectory(
                context
            )
        )
    }
    val openDocumentTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            selectedDirectoryUri = uri
            SettingUtils.setConfigurationDirectory(context, uri)
        }
    }
    SettingsItem(
        title = stringResource(R.string.settings_configuration_directory),
        summary = selectedDirectoryUri?.toString()
    ) {
        openDocumentTreeLauncher.launch(null) // 启动文件选择器
    }
}

@Composable
private fun DebugBlock() {
    val context = LocalContext.current

    SettingsGroup(title = stringResource(R.string.settings_debug)) {
        SettingsItem(
            title = stringResource(R.string.settings_get_log),
            summary = stringResource(R.string.settings_get_log_summary)
        ) {
            SettingUtils.shareLogs(context)
        }

        LogLevelItem()

        SettingsItem(
            title = stringResource(R.string.try_to_force_register_all_applications)
        ) {
            SettingUtils.tryForceRegisterAllApplications()
        }
    }
}

@Composable
private fun LogLevelItem() {
    val context = LocalContext.current
    val preferences = ConfigCenter.getSharedPreferences(context)
    val labels = listOf("ALL", "DEBUG", "INFO", "WARN", "ERROR", "NONE")
    var current by remember { mutableStateOf(preferences.getString("LogLevel", "0") ?: "0") }
    var showDialog by remember { mutableStateOf(false) }
    val currentIndex = current.toIntOrNull()?.coerceIn(0, labels.size - 1) ?: 0

    SettingsItem(
        title = stringResource(R.string.settings_log_level),
        summary = stringResource(R.string.settings_log_level_summary, labels[currentIndex])
    ) {
        showDialog = true
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.settings_log_level)) },
            text = {
                Column {
                    labels.forEachIndexed { index, label ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    preferences.edit().putString("LogLevel", index.toString()).apply()
                                    current = index.toString()
                                    showDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = index == currentIndex, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun AboutBlock() {
    val context = LocalContext.current
    val mainPageOperation = MainPageOperation(context)

    SettingsGroup(title = stringResource(R.string.action_about)) {
        SettingsItem(
            title = stringResource(R.string.helplib_title)
        ) {
            context.startActivity(Intent(context, HelpPage::class.java))
        }

        SettingsItem(
            title = stringResource(R.string.action_update)
        ) {
            mainPageOperation.gotoGitHubReleasePage()
            Toast.makeText(context, R.string.update_toast, Toast.LENGTH_LONG).show()
        }

        SettingsItem(
            title = stringResource(R.string.action_about)
        ) {
            mainPageOperation.showAboutDialog()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsPagePreview() {
    Utils.context = LocalContext.current
    Settings()
}

