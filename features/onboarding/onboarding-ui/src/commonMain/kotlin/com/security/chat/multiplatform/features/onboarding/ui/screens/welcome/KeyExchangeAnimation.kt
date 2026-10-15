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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.security.chat.multiplatform.common.icons.kit.DrawableRes
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import org.jetbrains.compose.resources.vectorResource
import securitychat.common.icons_kit.generated.resources.ic_key
import securitychat.common.icons_kit.generated.resources.ic_lock
import securitychat.common.icons_kit.generated.resources.ic_user

@Composable
internal fun KeyExchangeAnimation(
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
        val avatarSize = minOf(72.dp, maxHeight * 0.34f)
        val badgeSize = avatarSize * 0.52f
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
        val distance = widthPx * 0.3f
        val firstUserColor = if (AppTheme.isDarkTheme) Color(0xFF93B4FF) else Color(0xFF4F6BED)
        val secondUserColor = if (AppTheme.isDarkTheme) Color(0xFFFFC080) else Color(0xFFB86D24)

        Canvas(Modifier.fillMaxSize()) {
            val baseline = size.height * 0.2f
            val arc = size.height * 0.1f
            val left = center.x - distance
            val right = center.x + distance
            val upperPath = Path().apply {
                moveTo(left, baseline)
                cubicTo(
                    left + distance * 2f / 3f, baseline - arc * 4f / 3f,
                    right - distance * 2f / 3f, baseline - arc * 4f / 3f,
                    right, baseline,
                )
            }
            val lowerPath = Path().apply {
                moveTo(right, baseline)
                cubicTo(
                    right - distance * 2f / 3f, baseline + arc * 4f / 3f,
                    left + distance * 2f / 3f, baseline + arc * 4f / 3f,
                    left, baseline,
                )
            }
            val stroke = Stroke(
                width = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 5.dp.toPx())),
            )
            drawPath(upperPath, firstUserColor.copy(alpha = 0.3f), style = stroke)
            drawPath(lowerPath, secondUserColor.copy(alpha = 0.3f), style = stroke)
        }

        for (side in listOf(-1, 1)) {
            val ownerColor = if (side == -1) firstUserColor else secondUserColor
            val owner = if (side == -1) "left" else "right"
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .graphicsLayer {
                        translationX = distance * side
                        translationY = heightPx * 0.06f
                    }
                    .background(AppTheme.colors.backgroundPrimary, CircleShape)
                    .border(1.dp, ownerColor.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier.size(avatarSize * 0.52f),
                    imageVector = vectorResource(DrawableRes.ic_user),
                    contentDescription = null,
                    tint = ownerColor,
                )
            }
            Box(
                modifier = Modifier
                    .size(badgeSize)
                    .graphicsLayer {
                        translationX = distance * side
                        translationY = heightPx * 0.36f
                    }
                    .testTag("exchange_key_$owner")
                    .background(AppTheme.colors.backgroundPrimary, CircleShape)
                    .border(1.dp, ownerColor.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier.size(badgeSize * 0.55f),
                    imageVector = vectorResource(DrawableRes.ic_key),
                    contentDescription = null,
                    tint = ownerColor,
                )
            }
            Box(
                modifier = Modifier
                    .size(badgeSize)
                    .graphicsLayer {
                        val stage = FastOutSlowInEasing.transform(
                            ((progress() - 0.1f) / 0.8f).coerceIn(0f, 1f),
                        )
                        val arcProgress = 4f * stage * (1f - stage)
                        translationX = distance * side * (1f - 2f * stage)
                        translationY = -heightPx * 0.3f + heightPx * 0.1f * side * arcProgress
                        rotationZ = 12f * side * arcProgress
                    }
                    .testTag("exchange_lock_$owner")
                    .background(AppTheme.colors.backgroundPrimary, CircleShape)
                    .border(1.dp, ownerColor.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier.size(badgeSize * 0.55f),
                    imageVector = vectorResource(DrawableRes.ic_lock),
                    contentDescription = null,
                    tint = ownerColor,
                )
            }
        }
    }
}

@Preview
@Composable
internal fun KeyExchangeAnimationStartPreview() {
    AppTheme {
        Box(Modifier.size(412.dp, 232.dp).background(AppTheme.colors.backgroundPrimary)) {
            KeyExchangeAnimation(progress = { 0f })
        }
    }
}

@Preview
@Composable
internal fun KeyExchangeAnimationMiddlePreview() {
    AppTheme {
        Box(Modifier.size(412.dp, 232.dp).background(AppTheme.colors.backgroundPrimary)) {
            KeyExchangeAnimation(progress = { 0.5f })
        }
    }
}

@Preview
@Composable
internal fun KeyExchangeAnimationEndPreview() {
    AppTheme {
        Box(Modifier.size(412.dp, 232.dp).background(AppTheme.colors.backgroundPrimary)) {
            KeyExchangeAnimation(progress = { 1f })
        }
    }
}

@Preview
@Composable
internal fun KeyExchangeAnimationDarkPreview() {
    AppTheme(useDarkTheme = true) {
        Box(Modifier.size(412.dp, 232.dp).background(AppTheme.colors.backgroundPrimary)) {
            KeyExchangeAnimation(progress = { 1f })
        }
    }
}
