plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.kotlinxSerialization)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.add_chat.data"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.features.addChat.addChatDomain)
            implementation(projects.common.coreNetwork)
            implementation(projects.features.user.userDataStorage)
            implementation(projects.features.chats.chatsDataStorage)
            implementation(projects.features.users.usersDataStorage)
            implementation(projects.features.users.usersDataNetwork)
        }
    }
}
