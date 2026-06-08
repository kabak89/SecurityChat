plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.kotlinxSerialization)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.onboarding.component"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.features.onboarding.onboardingComponentApi)

            implementation(projects.features.onboarding.onboardingUi)
            implementation(projects.features.onboarding.onboardingDomain)
            implementation(projects.features.onboarding.onboardingData)
        }
    }
}
