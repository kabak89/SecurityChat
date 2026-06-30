plugins {
    id("securitychat.convention.base")
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.chats.domain"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.common.coreDomain)

            implementation(projects.common.error)
        }
    }
}
