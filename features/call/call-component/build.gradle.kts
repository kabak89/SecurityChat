plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.kotlinxSerialization)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.call.component"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.features.call.callComponentApi)

            implementation(projects.features.call.callUi)
            implementation(projects.features.call.callDomain)
            implementation(projects.features.call.callData)
        }
    }
}
