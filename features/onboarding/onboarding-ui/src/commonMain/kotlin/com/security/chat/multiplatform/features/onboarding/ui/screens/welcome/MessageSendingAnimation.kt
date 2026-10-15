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
import securitychat.common.icons_kit.generated.resources.ic_send
import securitychat.common.icons_kit.generated.resources.ic_user

@Composable
internal fun MessageSendingAnimation(
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
        val avatarSize = minOf(64.dp, maxHeight * 0.29f)
        val messageSize = minOf(52.dp, maxHeight * 0.24f)
        val lockSize = messageSize * 0.48f
        val keySize = avatarSize * 0.48f
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val avatarPx = with(density) { avatarSize.toPx() }
        val messagePx = with(density) { messageSize.toPx() }
        val distance = widthPx * 0.32f
        val firstUserColor = if (AppTheme.isDarkTheme) Color(0xFF93B4FF) else Color(0xFF4F6BED)
        val secondUserColor = if (AppTheme.isDarkTheme) Color(0xFFFFC080) else Color(0xFFB86D24)

        Canvas(Modifier.fillMaxSize()) {
            val baseline = size.height * 0.25f
            val arc = size.height * 0.14f
            val stroke = Stroke(
                width = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 5.dp.toPx())),
            )
            for (side in listOf(-1, 1)) {
                val startX = center.x + distance * side
                val endX = center.x - distance * side
                val controlY = baseline + side * arc * 4f / 3f
                val path = Path().apply {
                    moveTo(startX, baseline)
                    cubicTo(
                        startX + (endX - startX) / 3f, controlY,
                        startX + (endX - startX) * 2f / 3f, controlY,
                        endX, baseline,
                    )
                }
                val color = if (side == -1) firstUserColor else secondUserColor
                drawPath(path, color.copy(alpha = 0.25f), style = stroke)
            }
        }

        for (side in listOf(-1, 1)) {
            val sender = if (side == -1) "left" else "right"
            val senderColor = if (side == -1) firstUserColor else secondUserColor
            val recipientColor = if (side == -1) secondUserColor else firstUserColor

            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .graphicsLayer {
                        translationX = distance * side
                        translationY = heightPx * 0.16f
                    }
                    .background(AppTheme.colors.backgroundPrimary, CircleShape)
                    .border(1.dp, senderColor.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier.size(avatarSize * 0.52f),
                    imageVector = vectorResource(DrawableRes.ic_user),
                    contentDescription = null,
                    tint = senderColor,
                )
            }
            Box(
                modifier = Modifier
                    .size(keySize)
                    .graphicsLayer {
                        translationX = distance * side
                        translationY = heightPx * 0.4f
                    }
                    .testTag("message_key_$sender")
                    .background(AppTheme.colors.backgroundPrimary, CircleShape)
                    .border(1.dp, senderColor.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier.size(keySize * 0.55f),
                    imageVector = vectorResource(DrawableRes.ic_key),
                    contentDescription = null,
                    tint = senderColor,
                )
            }
            Box(
                modifier = Modifier
                    .size(messageSize)
                    .graphicsLayer {
                        val creation = messageSendingStage(progress(), 0f, 0.25f)
                        val flight = messageSendingStage(progress(), 0.55f, 1f)
                        alpha = creation
                        scaleX = 0.5f + creation * 0.5f
                        scaleY = scaleX
                        translationX = distance * side * (1f - 2f * flight)
                        translationY = -heightPx * 0.25f +
                                side * heightPx * 0.14f * 4f * flight * (1f - flight)
                    }
                    .testTag("message_plane_$sender")
                    .background(
                        AppTheme.colors.backgroundPrimary,
                        AppTheme.shapes.roundedRectangle16,
                    )
                    .border(
                        1.dp,
                        senderColor.copy(alpha = 0.45f),
                        AppTheme.shapes.roundedRectangle16,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier
                        .size(messageSize * 0.58f)
                        .graphicsLayer { rotationZ = if (side == -1) 0f else 180f },
                    imageVector = vectorResource(DrawableRes.ic_send),
                    contentDescription = null,
                    tint = senderColor,
                )
            }
            Box(
                modifier = Modifier
                    .size(lockSize)
                    .graphicsLayer {
                        val protection = messageSendingStage(progress(), 0.25f, 0.5f)
                        val flight = messageSendingStage(progress(), 0.55f, 1f)
                        val restingX = distance * side - side * avatarPx * 0.4f
                        val restingY = heightPx * 0.16f - avatarPx * 0.45f
                        val attachedX = distance * side - side * messagePx * 0.32f
                        val attachedY = -heightPx * 0.25f + messagePx * 0.32f
                        translationX = restingX + (attachedX - restingX) * protection -
                                2f * distance * side * flight
                        translationY = restingY + (attachedY - restingY) * protection +
                                side * heightPx * 0.14f * 4f * flight * (1f - flight)
                    }
                    .testTag("message_lock_$sender")
                    .background(AppTheme.colors.backgroundPrimary, CircleShape)
                    .border(1.dp, recipientColor.copy(alpha = 0.65f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier.size(lockSize * 0.6f),
                    imageVector = vectorResource(DrawableRes.ic_lock),
                    contentDescription = null,
                    tint = recipientColor,
                )
            }
        }
    }
}

private fun messageSendingStage(progress: Float, start: Float, end: Float): Float =
    FastOutSlowInEasing.transform(((progress - start) / (end - start)).coerceIn(0f, 1f))

@Preview
@Composable
internal fun MessageSendingAnimationStartPreview() {
    AppTheme {
        Box(Modifier.size(412.dp, 232.dp).background(AppTheme.colors.backgroundPrimary)) {
            MessageSendingAnimation(progress = { 0f })
        }
    }
}

@Preview
@Composable
internal fun MessageSendingAnimationCreatedPreview() {
    AppTheme {
        Box(Modifier.size(412.dp, 232.dp).background(AppTheme.colors.backgroundPrimary)) {
            MessageSendingAnimation(progress = { 0.25f })
        }
    }
}

@Preview
@Composable
internal fun MessageSendingAnimationProtectedPreview() {
    AppTheme {
        Box(Modifier.size(412.dp, 232.dp).background(AppTheme.colors.backgroundPrimary)) {
            MessageSendingAnimation(progress = { 0.55f })
        }
    }
}

@Preview
@Composable
internal fun MessageSendingAnimationFlightPreview() {
    AppTheme {
        Box(Modifier.size(412.dp, 232.dp).background(AppTheme.colors.backgroundPrimary)) {
            MessageSendingAnimation(progress = { 0.75f })
        }
    }
}

@Preview
@Composable
internal fun MessageSendingAnimationEndPreview() {
    AppTheme {
        Box(Modifier.size(412.dp, 232.dp).background(AppTheme.colors.backgroundPrimary)) {
            MessageSendingAnimation(progress = { 1f })
        }
    }
}

@Preview
@Composable
internal fun MessageSendingAnimationDarkPreview() {
    AppTheme(useDarkTheme = true) {
        Box(Modifier.size(412.dp, 232.dp).background(AppTheme.colors.backgroundPrimary)) {
            MessageSendingAnimation(progress = { 1f })
        }
    }
}
