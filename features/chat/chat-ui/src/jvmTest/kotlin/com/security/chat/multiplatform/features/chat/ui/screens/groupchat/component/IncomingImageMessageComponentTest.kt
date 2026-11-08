package com.security.chat.multiplatform.features.chat.ui.screens.groupchat.component

import androidx.compose.ui.unit.dp
import com.security.chat.multiplatform.common.core.test.util.ScreenshotTestBase
import org.junit.jupiter.api.Test

class IncomingImageMessageComponentTest : ScreenshotTestBase() {

    @Test
    fun incomingImageMessageComponentPreview() {
        runScreenshotTest(screenshotName = "IncomingImageMessageComponentPreview") {
            IncomingImageMessageComponentPreview()
        }
    }

    @Test
    fun `incoming photo shows the sender and truncates the quoted text`() {
        runScreenshotTest(
            screenshotName = "IncomingImageMessageWithTextQuotePreview",
            height = 280.dp,
        ) {
            IncomingImageMessageWithTextQuotePreview()
        }
    }

    @Test
    fun `incoming photo shows a quoted thumbnail when the sender header is hidden`() {
        runScreenshotTest(
            screenshotName = "IncomingImageMessageWithPhotoQuotePreview",
            height = 280.dp,
        ) {
            IncomingImageMessageWithPhotoQuotePreview()
        }
    }
}
