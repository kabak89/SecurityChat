package com.security.chat.multiplatform.common.ui.kit.components

import com.security.chat.multiplatform.common.core.test.util.ScreenshotTestBase
import org.junit.jupiter.api.Test

class ToolbarTest : ScreenshotTestBase() {

    @Test
    fun toolbarPreview() {
        runScreenshotTest(screenshotName = "ToolbarPreview") {
            ToolbarPreview()
        }
    }
}
