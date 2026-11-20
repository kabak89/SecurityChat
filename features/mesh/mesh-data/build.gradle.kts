import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN
import com.securitychat.gradle.ConventionBasePluginExtension.Companion.IS_DEBUG_KEY
import com.securitychat.gradle.ConventionBasePluginExtension.Companion.isDebug

plugins {
    id("securitychat.convention.base")
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.kotlinxSerialization)
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
            implementation(projects.features.chat.chatDataCommon)
            implementation(projects.features.chat.chatDataNetwork)
            implementation(projects.features.chat.chatDataStorage)
            implementation(projects.features.chats.chatsDataStorage)
            implementation(projects.features.chats.chatsDataCommon)
            implementation(projects.features.user.userDataStorage)
            implementation(projects.features.users.usersDataStorage)
            implementation(projects.features.users.usersDataCommon)
            implementation(projects.common.backgroundWork)
            implementation(projects.common.coreComponent)
            implementation(projects.common.coreNetwork)
            implementation(projects.common.coreThreading)
            implementation(projects.common.coreTime)
            implementation(projects.common.encryption)
            implementation(projects.common.error)
            implementation(projects.common.log)
            implementation(projects.common.settings)
            implementation(libs.kotlinx.serialization)
        }
        jvmMain.dependencies {
            implementation(libs.kable.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmTest.dependencies {
            implementation(libs.cryptography.provider.optimal)
            implementation(libs.junit.jupiter)
            implementation(libs.junit.platform.launcher)
            implementation(libs.sqldelight.driver.sqlite.get().toString()) {
                exclude(group = "org.xerial", module = "sqlite-jdbc")
            }
        }
    }
}
