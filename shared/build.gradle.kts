import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.sqldelight)
}

sqldelight {
    databases {
        create("ChatDb") {
            packageName = "com.security.chat.multiplatform"
            generateAsync = true
            dialect(libs.sqlite.dialect)
        }
    }
    linkSqlite = false
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
            export("com.arkivanov.decompose:decompose:3.3.0")
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

            implementation(libs.multiplatform.settings)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.driver.android)
            implementation(libs.android.sqlcipher)
            implementation(libs.sqlite.android)
            implementation(libs.androidx.security)
            implementation(libs.koin.android)

        }
        iosMain.dependencies {
            implementation(libs.sqldelight.driver.native)
            implementation(libs.sqliter.driver)
        }
        jvmMain.dependencies {
            implementation(libs.sqldelight.driver.sqlite)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.security.chat.multiplatform"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
