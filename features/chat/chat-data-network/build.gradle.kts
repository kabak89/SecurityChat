plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.kotlinxSerialization)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.chat.data.network"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(libs.ktor.serialization.kotlinx.json)

            implementation(projects.common.coreNetwork)
            implementation(projects.common.log)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmTest.dependencies {
            implementation(libs.junit.jupiter)
            implementation(libs.junit.platform.launcher)
        }
    }
}
