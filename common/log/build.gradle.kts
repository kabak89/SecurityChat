import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN
import com.securitychat.gradle.ConventionBasePluginExtension.Companion.ENABLE_LOGS_KEY
import com.securitychat.gradle.ConventionBasePluginExtension.Companion.enableLogs

plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.buildkonfig)
}

val moduleNamespace = "com.security.chat.multiplatform.common.log"

buildkonfig {
    packageName = moduleNamespace

    defaultConfigs {
        buildConfigField(
            type = BOOLEAN,
            name = ENABLE_LOGS_KEY,
            value = enableLogs(project).toString(),
        )
    }
}

conventionBasePlugin {
    namespace = moduleNamespace
}

kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(libs.timber)
        }
    }
}
