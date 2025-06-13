package com.security.chat.multiplatform.common.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

@Composable
fun <T : Any> SingleEventEffect(
    sideEffectFlow: Flow<T>,
    collector: (T) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(key1 = sideEffectFlow) {
        lifecycleOwner.repeatOnLifecycle(state = Lifecycle.State.STARTED) {
            sideEffectFlow.collect(collector = collector)
        }
    }
}