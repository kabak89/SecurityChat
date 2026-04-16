plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.common.ui.kit"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.resources)

            api(libs.ui.tooling.preview)

            api(projects.common.coreUi)
        }
        androidMain.dependencies {
            implementation(libs.androidx.ui.tooling)
        }
    }
}

compose.resources {
    publicResClass = true
}
