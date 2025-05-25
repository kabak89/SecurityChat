plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvm()
    sourceSets {
        jvmMain.dependencies {
            implementation(projects.shared)

            implementation(compose.desktop.currentOs)

            implementation(libs.decompose.extensions.compose)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.security.chat.desktop.MainKt"
    }
}