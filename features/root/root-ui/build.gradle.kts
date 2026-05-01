plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.root.ui"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.resources)

            implementation(projects.common.coreUi)
            implementation(projects.common.uiKit)

            api(projects.features.root.rootComponentApi)

            implementation(projects.features.splash.splashUi)
            implementation(projects.features.authorize.authorizeUi)
            implementation(projects.features.main.mainUi)
            implementation(projects.features.main.mainComponent)
            implementation(projects.features.settings.settingsDataStorage)
        }
        androidMain.dependencies {
            implementation(libs.androidx.ui.tooling)
        }
    }
}
