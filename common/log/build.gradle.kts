import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN

plugins {
    id("securitychat.convention.base")
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

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.common.log"
}

kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(libs.timber)
        }
    }
}
