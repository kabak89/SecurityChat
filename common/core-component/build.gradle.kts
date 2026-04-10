plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.kotlinxSerialization)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.common.core.component"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.decompose)
            api(libs.lifecycle.viewmodel)
            api(libs.koin.core)

            implementation(libs.kotlinx.serialization)
            implementation(libs.kotlinx.coroutines.core)

            api(projects.common.log)

            implementation(projects.common.coreThreading)
        }
    }
}
