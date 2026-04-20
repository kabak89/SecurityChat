package com.security.chat.multiplatform.common.core.network

import kotlinx.coroutines.flow.StateFlow

public interface ConnectivityObserver {

    public val isOnline: StateFlow<Boolean>
}
