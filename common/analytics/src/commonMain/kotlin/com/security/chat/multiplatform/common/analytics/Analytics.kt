package com.security.chat.multiplatform.common.analytics

public interface Analytics {

    public fun logEvent(name: String, params: Map<String, String> = emptyMap())
    public fun logScreenView(screenName: String, screenClass: String? = null)
}
