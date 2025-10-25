import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlinCocoapods)
}

val modulePackage = "com.security.chat.multiplatform.features.chats.data.storage"

sqldelight {
    databases {
        create("ChatsDb") {
            packageName = modulePackage
            generateAsync = true
            dialect(libs.sqlite.dialect)
        }
    }
    linkSqlite = false
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
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)

            implementation(projects.common.coreDb)
        }
        androidMain.dependencies { }
        iosMain.dependencies { }
        jvmMain.dependencies { }
        commonTest.dependencies { }
    }

    cocoapods {
        name = modulePackage
        ios.deploymentTarget = "14.0"
        version = "1.0.0"

        pod(
            name = "SQLCipher",
            version = libs.versions.iosSqlCipher.get(),
            linkOnly = true,
        )
    }
}

android {
    namespace = modulePackage
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
