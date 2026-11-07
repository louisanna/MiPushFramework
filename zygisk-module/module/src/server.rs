use std::{
    fs,
    io::{self, BufRead, BufReader, Write},
    os::unix::net::UnixStream,
    sync::Once,
};

use android_logger::Config;
use log::{debug, error, LevelFilter};

pub const DB_PATH: &str = "/data/adb/mipush_zygisk/apps.db";

pub fn companion_handler(stream: &mut UnixStream) {
    static LOG_INIT: Once = Once::new();
    LOG_INIT.call_once(|| {
        android_logger::init_once(
            Config::default()
                .with_max_level(LevelFilter::Debug)
                .with_tag("MiPushZygiskServer"),
        );
    });

    if let Err(err) = handle_query(stream) {
        error!("companion query failed: {err}");
        let _ = stream.write_all(&[0u8]);
    }
}

fn handle_query(stream: &mut UnixStream) -> io::Result<()> {
    let mut reader = BufReader::new(&mut *stream);

    let mut package_name = String::new();
    reader.read_line(&mut package_name)?;
    let package_name = package_name.trim_end();

    let mut process_name = String::new();
    reader.read_line(&mut process_name)?;
    let process_name = process_name.trim_end();

    let should_hook = check_config(package_name, process_name)?;
    debug!("query pkg={package_name} process={process_name} should_hook={should_hook}");

    stream.write_all(&[should_hook as u8])?;
    stream.flush()?;

    Ok(())
}

fn check_config(package_name: &str, process_name: &str) -> io::Result<bool> {
    // 优先读取 SQLite 数据库（应用导出的唯一数据源）；
    // 读不到或打开失败时回退到旧的 app.conf，保证兼容。
    match check_database(package_name) {
        Ok(Some(hit)) => return Ok(hit),
        Ok(None) => {}
        Err(err) => debug!("read database failed: {err}"),
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

/// 返回 Ok(None) 表示数据库不可用（不存在/没有表），交给 app.conf 回退。
fn check_database(package_name: &str) -> Result<Option<bool>, Box<dyn std::error::Error>> {
    if !std::path::Path::new(DB_PATH).exists() {
        return Ok(None);
    }

    let connection = rusqlite::Connection::open_with_flags(
        DB_PATH,
        rusqlite::OpenFlags::SQLITE_OPEN_READ_ONLY,
    )?;

    let mut statement = match connection
        .prepare("SELECT 1 FROM packages WHERE package = ?1 AND enabled = 1")
    {
        Ok(statement) => statement,
        Err(_) => return Ok(None),
    };

    if !crate::config::is_managed_package(package_name) {
        return Ok(Some(false));
    }

    let mut rows = statement.query([package_name])?;
    Ok(Some(rows.next()?.is_some()))
}

fn matches_line(line: &str, package_name: &str, process_name: &str) -> bool {
    if !crate::config::is_managed_package(package_name) {
        return false;
    }

    let line = line.trim();
    if line.is_empty() || line.starts_with('#') {
        return false;
    }

    match line.split_once('|') {
        Some((package, process)) => {
            let package = package.trim();
            let process = process.trim();
            package == package_name
                && crate::config::is_managed_package(package)
                && (process.is_empty()
                    || (process == process_name
                        && crate::config::is_valid_process_name(package, process)))
        }
        None => line == package_name && crate::config::is_managed_package(line),
    }
}

#[cfg(test)]
mod tests {
    use super::matches_line;

    #[test]
    fn package_only_matches_all_processes() {
        assert!(matches_line(
            "com.example.app",
            "com.example.app",
            "com.example.app"
        ));
        assert!(matches_line(
            "com.example.app",
            "com.example.app",
            "com.example.app:push"
        ));
    }

    #[test]
    fn process_specific_line_matches_only_that_process() {
        assert!(matches_line(
            "com.example.app|com.example.app:push",
            "com.example.app",
            "com.example.app:push",
        ));
        assert!(!matches_line(
            "com.example.app|com.example.app:push",
            "com.example.app",
            "com.example.app",
        ));
    }

    #[test]
    fn system_and_xiaomi_family_lines_never_match() {
        assert!(!matches_line(
            "com.xiaomi.smarthome",
            "com.xiaomi.smarthome",
            "com.xiaomi.smarthome"
        ));
        assert!(!matches_line(
            "com.android.settings",
            "com.android.settings",
            "com.android.settings"
        ));
    }
}
