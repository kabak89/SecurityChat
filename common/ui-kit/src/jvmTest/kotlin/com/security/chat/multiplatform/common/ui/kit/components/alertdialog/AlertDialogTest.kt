package com.security.chat.multiplatform.common.ui.kit.components.alertdialog

import com.security.chat.multiplatform.common.core.test.util.ScreenshotTestBase
import org.junit.jupiter.api.Test

class AlertDialogTest : ScreenshotTestBase() {

    @Test
    fun alertDialogComponentPreview() {
        runScreenshotTest(screenshotName = "AlertDialogComponentPreview") {
            AlertDialogComponentPreview()
        }
    }
}
