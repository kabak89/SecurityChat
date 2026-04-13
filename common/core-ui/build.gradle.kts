plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.common.core.ui"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.resources)

            api(libs.compose.material3)
            api(libs.decompose.extensions.compose)
            api(libs.lifecycle.viewmodel.compose)
            api(libs.lifecycle.runtime.compose)
            api(libs.koin.core)
            api(libs.koin.compose)
            api(libs.koin.composeVM)
            api(libs.kotlinx.coroutines.core)

            api(projects.common.iconsKit)
            api(projects.common.uiKit)
            api(projects.common.log)
            api(projects.common.localization)
        }
    }
}
