package com.sharn.handpan.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = HandpanGold,
    onPrimary = CharcoalBlack,
    primaryContainer = HandpanBronzeDark,
    onPrimaryContainer = HandpanGoldLight,
    secondary = HandpanBronze,
    onSecondary = CharcoalBlack,
    secondaryContainer = CharcoalSurfaceVariant,
    onSecondaryContainer = HandpanBronze,
    tertiary = HandpanTerracotta,
    onTertiary = Color.White,
    tertiaryContainer = CharcoalBorder,
    onTertiaryContainer = HandpanTerracotta,
    background = CharcoalBlack,
    onBackground = Color(0xFFF3EDE7),
    surface = CharcoalDark,
    onSurface = Color(0xFFF3EDE7),
    surfaceVariant = CharcoalSurfaceVariant,
    onSurfaceVariant = Color(0xFFD6C8BB),
    outline = CharcoalBorder,
    outlineVariant = Color(0xFF4A3E34)
)

private val LightColorScheme = lightColorScheme(
    primary = HandpanBronzeDark,
    onPrimary = Color.White,
    primaryContainer = WoodSurfaceVariantLight,
    onPrimaryContainer = HandpanBronzeDark,
    secondary = HandpanBronze,
    onSecondary = Color.White,
    secondaryContainer = WoodSurfaceLight,
    onSecondaryContainer = HandpanBronzeDark,
    tertiary = HandpanTerracotta,
    onTertiary = Color.White,
    tertiaryContainer = WoodBorderLight,
    onTertiaryContainer = HandpanTerracotta,
    background = WoodWarmLight,
    onBackground = CharcoalBlack,
    surface = WoodSurfaceLight,
    onSurface = CharcoalBlack,
    surfaceVariant = WoodSurfaceVariantLight,
    onSurfaceVariant = Color(0xFF42352B),
    outline = WoodBorderLight,
    outlineVariant = Color(0xFFC4B2A0)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to gorgeous calm dark theme suited for musical focus
    dynamicColor: Boolean = false, // Keep the custom Handpan aesthetic by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
