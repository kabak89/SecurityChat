package com.security.chat.multiplatform.features.chat.ui.screens.groupchat.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import org.jetbrains.compose.resources.vectorResource
import securitychat.common.icons_kit.generated.resources.Res
import securitychat.common.icons_kit.generated.resources.ic_image_broken

@Composable
internal fun ImageComponent(
    modifier: Modifier = Modifier,
    filePath: String,
) {
    Box(
        modifier = modifier,
    ) {
        if (LocalInspectionMode.current) {
            Spacer(
                modifier = Modifier
                    .background(Color.Red)
                    .height(40.dp)
                    .width(100.dp),
            )
        } else {
            val context = LocalPlatformContext.current
            val request = remember(context, filePath) {
                ImageRequest.Builder(context)
                    .data(filePath)
                    .crossfade(true)
                    .build()
            }
            AsyncImage(
                modifier = Modifier
                    .matchParentSize()
                    .align(Alignment.Center)
                    .blur(10.dp)
                    .alpha(0.2f),
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )
            SubcomposeAsyncImage(
                modifier = Modifier
                    .align(Alignment.Center),
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Inside,
                error = {
                    Icon(
                        modifier = Modifier
                            .size(48.dp),
                        imageVector = vectorResource(Res.drawable.ic_image_broken),
                        tint = AppTheme.colors.element,
                        contentDescription = null,
                    )
                },
            )
        }
    }
}