package com.securitychat.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency

class ModuleDependencyValidationPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        val sourcePath = target.path
        if (ModuleDependencyRules.isUnrestrictedSource(sourcePath)) return
        ModuleDependencyRules.requireSourceLayer(sourcePath)

        target.afterEvaluate {
            /** Check unused and provider-backed declarations without resolving configurations. */
            target.configurations.all {
                val configurationName = name
                dependencies.withType(ProjectDependency::class.java).all {
                    ModuleDependencyRules.validate(
                        sourcePath = sourcePath,
                        configurationName = configurationName,
                        targetPath = path,
                    )
                }
            }
        }
    }
}
