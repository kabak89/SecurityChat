package com.security.chat.multiplatform.features.chat.ui.screens.groupchat.component

import com.security.chat.multiplatform.common.core.test.util.ScreenshotTestBase
import org.junit.jupiter.api.Test

class IncomingMessageComponentTest : ScreenshotTestBase() {

    @Test
    fun incomingMessageComponentLongTextPreview() {
        runScreenshotTest(screenshotName = "IncomingMessageComponentLongTextPreview") {
            IncomingMessageComponentLongTextPreview()
        }
    }

    @Test
    fun incomingMessageComponentNoSenderName() {
        runScreenshotTest(screenshotName = "IncomingMessageComponentNoSenderName") {
            IncomingMessageComponentNoSenderName()
        }
    }

    @Test
    fun incomingMessageComponentPreview() {
        runScreenshotTest(screenshotName = "IncomingMessageComponentPreview") {
            IncomingMessageComponentPreview()
        }
    }
}
