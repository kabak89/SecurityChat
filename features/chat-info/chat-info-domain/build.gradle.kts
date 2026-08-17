plugins {
    id("securitychat.convention.base")
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.chat_info.domain"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.common.coreDomain)

            implementation(projects.common.error)
        }
    }
}
