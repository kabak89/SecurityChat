plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.common.platformspecific"
}

kotlin {
    sourceSets {
        commonMain.dependencies { }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)

            implementation(projects.features.root.rootComponent)
            implementation(projects.features.root.rootUi)
            implementation(projects.features.push.pushNavigationApi)
        }
    }
}
