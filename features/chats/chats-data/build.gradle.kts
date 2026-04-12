plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.kotlinxSerialization)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.chats.data"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization)

            implementation(libs.koin.core)

            implementation(libs.kotlinx.coroutines.core)

            implementation(projects.common.coreNetwork)

            implementation(projects.features.chats.chatsDomain)
            implementation(projects.features.chats.chatsDataStorage)
            implementation(projects.features.user.userDataStorage)
            implementation(projects.features.users.usersDataStorage)
            implementation(projects.features.users.usersDataNetwork)
        }
    }
}
