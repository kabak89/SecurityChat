plugins {
    id("securitychat.convention.base")
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.settings.data.storage"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)

            implementation(projects.common.settings)
            implementation(projects.common.appLifecycle)
            implementation(projects.common.coreComponent)
            implementation(projects.common.coreThreading)
        }
    }
}
