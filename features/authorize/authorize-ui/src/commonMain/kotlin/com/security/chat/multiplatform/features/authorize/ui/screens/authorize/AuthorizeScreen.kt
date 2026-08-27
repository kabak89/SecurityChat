package com.security.chat.multiplatform.features.authorize.ui.screens.authorize

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.security.chat.multiplatform.common.core.ui.Screen
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.authorize.component.api.AuthorizeComponent
import com.security.chat.multiplatform.features.authorize.ui.screens.authorize.entity.Connection
import com.security.chat.multiplatform.features.authorize.ui.screens.authorize.entity.Node
import com.security.chat.multiplatform.features.authorize.ui.screens.signin.SignInScreen
import com.security.chat.multiplatform.features.authorize.ui.screens.signup.SignUpScreen
import kotlinx.collections.immutable.ImmutableList

@Composable
public fun AuthorizeScreen(component: AuthorizeComponent) {
    Screen(
        component = component,
        screenName = "AuthorizeScreen",
    ) { state: AuthorizeState, vm: AuthorizeViewModel ->
        AuthorizeContent(
            component = component,
            state = state,
        )
    }
}

@Composable
private fun AuthorizeContent(
    component: AuthorizeComponent,
    state: AuthorizeState,
) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        val backgroundPrimary = AppTheme.colors.backgroundPrimary
        BackgroundLayer(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundPrimary)
                .blur(9.dp),
            nodes = state.nodes,
            connections = state.connections,
        )
        val overlayColor = remember { backgroundPrimary.copy(alpha = 0.85f) }
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .background(overlayColor),
        )
        Children(
            stack = component.childStack,
            animation = predictiveBackAnimation(
                backHandler = component.backHandler,
                fallbackAnimation = stackAnimation(slide()),
                onBack = component::onBackClicked,
            ),
            content = {
                when (val child = it.instance) {
                    is AuthorizeComponent.Child.SignUp -> SignUpScreen(component = child.component)
                    is AuthorizeComponent.Child.SignIn -> SignInScreen(component = child.component)
                }
            },
        )
    }
}

@Composable
private fun BackgroundLayer(
    modifier: Modifier = Modifier,
    nodes: ImmutableList<Node>,
    connections: ImmutableList<Connection>,
) {
    Box(
        modifier = modifier,
    ) {
        val contrastColor = AppTheme.colors.contrast
        NodesLayer(
            modifier = Modifier.fillMaxSize(),
            nodes = nodes,
            color = contrastColor,
        )
        connections.forEach { connection ->
            ConnectionLine(
                connection = connection,
                color = contrastColor,
            )
        }
    }
}

@Composable
private fun NodesLayer(
    modifier: Modifier = Modifier,
    nodes: ImmutableList<Node>,
    color: Color,
) {
    val nodeStates = nodes.map { node ->
        key(node) {
            val animatable = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                animatable.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(NODE_APPEAR_ANIMATION_LENGTH_MS),
                )
            }
            animatable
        }
    }

    Canvas(modifier = modifier) {
        nodes.forEachIndexed { index, node ->
            val progress = nodeStates[index].value
            val scale = 0.5f + 0.5f * progress
            val radius = (NODE_SIZE / 2).toPx() * scale

            drawCircle(
                color = color,
                radius = radius,
                center = Offset(
                    x = size.width * node.xRelative,
                    y = size.height * node.yRelative,
                ),
                alpha = progress,
            )
        }
    }
}

@Composable
private fun ConnectionLine(
    connection: Connection,
    color: Color,
) {
    key(connection.id) {
        val appearProgress = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            appearProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(CONNECTION_APPEAR_ANIMATION_LENGTH_MS),
            )
        }

        val messageProgress by animateFloatAsState(
            targetValue = connection.messageProgress?.progress ?: 0f,
            animationSpec = tween(
                durationMillis = CHANGES_STEP.inWholeMilliseconds.toInt(),
                easing = LinearEasing,
            ),
            label = "messageProgress",
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            val start = Offset(
                x = size.width * connection.start.xRelative,
                y = size.height * connection.start.yRelative,
            )
            val end = Offset(
                x = size.width * connection.end.xRelative,
                y = size.height * connection.end.yRelative,
            )

            val path = Path().apply {
                moveTo(start.x, start.y)
                val mid = Offset((start.x + end.x) / 2, (start.y + end.y) / 2)
                val diff = end - start
                val perp = Offset(-diff.y, diff.x)
                val factor = if (connection.id.hashCode() % 2 == 0) 0.2f else -0.2f
                val control = mid + perp * factor
                quadraticTo(control.x, control.y, end.x, end.y)
            }

            val pathMeasure = PathMeasure()
            pathMeasure.setPath(path, false)
            val segmentPath = Path()
            pathMeasure.getSegment(0f, pathMeasure.length * appearProgress.value, segmentPath)

            drawPath(
                path = segmentPath,
                color = color,
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                ),
                alpha = 0.5f * appearProgress.value,
            )

            connection.messageProgress?.let { mp ->
                val actualProgress = if (mp.direction == Connection.Direction.StartToEnd) {
                    messageProgress
                } else {
                    1f - messageProgress
                }

                val pos = pathMeasure.getPosition(pathMeasure.length * actualProgress)
                drawCircle(
                    color = color,
                    radius = MESSAGE_SIZE.toPx(),
                    center = pos,
                    alpha = appearProgress.value,
                )
            }
        }
    }
}

private const val NODE_APPEAR_ANIMATION_LENGTH_MS = 2000
private const val CONNECTION_APPEAR_ANIMATION_LENGTH_MS = 1000

private val NODE_SIZE = 30.dp
private val MESSAGE_SIZE = 5.dp
