package com.security.chat.multiplatform.features.onboarding.ui.screens.welcome

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ScrollAnimationProgressTest {

    @Test
    fun `animation starts at bottom of viewport and finishes at top`() {
        assertEquals(0f, scrollAnimationProgress(700f, 200, 900))
        assertEquals(0.5f, scrollAnimationProgress(350f, 200, 900))
        assertEquals(1f, scrollAnimationProgress(0f, 200, 900))
    }

    @Test
    fun `animation stays within bounds outside viewport`() {
        assertEquals(0f, scrollAnimationProgress(1000f, 200, 900))
        assertEquals(1f, scrollAnimationProgress(-250f, 200, 900))
    }

    @Test
    fun `unmeasured layout starts at zero`() {
        assertEquals(0f, scrollAnimationProgress(0f, 0, 900))
        assertEquals(0f, scrollAnimationProgress(0f, 200, 0))
    }
}
