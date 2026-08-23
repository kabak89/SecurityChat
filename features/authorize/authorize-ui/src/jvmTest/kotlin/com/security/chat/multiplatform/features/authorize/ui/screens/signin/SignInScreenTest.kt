package com.security.chat.multiplatform.features.authorize.ui.screens.signin

import com.security.chat.multiplatform.common.core.test.util.ScreenshotTestBase
import org.junit.jupiter.api.Test

class SignInScreenTest : ScreenshotTestBase() {

    @Test
    fun signInContentPreview() {
        runScreenshotTest(screenshotName = "SignInContentPreview") {
            SignInContentPreview()
        }
    }

    @Test
    fun signInContentPreviewLoading() {
        runScreenshotTest(screenshotName = "SignInContentPreviewLoading") {
            SignInContentPreviewLoading()
        }
    }
}
