package com.securitychat.gradle

internal enum class ModuleLayer(val id: String) {
    COMMON("common"),
    DOMAIN("domain"),
    UI("ui"),
    DATA("data"),
    DATA_NETWORK("data-network"),
    DATA_STORAGE("data-storage"),
    DATA_COMMON("data-common"),
    COMPONENT("component"),
    COMPONENT_API("component-api"),
    ;

    companion object {
        fun fromPath(path: String): ModuleLayer? {
            if (path.startsWith(":common:")) return COMMON

            val segments = path.split(':')
            if (segments.size != 4 || segments[1] != "features") return null

            val featurePrefix = "${segments[2]}-"
            if (!segments[3].startsWith(featurePrefix)) return null

            val layerId = segments[3].removePrefix(featurePrefix)
            return entries.firstOrNull { it != COMMON && it.id == layerId }
        }
    }
}
