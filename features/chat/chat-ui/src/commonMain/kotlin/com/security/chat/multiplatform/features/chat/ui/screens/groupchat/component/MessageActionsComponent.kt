package com.security.chat.multiplatform.features.chat.ui.screens.groupchat.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.chat.ui.screens.groupchat.entity.MessageStatusUM
import com.security.chat.multiplatform.features.chat.ui.screens.groupchat.entity.MessageUM
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import securitychat.common.localization.generated.resources.group_chat_delete_message
import securitychat.common.localization.generated.resources.group_chat_reply

internal const val MESSAGE_REPLY_ACTION_TAG: String = "message_reply_action"
internal const val MESSAGE_DELETE_ACTION_TAG: String = "message_delete_action"

@Composable
internal fun MessageActionsComponent(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    enabled: Boolean,
    onClicked: () -> Unit,
    onDismiss: () -> Unit,
    onReplyClicked: () -> Unit,
    onDeleteClicked: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled, onClick = onClicked),
    ) {
        content()
        val menuAlignment = if (onDeleteClicked == null) {
            Alignment.BottomStart
        } else {
            Alignment.BottomEnd
        }
        Box(modifier = Modifier.align(menuAlignment).padding(horizontal = 16.dp)) {
            DropdownMenu(
                modifier = Modifier
                    .width(240.dp),
                expanded = expanded,
                onDismissRequest = onDismiss,
                shape = AppTheme.shapes.roundedRectangle8,
                containerColor = AppTheme.colors.backgroundSecondary,
            ) {
                MessageActionsMenuItem(
                    label = StringRes.group_chat_reply,
                    tag = MESSAGE_REPLY_ACTION_TAG,
                    onClick = onReplyClicked,
                )
                if (onDeleteClicked != null) {
                    MessageActionsMenuItem(
                        label = StringRes.group_chat_delete_message,
                        tag = MESSAGE_DELETE_ACTION_TAG,
                        onClick = onDeleteClicked,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
internal fun MessageActionsMenuPreview() {
    AppTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.backgroundPrimary),
        ) {
            MessageActionsComponent(
                modifier = Modifier.fillMaxWidth(),
                expanded = true,
                enabled = true,
                onClicked = {},
                onDismiss = {},
                onReplyClicked = {},
                onDeleteClicked = {},
            ) {
                OutgoingMessageComponent(
                    modifier = Modifier.fillMaxWidth(),
                    message = MessageUM.Outgoing.Text(
                        id = "message",
                        text = "some text",
                        datetimeText = "12:10",
                        status = MessageStatusUM.Sent,
                    ),
                )
            }
        }
    }
}

@Composable
private fun MessageActionsMenuItem(label: StringResource, tag: String, onClick: () -> Unit) {
    DropdownMenuItem(
        modifier = Modifier.testTag(tag),
        text = {
            Text(
                text = stringResource(label),
                color = AppTheme.colors.textPrimary,
                style = AppTheme.typography.body,
            )
        },
        onClick = onClick,
    )
}
