plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.kotlinxSerialization)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.chat.data"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization)
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.paging.common)

            implementation(projects.common.coreNetwork)
            implementation(projects.common.coreTime)
            implementation(projects.common.coreThreading)
            implementation(projects.common.coreFiles)
            implementation(projects.common.log)

            implementation(projects.features.chat.chatDomain)
            implementation(projects.features.chat.chatDataCommon)
            implementation(projects.features.chat.chatDataNetwork)
            implementation(projects.features.chat.chatDataStorage)
            implementation(projects.features.user.userDataStorage)
            implementation(projects.features.users.usersDataStorage)
            implementation(projects.features.users.usersDataNetwork)
            implementation(projects.features.chats.chatsDataStorage)
        }
    }
}
