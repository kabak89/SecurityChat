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
            api(projects.common.coreComponent)

            implementation(projects.features.chats.chatsComponent)
            implementation(projects.features.chat.chatComponent)

            implementation(projects.features.settings.settingsComponentApi)
            implementation(projects.features.settings.settingsComponent)
        }
        androidMain.dependencies { }
        iosMain.dependencies { }
        jvmMain.dependencies { }
        commonTest.dependencies { }
    }

    android {
        namespace = "com.security.chat.multiplatform.features.main.component"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions.jvmTarget = JvmTarget.JVM_1_8
    }
}

