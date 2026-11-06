"""Prepare the tree for an SDK 37 / AGP 9 spike build (CI-only, not committed)."""

import io
import re


def read(path):
    return io.open(path, encoding="utf-8", newline="").read()


def write(path, s):
    io.open(path, "w", encoding="utf-8", newline="").write(s)


def remove_blocks(s, keyword):
    """Remove `keyword { ... }` blocks with brace matching (handles nesting)."""
    out = []
    i = 0
    while True:
        m = re.search(r"\b" + keyword + r"\s*\{", s[i:])
        if not m:
            out.append(s[i:])
            break
        start = i + m.start()
        brace = i + m.end() - 1
        depth = 0
        j = brace
        while j < len(s):
            if s[j] == "{":
                depth += 1
            elif s[j] == "}":
                depth -= 1
                if depth == 0:
                    break
            j += 1
        # drop trailing newline too
        end = j + 1
        while end < len(s) and s[end] in "\r\n":
            end += 1
        out.append(s[i:start])
        i = end
    return "".join(out)


def patch_root(s):
    s = s.replace(
        "classpath 'com.android.tools.build:gradle:7.4.2'",
        "classpath 'com.android.tools.build:gradle:9.5.0'",
    )
    s = s.replace("ext.kotlin_version = '1.8.0'", "ext.kotlin_version = '2.5.0-Beta2'")
    s = s.replace('classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"',
                  'classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"\n        classpath "org.jetbrains.kotlin:compose-compiler-gradle-plugin:$kotlin_version"')
    s = s.replace("classpath 'io.github.wurensen:gradle-android-plugin-aspectjx:3.3.2'\n", "")
    s = s.replace("classpath 'org.greenrobot:greendao-gradle-plugin:3.3.0'\n", "")
    s = re.sub(r"\n\s*apply plugin: '[^']*aspectjx'", "", s)
    s = re.sub(r"\n\s*maven \{ url = uri\([^\n]*aliyun[^\n]*\) \}", "", s)
    s = re.sub(r"\n\s*maven \{ url = uri\([^\n]*jitpack[^\n]*\) \}", "", s)
    s = re.sub(r"\n\s*maven \{ url '[^']*jitpack[^']*' \}", "", s)
    repo_block = ("repositories {\n"
                  "        maven { url 'https://maven.aliyun.com/repository/google' }\n"
                  "        maven { url 'https://maven.aliyun.com/repository/public' }\n"
                  "        mavenCentral()\n"
                  "        google()\n"
                  "        maven { url 'https://jitpack.io' }\n"
                  "    }")
    s = re.sub(r"repositories\s*\{[^}]*\}", repo_block, s)
    s = remove_blocks(s, "aspectjx")
    s = remove_blocks(s, "greendao")
    return s


def patch_module(s):
    s = s.replace("compileSdkVersion rootProject.ext.compileSdkVersion", "compileSdk 37")
    s = s.replace("targetSdkVersion rootProject.ext.targetSdkVersion", "targetSdk 30")
    s = s.replace("minSdkVersion rootProject.ext.minSdkVersion", "minSdk 21")
    s = re.sub(r"compileSdkVersion\s+\d+", "compileSdk 37", s)
    s = re.sub(r"targetSdkVersion\s+\d+", "targetSdk 30", s)
    s = re.sub(r"minSdkVersion\s+\d+", "minSdk 21", s)
    s = re.sub(r"\n\s*apply plugin: '[^']*aspectjx'", "", s)
    s = re.sub(r"\n\s*apply plugin: 'org.greenrobot.greendao'", "", s)
    s = s.replace("apply plugin: 'kotlin-android'\n", "")
    s = s.replace("apply plugin: 'org.jetbrains.kotlin.android'\n", "")
    s = s.replace("proguard-android.txt", "proguard-android-optimize.txt")
    s = re.sub(r"\bclassifier\s*=", "archiveClassifier =", s)
    s = re.sub(r"\n\s*archivesBaseName\s*=.*", "", s)
    s = s.replace("compileOnly files(project(':mipush_hook').ext.mipushLib)",
                  'implementation files("../mipush_hook/libs/miuipushsdkshared_3_7_9.jar")')
    s = remove_blocks(s, "aspectjx")
    s = remove_blocks(s, "greendao")
    s = remove_blocks(s, r"android\.libraryVariants\.all")
    s = remove_blocks(s, r"android\.applicationVariants\.all")
    if "composeOptions" in s:
        s = remove_blocks(s, "composeOptions")
        s = "apply plugin: 'org.jetbrains.kotlin.plugin.compose'\n" + s
        s += """
androidComponents {
    beforeVariants(selector().all()) { variantBuilder ->
        variantBuilder.enableUnitTest = true
    }
}
"""
    return s


