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

rootProject.name = "SecurityChat"
include(":androidApp")
include(":desktopApp")
include(":shared")

include(
    ":common:core-ui",
    ":common:core-domain",
    ":common:core-component",
)
include(
    ":features:splash:splash-component",
    ":features:splash:splash-domain",
)