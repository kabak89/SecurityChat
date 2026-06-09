import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.googleServices)
}

kotlin {
    target {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    dependencies {
        implementation(projects.shared)
        implementation(projects.common.platformSpecific)
    }
}

android {
    val appId = "com.security.chat.multiplatform.android"
    val signingProperties = Properties().apply {
        val file = rootProject.file("keystore.properties")
        if (file.exists()) {
            file.inputStream().use(::load)
        }
    }
    val releaseStoreFilePath = signingProperties.getProperty("storeFile")
        ?: providers.gradleProperty("RELEASE_STORE_FILE").orNull
    val releaseStorePassword = signingProperties.getProperty("storePassword")
        ?: providers.gradleProperty("RELEASE_STORE_PASSWORD").orNull
    val releaseKeyAlias = signingProperties.getProperty("keyAlias")
        ?: providers.gradleProperty("RELEASE_KEY_ALIAS").orNull
    val releaseKeyPassword = signingProperties.getProperty("keyPassword")
        ?: providers.gradleProperty("RELEASE_KEY_PASSWORD").orNull
    val hasReleaseSigning = !releaseStoreFilePath.isNullOrBlank() &&
            !releaseStorePassword.isNullOrBlank() &&
            !releaseKeyAlias.isNullOrBlank() &&
            !releaseKeyPassword.isNullOrBlank()

    namespace = appId
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = appId
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = libs.versions.appVersionCode.get().toInt()
        versionName = libs.versions.appVrersionName.get()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "DebugProbesKt.bin"
        }
    }

    val variantNameRelease = "release"

    signingConfigs {
        create(variantNameRelease) {
            if (hasReleaseSigning) {
                storeFile = file(releaseStoreFilePath)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }
    buildTypes {
        getByName(variantNameRelease) {
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName(variantNameRelease)
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