def add_namespace_and_buildconfig(path, s):
    if "namespace " not in s:
        manifest = path.replace("build.gradle", "src/main/AndroidManifest.xml")
        try:
            mf = read(manifest)
        except OSError:
            return s
        m = re.search(r'package="([^"]+)"', mf)
        if m:
            s = re.sub(r"android\s*\{", "android {\n    namespace '" + m.group(1) + "'", s, count=1)
    if "buildConfig true" not in s:
        s = re.sub(r"android\s*\{", "android {\n    buildFeatures {\n        buildConfig true\n    }", s, count=1)
    if "includeAndroidResources" not in s:
        s = re.sub(
            r"android\s*\{",
            "android {\n    testOptions {\n        unitTests {\n            includeAndroidResources = true\n        }\n    }",
            s,
            count=1,
        )
    return s


def patch_kts(path):
    s = read(path)
    s = s.replace('    id("io.github.wurensen.android-aspectjx")\n', "")
    s = s.replace('    id("org.jetbrains.kotlin.android")\n', "")
    s = remove_blocks(s, "aspectjx")
    s = remove_blocks(s, "kotlinOptions")
    s = re.sub(r"compileSdk\s*=\s*\d+", "compileSdk = 37", s)
    s += '\ndependencies {\n    implementation("org.aspectj:aspectjrt:1.9.22")\n}\n'
    write(path, s)


write("build.gradle", patch_root(read("build.gradle")))
write("gradle/wrapper/gradle-wrapper.properties",
      re.sub(r"gradle-[0-9.]+-bin\.zip", "gradle-9.8.2-bin.zip", read("gradle/wrapper/gradle-wrapper.properties")))

for mod in ("push", "common", "condom", "mipush_hook"):
    path = mod + "/build.gradle"
    try:
        s = patch_module(read(path))
        s = add_namespace_and_buildconfig(path, s)
        write(path, s)
    except OSError:
        pass

try:
    patch_kts("mipush_hook/build.gradle.kts")
except OSError:
    pass


def patch_link_dialog():
    path = "common/src/main/java/top/trumeet/common/widget/LinkAlertDialog.java"
    s = read(path)
    s2 = re.sub(
        r"\(int\) getContext\(\)\.getResources\(\)\s*\.getDimension\(R\.dimen\.abc_dialog_padding_material\)",
        "(int) (24 * getContext().getResources().getDisplayMetrics().density)",
        s,
    )
    if s2 != s:
        write(path, s2)


patch_link_dialog()

# 璁?:push 鐨?R 鍖呭惈渚濊禆妯″潡锛坈ommon锛夌殑璧勬簮锛氭棫 AGP 榛樿 false锛孉GP 8+ 榛樿 true
props_path = "gradle.properties"
props = read(props_path) if __import__("os").path.exists(props_path) else ""
if "nonTransitiveRClass" not in props:
    props += "\nandroid.nonTransitiveRClass=false\n"
    write(props_path, props)

