plugins {
    id("securitychat.convention.base")
}

conventionBasePlugin {
    namespace = "com.security.chat.multiplatform.common.encryption"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.bignum)
            implementation(libs.sha2)
            implementation(libs.cryptography.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
