plugins {
    id("securitychat.convention.base")
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.common.core.domain"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.koin.core)
            api(libs.kode.remo)
            api(libs.kotlinx.coroutines.core)

            api(projects.common.coreThreading)
            api(projects.common.log)
        }
    }
}
