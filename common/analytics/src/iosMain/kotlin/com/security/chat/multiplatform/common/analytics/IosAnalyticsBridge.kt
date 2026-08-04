package com.security.chat.multiplatform.common.analytics

import kotlin.concurrent.Volatile

/**
 * Kotlin side of the Swift bridge: the Firebase Analytics SDK lives in the iOS app, so events are
 * handed over to an implementation registered from Swift at startup.
 */
public interface IosAnalyticsBridge {

    public fun logEvent(name: String, params: Map<String, String>)

    public fun logScreenView(screenName: String, screenClass: String?)
}

/**
 * Must be called from Swift before Koin starts so early analytics calls are not dropped.
 */
public fun setIosAnalyticsBridge(bridge: IosAnalyticsBridge?) {
    IosAnalyticsBridgeHolder.bridge = bridge
}

internal object IosAnalyticsBridgeHolder {

    @Volatile
    var bridge: IosAnalyticsBridge? = null
}
