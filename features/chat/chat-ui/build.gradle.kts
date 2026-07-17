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
            implementation(libs.paging.compose)
            implementation(libs.kotlinx.datetime)
            implementation(libs.autolinktext)

            implementation(projects.common.coreUi)
            implementation(projects.common.uiKit)

            api(projects.features.chat.chatComponentApi)

            implementation(projects.features.chat.chatDomain)
            implementation(projects.features.push.pushDomain)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.ui.tooling)
        }
    }
}
