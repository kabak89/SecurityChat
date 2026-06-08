plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.onboarding.ui"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.resources)
            implementation(libs.navigationevent.compose)

            implementation(projects.common.coreUi)
            implementation(projects.common.uiKit)

            api(projects.features.onboarding.onboardingComponentApi)

            implementation(projects.features.onboarding.onboardingDomain)
        }
        androidMain.dependencies {
            implementation(libs.androidx.ui.tooling)
        }
    }
}
