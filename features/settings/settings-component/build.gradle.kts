plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.kotlinxSerialization)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.settings.component"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.features.settings.settingsComponentApi)

            implementation(projects.features.settings.settingsUi)
            implementation(projects.features.settings.settingsDomain)
            implementation(projects.features.settings.settingsData)
            implementation(projects.features.settings.settingsDataStorage)
            implementation(projects.features.chat.chatDataStorage)
            implementation(projects.features.profile.profileComponent)
        }
    }
}
