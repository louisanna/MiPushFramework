package top.trumeet.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 参照 MiPush 管理器（magisk317）的 HyperOS 风格：
 * 深色：近黑背景 + 深灰卡片 + 琥珀色强调色。
 * 浅色：白底 + 浅灰卡片 + 深琥珀色强调色。
 */
private val HyperDarkScheme = darkColorScheme(
    primary = Color(0xFFFFB74D),
    onPrimary = Color(0xFF1A1205),
    primaryContainer = Color(0xFF3A2A10),
    onPrimaryContainer = Color(0xFFFFDCA8),
    secondary = Color(0xFFFFC107),
    onSecondary = Color(0xFF1A1205),
    background = Color(0xFF0D0D0D),
    onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF141414),
    onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFB3B3B3),
    outline = Color(0xFF2E2E2E),
    outlineVariant = Color(0xFF262626),
    error = Color(0xFFFF6B5B),
    onError = Color(0xFF1A0503),
)

private val HyperLightScheme = lightColorScheme(
    primary = Color(0xFFB26A00),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDCA8),
    onPrimaryContainer = Color(0xFF2A1A00),
    secondary = Color(0xFF8A5A00),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFAF9F7),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF1EFEA),
    onSurfaceVariant = Color(0xFF4A4A4A),
    outline = Color(0xFFD5D0C8),
    outlineVariant = Color(0xFFE6E2DB),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
)

@Composable
fun Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) HyperDarkScheme else HyperLightScheme,
        typography = Typography,
        content = content
    )
}
