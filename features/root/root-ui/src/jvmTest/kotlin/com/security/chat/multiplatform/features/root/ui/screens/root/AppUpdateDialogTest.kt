package com.security.chat.multiplatform.features.root.ui.screens.root

import com.security.chat.multiplatform.common.core.test.util.ScreenshotTestBase
import org.junit.jupiter.api.Test

class AppUpdateDialogTest : ScreenshotTestBase() {

    @Test
    fun `update ready dialog in light theme`() {
        runScreenshotTest(screenshotName = "AppUpdateDialogPreview") {
            AppUpdateDialogPreview()
        }
    }

    @Test
    fun `update ready dialog in dark theme`() {
        runScreenshotTest(screenshotName = "AppUpdateDialogDarkPreview") {
            AppUpdateDialogDarkPreview()
        }
    }
}
