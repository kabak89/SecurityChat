package com.security.chat.multiplatform.common.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme

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
                            Box(
                                modifier = Modifier
                                    .background(AppTheme.colors.backgroundSecondary)
                                    .clickable(onClick = onNegativeButtonClicked),
                            ) {
                                Text(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = AppTheme.typography.default,
                                    text = negativeButtonText,
                                    color = AppTheme.colors.textPrimary,
                                )
                            }
                        }
                        if (positiveButtonText != null && onPositiveButtonClicked != null) {
                            if (negativeButtonText != null && onNegativeButtonClicked != null) {
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .background(AppTheme.colors.backgroundSecondary)
                                    .clickable(onClick = onPositiveButtonClicked),
                            ) {
                                Text(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = AppTheme.typography.default,
                                    text = positiveButtonText,
                                    color = AppTheme.colors.textPrimary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
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