def patch_manifest():
    path = "push/src/main/AndroidManifest.xml"
    import xml.etree.ElementTree as ET
    ns = "http://schemas.android.com/apk/res/android"
    ET.register_namespace("android", ns)
    tree = ET.parse(path)
    root = tree.getroot()
    changed = False
    for tag in ("activity", "service", "receiver"):
        for el in root.iter(tag):
            if el.find("intent-filter") is None:
                continue
            key = "{%s}exported" % ns
            if key not in el.attrib:
                el.set(key, "true")
                changed = True
    if changed:
        pass
    # 渚濊禆搴擄紙icebox sdk锛夌殑 receiver 娌℃湁 exported锛宼argetSdk 31+ 浼氳 manifest merger 鎷掔粷
    tools_ns = "http://schemas.android.com/tools"
    ET.register_namespace("tools", tools_ns)
    app = root.find("application")
    if app is not None:
        name = "com.catchingnow.icebox.sdk_client.StateReceiver"
        exists = any(el.get("{%s}name" % ns) == name for el in app.findall("receiver"))
        if not exists:
            rec = ET.SubElement(app, "receiver")
            rec.set("{%s}name" % ns, name)
            rec.set("{%s}exported" % ns, "true")
            rec.set("{%s}node" % tools_ns, "merge")
            changed = True

        # targetSdk 34+：startForeground 必须声明类型
        for svc in app.findall("service"):
            svc_name = svc.get("{%s}name" % ns)
            if svc_name in (".push.service.XMPushService",
                            "com.xiaomi.push.service.XMPushService"):
                if "{%s}foregroundServiceType" % ns not in svc.attrib:
                    svc.set("{%s}foregroundServiceType" % ns, "dataSync")
                    changed = True

    for perm in ("android.permission.FOREGROUND_SERVICE",
                 "android.permission.FOREGROUND_SERVICE_DATA_SYNC"):
        existing = [el for el in root.findall("uses-permission")
                    if el.get("{%s}name" % ns) == perm]
        if not existing:
            el = ET.Element("uses-permission")
            el.set("{%s}name" % ns, perm)
            root.append(el)
            changed = True
    if changed:
        tree.write(path, encoding="utf-8", xml_declaration=True)


patch_manifest()

push_path = "push/build.gradle"
push = read(push_path)
if "androidx.compose.foundation:foundation" not in push:
    push += ('\ndependencies {\n'
             '    implementation "androidx.compose.foundation:foundation:1.4.0"\n'
             '    implementation "org.aspectj:aspectjrt:1.9.22"\n'
             '}\n')
    write(push_path, push)

zygisk = "push/src/main/java/top/trumeet/mipushframework/utils/ZygiskDb.kt"
if __import__("os").path.exists(zygisk):
    zs = read(zygisk)
    zs2 = zs.replace("arrayOf(pkg, profile, now)", "arrayOf<Any?>(pkg, profile, now)")
    if zs2 != zs:
        write(zygisk, zs2)

print("prepared sdk37 tree")
import shutil
_generated = "push/build/generated/source/greendao"
if __import__("os").path.isdir(_generated):
    shutil.copytree(_generated, "push/src/main/java", dirs_exist_ok=True)
    print("copied generated greendao sources")

def patch_fgs():
    path = "push/src/main/java/com/nihility/service/ForegroundHelper.java"
    s = read(path)
    old = """        if (Global.ConfigCenter().isStartForegroundService()) {
            showForegroundNotificationToKeepAlive();
        } else {
            stopForegroundNotification();
        }"""
    new = """        showForegroundNotificationToKeepAlive();
        if (!Global.ConfigCenter().isStartForegroundService()) {
            stopForegroundNotification();
        }"""
    if old in s:
        write(path, s.replace(old, new))
        print("patched FGS fallback")


patch_fgs()

def patch_bridge_service():
    path = "push/src/main/java/com/xiaomi/xmsf/push/service/XMPushService.java"
    s = read(path)
    old = "        ContextCompat.startForegroundService(this, intent2);"
    new = "        startService(intent2);"
    if old in s:
        write(path, s.replace(old, new))
        print("patched bridge service start (no FGS)")


# patch_bridge_service()

def patch_foreground_starters():
    files = [
        "push/src/main/java/com/xiaomi/xmsf/push/service/receivers/KeepAliveReceiver.java",
        "push/src/main/java/com/xiaomi/xmsf/push/service/receivers/MiPushPingReceiver.java",
        "push/src/main/java/com/xiaomi/xmsf/push/service/receivers/NetworkStatusReceiver.java",
        "push/src/main/java/com/xiaomi/xmsf/push/service/receivers/PkgUninstallReceiver.java",
        "push/src/main/java/com/xiaomi/xmsf/push/control/PushControllerUtils.java",
    ]
    old_target = "com.xiaomi.push.service.XMPushService.class"
    new_target = "com.xiaomi.xmsf.push.service.XMPushService.class"
    changed = 0
    for path in files:
        s = read(path)
        s2 = s.replace(old_target, new_target)
        if s2 != s:
            write(path, s2)
            changed += 1
    print("patched foreground starters:", changed)


