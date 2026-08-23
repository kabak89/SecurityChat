package com.security.chat.multiplatform.features.settings.ui.screens.theme

import com.security.chat.multiplatform.common.core.test.util.ScreenshotTestBase
import org.junit.jupiter.api.Test

class ThemeScreenTest : ScreenshotTestBase() {

    @Test
    fun themeScreenPreview() {
        runScreenshotTest(screenshotName = "ThemeScreenPreview") {
            ThemeScreenPreview()
        }
    }
}
