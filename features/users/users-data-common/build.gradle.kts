plugins {
    id("securitychat.convention.base")
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.users.data.common"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)

            implementation(projects.common.coreNetwork)
            implementation(projects.features.users.usersDataNetwork)
            implementation(projects.features.users.usersDataStorage)
        }
    }
}
