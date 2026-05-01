plugins {
    id("securitychat.convention.base")
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.root.component.api"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.common.coreComponent)

            implementation(projects.features.splash.splashComponentApi)
            implementation(projects.features.authorize.authorizeComponentApi)
            implementation(projects.features.main.mainComponent)
        }
    }
}
