package com.security.chat.multiplatform.features.chat.ui.screens.groupchat.component

import androidx.compose.ui.unit.dp
import com.security.chat.multiplatform.common.core.test.util.ScreenshotTestBase
import org.junit.jupiter.api.Test

class MessageActionsComponentTest : ScreenshotTestBase() {
    @Test
    fun `message actions menu preview`() {
        runScreenshotTest(
            screenshotName = "MessageActionsMenuPreview",
            width = 1024.dp,
            height = 200.dp,
        ) {
            MessageActionsMenuPreview()
        }
    }
}
