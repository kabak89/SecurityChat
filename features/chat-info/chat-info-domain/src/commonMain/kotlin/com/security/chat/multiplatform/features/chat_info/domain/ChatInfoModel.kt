package com.security.chat.multiplatform.features.chat_info.domain

import com.security.chat.multiplatform.common.core.domain.BaseModel
import com.security.chat.multiplatform.common.core.domain.ScopedModel
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.chat_info.domain.repository.ChatInfoRepository

public interface ChatInfoModel : ScopedModel

internal class ChatInfoModelImpl(
    private val chatInfoRepository: ChatInfoRepository,
    dispatcherProvider: DispatcherProviderInterface,
) : ChatInfoModel,
    BaseModel(
        dispatcher = dispatcherProvider.Default,
    )
