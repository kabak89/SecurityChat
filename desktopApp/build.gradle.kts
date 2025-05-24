plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    jvm()
    sourceSets {
        jvmMain.dependencies {
            implementation(projects.shared)
        }
    }
}