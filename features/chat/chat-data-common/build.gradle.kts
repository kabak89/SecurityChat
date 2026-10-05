plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.kotlinxSerialization)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.chat.data.common"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization)
            implementation(libs.cryptography.core)
            implementation(libs.cryptography.provider.optimal)
            implementation(libs.kotlinx.io.core)
            implementation(libs.paging.common)
            implementation(libs.compose.resources)

            implementation(projects.common.localization)

            implementation(projects.common.coreTime)
            implementation(projects.common.coreNetwork)
            implementation(projects.common.coreThreading)
            implementation(projects.common.coreFiles)
            implementation(projects.common.log)
            implementation(projects.common.encryption)

            implementation(projects.features.chat.chatDataNetwork)
            implementation(projects.features.chat.chatDataStorage)
            implementation(projects.features.user.userDataStorage)
            implementation(projects.features.users.usersDataStorage)
            implementation(projects.features.users.usersDataNetwork)
            implementation(projects.features.chats.chatsDataStorage)
            implementation(projects.features.chats.chatsDataCommon)
        }
    }

    android {
        androidResources {
            enable = true
        }
    }
}
