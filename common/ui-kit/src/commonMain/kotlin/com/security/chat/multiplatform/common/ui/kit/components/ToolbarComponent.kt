package com.security.chat.multiplatform.common.ui.kit.components

import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.security.chat.multiplatform.common.icons.kit.DrawableRes
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import securitychat.common.icons_kit.generated.resources.ic_back
import securitychat.common.icons_kit.generated.resources.ic_settings

@Composable
public fun ToolbarComponent(
    modifier: Modifier = Modifier,
    startContent: SideContent? = null,
    centerContent: CenterContent? = null,
    endContent: SideContent? = null,
) {
    Row(
        modifier = modifier
            .height(56.dp),
    ) {
        if (startContent != null) {
            SideContent(startContent)
        }
        if (centerContent != null) {
            CenterContent(centerContent)
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
        if (endContent != null) {
            SideContent(endContent)
        }
    }
}

@Composable
private fun RowScope.CenterContent(content: CenterContent) {
    when (content) {
        is CenterContent.Title -> {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp),
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    text = content.text,
                    style = AppTheme.typography.title,
                    color = AppTheme.colors.textPrimary,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        is CenterContent.Custom -> {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                content.content()
            }
        }
    }
}

@Composable
private fun SideContent(content: SideContent) {
    when (content) {
        is SideContent.Button -> ButtonContent(
            icon = content.icon,
            onClicked = content.onClicked,
        )

        is SideContent.Custom -> content.content()
    }
}

@Composable
public fun ButtonContent(
    icon: DrawableResource,
    onClicked: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val indication: Indication = ripple(
        color = AppTheme.colors.element,
        bounded = false,
        radius = 20.dp,
    )
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(48.dp)
            .clickable(
                onClick = onClicked,
                indication = indication,
                interactionSource = interactionSource,
            ),
    ) {
        Icon(
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.Center),
            painter = painterResource(icon),
            contentDescription = null,
            tint = AppTheme.colors.element,
        )
    }
}

@Immutable
public sealed interface SideContent {
    @Immutable
    public data class Button(
        val icon: DrawableResource,
        val onClicked: () -> Unit,
    ) : SideContent

    @Immutable
    public data class Custom(val content: @Composable () -> Unit) : SideContent
}

@Immutable
public sealed interface CenterContent {
    @Immutable
    public data class Title(val text: String) : CenterContent

    @Immutable
    public data class Custom(val content: @Composable () -> Unit) : CenterContent
}

@Preview
@Composable
internal fun ToolbarPreview() {
    AppTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ToolbarsForPreview()
        }
    }
}

@Composable
private fun ToolbarsForPreview() {
    ToolbarComponent(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.colors.backgroundPrimary),
    )
    ToolbarComponent(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.colors.backgroundPrimary),
        startContent = SideContent.Button(icon = DrawableRes.ic_back, onClicked = {}),
    )
    ToolbarComponent(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.colors.backgroundPrimary),
        startContent = SideContent.Button(icon = DrawableRes.ic_back, onClicked = {}),
        endContent = SideContent.Button(icon = DrawableRes.ic_settings, onClicked = {}),
    )
    ToolbarComponent(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.colors.backgroundPrimary),
        startContent = SideContent.Button(icon = DrawableRes.ic_back, onClicked = {}),
        centerContent = CenterContent.Title(text = "Title"),
    )
    ToolbarComponent(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.colors.backgroundPrimary),
        startContent = SideContent.Custom(content = { CustomButton() }),
        centerContent = CenterContent.Title(text = "Title"),
    )
    ToolbarComponent(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.colors.backgroundPrimary),
        startContent = SideContent.Custom(content = { CustomButton() }),
        centerContent = CenterContent.Title(text = "Title"),
        endContent = SideContent.Custom(content = { CustomButton() }),
    )
    ToolbarComponent(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.colors.backgroundPrimary),
        startContent = SideContent.Button(icon = DrawableRes.ic_back, onClicked = {}),
        centerContent = CenterContent.Title(
            text = "Long long long long long long long long long long long long long long long long title",
        ),
    )
    ToolbarComponent(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.colors.backgroundPrimary),
        startContent = SideContent.Button(icon = DrawableRes.ic_back, onClicked = {}),
        centerContent = CenterContent.Title(text = "Title"),
        endContent = SideContent.Button(icon = DrawableRes.ic_back, onClicked = {}),
    )
    ToolbarComponent(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.colors.backgroundPrimary),
        startContent = SideContent.Button(icon = DrawableRes.ic_back, onClicked = {}),
        centerContent = CenterContent.Custom(content = { CustomTitle() }),
        endContent = SideContent.Button(icon = DrawableRes.ic_back, onClicked = {}),
    )
}

@Composable
private fun CustomButton() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .padding(horizontal = 8.dp),
    ) {
        Text(
            modifier = Modifier
                .align(Alignment.Center),
            text = "Click me",
            style = AppTheme.typography.title2,
            color = AppTheme.colors.textPrimary,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CustomTitle() {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            modifier = Modifier,
            text = "Title",
            style = AppTheme.typography.title,
            color = AppTheme.colors.textPrimary,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            modifier = Modifier,
            text = "Subtitle",
            style = AppTheme.typography.title2,
            color = AppTheme.colors.textSecondary,
            overflow = TextOverflow.Ellipsis,
        )
    }
}