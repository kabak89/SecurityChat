package com.security.chat.multiplatform.features.call.ui.screens.permissions

import com.security.chat.multiplatform.common.core.test.util.ScreenshotTestBase
import org.junit.jupiter.api.Test

class PermissionsScreenTest : ScreenshotTestBase() {

    @Test
    fun `permissions screen preview`() {
        runScreenshotTest(screenshotName = "PermissionsScreenPreview") {
            PermissionsScreenPreview()
        }
    }

    @Test
    fun `permissions screen restricted preview`() {
        runScreenshotTest(screenshotName = "PermissionsScreenRestrictedPreview") {
            PermissionsScreenRestrictedPreview()
        }
    }
}
