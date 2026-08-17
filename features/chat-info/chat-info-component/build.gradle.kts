plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.kotlinxSerialization)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.chat_info.component"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.features.chatInfo.chatInfoComponentApi)

            implementation(projects.features.chatInfo.chatInfoUi)
            implementation(projects.features.chatInfo.chatInfoDomain)
            implementation(projects.features.chatInfo.chatInfoData)
        }
    }
}
