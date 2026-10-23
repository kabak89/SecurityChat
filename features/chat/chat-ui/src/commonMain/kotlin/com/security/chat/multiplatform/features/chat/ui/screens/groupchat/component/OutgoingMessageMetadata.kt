package com.security.chat.multiplatform.features.chat.ui.screens.groupchat.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.chat.ui.screens.groupchat.entity.MessageStatusUM
import org.jetbrains.compose.resources.vectorResource
import securitychat.common.icons_kit.generated.resources.Res
import securitychat.common.icons_kit.generated.resources.ic_check
import securitychat.common.icons_kit.generated.resources.ic_double_check

@Composable
internal fun OutgoingMessageMetadata(
    modifier: Modifier = Modifier,
    datetimeText: String,
    status: MessageStatusUM,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val textColor = AppTheme.colors.textPrimary
        when (status) {
            MessageStatusUM.Created -> CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = textColor,
                strokeWidth = 2.dp,
            )

            MessageStatusUM.Sent -> Icon(
                modifier = Modifier.size(16.dp),
                imageVector = vectorResource(Res.drawable.ic_check),
                tint = textColor.copy(alpha = 0.5f),
                contentDescription = null,
            )

            MessageStatusUM.Received -> Icon(
                modifier = Modifier.size(16.dp),
                imageVector = vectorResource(Res.drawable.ic_double_check),
                tint = textColor.copy(alpha = 0.5f),
                contentDescription = null,
            )

            MessageStatusUM.Read -> Icon(
                modifier = Modifier.size(16.dp),
                imageVector = vectorResource(Res.drawable.ic_double_check),
                tint = textColor,
                contentDescription = null,
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = datetimeText,
            color = AppTheme.colors.textPrimary,
            style = AppTheme.typography.annotation,
        )
    }
}

@Preview
@Composable
internal fun OutgoingMessageMetadataSendingPreview() {
    OutgoingMessageMetadataPreview(status = MessageStatusUM.Created)
}

@Preview
@Composable
internal fun OutgoingMessageMetadataSentPreview() {
    OutgoingMessageMetadataPreview(status = MessageStatusUM.Sent)
}

@Preview
@Composable
internal fun OutgoingMessageMetadataReceivedPreview() {
    OutgoingMessageMetadataPreview(status = MessageStatusUM.Received)
}

@Preview
@Composable
internal fun OutgoingMessageMetadataReadPreview() {
    OutgoingMessageMetadataPreview(status = MessageStatusUM.Read)
}

@Composable
private fun OutgoingMessageMetadataPreview(status: MessageStatusUM) {
    AppTheme {
        Box(
            modifier = Modifier
                .background(AppTheme.colors.backgroundPrimary)
                .padding(16.dp),
        ) {
            OutgoingMessageMetadata(
                datetimeText = "12:10",
                status = status,
            )
        }
    }
}
