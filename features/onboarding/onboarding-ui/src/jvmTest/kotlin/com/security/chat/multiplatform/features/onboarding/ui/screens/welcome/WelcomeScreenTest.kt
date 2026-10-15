package com.security.chat.multiplatform.features.onboarding.ui.screens.welcome

import com.security.chat.multiplatform.common.core.test.util.ScreenshotTestBase
import org.junit.jupiter.api.Test

class WelcomeScreenTest : ScreenshotTestBase() {

    @Test
    fun `welcome screen with next button`() {
        runScreenshotTest(screenshotName = "WelcomeScreenPreview") {
            WelcomeScreenPreview()
        }
    }
}
