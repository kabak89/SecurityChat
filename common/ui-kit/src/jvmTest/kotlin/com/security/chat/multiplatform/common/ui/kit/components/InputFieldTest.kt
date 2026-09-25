package com.security.chat.multiplatform.common.ui.kit.components

import androidx.compose.ui.unit.dp
import com.security.chat.multiplatform.common.core.test.util.ScreenshotTestBase
import org.junit.jupiter.api.Test

class InputFieldTest : ScreenshotTestBase() {

    @Test
    fun `input field states in light theme`() {
        runScreenshotTest(
            screenshotName = "InputFieldPreview",
            height = 900.dp,
            animationTimeMillis = 100L,
        ) {
            InputFieldPreview()
        }
    }

    @Test
    fun `input field states in dark theme`() {
        runScreenshotTest(
            screenshotName = "InputFieldPreviewDark",
            height = 900.dp,
            animationTimeMillis = 100L,
        ) {
            InputFieldPreviewDark()
        }
    }
}
