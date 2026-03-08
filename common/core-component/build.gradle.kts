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
            api(libs.decompose)
            api(libs.lifecycle.viewmodel)
            api(libs.koin.core)

            implementation(libs.kotlinx.serialization)
            implementation(libs.kotlinx.coroutines.core)

            implementation(projects.common.coreThreading)
        }
        androidMain.dependencies { }
        iosMain.dependencies { }
        jvmMain.dependencies { }
        commonTest.dependencies { }
    }

    android {
        namespace = "com.security.chat.multiplatform.common.core.component"
        compileSdk = 36
        minSdk = 26
        compilerOptions.jvmTarget = JvmTarget.JVM_1_8
    }
}
