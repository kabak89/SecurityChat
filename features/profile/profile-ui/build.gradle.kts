plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.profile.ui"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.resources)

            implementation(projects.common.coreUi)
            implementation(projects.common.uiKit)

            api(projects.features.profile.profileComponentApi)

            implementation(projects.features.profile.profileDomain)
        }
        androidMain.dependencies {
            implementation(libs.androidx.ui.tooling)
        }
    }
}
