package com.security.chat.multiplatform.features.chat_info.ui.screens.addmember

import com.security.chat.multiplatform.common.core.test.util.ScreenshotTestBase
import org.junit.jupiter.api.Test

class AddMemberScreenTest : ScreenshotTestBase() {

    @Test
    fun addMemberScreenPreview() {
        runScreenshotTest(screenshotName = "AddMemberScreenPreview") {
            AddMemberScreenPreview()
        }
    }
}
