plugins {
    id("securitychat.convention.base")
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.chat.domain"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.common.coreDomain)
            api(projects.common.coreFiles)
            api(projects.common.error)
            api(libs.paging.common)
        }
    }
}
