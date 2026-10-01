plugins {
    id("securitychat.convention.base")
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.call.data"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.features.call.callDomain)
        }
    }
}
