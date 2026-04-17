plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.kotlinxSerialization)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.profile.component"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.features.profile.profileComponentApi)

            implementation(projects.features.profile.profileUi)
            implementation(projects.features.profile.profileDomain)
            implementation(projects.features.profile.profileData)
            implementation(projects.features.profile.profileDataStorage)
            implementation(projects.features.user.userDataNetwork)
        }
    }
}
