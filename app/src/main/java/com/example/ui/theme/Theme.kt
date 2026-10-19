package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SendPromoColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = StudioBlack,
    primaryContainer = StudioCardElevated,
    onPrimaryContainer = NeonCyan,
    secondary = ElectricViolet,
    onSecondary = TextWhite,
    secondaryContainer = StudioCard,
    onSecondaryContainer = ElectricViolet,
    tertiary = AcidEmerald,
    onTertiary = StudioBlack,
    background = StudioBlack,
    onBackground = TextWhite,
    surface = StudioCard,
    onSurface = TextWhite,
    surfaceVariant = StudioCardElevated,
    onSurfaceVariant = TextGray,
    outline = StudioBorder,
    error = ErrorRed,
    onError = TextWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // SendPromo defaults to modern dark DJ studio theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> SendPromoColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = StudioBlack.toArgb()
                window.navigationBarColor = StudioBlack.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
