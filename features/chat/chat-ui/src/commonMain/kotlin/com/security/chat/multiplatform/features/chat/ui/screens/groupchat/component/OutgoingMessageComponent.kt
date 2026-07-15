package com.security.chat.multiplatform.features.chat.ui.screens.groupchat.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.chat.ui.screens.groupchat.entity.MessageUM
import sh.calvin.autolinktext.rememberAutoLinkText

@Composable
internal fun OutgoingMessageComponent(
    modifier: Modifier = Modifier,
    message: MessageUM.Outgoing,
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
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            modifier = Modifier
                .align(alignment = Alignment.Bottom),
            text = message.datetimeText,
            color = AppTheme.colors.textPrimary,
            style = AppTheme.typography.annotation,
        )
    }
}

@Preview
@Composable
internal fun OutgoingMessageComponentPreview() {
    AppTheme {
        OutgoingMessageComponent(
            modifier = Modifier.background(AppTheme.colors.backgroundPrimary),
            message = MessageUM.Outgoing(
                id = "1",
                text = "some text",
                datetimeText = "12:10",
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
            message = MessageUM.Outgoing(
                id = "1",
                text = "some text text text text text text text text text text text text",
                datetimeText = "12:10",
            ),
        )
    }
}