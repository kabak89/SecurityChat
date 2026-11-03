package com.securitychat.gradle

import org.gradle.api.GradleException

internal object ModuleDependencyRules {

    private val ignoredModules = setOf(
        ":features:push:push-navigation-api",
        ":features:push:push-navigation-impl",
    )

    private val unrestrictedSources = setOf(
        ":shared",
        ":androidApp",
        ":common:platform-specific",
    )

    private val allowedTargets = mapOf(
        ModuleLayer.COMMON to setOf(ModuleLayer.COMMON),
        ModuleLayer.DOMAIN to setOf(ModuleLayer.COMMON),
        ModuleLayer.UI to setOf(
            ModuleLayer.COMMON,
            ModuleLayer.DOMAIN,
            ModuleLayer.COMPONENT_API,
            ModuleLayer.UI,
        ),
        ModuleLayer.DATA to setOf(
            ModuleLayer.COMMON,
            ModuleLayer.DOMAIN,
            ModuleLayer.DATA_NETWORK,
            ModuleLayer.DATA_STORAGE,
            ModuleLayer.DATA_COMMON,
        ),
        ModuleLayer.DATA_NETWORK to setOf(ModuleLayer.COMMON),
        ModuleLayer.DATA_STORAGE to setOf(ModuleLayer.COMMON),
        ModuleLayer.DATA_COMMON to setOf(
            ModuleLayer.COMMON,
            ModuleLayer.DATA_NETWORK,
            ModuleLayer.DATA_STORAGE,
        ),
        ModuleLayer.COMPONENT to ModuleLayer.entries.toSet(),
        ModuleLayer.COMPONENT_API to setOf(ModuleLayer.COMMON, ModuleLayer.COMPONENT_API),
    )

    fun isUnrestrictedSource(path: String): Boolean =
        path in unrestrictedSources || path in ignoredModules

    fun requireSourceLayer(path: String): ModuleLayer =
        ModuleLayer.fromPath(path) ?: throw GradleException(
            "Unknown module layer for $path. " +
                    "Expected :common:<module> or :features:<feature>:<feature>-<layer>. " +
                    "Supported feature layers: " +
                    ModuleLayer.entries.filter { it != ModuleLayer.COMMON }.joinToString { it.id },
        )

    fun validate(sourcePath: String, configurationName: String, targetPath: String) {
        if (isUnrestrictedSource(sourcePath) || targetPath in ignoredModules) return

        val sourceLayer = requireSourceLayer(sourcePath)
        val targetLayer = ModuleLayer.fromPath(targetPath)
        val allowedLayers = allowedTargets.getValue(sourceLayer)
        if (targetLayer in allowedLayers) return

        throw GradleException(
            "Forbidden module dependency:\n" +
                    "  $sourcePath (${sourceLayer.id}) -> " +
                    "$targetPath (${targetLayer?.id ?: "unknown"})\n" +
                    "Configuration: $configurationName\n" +
                    "Allowed target layers: ${allowedLayers.joinToString { it.id }}",
        )
    }
}
