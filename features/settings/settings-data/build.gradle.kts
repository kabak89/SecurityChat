plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.kotlinxSerialization)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.settings.data"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.features.settings.settingsDomain)
            implementation(projects.features.settings.settingsDataStorage)
            implementation(projects.features.user.userDataStorage)
            implementation(projects.features.chats.chatsDataStorage)
            implementation(projects.features.chat.chatDataStorage)
        }
    }
}
