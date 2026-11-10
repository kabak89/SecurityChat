plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.sqldelight)
}

val moduleNamespace = "com.security.chat.multiplatform.features.mesh.data.storage"

conventionBasePlugin {
    namespace = moduleNamespace
}

sqldelight {
    databases {
        create("MeshLogsDb") {
            packageName = moduleNamespace
            generateAsync = true
            dialect(libs.sqlite.dialect)
        }
    }
    linkSqlite = false
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.common.coreDb)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
