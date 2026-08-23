package com.security.chat.multiplatform.features.profile.ui.screens.main

import com.security.chat.multiplatform.common.core.test.util.ScreenshotTestBase
import org.junit.jupiter.api.Test

class ProfileMainScreenTest : ScreenshotTestBase() {

    @Test
    fun profileMainScreenPreview() {
        runScreenshotTest(screenshotName = "ProfileMainScreenPreview") {
            ProfileMainScreenPreview()
        }
    }

    @Test
    fun profileMainScreenPreviewUpdateDisabled() {
        runScreenshotTest(screenshotName = "ProfileMainScreenPreviewUpdateDisabled") {
            ProfileMainScreenPreviewUpdateDisabled()
        }
    }

    @Test
    fun profileMainScreenPreviewLoading() {
        runScreenshotTest(screenshotName = "ProfileMainScreenPreviewLoading") {
            ProfileMainScreenPreviewLoading()
        }
    }
}
