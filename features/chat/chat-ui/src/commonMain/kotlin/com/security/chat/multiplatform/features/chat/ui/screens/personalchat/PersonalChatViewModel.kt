package com.security.chat.multiplatform.features.chat.ui.screens.personalchat

import com.security.chat.multiplatform.common.core.ui.BaseViewModel

internal class PersonalChatViewModel(
) : BaseViewModel<PersonalChatState, PersonalChatEvent>() {

    override fun onPostStart() {
        super.onPostStart()


    }

    override fun createInitialState(): PersonalChatState {
        return PersonalChatState(
            message = "",
        )
    }

    fun onMessageEdited(message: String) {
        //TODO
    }

    fun onSendMessageClicked() {
        //TODO
    }

}