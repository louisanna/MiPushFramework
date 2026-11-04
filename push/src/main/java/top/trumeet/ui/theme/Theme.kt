package top.trumeet.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 参照 MiPush 管理器（magisk317）的 HyperOS 风格：
 * 近黑背景 + 深灰卡片 + 琥珀色强调色。
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

@Composable
fun Theme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HyperDarkScheme,
        typography = Typography,
        content = content
    )
}
