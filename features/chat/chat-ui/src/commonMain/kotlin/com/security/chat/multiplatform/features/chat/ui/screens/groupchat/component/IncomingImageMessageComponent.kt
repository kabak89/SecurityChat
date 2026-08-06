package com.security.chat.multiplatform.features.chat.ui.screens.groupchat.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.chat.ui.screens.groupchat.entity.MessageUM

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun IncomingImageMessageComponent(
    modifier: Modifier = Modifier,
    message: MessageUM.Incoming.Image,
    showSenderName: Boolean,
    onClicked: () -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    isFullscreenVisible: Boolean,
) {
    Column(
        modifier = modifier
            .padding(all = 16.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Spacer(Modifier.height(16.dp))
        if (showSenderName) {
            Row(
                modifier = Modifier,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = AppTheme.colors.element,
                            shape = AppTheme.shapes.circle,
                        ),
                ) {
                    val abbreviation =
                        remember(message.senderName) { message.senderName.take(2).uppercase() }
                    Text(
                        modifier = Modifier
                            .align(alignment = Alignment.Center),
                        text = abbreviation,
                        color = AppTheme.colors.textSecondary,
                        style = AppTheme.typography.body,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message.senderName,
                    color = AppTheme.colors.textPrimary,
                    style = AppTheme.typography.body,
                )
            }
            Spacer(modifier.height(8.dp))
        }
        if (sharedTransitionScope != null) {
            AnimatedVisibility(
                visible = !isFullscreenVisible,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                with(sharedTransitionScope) {
                    ImageComponent(
                        modifier = Modifier
                            .widthIn(min = 120.dp, max = 260.dp)
                            .heightIn(min = 120.dp, max = 340.dp)
                            .sharedElement(
                                sharedContentState = rememberSharedContentState(key = message.id),
                                animatedVisibilityScope = this@AnimatedVisibility,
                            )
                            .clickable(onClick = onClicked)
                            .clip(AppTheme.shapes.roundedRectangle8),
                        filePath = message.filePath,
                    )
                }
            }
        } else {
            ImageComponent(
                modifier = Modifier
                    .widthIn(min = 120.dp, max = 260.dp)
                    .heightIn(min = 120.dp, max = 340.dp)
                    .clickable(onClick = onClicked)
                    .clip(AppTheme.shapes.roundedRectangle8),
                filePath = message.filePath,
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

@Preview
@Composable
internal fun IncomingImageMessageComponentPreview() {
    AppTheme {
        IncomingImageMessageComponent(
            modifier = Modifier
                .background(AppTheme.colors.backgroundPrimary)
                .fillMaxWidth(),
            message = MessageUM.Incoming.Image(
                id = "1",
                text = "image",
                datetimeText = "12:10",
                filePath = "",
                senderName = "John",
            ),
            showSenderName = true,
            onClicked = {},
            sharedTransitionScope = null,
            isFullscreenVisible = false,
        )
    }
}
