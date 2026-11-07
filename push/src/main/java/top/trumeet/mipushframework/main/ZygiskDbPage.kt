package top.trumeet.mipushframework.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.trumeet.mipushframework.utils.ZygiskHelper
import top.trumeet.ui.theme.Theme

/**
 * apps.db 查看/编辑页面：
 * 列出模块当前伪装的应用，支持添加、删除、切换 profile。
 * 需要 root 且已刷入 MiPush Zygisk 模块。
 */
class ZygiskDbPage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Theme { ZygiskDbScreen() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZygiskDbScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var snapshot by remember { mutableStateOf<ZygiskHelper.Snapshot?>(null) }
    var loading by remember { mutableStateOf(true) }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    fun reload() {
        loading = true
        scope.launch {
            val data = withContext(Dispatchers.IO) { ZygiskHelper.snapshot(force = true) }
            snapshot = data
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    val data = snapshot
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("apps.db", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))

            if (data == null || loading) {
                Text("读取中…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }

            if (!data.usable) {
                Text(
                    "未检测到 Magisk 模块或 root 权限",
                    color = MaterialTheme.colorScheme.error
                )
                return@Column
            }

            Text(
                "共 ${data.packages.size} 个应用，profile=${data.profile ?: "默认"}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("包名") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.height(0.dp))
                Button(
                    enabled = input.isNotBlank() && !busy,
                    onClick = {
                        val pkg = input.trim()
                        busy = true
                        scope.launch {
                            withContext(Dispatchers.IO) { ZygiskHelper.addPackage(context, pkg) }
                            input = ""
                            busy = false
                            reload()
                        }
                    },
                    modifier = Modifier.padding(start = 8.dp)
                ) { Text("添加") }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZygiskHelper.PROFILES.forEach { profile ->
                    FilterChip(
                        selected = data.profile == profile,
                        enabled = !busy,
                        onClick = {
                            busy = true
                            scope.launch {
                                withContext(Dispatchers.IO) { ZygiskHelper.setProfile(context, profile) }
                                busy = false
                                reload()
                            }
                        },
                        label = { Text(profile) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            LazyColumn(Modifier.fillMaxWidth()) {
                items(data.packages.sorted()) { pkg ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(pkg, modifier = Modifier.weight(1f))
                        IconButton(
                            enabled = !busy,
                            onClick = {
                                busy = true
                                scope.launch {
                                    withContext(Dispatchers.IO) { ZygiskHelper.removePackage(context, pkg) }
                                    busy = false
                                    reload()
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "移除"
                            )
                        }
                    }
                }
            }
        }
    }
}
