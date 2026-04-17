package com.security.chat.multiplatform.common.ui.kit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.security.chat.multiplatform.common.icons.kit.DrawableRes
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import org.jetbrains.compose.resources.vectorResource
import securitychat.common.icons_kit.generated.resources.ic_send

@Composable
public fun ButtonPrimary(
    modifier: Modifier = Modifier,
    content: ButtonContent,
    enabled: Boolean = true,
    onClicked: () -> Unit,
) {
    val color = if (enabled) {
        AppTheme.colors.accent
    } else {
        AppTheme.colors.accent.copy(alpha = 0.3f)
    }
    val shape = AppTheme.shapes.roundedRectangle16
    Box(
        modifier = modifier
            .then(
                if (enabled) {
                    Modifier
                        .shadow(
                            elevation = 4.dp,
                            shape = shape,
                            ambientColor = AppTheme.colors.contrast,
                            spotColor = AppTheme.colors.contrast,
                        )
                        .clickable(onClick = onClicked)
                } else {
                    Modifier
                },
            )
            .background(
                color = color,
                shape = shape,
            )
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        when (content) {
            is ButtonContent.Custom -> {
                Box(
                    modifier = Modifier
                        .align(alignment = Alignment.Center),
                ) {
                    content.content()
                }
            }

            is ButtonContent.Text -> {
                Text(
                    modifier = Modifier
                        .align(alignment = Alignment.Center),
                    text = content.text,
                    color = AppTheme.colors.textPrimary,
                    style = AppTheme.typography.body,
                )
            }
        }
    }
}

public sealed interface ButtonContent {

    public data class Text(
        val text: String,
    ) : ButtonContent

    public data class Custom(
        val content: @Composable () -> Unit,
    ) : ButtonContent
}

@Preview
@Composable
internal fun ButtonPrimaryPreview() {
    AppTheme(useDarkTheme = false) {
        Previews()
    }
}

@Preview
@Composable
internal fun ButtonPrimaryPreviewDark() {
    AppTheme(useDarkTheme = true) {
        Previews()
    }
}

@Composable
private fun Previews() {
    Column(
        modifier = Modifier
            .background(AppTheme.colors.backgroundPrimary)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ButtonPrimary(
            modifier = Modifier,
            content = ButtonContent.Text("Press Me"),
            enabled = true,
            onClicked = {},
        )
        ButtonPrimary(
            modifier = Modifier,
            content = ButtonContent.Text("Press Me"),
            enabled = false,
            onClicked = {},
        )
        ButtonPrimary(
            modifier = Modifier.fillMaxWidth(),
            content = ButtonContent.Text("Press Me"),
            enabled = true,
            onClicked = {},
        )
        ButtonPrimary(
            modifier = Modifier.fillMaxWidth(),
            content = ButtonContent.Text("Long long long long long long long long long long long long long text"),
            enabled = true,
            onClicked = {},
        )
        ButtonPrimary(
            modifier = Modifier,
            content = ButtonContent.Custom(
                content = {
                    Row {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = vectorResource(DrawableRes.ic_send),
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Some text",
                            color = AppTheme.colors.textPrimary,
                            style = AppTheme.typography.title,
                        )
                    }
                },
            ),
            enabled = true,
            onClicked = {},
        )
    }
}