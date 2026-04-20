package com.security.chat.multiplatform.common.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.security.chat.multiplatform.common.core.component.DiScopeHolder
import com.security.chat.multiplatform.common.log.Log
import kotlinx.coroutines.awaitCancellation
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
public inline fun <reified T, S : Any, C> Screen(
    component: C,
    content: @Composable (state: S, vm: T) -> Unit,
) where T : BaseViewModel<S, *>,
        C : DiScopeHolder,
        C : ViewModelStoreOwner {
    try {
        if (component.getDiScope().closed) return
    } catch (e: Exception) {
        Log.e(e)
        return
    }

    val vm = koinViewModel<T>(
        viewModelStoreOwner = component,
        scope = component.getDiScope(),
        parameters = { parametersOf(component) },
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.onViewActive()
            try {
                awaitCancellation()
            } finally {
                vm.onViewInactive()
            }
        }
    }

    val state by vm.viewState.collectAsStateWithLifecycle()
    content(state, vm)
}