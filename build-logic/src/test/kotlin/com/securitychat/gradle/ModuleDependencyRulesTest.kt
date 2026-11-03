package com.securitychat.gradle

import org.gradle.api.GradleException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ModuleDependencyRulesTest {

    @Test
    fun `all layer pairs follow the agreed dependency matrix`() {
        val expected = mapOf(
            "common" to setOf("common"),
            "domain" to setOf("common"),
            "ui" to setOf("common", "domain", "component-api", "ui"),
            "data" to setOf("common", "domain", "data-network", "data-storage", "data-common"),
            "data-network" to setOf("common"),
            "data-storage" to setOf("common"),
            "data-common" to setOf("common", "data-network", "data-storage"),
            "component" to setOf(
                "common", "domain", "ui", "data", "data-network", "data-storage", "data-common",
                "component", "component-api",
            ),
            "component-api" to setOf("common", "component-api"),
        )

        for (source in ModuleLayer.entries) {
            for (target in ModuleLayer.entries) {
                val checkDependency = {
                    ModuleDependencyRules.validate(
                        sourcePath = pathFor(layer = source, feature = "chat-info"),
                        configurationName = "commonMainImplementation",
                        targetPath = pathFor(layer = target, feature = "other"),
                    )
                }
                if (target.id in expected.getValue(source.id)) {
                    checkDependency()
                } else {
                    assertThrows(GradleException::class.java, checkDependency)
                }
            }
        }
    }

    @Test
    fun `common location takes precedence over the module suffix`() {
        assertEquals(ModuleLayer.COMMON, ModuleLayer.fromPath(":common:core-domain"))
        assertEquals(ModuleLayer.COMMON, ModuleLayer.fromPath(":common:core-ui"))
    }

    @Test
    fun `feature names with hyphens and compound layer names are classified exactly`() {
        for (layer in ModuleLayer.entries.filter { it != ModuleLayer.COMMON }) {
            assertEquals(layer, ModuleLayer.fromPath(pathFor(layer = layer, feature = "chat-info")))
        }
        assertNull(ModuleLayer.fromPath(":features:chat:other-domain"))
        assertNull(ModuleLayer.fromPath(":features:chat:chat-data-extra"))
        assertNull(ModuleLayer.fromPath(":other:chat-domain"))
    }

    @Test
    fun `only exact app assembly and platform paths have unrestricted outgoing dependencies`() {
        for (path in listOf(":shared", ":androidApp", ":common:platform-specific")) {
            ModuleDependencyRules.validate(
                sourcePath = path,
                configurationName = "androidMainImplementation",
                targetPath = ":features:unknown:unknown-custom",
            )
        }

        ModuleDependencyRules.validate(
            sourcePath = ":features:chat:chat-domain",
            configurationName = "commonMainApi",
            targetPath = ":common:platform-specific",
        )
        assertThrows(GradleException::class.java) {
            ModuleDependencyRules.validate(
                sourcePath = ":common:platform-specific-extra",
                configurationName = "commonMainImplementation",
                targetPath = ":features:chat:chat-domain",
            )
        }
        assertThrows(GradleException::class.java) {
            ModuleDependencyRules.validate(
                sourcePath = ":features:chat:chat-component",
                configurationName = "commonMainImplementation",
                targetPath = ":shared",
            )
        }
    }

    @Test
    fun `only the two special push navigation modules are ignored in both directions`() {
        for (path in listOf(
            ":features:push:push-navigation-api",
            ":features:push:push-navigation-impl",
        )) {
            ModuleDependencyRules.validate(
                sourcePath = ":common:core-network",
                configurationName = "androidMainImplementation",
                targetPath = path,
            )
            ModuleDependencyRules.validate(
                sourcePath = path,
                configurationName = "androidMainImplementation",
                targetPath = ":features:unknown:unknown-custom",
            )
        }

        assertThrows(GradleException::class.java) {
            ModuleDependencyRules.validate(
                sourcePath = ":common:core-network",
                configurationName = "commonMainImplementation",
                targetPath = ":features:other:other-navigation-api",
            )
        }
    }

    private fun pathFor(layer: ModuleLayer, feature: String): String =
        if (layer == ModuleLayer.COMMON) ":common:$feature-domain" else {
            ":features:$feature:$feature-${layer.id}"
        }
}
