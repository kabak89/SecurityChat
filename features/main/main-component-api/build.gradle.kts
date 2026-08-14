plugins {
    id("securitychat.convention.base")
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.main.component.api"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.common.coreComponent)

            implementation(projects.features.chats.chatsComponentApi)
            implementation(projects.features.chat.chatComponentApi)
            implementation(projects.features.chatInfo.chatInfoComponentApi)
            implementation(projects.features.settings.settingsComponentApi)
            implementation(projects.features.addChat.addChatComponentApi)
        }
    }
}
