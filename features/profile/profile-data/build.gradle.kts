plugins {
    id("securitychat.convention.base")
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.profile.data"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.features.profile.profileDomain)
            implementation(projects.features.profile.profileDataStorage)
        }
    }
}
