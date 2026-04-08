import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    explicitApi = ExplicitApiMode.Strict

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    )

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization)
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.cryptography.core)
            implementation(libs.cryptography.provider.optimal)

            implementation(projects.common.coreNetwork)
            implementation(projects.common.coreTime)

            implementation(projects.features.chat.chatDomain)
            implementation(projects.features.user.userDataStorage)
            implementation(projects.features.users.usersDataStorage)
            implementation(projects.features.chats.chatsDataStorage)
            implementation(projects.features.chat.chatDataStorage)
        }
        androidMain.dependencies { }
        iosMain.dependencies { }
        jvmMain.dependencies { }
        commonTest.dependencies { }
    }

    android {
        namespace = "com.security.chat.multiplatform.features.chat.data"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions.jvmTarget = JvmTarget.JVM_1_8
    }
}

