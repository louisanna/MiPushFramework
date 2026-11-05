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
    repo_block = ("repositories {\n        google()\n        mavenCentral()\n"
                  "        maven { url 'https://jitpack.io' }\n"
                  "        maven { url 'https://maven.aliyun.com/repository/public' }\n"
                  "        gradlePluginPortal()\n    }")
    s = re.sub(r"repositories\s*\{[^}]*\}", repo_block, s)
    s = remove_blocks(s, "aspectjx")
    s = remove_blocks(s, "greendao")
    return s


def patch_module(s):
    s = re.sub(r"compileSdkVersion\s+\d+", "compileSdk 37", s)
    s = re.sub(r"targetSdkVersion\s+\d+", "targetSdk 37", s)
    s = re.sub(r"minSdkVersion\s+\d+", "minSdk 21", s)
    s = re.sub(r"\n\s*apply plugin: '[^']*aspectjx'", "", s)
    s = re.sub(r"\n\s*apply plugin: 'org.greenrobot.greendao'", "", s)
    s = s.replace("apply plugin: 'kotlin-android'\n", "")
    s = s.replace("apply plugin: 'org.jetbrains.kotlin.android'\n", "")
    s = s.replace("proguard-android.txt", "proguard-android-optimize.txt")
    s = re.sub(r"\bclassifier\s*=", "archiveClassifier =", s)
    s = re.sub(r"\n\s*archivesBaseName\s*=.*", "", s)
    s = s.replace("compileOnly files(project(':mipush_hook').ext.mipushLib)",
                  'compileOnly files("../mipush_hook/libs/miuipushsdkshared_3_7_9.jar")')
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
print("prepared sdk37 tree")
