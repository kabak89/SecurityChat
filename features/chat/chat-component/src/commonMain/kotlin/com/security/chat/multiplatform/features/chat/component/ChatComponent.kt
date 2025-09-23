package com.security.chat.multiplatform.features.chat.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.security.chat.multiplatform.common.core.component.BaseComponent
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.common.core.component.DiScopeHolder
import kotlinx.serialization.Serializable

public interface ChatComponent : BaseComponent, DiScopeHolder, BackHandlerOwner {

    public fun onBackClicked()

    public val childStack: Value<ChildStack<*, Child>>

    public sealed interface Child {

        public class PersonalChat(public val component: PersonalChatComponent) : Child

    }

}

public class ChatComponentImpl(
    private val onExit: () -> Unit,
    componentContext: ComponentContext,
) : ChatComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_CHAT,
    ) {

    private val navigation = StackNavigation<Params>()

    override val childStack: Value<ChildStack<*, ChatComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Params.serializer(),
            initialConfiguration = Params.PersonalChatParams,
            handleBackButton = true,
            childFactory = ::createChild,
        )

    override fun onBackClicked() {
        navigation.pop()
    }

    private fun createChild(
        params: Params,
        componentContext: ComponentContext,
    ): ChatComponent.Child {
        return when (params) {
            is Params.PersonalChatParams -> {
                ChatComponent.Child.PersonalChat(
                    component = PersonalChatComponentImpl(
                        componentContext = componentContext,
                        onExit = onExit,
                    ),
                )
            }
        }
    }

    @Serializable
    private sealed class Params {

        @Serializable
        data object PersonalChatParams : Params()

    }

}

public const val SCOPE_ID_CHAT: String = "SCOPE_ID_CHAT"