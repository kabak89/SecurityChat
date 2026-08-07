package com.security.chat.multiplatform.features.root.component.api

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.security.chat.multiplatform.common.core.component.DiScopeHolder
import com.security.chat.multiplatform.features.authorize.component.api.AuthorizeComponent
import com.security.chat.multiplatform.features.main.component.MainComponent
import com.security.chat.multiplatform.features.onboarding.component.api.OnboardingComponent
import com.security.chat.multiplatform.features.splash.component.SplashComponent

public interface RootComponent : BackHandlerOwner, DiScopeHolder {
    public val childStack: Value<ChildStack<*, Child>>

    public fun onBackClicked()

    public fun handleDeepLink(link: DeepLink)

    public sealed interface Child {
        public class Splash(public val component: SplashComponent) : Child
        public class Authorize(public val component: AuthorizeComponent) : Child
        public class Main(public val component: MainComponent) : Child
        public class Onboarding(public val component: OnboardingComponent) : Child
    }

    public sealed interface DeepLink {
        public data class OpenPrivateChat(val chatId: String) : DeepLink
        public data class OpenGroupChat(val chatId: String) : DeepLink
        public data class SendText(val text: String) : DeepLink
    }
}
