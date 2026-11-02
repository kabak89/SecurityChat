plugins {
    id("securitychat.convention.base")
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.chats.data.common"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)

            implementation(projects.features.chats.chatsDataNetwork)
            implementation(projects.features.chats.chatsDataStorage)
        }
    }
}
