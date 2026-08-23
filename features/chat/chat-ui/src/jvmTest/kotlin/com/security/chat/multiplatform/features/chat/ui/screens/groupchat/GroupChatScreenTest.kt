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
}
