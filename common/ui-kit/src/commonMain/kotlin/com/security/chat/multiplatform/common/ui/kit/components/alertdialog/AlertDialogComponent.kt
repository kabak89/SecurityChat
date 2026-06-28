package com.security.chat.multiplatform.common.ui.kit.components.alertdialog

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.security.chat.multiplatform.common.core.ui.entity.resolve
import com.security.chat.multiplatform.common.ui.kit.components.ButtonContent
import com.security.chat.multiplatform.common.ui.kit.components.ButtonPrimary
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.launch

@Composable
public fun AlertDialogComponent(
    content: AlertDialogContent,
    hazeState: HazeState,
    onDismissRequest: () -> Unit,
    onPositiveButtonClicked: (() -> Unit)? = null,
    onNegativeButtonClicked: (() -> Unit)? = null,
) {
    AlertDialogComponent(
        title = content.title.resolve(),
        message = content.message?.resolve(),
        positiveButtonText = content.positiveButtonText?.resolve(),
        negativeButtonText = content.negativeButtonText?.resolve(),
        hazeState = hazeState,
        onDismissRequest = onDismissRequest,
        onPositiveButtonClicked = onPositiveButtonClicked,
        onNegativeButtonClicked = onNegativeButtonClicked,
    )
}

@Composable
public fun AlertDialogComponent(
    title: String,
    hazeState: HazeState,
    onDismissRequest: () -> Unit,
    message: String? = null,
    positiveButtonText: String? = null,
    negativeButtonText: String? = null,
    onPositiveButtonClicked: (() -> Unit)? = null,
    onNegativeButtonClicked: (() -> Unit)? = null,
) {
    val navState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)

    val backProgress =
        (navState.transitionState as? NavigationEventTransitionState.InProgress)
            ?.takeIf { it.direction == NavigationEventTransitionState.TRANSITIONING_BACK }
            ?.latestEvent
            ?.progress
            ?: 0f

    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appear.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = ENTER_DURATION_MS),
        )
    }

    val scope = rememberCoroutineScope()
    var isDismissing by remember { mutableStateOf(false) }
    val dismiss: (() -> Unit) -> Unit = { onAnimationEnd ->
        if (!isDismissing) {
            isDismissing = true
            scope.launch {
                appear.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = EXIT_DURATION_MS),
                )
                onAnimationEnd()
            }
        }
    }

    if (!LocalInspectionMode.current) {
        NavigationBackHandler(
            state = navState,
            isBackEnabled = true,
            onBackCompleted = onDismissRequest,
        )
    }

    val backProgressEased = (backProgress * BACK_PROGRESS_SPEED).coerceAtMost(1f)
    val visibility = appear.value * (1f - backProgressEased)

    val backgroundPrimary = AppTheme.colors.backgroundSecondary

    val hazeStyle = remember(visibility) {
        HazeStyle(
            backgroundColor = backgroundPrimary,
            tint = HazeTint(color = backgroundPrimary.copy(alpha = TINT_ALPHA * visibility)),
            blurRadius = (MAX_BLUR_RADIUS_DP * visibility).dp,
            fallbackTint = HazeTint(
                color = backgroundPrimary.copy(alpha = FALLBACK_TINT_ALPHA * visibility),
            ),
            noiseFactor = 0f,
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .hazeEffect(
                state = hazeState,
                style = hazeStyle,
            )
            .clickable(
                onClick = { dismiss(onDismissRequest) },
                indication = null,
                interactionSource = null,
            ),
        contentAlignment = Alignment.Center,
    ) {
        val sidePadding =
            if (maxWidth > SCREEN_WIDTH_THRESHOLD_DP.dp + (SIDE_PADDING_DP * 2).dp) {
                (maxWidth - SCREEN_WIDTH_THRESHOLD_DP.dp) / 2
            } else {
                SIDE_PADDING_DP.dp
            }
        Box(
            modifier = Modifier
                .graphicsLayer { alpha = visibility }
                .wrapContentHeight()
                .padding(horizontal = sidePadding)
                .padding(bottom = 16.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = AppTheme.shapes.roundedRectangle16,
                    ambientColor = AppTheme.colors.contrast,
                    spotColor = AppTheme.colors.contrast,
                )
                .background(AppTheme.colors.backgroundPrimary)
                .clickable(onClick = {}, indication = null, interactionSource = null),
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
                                onClicked = { dismiss(onNegativeButtonClicked) },
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
                                onClicked = { dismiss(onPositiveButtonClicked) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val ENTER_DURATION_MS = 250
private const val EXIT_DURATION_MS = 200
private const val BACK_PROGRESS_SPEED = 2f
private const val MAX_BLUR_RADIUS_DP = 16f
private const val TINT_ALPHA = 0.1f
private const val FALLBACK_TINT_ALPHA = 0.6f
private const val SCREEN_WIDTH_THRESHOLD_DP = 500
private const val SIDE_PADDING_DP = 40
