plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.kotlinxSerialization)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.main.component"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.common.coreComponent)

            implementation(projects.features.chats.chatsComponent)
            implementation(projects.features.chat.chatComponent)
            implementation(projects.features.settings.settingsComponent)
        }
    }
}
