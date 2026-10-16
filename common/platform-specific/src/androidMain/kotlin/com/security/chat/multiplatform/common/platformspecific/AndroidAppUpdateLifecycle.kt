package com.security.chat.multiplatform.common.platformspecific

import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

internal fun ComponentActivity.observeAppUpdates(viewModel: MainViewModel) {
    val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        viewModel.onUpdateFlowResult(result.resultCode)
    }

    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.pendingUpdate.collect { info ->
                if (info != null) viewModel.startUpdateFlow(info, updateLauncher)
            }
        }
    }
    lifecycle.addObserver(
        object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                viewModel.onStart()
            }

            override fun onResume(owner: LifecycleOwner) {
                viewModel.checkForUpdates()
            }

            override fun onStop(owner: LifecycleOwner) {
                viewModel.onStop(isChangingConfigurations = isChangingConfigurations)
            }
        },
    )
}
