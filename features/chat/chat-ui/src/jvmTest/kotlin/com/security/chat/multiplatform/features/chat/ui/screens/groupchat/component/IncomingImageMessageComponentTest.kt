package com.security.chat.multiplatform.features.chat.ui.screens.groupchat.component

import com.security.chat.multiplatform.common.core.test.util.ScreenshotTestBase
import org.junit.jupiter.api.Test

class IncomingImageMessageComponentTest : ScreenshotTestBase() {

    @Test
    fun incomingImageMessageComponentPreview() {
        runScreenshotTest(screenshotName = "IncomingImageMessageComponentPreview") {
            IncomingImageMessageComponentPreview()
        }
    }
}
