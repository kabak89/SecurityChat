package com.securitychat.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog

open class ConventionBasePluginExtension(
    private val project: Project,
    private val libs: VersionCatalog,
) {
    companion object {
        const val IS_DEBUG_KEY = "IS_DEBUG"
        const val ENABLE_LOGS_KEY = "ENABLE_LOGS"

        /**
         * Gradle property `isDebug` (e.g. `-PisDebug=false`).
         * When absent, defaults to `true`.
         */
        fun isDebug(project: Project): Boolean = project.findProperty("isDebug")
            ?.toString()
            ?.toBooleanStrictOrNull() ?: true

        /**
         * Gradle property `enableLogs` (e.g. `-PenableLogs=true`).
         * When absent, defaults to `false`.
         */
        fun enableLogs(project: Project): Boolean = project.findProperty("enableLogs")
            ?.toString()
            ?.toBooleanStrictOrNull() ?: true

        /**
         * Gradle property `serverEnv` (e.g. `-PserverEnv=dev|stage|prod`).
         * When absent, defaults to `prod`.
         */
        fun serverEnv(project: Project): String = project.findProperty("serverEnv")
            ?.toString()
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.isNotBlank() } ?: "dev"
    }

    var namespace: String = ""
        set(value) {
            require(value.isNotBlank()) {
                "conventionBasePlugin.namespace must be non-blank (${project.path})"
            }
            field = value
            ConventionBasePlugin.configureAndroid(project, libs, value)
        }
}
