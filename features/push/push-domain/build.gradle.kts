plugins {
    id("securitychat.convention.base")
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.push.domain"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.common.coreDomain)
        }
    }
}
