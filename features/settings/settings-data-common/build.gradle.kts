plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.kotlinxSerialization)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.settings.data.common"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)

            implementation(projects.common.coreFiles)
            implementation(projects.common.coreNetwork)

            implementation(projects.features.settings.settingsDataStorage)
            implementation(projects.features.user.userDataStorage)
            implementation(projects.features.chats.chatsDataStorage)
            implementation(projects.features.chat.chatDataStorage)
        }
    }
}
