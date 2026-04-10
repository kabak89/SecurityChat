package com.securitychat.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog

open class ConventionBasePluginExtension(
    private val project: Project,
    private val libs: VersionCatalog,
) {
    var namespace: String = ""
        set(value) {
            require(value.isNotBlank()) {
                "conventionBasePlugin.namespace must be non-blank (${project.path})"
            }
            field = value
            ConventionBasePlugin.configureAndroid(project, libs, value)
        }
}
