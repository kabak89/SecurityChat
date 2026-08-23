plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.common.core.test"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
        }
        jvmMain.dependencies {
            api(libs.composeTest)
            api(libs.composeTestJunit)
            api(libs.kotlin.test)
            api(libs.junit.jupiter)
            api(libs.junit.platform.launcher)
            api(libs.kotlinx.coroutines.swing)
            api(libs.kotlinx.coroutines.test)
            api(compose.desktop.currentOs)
        }
    }
}
