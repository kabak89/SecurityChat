package com.security.chat.multiplatform.features.profile.ui.screens.deleteprofile

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.ui.Screen
import com.security.chat.multiplatform.common.core.ui.SingleEventEffect
import com.security.chat.multiplatform.common.icons.kit.DrawableRes
import com.security.chat.multiplatform.common.ui.kit.MAX_CONTENT_WIDTH_DP
import com.security.chat.multiplatform.common.ui.kit.components.CenterContent
import com.security.chat.multiplatform.common.ui.kit.components.SideContent
import com.security.chat.multiplatform.common.ui.kit.components.ToolbarComponent
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogComponent
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.profile.component.api.DeleteProfileComponent
import com.security.chat.multiplatform.features.profile.ui.screens.deleteprofile.entity.ObstacleDirection
import com.security.chat.multiplatform.features.profile.ui.screens.deleteprofile.entity.ObstacleDirection.Left
import com.security.chat.multiplatform.features.profile.ui.screens.deleteprofile.entity.ObstacleDirection.Right
import com.security.chat.multiplatform.features.profile.ui.screens.deleteprofile.entity.ObstacleSpec
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import securitychat.common.icons_kit.generated.resources.ic_back
import securitychat.common.icons_kit.generated.resources.ic_trash_can
import securitychat.common.localization.generated.resources.delete_profile_description
import securitychat.common.localization.generated.resources.delete_profile_drag_hint
import securitychat.common.localization.generated.resources.delete_profile_drag_text
import securitychat.common.localization.generated.resources.delete_profile_title
import kotlin.random.Random

@Composable
internal fun DeleteProfileScreen(
    component: DeleteProfileComponent,
) {
    Screen(
        component = component,
        screenName = "DeleteProfileScreen",
    ) { state: DeleteProfileState, vm: DeleteProfileViewModel ->
        DeleteProfileScreenContent(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            state = state,
            events = vm.viewEvent,
            onBackClicked = component::onBackClicked,
            onConfirmDeleteClicked = vm::onConfirmDeleteClicked,
        )
    }
}

