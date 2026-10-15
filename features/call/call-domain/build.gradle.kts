plugins {
    id("securitychat.convention.base")
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.call.domain"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.common.coreDomain)
            api(projects.common.error)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmTest.dependencies {
            implementation(libs.junit.jupiter)
            implementation(libs.junit.platform.launcher)
        }
    }
}
