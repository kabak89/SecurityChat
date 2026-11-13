plugins {
    id("securitychat.convention.base")
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.chat.data"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.paging.common)

            implementation(projects.common.error)
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
            implementation(projects.features.chats.chatsDataCommon)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.junit.jupiter)
            implementation(libs.junit.platform.launcher)
        }
    }
}
