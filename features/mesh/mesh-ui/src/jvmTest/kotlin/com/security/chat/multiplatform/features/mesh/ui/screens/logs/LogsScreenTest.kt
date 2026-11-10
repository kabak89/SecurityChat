package com.security.chat.multiplatform.features.mesh.ui.screens.logs

import com.security.chat.multiplatform.common.core.test.util.ScreenshotTestBase
import org.junit.jupiter.api.Test

class LogsScreenTest : ScreenshotTestBase() {

    @Test
    fun `empty logs screen preview`() {
        runScreenshotTest(screenshotName = "LogsScreenPreview") {
            LogsScreenPreview()
        }
    }

    @Test
    fun `log entries display timestamps`() {
        runScreenshotTest(screenshotName = "LogsScreenWithEntriesPreview") {
            LogsScreenWithEntriesPreview()
        }
    }
}
