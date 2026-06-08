plugins {
    id("securitychat.convention.base")
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.onboarding.data"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.features.onboarding.onboardingDomain)
            implementation(projects.features.user.userDataStorage)
        }
    }
}
