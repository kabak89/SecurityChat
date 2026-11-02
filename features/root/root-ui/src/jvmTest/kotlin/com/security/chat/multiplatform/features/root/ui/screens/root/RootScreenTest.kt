package com.security.chat.multiplatform.features.root.ui.screens.root

import com.security.chat.multiplatform.common.core.test.util.ScreenshotTestBase
import org.junit.jupiter.api.Test

class RootScreenTest : ScreenshotTestBase() {

    @Test
    fun `root screen uses light theme`() {
        runScreenshotTest(screenshotName = "RootScreenPreview") {
            RootScreenPreview()
        }
    }

    @Test
    fun `root screen uses dark theme`() {
        runScreenshotTest(screenshotName = "RootScreenDarkPreview") {
            RootScreenDarkPreview()
        }
    }
}
