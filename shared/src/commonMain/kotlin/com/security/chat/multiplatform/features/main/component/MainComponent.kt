package com.security.chat.multiplatform.features.main.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackHandlerOwner

interface MainComponent : BackHandlerOwner {


}

class DefaultMainComponent(
    componentContext: ComponentContext,
) : MainComponent, ComponentContext by componentContext {

}