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
    ":common:core-files",
    ":common:localization",
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
    ":common:crash-report",
    ":common:error",
    ":common:device-info",
    ":common:platform-specific",
    ":common:permission",
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
    ":features:user:user-data-network",
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
    ":features:chat:chat-data-network",
    ":features:chat:chat-data-common",
)
include(
    ":features:users:users-data-storage",
    ":features:users:users-data-network",
)
include(
    ":features:settings:settings-component",
    ":features:settings:settings-component-api",
    ":features:settings:settings-ui",
    ":features:settings:settings-domain",
    ":features:settings:settings-data",
    ":features:settings:settings-data-common",
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
include(
    ":features:push:push-domain",
    ":features:push:push-data",
    ":features:push:push-navigation-api",
    ":features:push:push-navigation-impl",
)
include(
    ":features:root:root-component",
    ":features:root:root-component-api",
    ":features:root:root-ui",
)
include(
    ":features:onboarding:onboarding-component",
    ":features:onboarding:onboarding-component-api",
    ":features:onboarding:onboarding-ui",
    ":features:onboarding:onboarding-domain",
    ":features:onboarding:onboarding-data",
)