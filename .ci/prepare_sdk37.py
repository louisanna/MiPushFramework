"""Prepare the tree for an SDK 37 / AGP 9 spike build (CI-only, not committed)."""

import io
import re

ROOT = "."


def rw(path, fn):
    s = io.open(path, encoding="utf-8", newline="").read()
    io.open(path, "w", encoding="utf-8", newline="").write(fn(s))


def patch_root(s):
    s = s.replace(
        "classpath 'com.android.tools.build:gradle:7.4.2'",
        "classpath 'com.android.tools.build:gradle:9.5.0'",
    )
    s = s.replace("ext.kotlin_version = '1.8.0'", "ext.kotlin_version = '2.5.0-Beta2'")
    s = s.replace("classpath 'io.github.wurensen:gradle-android-plugin-aspectjx:3.3.2'\n", "")
    s = re.sub(r"apply plugin: 'io\.github\.wurensen\.android-aspectjx'\n", "", s)
    s = re.sub(r"aspectjx\s*\{.*?\n\}\n", "", s, flags=re.S)
    return s


def patch_push(s):
    s = re.sub(r"compileSdkVersion \d+", "compileSdkVersion 37", s)
    s = re.sub(r"targetSdkVersion \d+", "targetSdkVersion 37", s)
    return s


def patch_wrapper(s):
    return re.sub(r"gradle-[0-9.]+-bin\.zip", "gradle-9.8.2-bin.zip", s)


def patch_proguard(s):
    return s.replace("proguard-android.txt", "proguard-android-optimize.txt")


def patch_namespace(path):
    s = io.open(path, encoding="utf-8", newline="").read()
    if "namespace " in s:
        return
    manifest = path.replace("build.gradle", "src/main/AndroidManifest.xml")
    try:
        mf = io.open(manifest, encoding="utf-8").read()
    except OSError:
        return
    m = re.search(r'package="([^"]+)"', mf)
    if not m:
        return
    s = re.sub(r"android\s*\{", "android {\n    namespace '" + m.group(1) + "'", s, count=1)
    io.open(path, "w", encoding="utf-8", newline="").write(s)


rw("build.gradle", patch_root)
rw("push/build.gradle", patch_push)
rw("gradle/wrapper/gradle-wrapper.properties", patch_wrapper)

for mod in ("push", "common", "condom", "mipush_hook"):
    path = mod + "/build.gradle"
    try:
        rw(path, patch_proguard)
        patch_namespace(path)
    except OSError:
        pass

print("prepared sdk37 tree")
