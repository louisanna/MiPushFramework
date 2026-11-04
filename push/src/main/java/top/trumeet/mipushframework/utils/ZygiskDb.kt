package top.trumeet.mipushframework.utils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.elvishew.xlog.XLog
import com.topjohnwu.superuser.Shell
import java.io.File

/**
 * 把 Zygisk 配置导出成 SQLite 数据库，供模块（Rust + rusqlite）直接读取：
 * /data/adb/mipush_zygisk/apps.db
 *
 * 数据库以 App 私有目录生成，再通过 root 原子替换到 /data/adb。
 * 模块在 DB 读不到时会回退到 app.conf（见 server.rs）。
 */
object ZygiskDb {
    private const val TAG = "ZygiskDb"
    const val DB_PATH = "/data/adb/mipush_zygisk/apps.db"

    fun export(context: Context, packages: Set<String>, profile: String?): Boolean {
        if (!ZygiskHelper.hasRoot()) return false
        return try {
            val file = context.getDatabasePath("zygisk_apps.db")
            file.parentFile?.mkdirs()
            if (file.exists()) file.delete()

            val db = SQLiteDatabase.openOrCreateDatabase(file, null)
            db.execSQL(
                "CREATE TABLE packages (" +
                        "package TEXT PRIMARY KEY, " +
                        "enabled INTEGER NOT NULL DEFAULT 1, " +
                        "profile TEXT, " +
                        "updated_at INTEGER NOT NULL DEFAULT 0)"
            )
            db.beginTransaction()
            val now = System.currentTimeMillis()
            for (pkg in packages) {
                db.execSQL(
                    "INSERT OR REPLACE INTO packages(package, enabled, profile, updated_at) VALUES(?, 1, ?, ?)",
                    arrayOf(pkg, profile, now)
                )
            }
            db.setTransactionSuccessful()
            db.endTransaction()
            db.close()

            val out = Shell.cmd(
                "cp '${file.absolutePath}' $DB_PATH.tmp",
                "chown 0:0 $DB_PATH.tmp",
                "chmod 600 $DB_PATH.tmp",
                "mv -f $DB_PATH.tmp $DB_PATH",
                "restorecon $DB_PATH 2>/dev/null || true"
            ).exec()
            file.delete()
            out.isSuccess
        } catch (t: Throwable) {
            XLog.tag(TAG).e("export db failed", t)
            false
        }
    }

    fun parseProfile(lines: List<String>): String? {
        for (raw in lines) {
            val line = raw.trim()
            if (line.startsWith("profile=")) {
                return line.substringAfter('=').trim().ifEmpty { null }
            }
        }
        return null
    }

    fun parsePackages(lines: List<String>): Set<String> {
        val result = LinkedHashSet<String>()
        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#") || line.contains('=')) continue
            result.add(line.substringBefore('|').trim())
        }
        return result
    }
}
