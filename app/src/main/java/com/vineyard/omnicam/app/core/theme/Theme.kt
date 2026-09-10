package com.vineyard.omnicam.app.core.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val SlateDarkColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = Color.Black,
    primaryContainer = SlateSurface,
    onPrimaryContainer = CyanAccentGlow,
    secondary = IndigoAccent,
    onSecondary = Color.White,
    tertiary = EmeraldLive,
    background = SlateBackground,
    onBackground = SlateTextPrimary,
    surface = SlateSurface,
    onSurface = SlateTextPrimary,
    surfaceVariant = SlateCard,
    onSurfaceVariant = SlateTextSecondary,
    outline = SlateBorder,
    error = RoseAlert,
    onError = Color.White
)

private val AmoledColorScheme = darkColorScheme(
    primary = CyanAccentGlow,
    onPrimary = Color.Black,
    primaryContainer = AmoledSurface,
    onPrimaryContainer = CyanAccent,
    secondary = IndigoAccent,
    onSecondary = Color.White,
    tertiary = EmeraldLive,
    background = AmoledBackground,
    onBackground = AmoledTextPrimary,
    surface = AmoledSurface,
    onSurface = AmoledTextPrimary,
    surfaceVariant = AmoledCard,
    onSurfaceVariant = AmoledTextSecondary,
    outline = AmoledBorder,
    error = RoseAlert,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = CyanAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F7FA),
    onPrimaryContainer = Color(0xFF006064),
    secondary = IndigoAccent,
    onSecondary = Color.White,
    tertiary = EmeraldLive,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightCard,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    error = RoseAlert,
    onError = Color.White
)

@Composable
fun OmniCamTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemInDark = isSystemInDarkTheme()

    val colorScheme = when (themeMode) {
        ThemeMode.SYSTEM -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (systemInDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (systemInDark) SlateDarkColorScheme else LightColorScheme
            }
        }
        ThemeMode.LIGHT -> LightColorScheme
        ThemeMode.DARK -> SlateDarkColorScheme
        ThemeMode.AMOLED -> AmoledColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
