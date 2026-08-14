plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.main.ui"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.common.coreUi)

            implementation(projects.features.main.mainComponentApi)
            implementation(projects.features.chats.chatsUi)
            implementation(projects.features.chat.chatUi)
            implementation(projects.features.chatInfo.chatInfoUi)
            implementation(projects.features.settings.settingsUi)
            implementation(projects.features.addChat.addChatUi)
        }
    }
}
