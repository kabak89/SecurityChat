plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.kotlinxSerialization)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.add_chat.component"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.features.addChat.addChatComponentApi)
            implementation(projects.features.addChat.addChatUi)
            implementation(projects.features.addChat.addChatDomain)
            implementation(projects.features.addChat.addChatData)
            implementation(projects.features.addChat.addChatDataCommon)
        }
    }
}
