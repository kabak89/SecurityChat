package com.security.chat.multiplatform.common.ui.kit.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.security.chat.multiplatform.common.icons.kit.DrawableRes
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import org.jetbrains.compose.resources.vectorResource
import securitychat.common.icons_kit.generated.resources.ic_attach
import securitychat.common.icons_kit.generated.resources.ic_send

@Composable
public fun InputField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChanged: (String) -> Unit,
    placeholder: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    errorMessage: String? = null,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.SingleLine,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    interactionSource: MutableInteractionSource? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val singleLine = lineLimits == TextFieldLineLimits.SingleLine
    val minLines = (lineLimits as? TextFieldLineLimits.MultiLine)?.minHeightInLines ?: 1
    val maxLines = (lineLimits as? TextFieldLineLimits.MultiLine)?.maxHeightInLines ?: 1
    val fieldInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val isFocused by fieldInteractionSource.collectIsFocusedAsState()
    val colors = AppTheme.colors
    val bodyStyle = AppTheme.typography.body
    val textColor = colors.textPrimary
    val textStyle = remember(bodyStyle, textColor) {
        bodyStyle.copy(color = textColor)
    }
    val shape = AppTheme.shapes.roundedRectangle16
    val borderColor = when {
        !enabled -> colors.element.copy(alpha = 0.4f)
        errorMessage != null -> colors.error
        isFocused -> colors.contrast
        else -> colors.element.copy(alpha = 0.4f)
    }
    val borderWidth = if (enabled && (isFocused || errorMessage != null)) 2.dp else 1.dp

    val backgroundSecondary = colors.backgroundSecondary
    val fieldBackgroundColor = remember(backgroundSecondary) {
        backgroundSecondary.copy(alpha = 0.12f)
    }
    Column(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicTextField(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    if (errorMessage != null) {
                        error(errorMessage)
                    }
                },
            value = value,
            onValueChange = onValueChanged,
            enabled = enabled,
            readOnly = readOnly,
            textStyle = textStyle,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            interactionSource = fieldInteractionSource,
            cursorBrush = SolidColor(colors.contrast),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .background(fieldBackgroundColor, shape)
                        .border(borderWidth, borderColor, shape)
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    leadingContent?.invoke()
                    Box(
                        modifier = Modifier.weight(1f),
                    ) {
                        if (value.isEmpty() && placeholder != null) {
                            BasicText(
                                text = placeholder,
                                style = AppTheme.typography.body.copy(
                                    color = colors.textSuppressed,
                                ),
                                maxLines = maxLines,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        innerTextField()
                    }
                    trailingContent?.invoke()
                }
            },
        )
        if (errorMessage != null) {
            BasicText(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = errorMessage,
                style = AppTheme.typography.annotation.copy(color = colors.error),
            )
        }
    }
}

@Preview(widthDp = 412, heightDp = 900)
@Composable
internal fun InputFieldPreview() {
    AppTheme(useDarkTheme = false) {
        InputFieldPreviews()
    }
}

@Preview(widthDp = 412, heightDp = 900)
@Composable
internal fun InputFieldPreviewDark() {
    AppTheme(useDarkTheme = true) {
        InputFieldPreviews()
    }
}

@Composable
private fun InputFieldPreviews() {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.backgroundPrimary)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        InputFieldExample(label = "Empty") {
            var value by remember { mutableStateOf("") }
            InputField(
                value = value,
                onValueChanged = { value = it },
                placeholder = "Your name",
            )
        }
        InputFieldExample(label = "Filled") {
            var value by remember { mutableStateOf("Alex Morgan") }
            InputField(
                value = value,
                onValueChanged = { value = it },
                placeholder = "Your name",
            )
        }
        InputFieldExample(label = "Focused") {
            var value by remember { mutableStateOf("Hello, Alex") }
            InputField(
                modifier = Modifier.focusRequester(focusRequester),
                value = value,
                onValueChanged = { value = it },
            )
        }
        InputFieldExample(label = "Error") {
            var value by remember { mutableStateOf("ab") }
            InputField(
                value = value,
                onValueChanged = { value = it },
                errorMessage = "Use at least 3 characters",
            )
        }
        InputFieldExample(label = "Disabled") {
            var value by remember { mutableStateOf("Unavailable") }
            InputField(
                value = value,
                onValueChanged = { value = it },
                enabled = false,
            )
        }
        InputFieldExample(label = "Read only") {
            var value by remember { mutableStateOf("@alex_morgan") }
            InputField(
                value = value,
                onValueChanged = { value = it },
                readOnly = true,
            )
        }
        InputFieldExample(label = "Leading and trailing content") {
            var value by remember { mutableStateOf("") }
            InputField(
                value = value,
                onValueChanged = { value = it },
                placeholder = "Message",
                leadingContent = {
                    Image(
                        modifier = Modifier.size(24.dp),
                        imageVector = vectorResource(DrawableRes.ic_attach),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(AppTheme.colors.element),
                    )
                },
                trailingContent = {
                    Image(
                        modifier = Modifier.size(24.dp),
                        imageVector = vectorResource(DrawableRes.ic_send),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(AppTheme.colors.element),
                    )
                },
            )
        }
        InputFieldExample(label = "Multiline") {
            var value by remember {
                mutableStateOf("Hello!\nLet's meet tomorrow.\nI'll send you the details here.")
            }
            InputField(
                value = value,
                onValueChanged = { value = it },
                lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 3),
                placeholder = "Message",
            )
        }
        InputFieldExample(label = "Empty multiline") {
            var value by remember { mutableStateOf("") }
            InputField(
                value = value,
                onValueChanged = { value = it },
                lineLimits = TextFieldLineLimits.MultiLine(
                    minHeightInLines = 3,
                    maxHeightInLines = 3,
                ),
                placeholder = "Write a longer message",
            )
        }
    }
}

@Composable
private fun InputFieldExample(
    label: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            color = AppTheme.colors.textSecondary,
            style = AppTheme.typography.annotation,
        )
        content()
    }
}
