plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.kotlinxSerialization)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.authorize.component"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.features.authorize.authorizeComponentApi)

            implementation(projects.features.authorize.authorizeUi)
            implementation(projects.features.authorize.authorizeDomain)
            implementation(projects.features.authorize.authorizeData)
        }
    }
}
