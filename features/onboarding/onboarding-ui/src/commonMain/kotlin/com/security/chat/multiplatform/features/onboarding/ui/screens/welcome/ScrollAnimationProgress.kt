package com.security.chat.multiplatform.features.onboarding.ui.screens.welcome

internal fun scrollAnimationProgress(
    animationTop: Float,
    animationHeight: Int,
    viewportHeight: Int,
): Float {
    if (viewportHeight <= 0 || animationHeight <= 0) return 0f
    val travel = (viewportHeight - animationHeight).coerceAtLeast(1)
    return (1f - animationTop / travel).coerceIn(0f, 1f)
}
