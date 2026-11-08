package com.security.chat.multiplatform.features.chat.ui.screens.groupchat.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.security.chat.multiplatform.common.icons.kit.DrawableRes
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.chat.ui.screens.groupchat.entity.MessageQuoteUM
import org.jetbrains.compose.resources.vectorResource
import securitychat.common.icons_kit.generated.resources.ic_reply

@Composable
internal fun MessageQuoteComponent(
    modifier: Modifier = Modifier,
    quote: MessageQuoteUM,
    onClicked: () -> Unit,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClicked)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            imageVector = vectorResource(DrawableRes.ic_reply),
            contentDescription = null,
            tint = AppTheme.colors.element,
        )
        when (quote) {
            is MessageQuoteUM.Text -> Text(
                text = quote.text.replace('\n', ' ').replace('\r', ' '),
                color = AppTheme.colors.textPrimary,
                style = AppTheme.typography.annotation,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            is MessageQuoteUM.Image -> ImageComponent(
                modifier = Modifier
                    .size(40.dp)
                    .clip(AppTheme.shapes.roundedRectangle8),
                filePath = quote.filePath,
            )
        }
    }
}

@Preview
@Composable
internal fun MessageQuoteComponentPreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .background(AppTheme.colors.backgroundPrimary)
                .padding(12.dp),
        ) {
            MessageQuoteComponent(
                modifier = Modifier.width(220.dp),
                quote = MessageQuoteUM.Text(
                    messageId = "original",
                    text = "A longer quoted message that must fit on a single line.",
                ),
                onClicked = {},
            )
            MessageQuoteComponent(
                modifier = Modifier.padding(top = 8.dp),
                quote = MessageQuoteUM.Image(messageId = "photo", filePath = ""),
                onClicked = {},
            )
        }
    }
}
