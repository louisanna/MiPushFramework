"""Package the spoof-only MiPushZygisk module (run inside the CI checkout)."""

import pathlib
import zipfile

src = pathlib.Path("src")
out_dir = pathlib.Path("out")
out_dir.mkdir(exist_ok=True)
out = out_dir / "MiPushZygisk_spoofonly.zip"

entries = {}
for name in ("META-INF/com/google/android/update-binary",
             "META-INF/com/google/android/updater-script"):
    entries[name] = (src / "magisk" / name).read_bytes()

entries["module.prop"] = (
    "id=mipush_zygisk\n"
    "name=MiPush Zygisk (spoof only)\n"
    "version=v0.6.1-spoof-only\n"
    "versionCode=21\n"
    "author=magisk317 / patched\n"
    "description=Spoof Xiaomi props for selected apps; framework process excluded, no MiPushCut\n"
).encode()

entries["customize.sh"] = """# MiPush Zygisk (spoof only)
ui_print "- MiPush Zygisk: spoof only (MiPushCut disabled, framework excluded)"
CONFIG_DIR=/data/adb/mipush_zygisk
mkdir -p "$CONFIG_DIR"
[ -f "$CONFIG_DIR/app.conf" ] || cp "$MODPATH/defaults/app.conf" "$CONFIG_DIR/app.conf"
if [ -f "$MODPATH/defaults/device.conf" ]; then
  [ -f "$CONFIG_DIR/device.conf" ] || cp "$MODPATH/defaults/device.conf" "$CONFIG_DIR/device.conf"
else
  [ -f "$CONFIG_DIR/device.conf" ] || : > "$CONFIG_DIR/device.conf"
fi
chmod 600 "$CONFIG_DIR/app.conf" "$CONFIG_DIR/device.conf" 2>/dev/null || true
set_perm_recursive "$MODPATH" 0 0 0755 0644
""".encode()

entries["uninstall.sh"] = "#!/system/bin/sh\nrm -rf /data/adb/mipush_zygisk 2>/dev/null\nexit 0\n".encode()
entries["defaults/app.conf"] = (src / "magisk/defaults/app.conf").read_bytes()

device_conf = src / "magisk/defaults/device.conf"
entries["defaults/device.conf"] = device_conf.read_bytes() if device_conf.exists() else (
    "# MiPush Zygisk device overrides\n"
).encode()

ctl = src / "magisk/bin/mipushctl"
if ctl.exists():
    entries["bin/mipushctl"] = ctl.read_bytes()

entries["zygisk/arm64-v8a.so"] = pathlib.Path(
    "src/module/target/aarch64-linux-android/release/libmipush_zygisk.so").read_bytes()
entries["zygisk/armeabi-v7a.so"] = pathlib.Path(
    "src/module/target/armv7-linux-androideabi/release/libmipush_zygisk.so").read_bytes()

with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
    for name, data in entries.items():
        z.writestr(name, data)

print("built", out, out.stat().st_size)
