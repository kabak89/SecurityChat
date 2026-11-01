plugins {
    id("securitychat.convention.base")
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.profile.data"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(projects.common.coreNetwork)
            implementation(projects.common.encryption)

            implementation(projects.features.profile.profileDomain)
            implementation(projects.features.profile.profileDataStorage)
            implementation(projects.features.user.userDataStorage)
            implementation(projects.features.user.userDataNetwork)
            implementation(projects.features.settings.settingsDataCommon)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.junit.jupiter)
            implementation(libs.junit.platform.launcher)
        }
    }
}
