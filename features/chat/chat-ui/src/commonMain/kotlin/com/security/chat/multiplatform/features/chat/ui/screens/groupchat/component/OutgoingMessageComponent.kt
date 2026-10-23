package com.security.chat.multiplatform.features.chat.ui.screens.groupchat.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.chat.ui.screens.groupchat.entity.MessageStatusUM
import com.security.chat.multiplatform.features.chat.ui.screens.groupchat.entity.MessageUM
import sh.calvin.autolinktext.rememberAutoLinkText

@Composable
internal fun OutgoingMessageComponent(
    modifier: Modifier = Modifier,
    message: MessageUM.Outgoing.Text,
) {
    FlowRow(
        modifier = modifier
            .padding(all = 16.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        SelectionContainer {
            Text(
                modifier = Modifier
                    .padding(start = 40.dp),
                text = AnnotatedString.rememberAutoLinkText(text = message.text),
                color = AppTheme.colors.textPrimary,
                style = AppTheme.typography.body,
            )
        }
        OutgoingMessageMetadata(
            modifier = Modifier
                .align(alignment = Alignment.Bottom)
                .padding(start = 8.dp),
            datetimeText = message.datetimeText,
            status = message.status,
        )
    }
}

@Preview
@Composable
internal fun OutgoingMessageComponentPreview() {
    AppTheme {
        OutgoingMessageComponent(
            modifier = Modifier.background(AppTheme.colors.backgroundPrimary),
            message = MessageUM.Outgoing.Text(
                id = "1",
                text = "some text",
                datetimeText = "12:10",
                status = MessageStatusUM.Sent,
            ),
        )
    }
}

@Preview
@Composable
internal fun OutgoingMessageComponentSendingPreview() {
    AppTheme {
        OutgoingMessageComponent(
            modifier = Modifier
                .background(AppTheme.colors.backgroundPrimary)
                .fillMaxWidth(),
            message = MessageUM.Outgoing.Text(
                id = "1",
                text = "some text text text text text text text text text text text text",
                datetimeText = "12:10",
                status = MessageStatusUM.Created,
            ),
        )
    }
}

@Preview
@Composable
internal fun OutgoingMessageComponentLongTextPreview() {
    AppTheme {
        OutgoingMessageComponent(
            modifier = Modifier.background(AppTheme.colors.backgroundPrimary),
            message = MessageUM.Outgoing.Text(
                id = "1",
                text = "some text text text text text text text text text text text text",
                datetimeText = "12:10",
                status = MessageStatusUM.Sent,
            ),
        )
    }
}
