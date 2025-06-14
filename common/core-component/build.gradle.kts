import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    explicitApi = ExplicitApiMode.Strict

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
    )

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.decompose)

            implementation(libs.decompose.extensions.compose)
            implementation(libs.kotlinx.serialization)

            implementation(libs.lifecycle.common)
            implementation(libs.lifecycle.viewmodel)

            implementation(libs.koin.core)
//            implementation(libs.koin.compose)
//            implementation(libs.koin.composeVM)

            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies { }
        iosMain.dependencies { }
        jvmMain.dependencies { }
        commonTest.dependencies { }
    }
}

android {
    namespace = "com.security.chat.multiplatform.common.core.component"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
