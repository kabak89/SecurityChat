package com.security.chat.multiplatform.features.chat_info.ui.screens.chatinfo

import com.security.chat.multiplatform.common.core.test.util.ScreenshotTestBase
import org.junit.jupiter.api.Test

class ChatInfoScreenTest : ScreenshotTestBase() {

    @Test
    fun chatInfoScreenPreview() {
        runScreenshotTest(screenshotName = "ChatInfoScreenPreview") {
            ChatInfoScreenPreview()
        }
    }

    @Test
    fun chatInfoScreenPreviewLoading() {
        runScreenshotTest(screenshotName = "ChatInfoScreenPreviewLoading") {
            ChatInfoScreenPreviewLoading()
        }
    }
}
