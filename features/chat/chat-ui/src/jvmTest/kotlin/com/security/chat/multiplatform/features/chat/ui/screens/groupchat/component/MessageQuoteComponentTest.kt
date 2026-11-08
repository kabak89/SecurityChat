package com.security.chat.multiplatform.features.chat.ui.screens.groupchat.component

import androidx.compose.ui.unit.dp
import com.security.chat.multiplatform.common.core.test.util.ScreenshotTestBase
import org.junit.jupiter.api.Test

class MessageQuoteComponentTest : ScreenshotTestBase() {
    @Test
    fun `quote truncates text and shows a photo thumbnail`() {
        runScreenshotTest(
            screenshotName = "MessageQuoteComponentPreview",
            width = 260.dp,
            height = 120.dp,
        ) {
            MessageQuoteComponentPreview()
        }
    }
}
