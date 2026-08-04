package com.security.chat.multiplatform.common.analytics

/**
 * Firebase Analytics has no JVM SDK, so on desktop all calls are no-ops.
 */
internal class AnalyticsJvm : Analytics {

    override fun logEvent(name: String, params: Map<String, String>): Unit = Unit

    override fun logScreenView(screenName: String, screenClass: String?): Unit = Unit
}
