package br.gov.caninde.transparencia.presentation

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─── Paleta ───────────────────────────────────────────────────────────────────

object AppColors {
    val Navy900   = Color(0xFF0D2137)
    val Navy800   = Color(0xFF1B3A5C)
    val Navy700   = Color(0xFF255080)
    val Blue500   = Color(0xFF378ADD)
    val Blue300   = Color(0xFF7EB9E8)
    val Blue100   = Color(0xFFE6F1FB)

    val Green700  = Color(0xFF2D5A0E)
    val Green500  = Color(0xFF3B6D11)
    val Green100  = Color(0xFFEAF3DE)

    val Amber700  = Color(0xFF854F0B)
    val Amber100  = Color(0xFFFAEEDA)

    val Red700    = Color(0xFFA32D2D)
    val Red100    = Color(0xFFFCEBEB)

    val Purple100 = Color(0xFFEEEDFE)
    val Purple700 = Color(0xFF534AB7)

    val Surface   = Color(0xFFF5F7FA)
    val Card      = Color(0xFFFFFFFF)
    val Divider   = Color(0xFFE8EBF0)
    val TextPrimary   = Color(0xFF111827)
    val TextSecondary = Color(0xFF4B5563)
    val TextTertiary  = Color(0xFF9CA3AF)
}

private val LightColorScheme = lightColorScheme(
    primary          = AppColors.Navy800,
    onPrimary        = Color.White,
    primaryContainer = AppColors.Blue100,
    secondary        = AppColors.Blue500,
    onSecondary      = Color.White,
    background       = AppColors.Surface,
    surface          = AppColors.Card,
    onSurface        = AppColors.TextPrimary,
    outline          = AppColors.Divider,
)

@Composable
fun TransparenciaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography  = Typography(),
        content     = content
    )
}
