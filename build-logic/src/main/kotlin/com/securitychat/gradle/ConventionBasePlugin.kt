package com.securitychat.gradle

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugin.use.PluginDependency
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget

class ConventionBasePlugin : Plugin<Project> {

    override fun apply(target: Project) {
        val libs: VersionCatalog =
            target.extensions.getByType<VersionCatalogsExtension>().named("libs")
        target.plugins.apply(libs.requirePluginId("kotlinMultiplatform"))
        target.plugins.apply(libs.requirePluginId("android-kotlin-multiplatform-library"))

        target.extensions.create(
            "conventionBasePlugin",
            ConventionBasePluginExtension::class.java,
            target,
            libs,
        )

        target.extensions.configure<KotlinMultiplatformExtension> {
            explicitApi = ExplicitApiMode.Strict
            iosArm64()
            iosSimulatorArm64()
            jvm()
        }

        target.afterEvaluate {
            val extension = target.extensions.getByType(ConventionBasePluginExtension::class.java)
            check(extension.namespace.isNotBlank()) {
                "conventionBasePlugin { namespace = \"...\" } must be set before or with kotlin { } (${target.path})"
            }
        }
    }

    companion object {
        internal fun configureAndroid(target: Project, libs: VersionCatalog, namespace: String) {
            val compileSdkVersion = libs.requireVersionInt("android.compileSdk")
            val minSdkVersion = libs.requireVersionInt("android.minSdk")

            val kmp = target.extensions.getByType(KotlinMultiplatformExtension::class.java)
            (kmp as ExtensionAware).extensions.configure(
                "android",
                Action<KotlinMultiplatformAndroidLibraryExtension> {
                    this.namespace = namespace
                    this.compileSdk = compileSdkVersion
                    this.minSdk = minSdkVersion
                },
            )

            kmp.targets.withType(KotlinAndroidTarget::class.java).configureEach {
                compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
            }
        }
    }
}

internal fun VersionCatalog.requireVersionInt(alias: String): Int {
    return findVersion(alias)
        .map { it.requiredVersion.toInt() }
        .orElseThrow {
            IllegalStateException(
                "Version alias \"$alias\" not found in libs (gradle/libs.versions.toml [versions])",
            )
        }
}

internal fun VersionCatalog.requirePluginId(alias: String): String {
    val plugin: PluginDependency =
        try {
            findPlugin(alias).get().get()
        } catch (e: Exception) {
            throw IllegalStateException(
                "Plugin alias \"$alias\" not found in libs (gradle/libs.versions.toml [plugins])",
                e,
            )
        }
    return plugin.getPluginId()
}
