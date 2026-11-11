plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.kotlinxSerialization)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.common.background.work"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.common.coreComponent)
            implementation(projects.common.log)
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.androidx.work.runtime)
        }
        iosMain.dependencies {
            implementation(libs.kotlinx.serialization)
        }
        jvmMain.dependencies {
            implementation(projects.common.coreNetwork)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.junit.jupiter)
            runtimeOnly(libs.junit.platform.launcher)
        }
    }
}
