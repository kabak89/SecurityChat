package com.security.chat.multiplatform.features.user.data.network.mapper

import com.security.chat.multiplatform.features.user.data.network.entity.ProfileNM
import com.security.chat.multiplatform.features.user.data.network.entity.ProfileResponse

internal fun ProfileResponse.toNM(): ProfileNM {
    return ProfileNM(
        userId = id,
        login = login,
    )
}