plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.kotlinxSerialization)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.chat.component"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.features.chat.chatComponentApi)

            implementation(projects.features.chat.chatUi)
            implementation(projects.features.chat.chatDomain)
            implementation(projects.features.chat.chatData)
            implementation(projects.features.chat.chatDataStorage)
        }
    }
}
