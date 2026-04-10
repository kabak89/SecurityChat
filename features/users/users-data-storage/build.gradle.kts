plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlinCocoapods)
}

val modulePackage = "com.security.chat.multiplatform.features.users.data.storage"

sqldelight {
    databases {
        create("UsersDb") {
            packageName = modulePackage
            generateAsync = true
            dialect(libs.sqlite.dialect)
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
    }

    cocoapods {
        name = modulePackage
        ios.deploymentTarget = "14.0"
        version = "1.0.0"

        pod(
            name = "SQLCipher",
            version = libs.versions.iosSqlCipher.get(),
            linkOnly = true,
        )
    }
}
