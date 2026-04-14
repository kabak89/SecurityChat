plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.settings.ui"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.resources)

            implementation(projects.common.coreUi)
            implementation(projects.common.uiKit)

            api(projects.features.settings.settingsComponentApi)

            implementation(projects.features.settings.settingsDomain)
            implementation(projects.features.profile.profileUi)
        }
        androidMain.dependencies {
            implementation(libs.androidx.ui.tooling)
        }
    }
}
