plugins {
    id("securitychat.convention.base")
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.push.navigation.impl"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)

            implementation(projects.common.platformSpecific)
            implementation(projects.features.push.pushNavigationApi)
        }
    }
}
