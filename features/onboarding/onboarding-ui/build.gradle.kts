plugins {
    id("securitychat.convention.base")
    id("securitychat.convention.screenshot")
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
            implementation(projects.common.permission)

            api(projects.features.onboarding.onboardingComponentApi)

            implementation(projects.features.onboarding.onboardingDomain)
        }
        androidMain.dependencies {
            implementation(libs.androidx.ui.tooling)
        }
        jvmTest.dependencies {
            implementation(projects.common.coreTest)
        }
    }
}
