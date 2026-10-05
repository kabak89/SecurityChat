package com.security.chat.multiplatform.features.call.domain.repo

import com.security.chat.multiplatform.features.call.domain.entity.CallParticipant
import kotlinx.coroutines.flow.Flow

public interface CallRepo {

    public fun getFocusedParticipantsFlow(chatId: String): Flow<List<CallParticipant>>
}
