package com.security.chat.multiplatform.features.call.ui.screens.permissions

import com.security.chat.multiplatform.common.core.test.util.ScreenshotTestBase
import org.junit.jupiter.api.Test

class PermissionsScreenTest : ScreenshotTestBase() {

    @Test
    fun `microphone and local network permissions screen preview`() {
        runScreenshotTest("PermissionsScreenMicrophoneAndLocalNetworkPreview") {
            PermissionsScreenMicrophoneAndLocalNetworkPreview()
        }
    }

    @Test
    fun `local network permission screen preview`() {
        runScreenshotTest("PermissionsScreenLocalNetworkPreview") {
            PermissionsScreenLocalNetworkPreview()
        }
    }

    @Test
    fun `local network permission restricted preview`() {
        runScreenshotTest("PermissionsScreenLocalNetworkRestrictedPreview") {
            PermissionsScreenLocalNetworkRestrictedPreview()
        }
    }

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
