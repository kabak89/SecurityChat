package com.security.chat.multiplatform.features.chat.ui.screens.groupchat.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import com.security.chat.multiplatform.common.core.ui.entity.UiLceState
import com.security.chat.multiplatform.common.core.ui.entity.isLoading
import com.security.chat.multiplatform.common.icons.kit.DrawableRes
import com.security.chat.multiplatform.common.ui.kit.components.ButtonContent
import securitychat.common.icons_kit.generated.resources.ic_sync

@Composable
internal fun SyncComponent(
    modifier: Modifier = Modifier,
    syncState: UiLceState,
    onSyncClicked: () -> Unit,
) {
    val inProgress = remember(syncState) {
        syncState.isLoading
    }

    val rotation = if (inProgress) {
        val transition = rememberInfiniteTransition(label = "sync-rotation")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = -360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 800),
                repeatMode = RepeatMode.Restart,
            ),
            label = "sync-rotation-angle",
        ).value
    } else {
        0f
    }

    Box(
        modifier = modifier.rotate(rotation),
    ) {
        ButtonContent(
            icon = DrawableRes.ic_sync,
            onClicked = onSyncClicked,
        )
    }
}