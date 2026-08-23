package com.security.chat.multiplatform.features.chats.ui.screens.chatlist

import com.security.chat.multiplatform.common.core.test.util.ScreenshotTestBase
import org.junit.jupiter.api.Test

class ChatListScreenTest : ScreenshotTestBase() {

    @Test
    fun chatListContentPreview() {
        runScreenshotTest(screenshotName = "ChatListContentPreview") {
            ChatListContentPreview()
        }
    }

    @Test
    fun chatListContentLoadingPreview() {
        runScreenshotTest(screenshotName = "ChatListContentLoadingPreview") {
            ChatListContentLoadingPreview()
        }
    }
}
