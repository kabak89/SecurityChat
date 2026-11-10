import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN
import com.securitychat.gradle.ConventionBasePluginExtension.Companion.IS_DEBUG_KEY
import com.securitychat.gradle.ConventionBasePluginExtension.Companion.isDebug

plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.buildkonfig)
}

val moduleNamespace = "com.security.chat.multiplatform.features.mesh.data"

conventionBasePlugin {
    namespace = moduleNamespace
}

buildkonfig {
    packageName = moduleNamespace

    defaultConfigs {
        buildConfigField(
            type = BOOLEAN,
            name = IS_DEBUG_KEY,
            value = isDebug(project).toString(),
        )
    }
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.koin.core)

            implementation(projects.features.mesh.meshDomain)
            implementation(projects.features.mesh.meshDataStorage)
            implementation(projects.common.coreComponent)
            implementation(projects.common.coreTime)
            implementation(projects.common.log)
        }
        jvmMain.dependencies {
            implementation(libs.kable.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmTest.dependencies {
            implementation(libs.junit.jupiter)
            implementation(libs.junit.platform.launcher)
            implementation(libs.sqldelight.driver.sqlite.get().toString()) {
                exclude(group = "org.xerial", module = "sqlite-jdbc")
            }
        }
    }
}
