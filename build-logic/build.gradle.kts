import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

group = "com.securitychat.gradle"

/**
 * Convention plugins are loaded into the Gradle daemon, so their bytecode target is bound to the
 * daemon JDK and is deliberately kept separate from the app-wide `javaVersion`.
 */
val buildLogicJavaVersion = JavaVersion.toVersion(libs.versions.buildLogicJavaVersion.get())

java {
    sourceCompatibility = buildLogicJavaVersion
    targetCompatibility = buildLogicJavaVersion
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(buildLogicJavaVersion.toString()))
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.android.gradleApi)
    compileOnly(libs.kotlin.gradlePlugin)

    testImplementation(gradleTestKit())
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        register("securitychatConventionDependencies") {
            id = "securitychat.convention.dependencies"
            implementationClass = "com.securitychat.gradle.ModuleDependencyValidationPlugin"
        }
        register("securitychatConventionBase") {
            id = "securitychat.convention.base"
            implementationClass = "com.securitychat.gradle.ConventionBasePlugin"
        }
        register("securitychatConventionScreenshot") {
            id = "securitychat.convention.screenshot"
            implementationClass = "com.securitychat.gradle.ScreenshotTestPlugin"
        }
        register("securitychatConventionUiTest") {
            id = "securitychat.convention.ui-test"
            implementationClass = "com.securitychat.gradle.UiTestPlugin"
        }
    }
}
