plugins {
    id("securitychat.convention.base")
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.common.core.db"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.common.coreThreading)

            implementation(projects.common.settings)

            api(libs.koin.core)
            api(libs.sqldelight.coroutines.extensions)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.driver.android)
            implementation(libs.android.sqlcipher)
            implementation(libs.sqlite.android)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.driver.native)
            implementation(libs.sqliter.driver)
        }
        jvmMain.dependencies {
            implementation(libs.sqldelight.driver.sqlite)
        }
    }
}
