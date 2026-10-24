plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlinCocoapods)
}

val modulePackage = "com.security.chat.multiplatform.features.chat.data.storage"

sqldelight {
    databases {
        create("ChatDb") {
            packageName = modulePackage
            generateAsync = true
            dialect(libs.sqlite.dialect)
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
        }
    }
    linkSqlite = false
}

conventionBasePlugin {
    namespace = modulePackage
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)

            implementation(projects.common.coreDb)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.junit.jupiter)
            implementation(libs.junit.platform.launcher)
            implementation(libs.sqldelight.driver.sqlite.get().toString()) {
                exclude(group = "org.xerial", module = "sqlite-jdbc")
            }
        }
    }

    cocoapods {
        name = modulePackage
        ios.deploymentTarget = libs.versions.deploymentTarget.get()
        version = "1.0.0"

        pod(
            name = "SQLCipher",
            version = libs.versions.iosSqlCipher.get(),
            linkOnly = true,
        )
    }
}
