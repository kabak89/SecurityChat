package com.security.chat.multiplatform.features.chat.ui.screens.groupchat.component

import com.security.chat.multiplatform.common.core.test.util.ScreenshotTestBase
import org.junit.jupiter.api.Test

class OutgoingImageMessageComponentTest : ScreenshotTestBase() {

    @Test
    fun outgoingOutgoingImageMessageComponentPreview() {
        runScreenshotTest(screenshotName = "OutgoingOutgoingImageMessageComponentPreview") {
            OutgoingOutgoingImageMessageComponentPreview()
        }
    }
}
