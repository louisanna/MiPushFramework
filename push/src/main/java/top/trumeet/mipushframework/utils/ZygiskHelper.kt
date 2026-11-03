package top.trumeet.mipushframework.utils

import android.content.Context
import com.elvishew.xlog.XLog
import com.topjohnwu.superuser.Shell
import java.io.File

/**
 * MiPushZygisk 模块（/data/adb/mipush_zygisk）的读写封装。
 *
 * 模块本身在进程启动前读取 app.conf，给列表里的包伪造小米设备属性；
 * 这里只负责：读取状态/包列表、写 profile、增删强制注册的包。
 * 所有操作都需要 root（libsu），并且都是阻塞调用，必须放在 IO 线程。
 */
object ZygiskHelper {
    private const val TAG = "ZygiskHelper"
    const val CONFIG_DIR = "/data/adb/mipush_zygisk"
    const val CONFIG_FILE = "$CONFIG_DIR/app.conf"

    /** 模块只支持这两个伪装档位（见 bin/mipushctl）。 */
    val PROFILES = listOf("miui14", "os4")

    @Volatile
    private var cache: Snapshot? = null

    data class Snapshot(
        val hasRoot: Boolean,
        val modulePresent: Boolean,
        val profile: String?,
        val packages: Set<String>,
    ) {
        val usable: Boolean get() = hasRoot && modulePresent
    }

    fun hasRoot(): Boolean = try {
        Shell.isAppGrantedRoot() == true
    } catch (t: Throwable) {
        false
    }

    private fun shell(cmd: String): List<String> = try {
        val result = Shell.cmd(cmd).exec()
        if (result.isSuccess) result.out else emptyList()
    } catch (t: Throwable) {
        XLog.tag(TAG).e("shell failed: $cmd", t)
        emptyList()
    }

    fun snapshot(force: Boolean = false): Snapshot {
        cache?.let { if (!force) return it }
        val root = hasRoot()
        if (!root) {
            return Snapshot(false, false, null, emptySet()).also { cache = it }
        }
        val lines = shell("cat $CONFIG_FILE")
        val present = lines.isNotEmpty() || shell("test -d $CONFIG_DIR && echo yes").isNotEmpty()
        var profile: String? = null
        val packages = LinkedHashSet<String>()
        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            if (line.startsWith("profile=")) {
                profile = line.substringAfter('=').trim().ifEmpty { null }
                continue
            }
            if (line.contains('=')) continue
            packages.add(line.substringBefore('|').trim())
        }
        return Snapshot(root, present, profile, packages).also { cache = it }
    }

    fun packages(context: Context): Set<String> {
        // context 目前只用于日志/缓存语义，保留参数方便以后加 SAF 读取
        return snapshot().packages
    }

    fun addPackage(context: Context, pkg: String): Boolean = updateConfig(context) { lines ->
        val exists = lines.any { it.trim().substringBefore('|') == pkg }
        if (exists) return@updateConfig lines
        val result = ArrayList<String>(lines)
        val insertAt = result.indexOfLast { it.trim().startsWith("profile=") }
            .let { if (it >= 0) it + 1 else result.size }
        result.add(insertAt, pkg)
        result
    }

    fun removePackage(context: Context, pkg: String): Boolean = updateConfig(context) { lines ->
        lines.filterNot { it.trim().substringBefore('|') == pkg }
    }

    fun setProfile(context: Context, profile: String): Boolean = updateConfig(context) { lines ->
        val result = ArrayList<String>()
        var replaced = false
        for (line in lines) {
            if (line.trim().startsWith("profile=")) {
                if (!replaced) {
                    result.add("profile=$profile")
                    replaced = true
                }
            } else {
                result.add(line)
            }
        }
        if (!replaced) {
            result.add(0, "profile=$profile")
        }
        result
    }

    private fun updateConfig(context: Context, transform: (List<String>) -> List<String>): Boolean {
        if (!hasRoot()) return false
        val lines = shell("cat $CONFIG_FILE")
        if (lines.isEmpty()) return false
        val updated = transform(lines)
        if (updated == lines) return true
        return try {
            val tmp = File(context.cacheDir, "mipush_app.conf")
            tmp.writeText(updated.joinToString("\n") + "\n")
            val out = Shell.cmd(
                "cp ${tmp.absolutePath} $CONFIG_FILE",
                "chmod 600 $CONFIG_FILE",
                "restorecon $CONFIG_FILE 2>/dev/null || true"
            ).exec()
            tmp.delete()
            cache = null
            out.isSuccess
        } catch (t: Throwable) {
            XLog.tag(TAG).e("write config failed", t)
            false
        }
    }
}
