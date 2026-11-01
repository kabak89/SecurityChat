package com.security.chat.multiplatform.features.settings.ui.screens.notifications

import com.security.chat.multiplatform.common.core.test.util.ScreenshotTestBase
import org.junit.jupiter.api.Test

class NotificationsScreenTest : ScreenshotTestBase() {

    @Test
    fun `notifications screen preview`() {
        runScreenshotTest(screenshotName = "NotificationsScreenPreview") {
            NotificationsScreenPreview()
        }
    }

    @Test
    fun `notifications allowed preview`() {
        runScreenshotTest(screenshotName = "NotificationsScreenAllowedPreview") {
            NotificationsScreenAllowedPreview()
        }
    }

    @Test
    fun `notifications restricted preview`() {
        runScreenshotTest(screenshotName = "NotificationsScreenRestrictedPreview") {
            NotificationsScreenRestrictedPreview()
        }
    }
}
