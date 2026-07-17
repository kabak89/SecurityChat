plugins {
    id("securitychat.convention.base")
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.common.core.files"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.koin.core)

            implementation(projects.common.coreThreading)
        }
    }
}
