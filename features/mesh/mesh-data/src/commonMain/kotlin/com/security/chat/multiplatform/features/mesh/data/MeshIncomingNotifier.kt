package com.security.chat.multiplatform.features.mesh.data

public fun interface MeshIncomingNotifier {

    public suspend fun notify(chatId: String)
}
