package com.security.chat.multiplatform.features.users.data.storage.mapper

import com.security.chat.multiplatform.features.users.data.storage.UsersTable
import com.security.chat.multiplatform.features.users.data.storage.entity.UserSM

internal fun UserSM.toTable(): UsersTable {
    return UsersTable(
        id = id,
        publicKey = publicKey,
        name = name,
    )
}

internal fun UsersTable.toSM(): UserSM {
    return UserSM(
        id = id,
        publicKey = publicKey,
        name = name,
    )
}