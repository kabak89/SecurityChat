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
}

gradlePlugin {
    plugins {
        register("securitychatConventionBase") {
            id = "securitychat.convention.base"
            implementationClass = "com.securitychat.gradle.ConventionBasePlugin"
        }
    }
}
