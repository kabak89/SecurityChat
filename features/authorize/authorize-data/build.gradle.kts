plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.kotlinxSerialization)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.authorize.data"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.common.coreNetwork)
            implementation(projects.common.deviceInfo)
            implementation(projects.common.encryption)

            implementation(projects.features.authorize.authorizeDomain)
            implementation(projects.features.user.userDataStorage)
        }
    }
}
