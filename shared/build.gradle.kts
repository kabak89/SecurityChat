import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    android {
        namespace = "com.security.chat.multiplatform.kmp"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
            export(libs.decompose)
            export(libs.essenty.lifecycle)
        }
    }

    jvm()

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
            implementation(projects.common.settings)
            implementation(projects.common.coreNetwork)
            implementation(projects.common.coreThreading)
            implementation(projects.common.coreDb)
            implementation(projects.common.coreTime)
            implementation(projects.common.iconsKit)
            implementation(projects.common.uiKit)
            implementation(projects.common.appLifecycle)

            implementation(projects.features.user.userDataStorage)
            implementation(projects.features.users.usersDataStorage)
            implementation(projects.features.settings.settingsDataStorage)

            implementation(projects.features.splash.splashComponent)
            implementation(projects.features.splash.splashUi)

            implementation(projects.features.authorize.authorizeComponent)
            implementation(projects.features.authorize.authorizeUi)

            implementation(projects.features.main.mainComponent)
            implementation(projects.features.main.mainUi)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
        }
        iosMain.dependencies { }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)

            implementation(libs.kotlinx.coroutines.swing)
        }
        commonTest.dependencies { }
    }
}

compose.desktop {
    application {
        mainClass = "com.security.chat.multiplatform.MainKt"
    }
}
