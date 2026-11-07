package top.trumeet.mipushframework.utils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.elvishew.xlog.XLog
import com.topjohnwu.superuser.Shell
import java.io.File

/**
 * 把 Zygisk 配置导出成 SQLite 数据库：
 * /data/adb/mipush_zygisk/apps.db
 *
 * 模块只读取该数据库（旧 app.conf 已废弃）。
 * 数据库以 App 私有目录生成，再通过 root 原子替换到 /data/adb。
 */
object ZygiskDb {
    private const val TAG = "ZygiskDb"
    const val DB_PATH = "/data/adb/mipush_zygisk/apps.db"
    private const val PUBLIC_DB = "/data/local/tmp/.mipush_apps_read.db"

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
                    arrayOf<Any?>(pkg, profile, now)
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

    /** 不依赖 Context 的读取（直接打开 /data/local/tmp 副本）。 */
    fun readDirect(): Pair<Set<String>, String?>? {
        if (!ZygiskHelper.hasRoot()) return null
        return try {
            val out = Shell.cmd(
                "cp $DB_PATH $PUBLIC_DB",
                "chmod 644 $PUBLIC_DB",
                "restorecon $PUBLIC_DB 2>/dev/null || true"
            ).exec()
            if (!out.isSuccess) return null
            val file = File(PUBLIC_DB)
            if (!file.exists()) return null
            val db = SQLiteDatabase.openDatabase(
                file.absolutePath, null, SQLiteDatabase.OPEN_READONLY
            )
            val packages = LinkedHashSet<String>()
            var profile: String? = null
            db.rawQuery("SELECT package, profile FROM packages WHERE enabled = 1", null).use { cursor ->
                while (cursor.moveToNext()) {
                    packages.add(cursor.getString(0))
                    if (profile == null) profile = cursor.getString(1)
                }
            }
            db.close()
            file.delete()
            packages to profile
        } catch (t: Throwable) {
            XLog.tag(TAG).e("read db failed", t)
            null
        }
    }

    /** 读取数据库内容；没有 root 或数据库不存在时返回 null。 */
    fun read(context: Context): Pair<Set<String>, String?>? {
        if (!ZygiskHelper.hasRoot()) return null
        return try {
            val out = Shell.cmd(
                "cp $DB_PATH $PUBLIC_DB",
                "chmod 644 $PUBLIC_DB",
                "restorecon $PUBLIC_DB 2>/dev/null || true"
            ).exec()
            if (!out.isSuccess) return null

            val source = File(PUBLIC_DB)
            if (!source.exists()) return null
            val local = File(context.cacheDir, "zygisk_apps_read.db")
            source.copyTo(local, overwrite = true)

            val db = SQLiteDatabase.openDatabase(
                local.absolutePath, null, SQLiteDatabase.OPEN_READONLY
            )
            val packages = LinkedHashSet<String>()
            var profile: String? = null
            db.rawQuery(
                "SELECT package, profile FROM packages WHERE enabled = 1", null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    packages.add(cursor.getString(0))
                    if (profile == null) {
                        profile = cursor.getString(1)
                    }
                }
            }
            db.close()
            local.delete()
            source.delete()
            packages to profile
        } catch (t: Throwable) {
            XLog.tag(TAG).e("read db failed", t)
            null
        }
    }
}
