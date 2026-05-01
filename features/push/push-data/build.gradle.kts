plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.kotlinxSerialization)
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.features.push.data"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)

            implementation(projects.common.coreNetwork)
            implementation(projects.common.settings)
            implementation(projects.common.log)
            implementation(projects.common.deviceInfo)

            implementation(projects.features.push.pushDomain)
            implementation(projects.features.push.pushNavigationApi)
            implementation(projects.features.user.userDataStorage)
        }
        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.messaging)
        }
    }

    android {
        androidResources {
            enable = true
        }
    }
}
