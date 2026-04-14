plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.chats.ui"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.resources)

            implementation(projects.common.coreUi)
            implementation(projects.common.uiKit)

            api(projects.features.chats.chatsComponentApi)

            implementation(projects.features.chats.chatsDomain)
        }
        androidMain.dependencies {
            implementation(libs.androidx.ui.tooling)
        }
    }
}