@Composable
private fun DeleteProfileScreenContent(
    modifier: Modifier,
    state: DeleteProfileState,
    events: Flow<DeleteProfileEvent>,
    onBackClicked: () -> Unit,
    onConfirmDeleteClicked: () -> Unit,
) {
    SingleEventEffect(
        sideEffectFlow = events,
        collector = { },
    )
    val hazeState = rememberHazeState()
    Box(
        modifier = modifier
            .background(AppTheme.colors.backgroundPrimary)
            .fillMaxSize()
            .systemBarsPadding()
            .hazeSource(hazeState),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            ToolbarComponent(
                modifier = Modifier
                    .fillMaxWidth(),
                startContent = SideContent.Button(
                    icon = DrawableRes.ic_back,
                    onClicked = onBackClicked,
                ),
                centerContent = CenterContent.Title(
                    text = stringResource(StringRes.delete_profile_title),
                ),
                endContent = null,
            )
            Column(
                modifier = Modifier
                    .widthIn(max = MAX_CONTENT_WIDTH_DP.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 16.dp),
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = stringResource(StringRes.delete_profile_description),
                    color = AppTheme.colors.textPrimary,
                    style = AppTheme.typography.body,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = stringResource(StringRes.delete_profile_drag_hint),
                    color = AppTheme.colors.textPrimary,
                    style = AppTheme.typography.body,
                )
                Spacer(Modifier.height(16.dp))
                ConfirmComponent(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(AppTheme.colors.backgroundSecondary),
                    isLoading = state.showLoading,
                    onConfirmed = onConfirmDeleteClicked,
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
    if (state.alertDialogDescriptor != null) {
        AlertDialogComponent(
            content = state.alertDialogDescriptor.content,
            onDismissRequest = state.alertDialogDescriptor.dismissAction,
            onPositiveButtonClicked = state.alertDialogDescriptor.positiveAction,
            onNegativeButtonClicked = state.alertDialogDescriptor.negativeAction,
            hazeState = hazeState,
        )
    }
}

@Composable
private fun ConfirmComponent(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    onConfirmed: () -> Unit,
) {
    var parentCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(
        modifier = modifier
            .onGloballyPositioned { parentCoords = it },
    ) {
        val obstacles = listOf(
            Left,
            Right,
            Left,
            Right,
        )
        val obstaclesCount = obstacles.size
        val obstacleCoords = remember { mutableStateMapOf<Int, LayoutCoordinates>() }
        var textCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
        var trashCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
        val density = LocalDensity.current

        val nonNullTextCoords = textCoords
        val nonNullTrashCoords = trashCoords
        val nonNullParentCoords = parentCoords

        if (nonNullTextCoords != null && nonNullTrashCoords != null && nonNullParentCoords != null) {
            repeat(obstaclesCount) { obstacleId ->
                val parentHeight = with(density) { nonNullParentCoords.size.height.toDp() }
                val textHeight = with(density) { nonNullTextCoords.size.height.toDp() }
                val trashHeight =
                    remember(Unit) { with(density) { nonNullTrashCoords.size.height.toDp() } }
                val segmentHeight = (parentHeight - textHeight - trashHeight) / obstaclesCount
                val topOffset = textHeight + (obstacleId) * segmentHeight

                Obstacle(
                    modifier = Modifier,
                    topOffset = topOffset,
                    maxHeight = segmentHeight - textHeight,
                    parentCoords = nonNullParentCoords,
                    holeWidth = with(density) { nonNullTextCoords.size.width.toDp() },
                    onCoordinatesChanged = { obstacleCoords[obstacleId] = it },
                    direction = obstacles[obstacleId],
                )
            }
        }

        var trashActivated by remember { mutableStateOf(false) }
        val haptic = LocalHapticFeedback.current
        var trashActivatedOnce by remember { mutableStateOf(false) }

        LaunchedEffect(trashActivated) {
            if (trashActivated) {
                trashActivatedOnce = true
            }

            if (trashActivated || trashActivatedOnce) {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            }
        }

        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }

        val hazeState = rememberHazeState()

        Text(
            text = stringResource(StringRes.delete_profile_drag_text),
            color = AppTheme.colors.textSecondary,
            style = AppTheme.typography.title,
            modifier = Modifier
                .offset(
                    x = with(density) { offsetX.toDp() },
                    y = with(density) { offsetY.toDp() },
                )
                .align(alignment = Alignment.TopCenter)
                .background(
                    color = AppTheme.colors.backgroundPrimary,
                    shape = AppTheme.shapes.roundedRectangle16,
                )
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val nonNullTextCoords = textCoords ?: return@detectDragGestures
                            val nonNullParentCoords = parentCoords ?: return@detectDragGestures
                            val nonTrashCoords = trashCoords ?: return@detectDragGestures

                            val movedTextRect =
                                nonNullTextCoords.boundsInRoot().translate(dragAmount)

                            val isOverlapping = obstacleCoords.values.any { coords ->
                                movedTextRect.overlaps(coords.boundsInRoot())
                            }

                            val parentBounds = nonNullParentCoords.boundsInRoot()
                            val movedOutsideParent = movedTextRect.left < parentBounds.left ||
                                    movedTextRect.top < parentBounds.top ||
                                    movedTextRect.right > parentBounds.right ||
                                    movedTextRect.bottom > parentBounds.bottom

                            val newX = dragAmount.x
                            val newY = dragAmount.y

                            if (!isOverlapping && !movedOutsideParent) {
                                offsetX += newX
                                offsetY += newY
                            } else {
                                haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                            }

                            trashActivated = movedTextRect.overlaps(nonTrashCoords.boundsInRoot())
                        },
                        onDragEnd = {
                            if (trashActivated) {
                                onConfirmed()
                            }
                        },
                    )
                }
                .onGloballyPositioned { layoutCoordinates ->
                    textCoords = layoutCoordinates
                }
                .padding(8.dp)
                .hazeSource(state = hazeState),
        )
        val backgroundColor = AppTheme.colors.backgroundSecondary
        val hazeStyle = remember {
            HazeStyle(
                backgroundColor = backgroundColor,
                tint = HazeTint(
                    color = backgroundColor.copy(alpha = 0.05f),
                ),
                blurRadius = 4.dp,
                fallbackTint = HazeTint(
                    color = backgroundColor.copy(alpha = 0.6f),
                ),
            )
        }
        TrashIcon(
            modifier = Modifier
                .align(alignment = Alignment.BottomCenter)
                .clip(AppTheme.shapes.roundedRectangle16)
                .onGloballyPositioned { layoutCoordinates -> trashCoords = layoutCoordinates }
                .hazeEffect(
                    state = hazeState,
                    style = hazeStyle,
                ),
            isLoading = isLoading,
            trashActivated = trashActivated,
        )
    }
}

