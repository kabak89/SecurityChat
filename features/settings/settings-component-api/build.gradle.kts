plugins {
    id("securitychat.convention.base")
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.settings.component.api"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.common.coreComponent)

            implementation(projects.features.profile.profileComponentApi)
        }
    }
}
