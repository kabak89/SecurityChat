import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_1_8)
                }
            }
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
            export("com.arkivanov.decompose:decompose:3.4.0")
            export("com.arkivanov.essenty:lifecycle:2.5.0")
        }
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            api(libs.decompose)

            implementation(libs.decompose.extensions.compose)
            implementation(libs.kotlinx.serialization)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)

            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.lifecycle.runtime.compose)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.composeVM)

            implementation(libs.kotlinx.coroutines.core)

            implementation(libs.kode.remo)

            implementation(libs.sqldelight.coroutines.extensions)

            implementation(libs.sha2)

            implementation(libs.cryptography.core)

            implementation(projects.common.coreUi)
            implementation(projects.common.coreDomain)
            implementation(projects.common.coreComponent)
            implementation(projects.common.settings)
            implementation(projects.common.coreNetwork)
            implementation(projects.common.coreThreading)
            implementation(projects.common.coreDb)
            implementation(projects.common.iconsKit)
            implementation(projects.common.uiKit)

            implementation(projects.features.splash.splashComponent)
            implementation(projects.features.splash.splashDomain)
            implementation(projects.features.splash.splashUi)
            implementation(projects.features.splash.splashData)

            implementation(projects.features.user.userDataStorage)

            implementation(projects.features.main.mainComponent)
            implementation(projects.features.main.mainUi)

            implementation(projects.features.chats.chatsComponent)
            implementation(projects.features.chats.chatsUi)
            implementation(projects.features.chats.chatsDomain)
            implementation(projects.features.chats.chatsData)
            implementation(projects.features.chats.chatsDataStorage)

            implementation(projects.features.chat.chatComponent)
            implementation(projects.features.chat.chatUi)
            implementation(projects.features.chat.chatDomain)
            implementation(projects.features.chat.chatData)

            implementation(projects.features.users.usersDataStorage)

            implementation(projects.features.settings.settingsComponent)
            implementation(projects.features.settings.settingsUi)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
        }
        iosMain.dependencies { }
        jvmMain.dependencies { }
        commonTest.dependencies { }
    }
}

android {
    namespace = "com.security.chat.multiplatform"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    packaging {
        resources.excludes += "DebugProbesKt.bin"
    }
}
