CONFIG_DIR=/data/adb/mipush_zygisk
DB_FILE=$CONFIG_DIR/apps.db
DEFAULT_DB=$MODPATH/defaults/apps.db

mkdir -p "$CONFIG_DIR"
chmod 700 "$CONFIG_DIR"

if [ ! -f "$DB_FILE" ]; then
  if [ -f "$DEFAULT_DB" ]; then
    cp "$DEFAULT_DB" "$DB_FILE"
    chmod 600 "$DB_FILE"
  else
    ui_print "!! defaults/apps.db missing, module will not spoof anything"
  fi
fi

# 清理历史遗留的 app.conf（新版本只读 apps.db）
if [ -f "$CONFIG_DIR/app.conf" ]; then
  rm -f "$CONFIG_DIR/app.conf"
fi
if [ -f /data/local/tmp/mipush_zygisk_app.conf ]; then
  rm -f /data/local/tmp/mipush_zygisk_app.conf
fi

ui_print "MiPush Zygisk config: $DB_FILE"
