package com.example.forpetapp.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 🐾 발자국동행 전용 웜 베이지 컬러 스킴 (야간 다크모드 배제 & 포근한 베이지 테마 통일)
private val WarmBeigeColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    secondary = Color(0xFF0EA5E9),
    tertiary = Color(0xFFF59E0B),
    background = WarmBeigeBackground,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = WarmTextPrimary,
    onSurface = WarmTextPrimary
)

@Composable
fun ForpetappTheme(
    darkTheme: Boolean = false, // 야간모드 영구 OFF
    dynamicColor: Boolean = false, // 고유의 웜 베이지 브랜딩 유지
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = true
                controller.isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = WarmBeigeColorScheme,
        typography = Typography,
        content = content
    )
}
