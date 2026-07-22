package com.security.chat.multiplatform.features.chat.ui.screens.groupchat.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.chat.ui.screens.groupchat.entity.MessageUM

@Composable
internal fun ImageMessageComponent(
    modifier: Modifier = Modifier,
    message: MessageUM,
    isOutgoing: Boolean,
) {
    FlowRow(
        modifier = modifier
            .padding(all = 16.dp),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
    ) {
        Text(
            modifier = Modifier
                .padding(
                    start = if (isOutgoing) 40.dp else 0.dp,
                    end = if (isOutgoing) 0.dp else 40.dp,
                ),
            text = message.text,
            color = AppTheme.colors.textPrimary,
            style = AppTheme.typography.body,
        )
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
internal fun IncomingImageMessageComponentPreview() {
    AppTheme {
        ImageMessageComponent(
            modifier = Modifier.background(AppTheme.colors.backgroundPrimary),
            message = MessageUM.Incoming.Image(
                id = "1",
                text = "image",
                datetimeText = "12:10",
            ),
            isOutgoing = false,
        )
    }
}

@Preview
@Composable
internal fun OutgoingImageMessageComponentPreview() {
    AppTheme {
        ImageMessageComponent(
            modifier = Modifier.background(AppTheme.colors.backgroundPrimary),
            message = MessageUM.Outgoing.Image(
                id = "1",
                text = "image",
                datetimeText = "12:10",
            ),
            isOutgoing = true,
        )
    }
}
