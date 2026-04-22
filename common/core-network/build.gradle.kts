import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import com.securitychat.gradle.ConventionBasePluginExtension.Companion.serverEnv
import java.util.Properties

plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.buildkonfig)
}

val moduleNamespace = "com.security.chat.multiplatform.common.core.network"
private val devBaseHostKey = "devBaseHost"
private val prodBaseHostKey = "prodBaseHost"
private val serverEnvDev = "dev"
private val serverEnvProd = "prod"

buildkonfig {
    packageName = moduleNamespace

    defaultConfigs {
        val env = serverEnv(project)

        buildConfigField(
            type = STRING,
            name = "baseHost",
            value = resolveBaseHostByEnv(project = project, env = env),
        )
    }
}

conventionBasePlugin {
    namespace = moduleNamespace
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.ktor.client.core)

            implementation(libs.koin.core)

            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.websockets)

            implementation(projects.common.coreComponent)
            implementation(projects.common.error)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
    }
}

private fun getOptionalStringFromLocalProperty(project: Project, key: String): String? {
    val properties = Properties()
    val localPropertiesFile = project.rootProject.file("local.properties")

    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { properties.load(it) }
    }

    return properties.getProperty(key)
}

private fun resolveBaseHostByEnv(project: Project, env: String): String {
    val hostKey = when (env) {
        serverEnvDev -> devBaseHostKey
        serverEnvProd -> prodBaseHostKey
        else -> devBaseHostKey
    }

    return getOptionalStringFromLocalProperty(project, hostKey)
        ?: if (env == serverEnvProd) {
            //release IP
            "178.104.240.201"
        } else {
            error("Property '$hostKey' not found in local.properties")
        }
}
