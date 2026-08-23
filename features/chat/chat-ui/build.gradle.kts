plugins {
    id("securitychat.convention.base")
    id("securitychat.convention.screenshot")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.chat.ui"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.resources)
            implementation(libs.paging.compose)
            implementation(libs.kotlinx.datetime)
            implementation(libs.autolinktext)
            implementation(libs.coil.compose)
            implementation(libs.zoomimage.compose.coil3)
            implementation(libs.navigationevent.compose)

            implementation(projects.common.coreUi)
            implementation(projects.common.uiKit)
            implementation(projects.common.analytics)

            api(projects.features.chat.chatComponentApi)

            implementation(projects.features.chat.chatDomain)
            implementation(projects.features.push.pushDomain)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.ui.tooling)
        }
        jvmTest.dependencies {
            implementation(projects.common.coreTest)
        }
    }
}
