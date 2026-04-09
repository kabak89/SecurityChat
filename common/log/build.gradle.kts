import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.buildkonfig)
}

/**
 * Gradle property `isDebug` (e.g. `-PisDebug=false`).
 * When absent, defaults to `true`.
 */
val isDebug: Boolean = findProperty("isDebug")?.toString()?.toBooleanStrictOrNull() ?: true

buildkonfig {
    packageName = "com.security.chat.multiplatform.common.log"

    defaultConfigs {
        buildConfigField(BOOLEAN, "IS_DEBUG", isDebug.toString())
    }
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
        commonMain.dependencies { }
        androidMain.dependencies {
            implementation(libs.timber)
        }
        iosMain.dependencies { }
        jvmMain.dependencies { }
        commonTest.dependencies { }
    }

    // Usage of old DSL because of libs.plugins.buildkonfig
    @Suppress("DEPRECATION")
    androidLibrary {
        namespace = "com.security.chat.multiplatform.common.log"
        compileSdk = 36
        minSdk = 26
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }
}
