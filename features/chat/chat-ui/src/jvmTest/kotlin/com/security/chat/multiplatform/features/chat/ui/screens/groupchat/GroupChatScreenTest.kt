package com.security.chat.multiplatform.features.chat.ui.screens.groupchat

import com.security.chat.multiplatform.common.core.test.util.ScreenshotTestBase
import org.junit.jupiter.api.Test

class GroupChatScreenTest : ScreenshotTestBase() {

    @Test
    fun groupChatScreenPreview() {
        runScreenshotTest(screenshotName = "GroupChatScreenPreview") {
            GroupChatScreenPreview()
        }
    }

    @Test
    fun `reply panel and quoted messages preview`() {
        runScreenshotTest(screenshotName = "GroupChatReplyPreview") {
            GroupChatReplyPreview()
        }
    }

    @Test
    fun `delete message confirmation preview`() {
        runScreenshotTest(screenshotName = "GroupChatDeleteConfirmationPreview") {
            GroupChatDeleteConfirmationPreview()
        }
    }
}
