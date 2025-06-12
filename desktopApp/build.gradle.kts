plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    id("org.jetbrains.compose.hot-reload")
}

kotlin {
    jvm()
    sourceSets {
        jvmMain.dependencies {
            implementation(projects.shared)

            implementation(compose.desktop.currentOs)

            implementation(libs.decompose.extensions.compose)

            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.security.chat.desktop.MainKt"
    }
}