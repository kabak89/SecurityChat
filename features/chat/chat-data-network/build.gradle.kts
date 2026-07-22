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
    }
}
