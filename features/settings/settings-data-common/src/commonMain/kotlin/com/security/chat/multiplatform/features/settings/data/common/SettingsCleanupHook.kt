package com.security.chat.multiplatform.features.settings.data.common

public fun interface SettingsCleanupHook {

    /** Runs after user data is cleared and before chat storages are cleared. */
    public suspend fun onUserDataCleared()
}
