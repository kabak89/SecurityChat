package com.security.chat.multiplatform.common.ui.kit.components.alertdialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.security.chat.multiplatform.common.core.ui.entity.resolve
import com.security.chat.multiplatform.common.ui.kit.components.ButtonContent
import com.security.chat.multiplatform.common.ui.kit.components.ButtonPrimary
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme

@Composable
public fun AlertDialogComponent(
    content: AlertDialogContent,
    onDismissRequest: () -> Unit,
    onPositiveButtonClicked: (() -> Unit)? = null,
    onNegativeButtonClicked: (() -> Unit)? = null,
) {
    AlertDialogComponent(
        title = content.title.resolve(),
        message = content.message?.resolve(),
        positiveButtonText = content.positiveButtonText?.resolve(),
        negativeButtonText = content.negativeButtonText?.resolve(),
        onDismissRequest = onDismissRequest,
        onPositiveButtonClicked = onPositiveButtonClicked,
        onNegativeButtonClicked = onNegativeButtonClicked,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun AlertDialogComponent(
    title: String,
    message: String? = null,
    positiveButtonText: String? = null,
    negativeButtonText: String? = null,
    onDismissRequest: () -> Unit,
    onPositiveButtonClicked: (() -> Unit)? = null,
    onNegativeButtonClicked: (() -> Unit)? = null,
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
    ) {
        Box(
            modifier = Modifier
                .shadow(
                    elevation = 4.dp,
                    shape = AppTheme.shapes.roundedRectangle16,
                    ambientColor = AppTheme.colors.contrast,
                    spotColor = AppTheme.colors.contrast,
                )
                .background(AppTheme.colors.accent)
                .wrapContentHeight()
                .fillMaxWidth()
                .background(AppTheme.colors.backgroundPrimary),
        ) {
            Column(
                modifier = Modifier
                    .padding(all = 16.dp),
            ) {
                Text(
                    text = title,
                    color = AppTheme.colors.textPrimary,
                    fontSize = 16.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.W600,
                )
                message?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = it,
                        color = AppTheme.colors.textPrimary,
                        fontSize = 14.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.W400,
                    )
                }
                if (positiveButtonText != null || negativeButtonText != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        if (negativeButtonText != null && onNegativeButtonClicked != null) {
                            ButtonPrimary(
                                content = ButtonContent.Text(
                                    text = negativeButtonText,
                                ),
                                onClicked = onNegativeButtonClicked,
                            )
                        }
                        if (positiveButtonText != null && onPositiveButtonClicked != null) {
                            if (negativeButtonText != null && onNegativeButtonClicked != null) {
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                            ButtonPrimary(
                                content = ButtonContent.Text(
                                    text = positiveButtonText,
                                ),
                                onClicked = onPositiveButtonClicked,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
internal fun AlertDialogComponentPreview() {
    AppTheme {
        AlertDialogComponent(
            title = "title",
            message = "message",
            positiveButtonText = "positive",
            negativeButtonText = "negative",
            onDismissRequest = { },
            onPositiveButtonClicked = { },
            onNegativeButtonClicked = { },
        )
    }
}

@Preview
@Composable
internal fun AlertDialogComponentPreviewNoMessage() {
    AppTheme {
        AlertDialogComponent(
            title = "title",
            message = null,
            positiveButtonText = "positive",
            negativeButtonText = "negative",
            onDismissRequest = { },
            onPositiveButtonClicked = { },
            onNegativeButtonClicked = { },
        )
    }
}

@Preview
@Composable
internal fun AlertDialogComponentPreviewOneButton() {
    AppTheme {
        AlertDialogComponent(
            title = "title",
            message = "message",
            positiveButtonText = "positive",
            negativeButtonText = null,
            onDismissRequest = { },
            onPositiveButtonClicked = { },
            onNegativeButtonClicked = { },
        )
    }
}

@Preview
@Composable
internal fun AlertDialogComponentPreviewNoButtons() {
    AppTheme {
        AlertDialogComponent(
            title = "title",
            message = "message",
            positiveButtonText = null,
            negativeButtonText = null,
            onDismissRequest = { },
            onPositiveButtonClicked = { },
            onNegativeButtonClicked = { },
        )
    }
}
