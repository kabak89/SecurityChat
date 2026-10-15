package com.security.chat.multiplatform.features.onboarding.ui.screens.welcome

import androidx.compose.ui.unit.dp
import com.security.chat.multiplatform.common.core.test.util.ScreenshotTestBase
import org.junit.jupiter.api.Test

class KeyGenerationAnimationTest : ScreenshotTestBase() {

    @Test
    fun `animation starts with user only`() {
        runScreenshotTest("KeyGenerationAnimationStartPreview", height = 232.dp) {
            KeyGenerationAnimationStartPreview()
        }
    }

    @Test
    fun `key appears before lock`() {
        runScreenshotTest("KeyGenerationAnimationKeyPreview", height = 232.dp) {
            KeyGenerationAnimationKeyPreview()
        }
    }

    @Test
    fun `completed animation shows user key and lock`() {
        runScreenshotTest("KeyGenerationAnimationEndPreview", height = 232.dp) {
            KeyGenerationAnimationEndPreview()
        }
    }

    @Test
    fun `completed animation supports dark theme`() {
        runScreenshotTest("KeyGenerationAnimationDarkPreview", height = 232.dp) {
            KeyGenerationAnimationDarkPreview()
        }
    }
}
