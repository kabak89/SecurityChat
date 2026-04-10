plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.kotlinxSerialization)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.chats.component"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.features.chats.chatsComponentApi)

            implementation(projects.features.chats.chatsUi)
            implementation(projects.features.chats.chatsDomain)
            implementation(projects.features.chats.chatsData)
            implementation(projects.features.chats.chatsDataStorage)
        }
    }
}
