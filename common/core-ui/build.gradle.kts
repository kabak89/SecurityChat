import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
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
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            api(libs.compose.material3)

            api(libs.decompose.extensions.compose)

            api(libs.lifecycle.viewmodel.compose)
            api(libs.lifecycle.runtime.compose)

            api(libs.koin.core)
            api(libs.koin.compose)
            api(libs.koin.composeVM)

            api(libs.kotlinx.coroutines.core)

            api(projects.common.iconsKit)
            api(projects.common.uiKit)

            api(projects.common.log)
        }
        androidMain.dependencies { }
        iosMain.dependencies { }
        jvmMain.dependencies { }
        commonTest.dependencies { }
    }

    android {
        namespace = "com.security.chat.multiplatform.common.core.ui"
        compileSdk = 36
        minSdk = 26
        compilerOptions.jvmTarget = JvmTarget.JVM_1_8
    }
}

