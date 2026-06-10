plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.kotlinxSerialization)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.settings.data"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.features.settings.settingsDomain)
            implementation(projects.features.settings.settingsDataCommon)
            implementation(projects.features.settings.settingsDataStorage)
        }
    }
}
