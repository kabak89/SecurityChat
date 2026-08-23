package com.security.chat.multiplatform.features.authorize.ui.screens.signup

import com.security.chat.multiplatform.common.core.test.util.ScreenshotTestBase
import org.junit.jupiter.api.Test

class SignUpScreenTest : ScreenshotTestBase() {

    @Test
    fun signUpContentPreview() {
        runScreenshotTest(screenshotName = "SignUpContentPreview") {
            SignUpContentPreview()
        }
    }

    @Test
    fun signUpContentPreviewDisabled() {
        runScreenshotTest(screenshotName = "SignUpContentPreviewDisabled") {
            SignUpContentPreviewDisabled()
        }
    }

    @Test
    fun signUpContentPreviewLoading() {
        runScreenshotTest(screenshotName = "SignUpContentPreviewLoading") {
            SignUpContentPreviewLoading()
        }
    }
}
