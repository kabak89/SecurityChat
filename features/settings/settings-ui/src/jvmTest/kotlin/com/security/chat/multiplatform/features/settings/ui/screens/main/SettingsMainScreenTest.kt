package com.security.chat.multiplatform.features.settings.ui.screens.main

import com.security.chat.multiplatform.common.core.test.util.ScreenshotTestBase
import org.junit.jupiter.api.Test

class SettingsMainScreenTest : ScreenshotTestBase() {

    @Test
    fun settingsMainScreenPreview() {
        runScreenshotTest(screenshotName = "SettingsMainScreenPreview") {
            SettingsMainScreenPreview()
        }
    }

    @Test
    fun settingsMainScreenPreviewWithDialog() {
        runScreenshotTest(screenshotName = "SettingsMainScreenPreviewWithDialog") {
            SettingsMainScreenPreviewWithDialog()
        }
    }
}
