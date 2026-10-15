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

@OptIn(ExperimentalTestApi::class)

class KeyExchangeAnimationTest : ScreenshotTestBase() {

    @Test
    fun `only locks switch owners and reverse progress restores their positions`() {
        runSkikoComposeUiTest(size = Size(1024f, 232f)) {
            val progress = mutableFloatStateOf(0f)
            setContent {
                AppTheme {
                    Box(Modifier.size(412.dp, 232.dp)) {
                        KeyExchangeAnimation(progress = { progress.floatValue })
                    }
                }
            }
            val leftKey = onNodeWithTag("exchange_key_left")
            val rightKey = onNodeWithTag("exchange_key_right")
            val leftLock = onNodeWithTag("exchange_lock_left")
            val rightLock = onNodeWithTag("exchange_lock_right")
            val originalLeftKey = leftKey.fetchSemanticsNode().boundsInRoot
            val originalRightKey = rightKey.fetchSemanticsNode().boundsInRoot
            val originalLeftLock = leftLock.fetchSemanticsNode().boundsInRoot
            val originalRightLock = rightLock.fetchSemanticsNode().boundsInRoot

            runOnIdle { progress.floatValue = 0.5f }
            assertEquals(originalLeftKey, leftKey.fetchSemanticsNode().boundsInRoot)
            assertEquals(originalRightKey, rightKey.fetchSemanticsNode().boundsInRoot)

            runOnIdle { progress.floatValue = 1f }
            assertEquals(originalLeftKey, leftKey.fetchSemanticsNode().boundsInRoot)
            assertEquals(originalRightKey, rightKey.fetchSemanticsNode().boundsInRoot)
            assertEquals(
                originalRightKey.center.x,
                leftLock.fetchSemanticsNode().boundsInRoot.center.x,
                absoluteTolerance = 0.01f,
            )
            assertEquals(
                originalLeftKey.center.x,
                rightLock.fetchSemanticsNode().boundsInRoot.center.x,
                absoluteTolerance = 0.01f,
            )

            runOnIdle { progress.floatValue = 0f }
            assertEquals(originalLeftLock, leftLock.fetchSemanticsNode().boundsInRoot)
            assertEquals(originalRightLock, rightLock.fetchSemanticsNode().boundsInRoot)
        }
    }

    @Test
    fun `users start with their own keys and locks`() {
        runScreenshotTest("KeyExchangeAnimationStartPreview", height = 232.dp) {
            KeyExchangeAnimationStartPreview()
        }
    }

    @Test
    fun `locks travel between users while keys stay in place`() {
        runScreenshotTest("KeyExchangeAnimationMiddlePreview", height = 232.dp) {
            KeyExchangeAnimationMiddlePreview()
        }
    }

    @Test
    fun `users finish with exchanged locks and their original keys`() {
        runScreenshotTest("KeyExchangeAnimationEndPreview", height = 232.dp) {
            KeyExchangeAnimationEndPreview()
        }
    }

    @Test
    fun `exchange supports dark theme`() {
        runScreenshotTest("KeyExchangeAnimationDarkPreview", height = 232.dp) {
            KeyExchangeAnimationDarkPreview()
        }
    }
}
