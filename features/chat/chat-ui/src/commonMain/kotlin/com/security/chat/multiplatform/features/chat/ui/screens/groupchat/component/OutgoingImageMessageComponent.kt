package com.security.chat.multiplatform.features.chat.ui.screens.groupchat.component

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.chat.ui.screens.groupchat.entity.MessageStatusUM
import com.security.chat.multiplatform.features.chat.ui.screens.groupchat.entity.MessageUM

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun OutgoingImageMessageComponent(
    modifier: Modifier = Modifier,
    message: MessageUM.Outgoing.Image,
    onClicked: () -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    isFullscreenVisible: Boolean = false,
) {
    Column(
        modifier = modifier
            .padding(all = 16.dp),
        horizontalAlignment = Alignment.End,
    ) {
        if (sharedTransitionScope != null) {
            var imageSize by remember { mutableStateOf<IntSize?>(null) }
            val density = LocalDensity.current

            Box(
                modifier = Modifier
                    .then(
                        if (isFullscreenVisible && imageSize != null) {
                            Modifier.size(
                                width = with(density) { imageSize!!.width.toDp() },
                                height = with(density) { imageSize!!.height.toDp() },
                            )
                        } else {
                            Modifier
                        },
                    ),
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isFullscreenVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    with(sharedTransitionScope) {
                        OutgoingImageContent(
                            modifier = Modifier
                                .widthIn(min = 120.dp, max = 260.dp)
                                .heightIn(min = 120.dp, max = 340.dp)
                                .onGloballyPositioned {
                                    if (!isFullscreenVisible) {
                                        imageSize = it.size
                                    }
                                }
                                .sharedElement(
                                    sharedContentState = rememberSharedContentState(key = message.id),
                                    animatedVisibilityScope = this@AnimatedVisibility,
                                )
                                .clickable(onClick = onClicked)
                                .clip(AppTheme.shapes.roundedRectangle8),
                            message = message,
                        )
                    }
                }
            }
        } else {
            OutgoingImageContent(
                modifier = Modifier
                    .widthIn(min = 120.dp, max = 260.dp)
                    .heightIn(min = 120.dp, max = 340.dp)
                    .clickable(onClick = onClicked)
                    .clip(AppTheme.shapes.roundedRectangle8),
                message = message,
            )
        }
        Spacer(modifier.height(8.dp))
        Text(
            modifier = Modifier,
            text = message.datetimeText,
            color = AppTheme.colors.textPrimary,
            style = AppTheme.typography.annotation,
        )
    }
}

@Composable
private fun OutgoingImageContent(
    modifier: Modifier,
    message: MessageUM.Outgoing.Image,
) {
    Box(
        modifier = modifier,
        propagateMinConstraints = true,
    ) {
        ImageComponent(filePath = message.filePath)
        if (message.status == MessageStatusUM.Sending) {
            val backgroundPrimary = AppTheme.colors.backgroundPrimary
            val progressBackground = remember(backgroundPrimary) {
                backgroundPrimary.copy(alpha = 0.8f)
            }
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .wrapContentSize()
                    .background(
                        color = progressBackground,
                        shape = AppTheme.shapes.circle,
                    )
                    .padding(10.dp)
                    .size(28.dp),
                color = AppTheme.colors.textPrimary,
                strokeWidth = 3.dp,
            )
        }
    }
}

@Preview
@Composable
internal fun OutgoingOutgoingImageMessageComponentPreview() {
    OutgoingImageMessagePreview(status = MessageStatusUM.Sent)
}

@Preview
@Composable
internal fun OutgoingImageMessageSendingPreview() {
    OutgoingImageMessagePreview(status = MessageStatusUM.Sending)
}

@Composable
private fun OutgoingImageMessagePreview(status: MessageStatusUM) {
    AppTheme {
        OutgoingImageMessageComponent(
            modifier = Modifier
                .background(AppTheme.colors.backgroundPrimary)
                .fillMaxWidth(),
            message = MessageUM.Outgoing.Image(
                id = "1",
                text = "image",
                datetimeText = "12:10",
                filePath = "",
                status = status,
            ),
            onClicked = {},
        )
    }
}
