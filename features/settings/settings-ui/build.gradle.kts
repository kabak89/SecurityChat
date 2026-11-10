import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN
import com.securitychat.gradle.ConventionBasePluginExtension.Companion.IS_DEBUG_KEY
import com.securitychat.gradle.ConventionBasePluginExtension.Companion.isDebug

plugins {
    id("securitychat.convention.base")
    id("securitychat.convention.screenshot")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.buildkonfig)
}

val moduleNamespace = "com.security.chat.multiplatform.features.settings.ui"

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

conventionBasePlugin {
    namespace = moduleNamespace
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.resources)

            implementation(projects.common.coreUi)
            implementation(projects.common.uiKit)
            implementation(projects.common.permission)
            implementation(projects.common.deviceInfo)

            api(projects.features.settings.settingsComponentApi)

            implementation(projects.features.settings.settingsDomain)
            implementation(projects.features.mesh.meshUi)
            implementation(projects.features.profile.profileUi)
        }
        androidMain.dependencies {
            implementation(libs.androidx.ui.tooling)
        }
        jvmTest.dependencies {
            implementation(projects.common.coreTest)
        }
    }
}
