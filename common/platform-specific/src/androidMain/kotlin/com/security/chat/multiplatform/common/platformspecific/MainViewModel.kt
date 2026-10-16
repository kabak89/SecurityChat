package com.security.chat.multiplatform.common.platformspecific

import android.app.Activity
import android.app.Application
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.security.chat.multiplatform.common.log.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class MainViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val controller = AndroidAppUpdateController(application)

    private val _pendingUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    val pendingUpdate = _pendingUpdate.asStateFlow()

    private var checkJob: Job? = null
    private var hasCheckedForUpdate = false
    private var isUpdateFlowRunning = false
    private var isRestartDismissed = false
    private var isInstalling = false
    private var shouldResetOnStart = false

    var isUpdateReady: Boolean by mutableStateOf(false)
        private set

    private val installListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADED -> showRestartPrompt()
            InstallStatus.FAILED -> Log.e("Google Play update failed: ${state.installErrorCode()}")
        }
    }

    init {
        controller.registerListener(installListener)
    }

    fun onStart() {
        if (shouldResetOnStart) {
            hasCheckedForUpdate = false
            isRestartDismissed = false
            _pendingUpdate.value = null
            shouldResetOnStart = false
        }
    }

    fun onStop(isChangingConfigurations: Boolean) {
        /** Capture this before Play delivers its result on the next start. */
        if (!isChangingConfigurations) {
            shouldResetOnStart = !isUpdateFlowRunning
        }
    }

    fun checkForUpdates() {
        if (checkJob?.isActive == true) return
        checkJob = viewModelScope.launch {
            try {
                val info = controller.checkForUpdates()
                if (info.installStatus() == InstallStatus.DOWNLOADED) {
                    hasCheckedForUpdate = true
                    showRestartPrompt()
                } else if (!hasCheckedForUpdate) {
                    hasCheckedForUpdate = true
                    if (info.installStatus() != InstallStatus.PENDING &&
                        info.installStatus() != InstallStatus.DOWNLOADING &&
                        info.installStatus() != InstallStatus.INSTALLING &&
                        info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                        info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                    ) {
                        _pendingUpdate.value = info
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                hasCheckedForUpdate = true
                Log.e(error, "Could not check for Google Play updates")
            }
        }
    }

    fun startUpdateFlow(
        info: AppUpdateInfo,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
    ) {
        if (!beginUpdateFlow(info)) return
        try {
            if (!controller.startUpdateFlow(info, launcher)) onUpdateFlowFailed()
        } catch (error: Exception) {
            onUpdateFlowFailed(error)
        }
    }

    private fun beginUpdateFlow(info: AppUpdateInfo): Boolean {
        if (_pendingUpdate.value !== info || isUpdateFlowRunning) return false
        _pendingUpdate.value = null
        isUpdateFlowRunning = true
        return true
    }

    fun onUpdateFlowResult(resultCode: Int) {
        isUpdateFlowRunning = false
        if (resultCode != Activity.RESULT_OK && resultCode != Activity.RESULT_CANCELED) {
            Log.e("Google Play update flow failed: $resultCode")
        }
    }

    private fun onUpdateFlowFailed(error: Exception? = null) {
        isUpdateFlowRunning = false
        if (error == null) {
            Log.e("Could not start Google Play update flow")
        } else {
            Log.e(error, "Could not start Google Play update flow")
        }
    }

    fun dismissRestart() {
        isRestartDismissed = true
        isUpdateReady = false
    }

    fun restart() {
        if (isInstalling) return
        isInstalling = true
        isUpdateReady = false
        viewModelScope.launch {
            try {
                controller.completeUpdate()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                isInstalling = false
                isRestartDismissed = false
                showRestartPrompt()
                Log.e(error, "Could not install Google Play update")
            }
        }
    }

    override fun onCleared() {
        controller.unregisterListener(installListener)
    }

    private fun showRestartPrompt() {
        if (!isRestartDismissed && !isInstalling) isUpdateReady = true
    }
}
