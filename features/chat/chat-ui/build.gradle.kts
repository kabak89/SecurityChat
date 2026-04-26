plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.chat.ui"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.resources)
            implementation(libs.haze)
            implementation(libs.paging.compose)

            implementation(projects.common.coreUi)
            implementation(projects.common.uiKit)

            api(projects.features.chat.chatComponentApi)

            implementation(projects.features.chat.chatDomain)
        }
        androidMain.dependencies {
            implementation(libs.androidx.ui.tooling)
        }
    }
}
