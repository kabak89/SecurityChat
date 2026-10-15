package com.security.chat.multiplatform.features.onboarding.ui.screens.welcome

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.dp
import com.security.chat.multiplatform.common.core.test.util.ScreenshotTestBase
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class MessageSendingAnimationTest : ScreenshotTestBase() {

    @Test
    fun `messages travel with recipient locks while private keys stay with owners`() {
        runSkikoComposeUiTest(size = Size(1024f, 232f)) {
            val progress = mutableFloatStateOf(0.25f)
            setContent {
                AppTheme {
                    Box(Modifier.size(412.dp, 232.dp)) {
                        MessageSendingAnimation(progress = { progress.floatValue })
                    }
                }
            }
            val leftKey = onNodeWithTag("message_key_left")
            val rightKey = onNodeWithTag("message_key_right")
            val leftPlane = onNodeWithTag("message_plane_left")
            val rightPlane = onNodeWithTag("message_plane_right")
            val leftLock = onNodeWithTag("message_lock_left")
            val rightLock = onNodeWithTag("message_lock_right")
            val originalLeftKey = leftKey.fetchSemanticsNode().boundsInRoot
            val originalRightKey = rightKey.fetchSemanticsNode().boundsInRoot
            val originalLeftPlane = leftPlane.fetchSemanticsNode().boundsInRoot
            val originalRightPlane = rightPlane.fetchSemanticsNode().boundsInRoot

            runOnIdle { progress.floatValue = 0.55f }
            assertTrue(
                leftPlane.fetchSemanticsNode().boundsInRoot.contains(
                    leftLock.fetchSemanticsNode().boundsInRoot.center,
                ),
            )
            assertTrue(
                rightPlane.fetchSemanticsNode().boundsInRoot.contains(
                    rightLock.fetchSemanticsNode().boundsInRoot.center,
                ),
            )

            runOnIdle { progress.floatValue = 1f }
            assertEquals(originalLeftKey, leftKey.fetchSemanticsNode().boundsInRoot)
            assertEquals(originalRightKey, rightKey.fetchSemanticsNode().boundsInRoot)
            assertEquals(originalRightPlane, leftPlane.fetchSemanticsNode().boundsInRoot)
            assertEquals(originalLeftPlane, rightPlane.fetchSemanticsNode().boundsInRoot)
            assertTrue(
                leftPlane.fetchSemanticsNode().boundsInRoot.contains(
                    leftLock.fetchSemanticsNode().boundsInRoot.center,
                ),
            )
            assertTrue(
                rightPlane.fetchSemanticsNode().boundsInRoot.contains(
                    rightLock.fetchSemanticsNode().boundsInRoot.center,
                ),
            )

            runOnIdle { progress.floatValue = 0.25f }
            assertEquals(originalLeftPlane, leftPlane.fetchSemanticsNode().boundsInRoot)
            assertEquals(originalRightPlane, rightPlane.fetchSemanticsNode().boundsInRoot)
        }
    }

    @Test
    fun `users start with their partners locks`() {
        runScreenshotTest("MessageSendingAnimationStartPreview", height = 232.dp) {
            MessageSendingAnimationStartPreview()
        }
    }

    @Test
    fun `users create paper plane messages`() {
        runScreenshotTest("MessageSendingAnimationCreatedPreview", height = 232.dp) {
            MessageSendingAnimationCreatedPreview()
        }
    }

    @Test
    fun `messages are protected with recipient locks before sending`() {
        runScreenshotTest("MessageSendingAnimationProtectedPreview", height = 232.dp) {
            MessageSendingAnimationProtectedPreview()
        }
    }

    @Test
    fun `protected messages fly in opposite directions`() {
        runScreenshotTest("MessageSendingAnimationFlightPreview", height = 232.dp) {
            MessageSendingAnimationFlightPreview()
        }
    }

    @Test
    fun `messages arrive at their recipients`() {
        runScreenshotTest("MessageSendingAnimationEndPreview", height = 232.dp) {
            MessageSendingAnimationEndPreview()
        }
    }

    @Test
    fun `message delivery supports dark theme`() {
        runScreenshotTest("MessageSendingAnimationDarkPreview", height = 232.dp) {
            MessageSendingAnimationDarkPreview()
        }
    }
}
