package com.security.chat.multiplatform.features.authorize.component

import com.arkivanov.decompose.ComponentContext

interface SignInComponent {
}

class DefaultSignInComponent(
    componentContext: ComponentContext,
//    private val onItemSelected: (id: Long) -> Unit,
) : SignInComponent, ComponentContext by componentContext {

//    override fun onItemClicked(id: Long) {
//        onItemSelected(id)
//    }

}