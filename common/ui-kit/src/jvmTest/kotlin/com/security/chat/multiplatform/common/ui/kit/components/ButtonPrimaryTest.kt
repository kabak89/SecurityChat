package com.security.chat.multiplatform.common.ui.kit.components

import com.security.chat.multiplatform.common.core.test.util.ScreenshotTestBase
import org.junit.jupiter.api.Test

class ButtonPrimaryTest : ScreenshotTestBase() {

    @Test
    fun buttonPrimaryPreview() {
        runScreenshotTest(screenshotName = "ButtonPrimaryPreview") {
            ButtonPrimaryPreview()
        }
    }

    @Test
    fun buttonPrimaryPreviewDark() {
        runScreenshotTest(screenshotName = "ButtonPrimaryPreviewDark") {
            ButtonPrimaryPreviewDark()
        }
    }
}