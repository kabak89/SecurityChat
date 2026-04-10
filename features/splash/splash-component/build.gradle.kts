plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.kotlinxSerialization)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.splash.component"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.features.splash.splashComponentApi)

            implementation(projects.features.splash.splashUi)
            implementation(projects.features.splash.splashDomain)
            implementation(projects.features.splash.splashData)
        }
    }
}
