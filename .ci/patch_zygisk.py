"""Patch the upstream MiPushZygisk checkout for our fork (run in CI)."""

import pathlib
import re

src = pathlib.Path("src/module")

# 1) never spoof the framework package itself
config = src / "src/config.rs"
text = config.read_text(encoding="utf-8")
text = text.replace(
    'pub const XMSF_PACKAGE_NAME: &str = "com.xiaomi.xmsf";',
    'pub const XMSF_PACKAGE_NAME: &str = "com.xiaomi.xmsX";',
)
config.write_text(text, encoding="utf-8")

# 2) rusqlite for the apps.db config
cargo = src / "Cargo.toml"
text = cargo.read_text(encoding="utf-8")
if "rusqlite" not in text:
    text = text.replace(
        "[dependencies]",
        '[dependencies]\nrusqlite = { version = "0.31", features = ["bundled"] }',
        1,
    )
    cargo.write_text(text, encoding="utf-8")

# 3) read apps.db first, fall back to app.conf
server = src / "src/server.rs"
text = server.read_text(encoding="utf-8")

new_check = '''fn check_config(package_name: &str, process_name: &str) -> io::Result<bool> {
    if let Some(result) = check_db(package_name) {
        return Ok(result);
    }
    let content = match fs::read_to_string(crate::config::CONFIG_PATH) {
        Ok(content) => content,
        Err(err) if err.kind() == io::ErrorKind::NotFound => return Ok(false),
        Err(err) => return Err(err),
    };

    Ok(content
        .lines()
        .any(|line| matches_line(line, package_name, process_name)))
}

/// 优先读 App 导出的 SQLite 配置；DB 不存在或读失败时返回 None 回退 app.conf。
fn check_db(package_name: &str) -> Option<bool> {
    if !crate::config::is_managed_package(package_name) {
        return Some(false);
    }
    let db = rusqlite::Connection::open_with_flags(
        "/data/adb/mipush_zygisk/apps.db",
        rusqlite::OpenFlags::SQLITE_OPEN_READ_ONLY,
    )
    .ok()?;
    let enabled: Option<i64> = db
        .query_row(
            "SELECT enabled FROM packages WHERE package = ?1 LIMIT 1",
            [package_name],
            |row| row.get(0),
        )
        .ok();
    Some(enabled.unwrap_or(0) != 0)
}

fn matches_line('''

pattern = re.compile(r"fn check_config\(.*?\n\}\n\nfn matches_line\(", re.S)
if not pattern.search(text):
    raise SystemExit("check_config block not found")
text = pattern.sub(new_check, text, count=1)
server.write_text(text, encoding="utf-8")
print("patched config.rs / Cargo.toml / server.rs")
