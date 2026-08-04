import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.composeHotReload)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.kmp"
}

kotlin {
    val iosDeploymentTarget = libs.versions.deploymentTarget.get()
    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "shared"
            isStatic = true
            export(libs.decompose)
            export(libs.essenty.lifecycle)
            export(libs.nsexception.kt.core)
            export(projects.common.crashReport)
            export(projects.common.analytics)
            freeCompilerArgs += listOf(
                "-Xoverride-konan-properties=" +
                        "osVersionMin.ios_arm64=$iosDeploymentTarget;" +
                        "osVersionMin.ios_simulator_arm64=$iosDeploymentTarget",
            )
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.decompose)

            implementation(libs.decompose.extensions.compose)

            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)

            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.lifecycle.runtime.compose)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.composeVM)

            implementation(libs.kotlinx.coroutines.core)

            implementation(projects.common.coreUi)
            implementation(projects.common.coreComponent)
            api(projects.common.crashReport)
            api(projects.common.analytics)
            implementation(projects.common.coreFiles)
            implementation(projects.common.settings)
            implementation(projects.common.coreNetwork)
            implementation(projects.common.coreThreading)
            implementation(projects.common.coreDb)
            implementation(projects.common.coreTime)
            implementation(projects.common.iconsKit)
            implementation(projects.common.uiKit)
            implementation(projects.common.appLifecycle)
            implementation(projects.common.deviceInfo)
            implementation(projects.common.permission)
            implementation(projects.common.platformSpecific)

            implementation(projects.features.root.rootComponent)
            implementation(projects.features.root.rootUi)

            implementation(projects.features.chat.chatDataCommon)
            implementation(projects.features.user.userDataStorage)
            implementation(projects.features.users.usersDataStorage)
            implementation(projects.features.chats.chatsDataStorage)
            implementation(projects.features.chat.chatDataStorage)
            implementation(projects.features.chat.chatDataNetwork)

            implementation(projects.features.push.pushDomain)
            implementation(projects.features.push.pushData)
            implementation(projects.features.push.pushNavigationImpl)

            implementation(projects.features.settings.settingsDataStorage)
            implementation(projects.features.settings.settingsDataCommon)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
        }
        iosMain.dependencies {
            /** Exported: the NSExceptionKt Swift package extends the class this library defines. */
            api(libs.nsexception.kt.core)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)

            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.security.chat.multiplatform.MainKt"
        nativeDistributions {
            /**
             * Explicit module list instead of `includeAllModules`: JDKs built with JEP 493 ship no
             * `jmods` folder and jlink refuses to put `jdk.jlink` into the produced image.
             * Base set comes from the `suggestRuntimeModules` task; `jdk.crypto.ec` is added for
             * TLS handshakes in the OkHttp engine and `jdk.localedata` for non-English locales,
             * as neither is discoverable by static analysis.
             */
            modules(
                "java.instrument",
                "java.management",
                "java.naming",
                "java.prefs",
                "java.sql",
                "jdk.crypto.ec",
                "jdk.localedata",
                "jdk.unsupported",
            )
        }
    }
}
