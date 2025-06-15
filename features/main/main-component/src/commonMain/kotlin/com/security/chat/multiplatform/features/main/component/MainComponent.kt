package com.security.chat.multiplatform.features.main.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackHandlerOwner

public interface MainComponent : BackHandlerOwner {


}

public class MainComponentImpl(
    componentContext: ComponentContext,
) : MainComponent, ComponentContext by componentContext {

}