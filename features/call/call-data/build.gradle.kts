plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.kotlinxSerialization)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.call.data"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization)

            implementation(projects.common.coreNetwork)
            implementation(projects.common.log)
            implementation(projects.features.call.callDomain)
            implementation(projects.features.user.userDataStorage)
            implementation(projects.features.users.usersDataCommon)
        }
    }
}
