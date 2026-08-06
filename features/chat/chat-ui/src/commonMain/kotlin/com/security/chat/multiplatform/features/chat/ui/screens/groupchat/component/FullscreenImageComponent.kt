package com.security.chat.multiplatform.features.chat.ui.screens.groupchat.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.github.panpf.zoomimage.CoilZoomAsyncImage

@Composable
internal fun FullscreenImageComponent(
    modifier: Modifier = Modifier,
    filePath: String,
    enableZoom: Boolean,
    //TODO add button for closing
    onDismiss: () -> Unit,
) {
    if (enableZoom) {
        CoilZoomAsyncImage(
            modifier = modifier,
            model = filePath,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            scrollBar = null,
        )
    } else {
        AsyncImage(
            modifier = modifier,
            model = filePath,
            contentDescription = null,
            contentScale = ContentScale.Fit,
        )
    }
}
