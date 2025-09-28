enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "SecurityChat"
include(":androidApp")
include(":desktopApp")
include(":shared")

include(
    ":common:core-ui",
    ":common:core-domain",
    ":common:core-component",
    ":common:settings",
    ":common:core-network",
    ":common:core-threading",
    ":common:icons-kit",
)
include(
    ":features:splash:splash-component",
    ":features:splash:splash-domain",
    ":features:splash:splash-ui",
    ":features:splash:splash-data",
)
include(
    ":features:user:user-data-storage",
)
include(
    ":features:main:main-component",
    ":features:main:main-ui",
)
include(
    ":features:chats:chats-component",
    ":features:chats:chats-ui",
    ":features:chats:chats-domain",
    ":features:chats:chats-data",
)
include(
    ":features:chat:chat-component",
    ":features:chat:chat-ui",
)