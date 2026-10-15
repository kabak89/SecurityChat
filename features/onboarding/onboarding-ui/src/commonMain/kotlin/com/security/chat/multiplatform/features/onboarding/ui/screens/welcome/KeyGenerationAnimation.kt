package com.security.chat.multiplatform.features.onboarding.ui.screens.welcome

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.security.chat.multiplatform.common.icons.kit.DrawableRes
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import org.jetbrains.compose.resources.vectorResource
import securitychat.common.icons_kit.generated.resources.ic_key
import securitychat.common.icons_kit.generated.resources.ic_lock
import securitychat.common.icons_kit.generated.resources.ic_user

@Composable
internal fun KeyGenerationAnimation(
    progress: () -> Float,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .background(
                color = AppTheme.colors.backgroundSecondary.copy(alpha = 0.12f),
                shape = AppTheme.shapes.roundedRectangle16,
            ),
        contentAlignment = Alignment.Center,
    ) {
        val avatarSize = minOf(88.dp, maxHeight * 0.46f)
        val badgeSize = avatarSize * 0.72f
        val distance = with(LocalDensity.current) { (maxWidth * 0.28f).toPx() }
        val lineColor = AppTheme.colors.iconPrimary.copy(alpha = 0.2f)
        Canvas(Modifier.fillMaxSize()) {
            val keyProgress = animationStage(progress(), start = 0.1f, end = 0.55f)
            val lockProgress = animationStage(progress(), start = 0.55f, end = 1f)
            drawLine(
                color = lineColor,
                start = center,
                end = center - Offset(distance * keyProgress, 0f),
                alpha = keyProgress,
                strokeWidth = 2.dp.toPx(),
            )
            drawLine(
                color = lineColor,
                start = center,
                end = center + Offset(distance * lockProgress, 0f),
                alpha = lockProgress,
                strokeWidth = 2.dp.toPx(),
            )
        }
        Box(
            modifier = Modifier
                .size(badgeSize)
                .graphicsLayer {
                    val stage = animationStage(progress(), start = 0.1f, end = 0.55f)
                    alpha = stage
                    scaleX = 0.5f + stage * 0.5f
                    scaleY = scaleX
                    translationX = -distance * stage
                    rotationZ = -30f * (1f - stage)
                }
                .background(AppTheme.colors.backgroundPrimary, CircleShape)
                .border(1.dp, lineColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(badgeSize * 0.5f),
                imageVector = vectorResource(DrawableRes.ic_key),
                contentDescription = null,
                tint = AppTheme.colors.iconPrimary,
            )
        }
        Box(
            modifier = Modifier
                .size(badgeSize)
                .graphicsLayer {
                    val stage = animationStage(progress(), start = 0.55f, end = 1f)
                    alpha = stage
                    scaleX = 0.5f + stage * 0.5f
                    scaleY = scaleX
                    translationX = distance * stage
                    rotationZ = 20f * (1f - stage)
                }
                .background(AppTheme.colors.backgroundPrimary, CircleShape)
                .border(1.dp, lineColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(badgeSize * 0.5f),
                imageVector = vectorResource(DrawableRes.ic_lock),
                contentDescription = null,
                tint = AppTheme.colors.iconPrimary,
            )
        }
        Box(
            modifier = Modifier
                .size(avatarSize)
                .background(AppTheme.colors.backgroundPrimary, CircleShape)
                .border(1.dp, lineColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(avatarSize * 0.5f),
                imageVector = vectorResource(DrawableRes.ic_user),
                contentDescription = null,
                tint = AppTheme.colors.iconPrimary,
            )
        }
    }
}

private fun animationStage(progress: Float, start: Float, end: Float): Float =
    FastOutSlowInEasing.transform(((progress - start) / (end - start)).coerceIn(0f, 1f))

@Preview
@Composable
internal fun KeyGenerationAnimationStartPreview() {
    AppTheme {
        Box(Modifier.size(412.dp, 232.dp).background(AppTheme.colors.backgroundPrimary)) {
            KeyGenerationAnimation(progress = { 0f })
        }
    }
}

@Preview
@Composable
internal fun KeyGenerationAnimationKeyPreview() {
    AppTheme {
        Box(Modifier.size(412.dp, 232.dp).background(AppTheme.colors.backgroundPrimary)) {
            KeyGenerationAnimation(progress = { 0.55f })
        }
    }
}

@Preview
@Composable
internal fun KeyGenerationAnimationEndPreview() {
    AppTheme {
        Box(Modifier.size(412.dp, 232.dp).background(AppTheme.colors.backgroundPrimary)) {
            KeyGenerationAnimation(progress = { 1f })
        }
    }
}

@Preview
@Composable
internal fun KeyGenerationAnimationDarkPreview() {
    AppTheme(useDarkTheme = true) {
        Box(Modifier.size(412.dp, 232.dp).background(AppTheme.colors.backgroundPrimary)) {
            KeyGenerationAnimation(progress = { 1f })
        }
    }
}
