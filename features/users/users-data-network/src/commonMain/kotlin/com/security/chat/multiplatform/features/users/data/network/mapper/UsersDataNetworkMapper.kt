package com.security.chat.multiplatform.features.users.data.network.mapper

import com.security.chat.multiplatform.features.users.data.network.entity.FindUserResponse
import com.security.chat.multiplatform.features.users.data.network.entity.UserNM

internal fun FindUserResponse.toNM(): UserNM {
    return UserNM(
        userId = userId,
        name = login,
        publicKey = publicKey,
    )
}