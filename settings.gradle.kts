enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
pluginManagement {
    includeBuild("build-logic")
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
include(":shared")
include(":androidApp")

include(
    ":common:core-ui",
    ":common:core-domain",
    ":common:core-component",
    ":common:core-db",
    ":common:core-time",
    ":common:settings",
    ":common:core-network",
    ":common:core-threading",
    ":common:icons-kit",
    ":common:ui-kit",
    ":common:app-lifecycle",
    ":common:log",
)
include(
    ":features:splash:splash-component",
    ":features:splash:splash-component-api",
    ":features:splash:splash-domain",
    ":features:splash:splash-ui",
    ":features:splash:splash-data",
)
include(
    ":features:authorize:authorize-component",
    ":features:authorize:authorize-component-api",
    ":features:authorize:authorize-domain",
    ":features:authorize:authorize-ui",
    ":features:authorize:authorize-data",
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
    ":features:chats:chats-component-api",
    ":features:chats:chats-ui",
    ":features:chats:chats-domain",
    ":features:chats:chats-data",
    ":features:chats:chats-data-storage",
)
include(
    ":features:chat:chat-component",
    ":features:chat:chat-component-api",
    ":features:chat:chat-ui",
    ":features:chat:chat-domain",
    ":features:chat:chat-data",
    ":features:chat:chat-data-storage",
)
include(
    ":features:users:users-data-storage",
)
include(
    ":features:settings:settings-component",
    ":features:settings:settings-component-api",
    ":features:settings:settings-ui",
    ":features:settings:settings-domain",
    ":features:settings:settings-data",
    ":features:settings:settings-data-storage",
)
include(
    ":features:profile:profile-component",
    ":features:profile:profile-component-api",
    ":features:profile:profile-ui",
    ":features:profile:profile-domain",
    ":features:profile:profile-data",
    ":features:profile:profile-data-storage",
)