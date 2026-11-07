package top.trumeet.mipushframework.utils

import android.content.Context
import com.elvishew.xlog.XLog
import com.topjohnwu.superuser.Shell

/**
 * MiPushZygisk 模块（/data/adb/mipush_zygisk）的读写封装。
 *
 * 模块只读取 SQLite 数据库 apps.db（旧 app.conf 已废弃），
 * 这里负责：读取状态/包列表、写 profile、增删包。
 * 所有操作都需要 root（libsu），并且都是阻塞调用，必须放在 IO 线程。
 */
object ZygiskHelper {
    private const val TAG = "ZygiskHelper"
    const val CONFIG_DIR = "/data/adb/mipush_zygisk"

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
        val present = root && shell("test -d $CONFIG_DIR && echo yes").isNotEmpty()
        var profile: String? = null
        val packages = LinkedHashSet<String>()
        if (present) {
            val data = ZygiskDb.readDirect()
            if (data != null) {
                profile = data.second
                packages.addAll(data.first)
            }
        }
        return Snapshot(root, present, profile, packages).also { cache = it }
    }

    fun packages(context: Context): Set<String> {
        // context 目前只用于日志/缓存语义，保留参数方便以后扩展
        return snapshot().packages
    }

    fun addPackage(context: Context, pkg: String): Boolean = update(context) { packages, profile ->
        packages.add(pkg)
        packages to profile
    }

    fun removePackage(context: Context, pkg: String): Boolean = update(context) { packages, profile ->
        packages.remove(pkg)
        packages to profile
    }

    fun setProfile(context: Context, profile: String): Boolean = update(context) { packages, _ ->
        packages to profile
    }

    private fun update(
        context: Context,
        transform: (MutableSet<String>, String?) -> Pair<Set<String>, String?>
    ): Boolean {
        if (!hasRoot()) return false
        val data = ZygiskDb.read(context) ?: return false
        val packages = LinkedHashSet(data.first)
        val (updated, profile) = transform(packages, data.second)
        val ok = ZygiskDb.export(context, updated, profile)
        if (ok) cache = null
        return ok
    }
}
