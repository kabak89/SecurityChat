package com.security.chat.multiplatform.features.onboarding.ui.screens.notificationpermission

import com.security.chat.multiplatform.common.core.test.util.ScreenshotTestBase
import org.junit.jupiter.api.Test

class NotificationPermissionScreenTest : ScreenshotTestBase() {

    @Test
    fun notificationPermissionScreenPreview() {
        runScreenshotTest(screenshotName = "NotificationPermissionScreenPreview") {
            NotificationPermissionScreenPreview()
        }
    }

    @Test
    fun `Bluetooth permission screen preview`() {
        runScreenshotTest(screenshotName = "BluetoothPermissionScreenPreview") {
            BluetoothPermissionScreenPreview()
        }
    }

    @Test
    fun `Bluetooth location permission onboarding preview`() {
        runScreenshotTest(screenshotName = "BluetoothLocationPermissionScreenPreview") {
            BluetoothLocationPermissionScreenPreview()
        }
    }

    @Test
    fun `Bluetooth background permission onboarding preview`() {
        runScreenshotTest(screenshotName = "BluetoothBackgroundPermissionScreenPreview") {
            BluetoothBackgroundPermissionScreenPreview()
        }
    }
}