# patch_foreground_starters()


def patch_bridge_foreground():
    path = "push/src/main/java/com/xiaomi/xmsf/push/service/XMPushService.java"
    s = read(path)
    if "onStartCommand" in s:
        return
    s = s.replace(
        "import com.xiaomi.xmsf.utils.ConvertUtils;",
        "import com.nihility.service.ForegroundHelper;\nimport com.xiaomi.xmsf.utils.ConvertUtils;",
    )
    s = s.replace(
        "    @Override\n    protected void onHandleIntent(Intent intent) {",
        "    @Override\n    public int onStartCommand(Intent intent, int flags, int startId) {\n"
        "        // 被 startForegroundService 拉起时，必须在 5 秒内 startForeground\n"
        "        new ForegroundHelper(this).startForeground();\n"
        "        return super.onStartCommand(intent, flags, startId);\n"
        "    }\n\n"
        "    @Override\n    protected void onHandleIntent(Intent intent) {",
    )
    write(path, s)
    print("patched bridge foreground")


# patch_bridge_foreground()

def add_fgs_compat():
    path = "push/src/main/java/com/xiaomi/xmsf/push/service/FgsCompat.java"
    content = """package com.xiaomi.xmsf.push.service;

import android.app.Service;

/**
 * 由 ASM 注入到 SDK XMPushService.onStartCommand 开头，
 * 保证被 startForegroundService 拉起时 5 秒内调用 startForeground。
 */
public final class FgsCompat {
    private FgsCompat() {
    }

    public static void ensure(Service service) {
        try {
            new com.nihility.service.ForegroundHelper(service).startForeground();
        } catch (Throwable ignored) {
        }
    }
}
"""
    write(path, content)
    print("added FgsCompat")


add_fgs_compat()

# --- targetSdk 33+ 通知所需：权限、Hook provider、PendingIntent flags、aspects jar ---

def patch_notification_permission():
    path = "push/src/main/AndroidManifest.xml"
    s = read(path)
    if "POST_NOTIFICATIONS" not in s:
        s = s.replace("</manifest>",
                      '    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />\n</manifest>')
        write(path, s)
    if 'name="Hook"' not in s:
        block = """
        <provider
            android:name="androidx.startup.InitializationProvider"
            android:authorities="${applicationId}.androidx-startup"
            android:exported="false"
            tools:node="merge">
            <meta-data android:name="Hook" android:value="androidx.startup" />
        </provider>
"""
        s = read(path).replace("    </application>", block + "    </application>")
        write(path, s)
    print("notification manifest patched")


def patch_pending_intent_flags():
    files = [
        "push/src/main/java/com/xiaomi/push/service/MyMIPushNotificationHelper.java",
        "push/src/main/java/com/xiaomi/xmsf/push/notification/NotificationController.java",
        "push/src/main/java/com/xiaomi/xmsf/MiPushFrameworkApp.java",
    ]
    for path in files:
        s = read(path)
        s2 = s.replace("PendingIntent.FLAG_UPDATE_CURRENT",
                       "PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE")
        s2 = s2.replace("PendingIntent.FLAG_NO_CREATE",
                        "PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE")
        if s2 != s:
            write(path, s2)
    kt = "push/src/main/java/com/xiaomi/push/revival/NotificationsRevivalForSelfUpdated.kt"
    s = read(kt)
    s2 = s.replace("FLAG_UPDATE_CURRENT)", "FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)")
    s2 = s2.replace("FLAG_NO_CREATE)", "FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)")
    if s2 != s:
        write(kt, s2)
    print("pending intent flags patched")


def patch_aspects_dependency():
    path = "push/build.gradle"
    s = read(path)
    s2 = s.replace("implementation project(':mipush_hook')",
                   'implementation files("../mipush_hook/libs/mipush_hook_aspects.jar")')
    if s2 != s:
        write(path, s2)
    print("aspects jar dependency patched")


patch_notification_permission()
patch_pending_intent_flags()
patch_aspects_dependency()
