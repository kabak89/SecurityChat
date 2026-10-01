package com.security.chat.multiplatform.features.call.ui.screens.call

import com.security.chat.multiplatform.common.core.test.util.ScreenshotTestBase
import org.junit.jupiter.api.Test

class CallScreenTest : ScreenshotTestBase() {

    @Test
    fun `call screen preview`() {
        runScreenshotTest(screenshotName = "CallScreenPreview") {
            CallScreenPreview()
        }
    }
}
