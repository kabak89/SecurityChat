package com.security.chat.multiplatform.features.chat.ui.screens.groupchat.component

import com.security.chat.multiplatform.common.core.test.util.ScreenshotTestBase
import org.junit.jupiter.api.Test

class OutgoingMessageComponentTest : ScreenshotTestBase() {

    @Test
    fun outgoingMessageComponentLongTextPreview() {
        runScreenshotTest(screenshotName = "OutgoingMessageComponentLongTextPreview") {
            OutgoingMessageComponentLongTextPreview()
        }
    }

    @Test
    fun outgoingMessageComponentPreview() {
        runScreenshotTest(screenshotName = "OutgoingMessageComponentPreview") {
            OutgoingMessageComponentPreview()
        }
    }

    @Test
    fun `outgoing text message shows sending indicator next to time`() {
        runScreenshotTest(
            screenshotName = "OutgoingMessageComponentSendingPreview",
            animationTimeMillis = 300L,
        ) {
            OutgoingMessageComponentSendingPreview()
        }
    }
}
