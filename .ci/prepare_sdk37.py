"""Prepare the tree for an SDK 37 / AGP 9 spike build (CI-only, not committed)."""

import io
import re

root = "."


def rw(path, fn):
    s = io.open(path, encoding="utf-8", newline="").read()
    s2 = fn(s)
    io.open(path, "w", encoding="utf-8", newline="").write(s2)


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


rw("build.gradle", patch_root)
rw("push/build.gradle", patch_push)
rw("gradle/wrapper/gradle-wrapper.properties", patch_wrapper)
print("prepared sdk37 tree")
