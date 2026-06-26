package com.security.chat.multiplatform.common.ui.kit.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import platform.UIKit.UIApplication
import platform.UIKit.UIStatusBarStyleDarkContent
import platform.UIKit.UIStatusBarStyleLightContent
import platform.UIKit.setStatusBarStyle

@Composable
internal actual fun SystemBarsEffect(useDarkTheme: Boolean) {
    LaunchedEffect(useDarkTheme) {
        val style = if (useDarkTheme) {
            UIStatusBarStyleLightContent
        } else {
            UIStatusBarStyleDarkContent
        }

        @Suppress("DEPRECATION")
        UIApplication.sharedApplication.setStatusBarStyle(style, animated = true)
    }
}
