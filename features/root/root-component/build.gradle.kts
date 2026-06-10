plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.kotlinxSerialization)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.root.component"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.features.root.rootComponentApi)

            implementation(projects.common.coreThreading)
            implementation(projects.common.coreNetwork)

            implementation(projects.features.push.pushDomain)
            implementation(projects.features.root.rootUi)
            implementation(projects.features.main.mainComponent)
            implementation(projects.features.splash.splashComponent)
            implementation(projects.features.authorize.authorizeComponent)
            implementation(projects.features.onboarding.onboardingComponent)
            implementation(projects.features.settings.settingsDataCommon)
        }
    }
}
