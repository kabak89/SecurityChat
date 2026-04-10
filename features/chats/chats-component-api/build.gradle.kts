plugins {
    id("securitychat.convention.base")
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.chats.component.api"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.common.coreComponent)
        }
    }
}
