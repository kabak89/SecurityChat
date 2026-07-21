plugins {
    id("securitychat.convention.base")
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.chat.data.common"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.cryptography.core)
            implementation(libs.cryptography.provider.optimal)
            implementation(libs.kotlinx.io.core)
            implementation(libs.paging.common)

            implementation(projects.common.coreTime)
            implementation(projects.common.coreThreading)
            implementation(projects.common.log)

            implementation(projects.features.chat.chatDataNetwork)
            implementation(projects.features.chat.chatDataStorage)
            implementation(projects.features.user.userDataStorage)
            implementation(projects.features.users.usersDataStorage)
            implementation(projects.features.users.usersDataNetwork)
            implementation(projects.features.chats.chatsDataStorage)
        }
    }
}
