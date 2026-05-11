package com.security.chat.multiplatform.features.chat.ui.screens.personalchat.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.chat.ui.screens.personalchat.entity.MessageUM

@Composable
internal fun IncomingMessageComponent(
    modifier: Modifier = Modifier,
    message: MessageUM.Incoming,
) {
    FlowRow(
        modifier = modifier
            .padding(all = 16.dp)
            .padding(end = 40.dp),
        horizontalArrangement = Arrangement.Start,
    ) {
        Text(
            modifier = Modifier
                .padding(end = 8.dp),
            text = message.text,
            color = AppTheme.colors.textPrimary,
            style = AppTheme.typography.body,
        )
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
internal fun IncomingMessageComponentPreview() {
    AppTheme {
        IncomingMessageComponent(
            modifier = Modifier.background(AppTheme.colors.backgroundPrimary),
            message = MessageUM.Incoming(
                id = "1",
                text = "some text",
                datetimeText = "12:10",
            ),
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
            ),
        )
    }
}