@Composable
private fun Obstacle(
    modifier: Modifier = Modifier,
    topOffset: Dp,
    maxHeight: Dp,
    parentCoords: LayoutCoordinates,
    holeWidth: Dp,
    direction: ObstacleDirection,
    onCoordinatesChanged: (LayoutCoordinates) -> Unit,
) {
    val density = LocalDensity.current

    val spec = remember(parentCoords, direction) {
        val minObstacleWidth = with(density) { 16.dp.toPx() }.toInt()
        val minObstacleHeight = with(density) { 16.dp.toPx() }.toInt()
        val horizontalObstaclePadding = with(density) { 8.dp.toPx() }.toInt()
        val verticalObstaclePadding = with(density) { 4.dp.toPx() }.toInt()
        val holeWidthPx = with(density) { holeWidth.toPx() }.toInt()
        val parentWidth = parentCoords.size.width

        val width = Random.nextInt(
            minObstacleWidth,
            parentWidth - holeWidthPx - horizontalObstaclePadding,
        )

        val maxObstacleHeight = with(density) { maxHeight.toPx() }.toInt() - verticalObstaclePadding

        val height = if (maxObstacleHeight < minObstacleHeight) {
            0
        } else {
            Random.nextInt(
                minObstacleHeight,
                maxObstacleHeight,
            )
        }

        val xCoordinate = when (direction) {
            Left -> Random.nextInt(0, parentWidth - width - horizontalObstaclePadding - holeWidthPx)
            Right -> Random.nextInt(holeWidthPx + horizontalObstaclePadding, parentWidth - width)
        }.toFloat()

        val maxYShift = with(density) { maxHeight.toPx() }.toInt() - height
        val topOffsetPx = with(density) { topOffset.toPx() }
        val yCoordinate = if (maxYShift > 0) {
            topOffsetPx + Random.nextInt(0, maxYShift)
        } else {
            0f
        }

        ObstacleSpec(
            width = width,
            height = height,
            xCoordinate = xCoordinate,
            yCoordinate = yCoordinate,
        )
    }
    Box(
        modifier = modifier
            .graphicsLayer {
                translationX = spec.xCoordinate
                translationY = spec.yCoordinate
            }
            .width(with(density) { spec.width.toDp() })
            .height(with(density) { spec.height.toDp() })
            .background(
                color = AppTheme.colors.backgroundPrimary,
                shape = AppTheme.shapes.roundedRectangle16,
            )
            .onGloballyPositioned(onCoordinatesChanged),
    )
}

@Composable
private fun TrashIcon(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    trashActivated: Boolean,
) {
    Icon(
        modifier = modifier
            .then(
                if (isLoading) {
                    val size by rememberInfiniteTransition(label = "delete-size")
                        .animateFloat(
                            initialValue = 40f,
                            targetValue = 400f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 3000),
                                repeatMode = RepeatMode.Restart,
                            ),
                            label = "delete-size",
                        )

                    val rotation = rememberInfiniteTransition(label = "delete-rotation")
                        .animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 600),
                                repeatMode = RepeatMode.Restart,
                            ),
                            label = "delete-rotation-angle",
                        ).value

                    Modifier
                        .size(size.dp)
                        .rotate(rotation)
                } else {
                    Modifier
                        .animateContentSize()
                        .size(if (trashActivated) 50.dp else 40.dp)
                },
            ),
        imageVector = vectorResource(DrawableRes.ic_trash_can),
        contentDescription = null,
    )
}

@Preview
@Composable
internal fun DeleteProfileScreenPreview() {
    AppTheme {
        DeleteProfileScreenContent(
            modifier = Modifier.fillMaxSize(),
            state = DeleteProfileState(
                showLoading = false,
                alertDialogDescriptor = null,
            ),
            events = emptyFlow(),
            onBackClicked = {},
            onConfirmDeleteClicked = {},
        )
    }
}