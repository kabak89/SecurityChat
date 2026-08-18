plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.kotlinxSerialization)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.chat_info.data"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.common.coreNetwork)

            implementation(projects.features.chatInfo.chatInfoDomain)
            implementation(projects.features.addChat.addChatDataCommon)
            implementation(projects.features.chats.chatsDataCommon)
            implementation(projects.features.users.usersDataCommon)
            implementation(projects.features.user.userDataStorage)
        }
    }
}
