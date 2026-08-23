package com.security.chat.multiplatform.features.add_chat.ui

import com.security.chat.multiplatform.common.core.test.util.ScreenshotTestBase
import org.junit.jupiter.api.Test

class AddChatScreenTest : ScreenshotTestBase() {

    @Test
    fun addChatContentGroupPreview() {
        runScreenshotTest(screenshotName = "AddChatContentGroupPreview") {
            AddChatContentGroupPreview()
        }
    }
}
