plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.kotlinxSerialization)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.call.data"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization)

            implementation(projects.common.coreNetwork)
            implementation(projects.common.log)
            implementation(projects.features.call.callDomain)
            implementation(projects.features.user.userDataStorage)
            implementation(projects.features.users.usersDataCommon)
        }
        commonTest.dependencies {
            implementation(projects.common.encryption)
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.ktor.client.websockets)
        }
        jvmTest.dependencies {
            implementation(libs.junit.jupiter)
            implementation(libs.junit.platform.launcher)
        }
        androidMain.dependencies {
            implementation(libs.webrtc.android)
        }
    }
}
