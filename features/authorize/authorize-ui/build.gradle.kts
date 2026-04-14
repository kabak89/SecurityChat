plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.authorize.ui"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.resources)

            implementation(projects.common.coreUi)
            implementation(projects.common.uiKit)

            api(projects.features.authorize.authorizeComponentApi)

            implementation(projects.features.authorize.authorizeDomain)
        }
        androidMain.dependencies {
            implementation(libs.androidx.ui.tooling)
        }
    }
}
