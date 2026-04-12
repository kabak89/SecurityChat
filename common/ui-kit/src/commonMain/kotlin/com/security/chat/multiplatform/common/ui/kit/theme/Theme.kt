package com.security.chat.multiplatform.common.ui.kit.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight.Companion.W500
import androidx.compose.ui.text.font.FontWeight.Companion.W600
import androidx.compose.ui.unit.sp

private val lightColorPalette: AppColors =
    AppColors(
        backgroundPrimary = Color(0xFFFFFFFF),
        backgroundSecondary = Color(0xFFA9A9A9),
        textPrimary = Color(0xFF000000),
        textSecondary = Color(0xFF131313),
        iconPrimary = Color(0xFF000000),
        element = Color(0xFF808080),
    )

private val darkColorPalette: AppColors =
    AppColors(
        backgroundPrimary = Color(0xFF000000),
        backgroundSecondary = Color(0xFF343434),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFE1E1E1),
        iconPrimary = Color(0xFFFFFFFF),
        element = Color(0xFF808080),
    )

@Composable
public fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (useDarkTheme) {
        darkColorPalette
    } else {
        lightColorPalette
    }

    MaterialTheme(
        typography = Typography(),
    ) {
        CompositionLocalProvider(
            LocalAppColors provides colors,
            LocalAppTypography provides AppTheme.typography,
            LocalContentColor provides colors.textPrimary,
            LocalUseDarkTheme provides useDarkTheme,
            content = content,
        )
    }
}

public object AppTheme {
    public val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current

    public val typography: AppTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalAppTypography.current

    public val isDarkTheme: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalUseDarkTheme.current
}

private val LocalAppColors = staticCompositionLocalOf<AppColors> {
    error("No colors provided")
}

internal val LocalAppTypography: ProvidableCompositionLocal<AppTypography> =
    staticCompositionLocalOf { AppTypography() }

@Immutable
public class AppTypography internal constructor(
    public val default: TextStyle = TextStyle(
        fontSize = 16.sp,
    ),
    public val title: TextStyle = TextStyle(
        fontSize = 18.sp,
        fontWeight = W600,
    ),
    public val title2: TextStyle = TextStyle(
        fontSize = 16.sp,
        fontWeight = W500,
    ),
)

private val LocalUseDarkTheme: ProvidableCompositionLocal<Boolean> =
    staticCompositionLocalOf { false }