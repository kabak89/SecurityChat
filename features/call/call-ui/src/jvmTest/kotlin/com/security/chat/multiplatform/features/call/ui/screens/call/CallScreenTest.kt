package com.security.chat.multiplatform.features.call.ui.screens.call

import com.security.chat.multiplatform.common.core.test.util.ScreenshotTestBase
import org.junit.jupiter.api.Test

class CallScreenTest : ScreenshotTestBase() {

    @Test
    fun `existing call shows participant status and join button`() {
        runScreenshotTest("CallScreenJoinPreview") { CallScreenJoinPreview() }
    }

    @Test
    fun `call screen preview`() {
        runScreenshotTest("CallScreenPreview") { CallScreenPreview() }
    }

    @Test
    fun `empty call screen preview`() {
        runScreenshotTest("CallScreenEmptyPreview") { CallScreenEmptyPreview() }
    }

    @Test
    fun `connecting call can be ended`() {
        runScreenshotTest("CallScreenConnectingPreview") { CallScreenConnectingPreview() }
    }

    @Test
    fun `active call screen preview`() {
        runScreenshotTest("CallScreenActivePreview") { CallScreenActivePreview() }
    }

    @Test
    fun `call error screen preview`() {
        runScreenshotTest("CallScreenErrorPreview") { CallScreenErrorPreview() }
    }
}
