package com.security.chat.multiplatform.features.chat.ui.screens.groupchat.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.chat.ui.screens.groupchat.entity.MessageUM

@Composable
internal fun OutgoingImageMessageComponent(
    modifier: Modifier = Modifier,
    message: MessageUM.Outgoing.Image,
) {
    Column(
        modifier = modifier
            .padding(all = 16.dp),
        horizontalAlignment = Alignment.End,
    ) {
        ImageComponent(
            modifier = Modifier
                .widthIn(min = 120.dp, max = 260.dp)
                .heightIn(min = 120.dp, max = 340.dp)
                .clip(AppTheme.shapes.roundedRectangle8),
            filePath = message.filePath,
        )
        Spacer(modifier = Modifier.height(8.dp))
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
internal fun OutgoingOutgoingImageMessageComponentPreview() {
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
            ),
        )
    }
}
