package com.security.chat.multiplatform.features.chat.ui.screens.groupchat.component

import com.security.chat.multiplatform.common.core.test.util.ScreenshotTestBase
import org.junit.jupiter.api.Test

class OutgoingMessageMetadataTest : ScreenshotTestBase() {

    @Test
    fun `sending metadata shows progress beside time`() {
        runScreenshotTest(
            screenshotName = "OutgoingMessageMetadataSendingPreview",
            animationTimeMillis = 300L,
        ) {
            OutgoingMessageMetadataSendingPreview()
        }
    }

    @Test
    fun `sent metadata shows time without progress`() {
        runScreenshotTest(
            screenshotName = "OutgoingMessageMetadataSentPreview",
        ) {
            OutgoingMessageMetadataSentPreview()
        }
    }

    @Test
    fun `received metadata shows a translucent double check`() {
        runScreenshotTest(
            screenshotName = "OutgoingMessageMetadataReceivedPreview",
        ) {
            OutgoingMessageMetadataReceivedPreview()
        }
    }

    @Test
    fun `read metadata shows a clear double check`() {
        runScreenshotTest(
            screenshotName = "OutgoingMessageMetadataReadPreview",
        ) {
            OutgoingMessageMetadataReadPreview()
        }
    }
}
