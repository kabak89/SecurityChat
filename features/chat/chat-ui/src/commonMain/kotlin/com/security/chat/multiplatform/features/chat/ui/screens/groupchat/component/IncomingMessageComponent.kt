package com.security.chat.multiplatform.features.chat.ui.screens.groupchat.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.chat.ui.screens.groupchat.entity.MessageUM
import sh.calvin.autolinktext.rememberAutoLinkText

@Composable
internal fun IncomingMessageComponent(
    modifier: Modifier = Modifier,
    message: MessageUM.Incoming,
    showSenderName: Boolean,
) {
    Column(
        modifier = modifier,
    ) {
        Spacer(Modifier.height(16.dp))
        if (showSenderName) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp),
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
            Spacer(Modifier.height(8.dp))
        }
        FlowRow(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(end = 40.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.Start,
        ) {
            SelectionContainer {
                Text(
                    modifier = Modifier
                        .padding(end = 8.dp),
                    text = AnnotatedString.rememberAutoLinkText(text = message.text),
                    color = AppTheme.colors.textPrimary,
                    style = AppTheme.typography.body,
                )
            }
            Text(
                modifier = Modifier
                    .align(alignment = Alignment.Bottom),
                text = message.datetimeText,
                color = AppTheme.colors.textPrimary,
                style = AppTheme.typography.annotation,
            )
        }
    }
}

@Preview
@Composable
internal fun IncomingMessageComponentPreview() {
    AppTheme {
        IncomingMessageComponent(
            modifier = Modifier.background(AppTheme.colors.backgroundPrimary),
            message = MessageUM.Incoming(
                id = "1",
                text = "some text",
                datetimeText = "12:10",
                senderName = "John",
            ),
            showSenderName = true,
        )
    }
}

@Preview
@Composable
internal fun IncomingMessageComponentLongTextPreview() {
    AppTheme {
        IncomingMessageComponent(
            modifier = Modifier.background(AppTheme.colors.backgroundPrimary),
            message = MessageUM.Incoming(
                id = "1",
                text = "some text text text text text text text text text text text text",
                datetimeText = "12:10",
                senderName = "John",
            ),
            showSenderName = true,
        )
    }
}

@Preview
@Composable
internal fun IncomingMessageComponentNoSenderName() {
    AppTheme {
        IncomingMessageComponent(
            modifier = Modifier.background(AppTheme.colors.backgroundPrimary),
            message = MessageUM.Incoming(
                id = "1",
                text = "some text text text text text text text text text text text text",
                datetimeText = "12:10",
                senderName = "John",
            ),
            showSenderName = false,
        )
    }
}