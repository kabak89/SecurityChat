package com.security.chat.multiplatform.features.onboarding.ui.screens.welcome

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.dp
import com.security.chat.multiplatform.common.core.test.util.ScreenshotTestBase
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class WelcomeScreenTest : ScreenshotTestBase() {

    @Test
    fun `last animation completes at continue button on different screen heights`() {
        for (height in listOf(600, 900, 1200)) {
            runSkikoComposeUiTest(size = Size(1024f, height.toFloat())) {
                var continued = false
                setContent {
                    AppTheme {
                        Box(Modifier.size(412.dp, height.dp)) {
                            WelcomeScreenContent(onContinue = { continued = true })
                        }
                    }
                }

                val button = onNodeWithTag(WELCOME_CONTINUE_BUTTON_TAG)
                button.performScrollTo()
                val viewport = onNodeWithTag(WELCOME_VIEWPORT_TAG).fetchSemanticsNode()
                val animation = onNodeWithTag(MESSAGE_SENDING_ANIMATION_TAG).fetchSemanticsNode()
                val buttonNode = button.fetchSemanticsNode()
                assertTrue(
                    animation.positionInRoot.y <= viewport.positionInRoot.y + 1f,
                    "Last animation has not reached the top on a ${height}dp screen",
                )
                assertTrue(
                    buttonNode.positionInRoot.y >= viewport.positionInRoot.y &&
                            buttonNode.positionInRoot.y + buttonNode.size.height <=
                            viewport.positionInRoot.y + viewport.size.height + 1f,
                    "Continue button is not fully visible on a ${height}dp screen",
                )
                button.performClick()
                runOnIdle { assertTrue(continued) }
            }
        }
    }

    @Test
    fun `welcome screen starts with app description`() {
        runScreenshotTest(screenshotName = "WelcomeScreenPreview") {
            WelcomeScreenPreview()
        }
    }

    @Test
    fun `welcome screen ends with next button`() {
        runScreenshotTest(screenshotName = "WelcomeScreenEndPreview") {
            WelcomeScreenEndPreview()
        }
    }
}
