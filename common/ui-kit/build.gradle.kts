plugins {
    id("securitychat.convention.base")
    id("securitychat.convention.screenshot")
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
            implementation(libs.navigationevent.compose)

            api(libs.ui.tooling.preview)
            api(libs.haze)

            api(projects.common.coreUi)
        }
        androidMain.dependencies {
            implementation(libs.androidx.ui.tooling)
            implementation(libs.androidx.activity.compose)
        }
        jvmTest.dependencies {
            implementation(projects.common.coreTest)
        }
    }
}
