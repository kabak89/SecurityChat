package com.security.chat.multiplatform.features.authorize.component

import com.arkivanov.decompose.ComponentContext
import com.security.chat.multiplatform.common.base.BaseComponent
import com.security.chat.multiplatform.common.base.BaseComponentImpl
import com.security.chat.multiplatform.common.base.DiScopeHolder

interface SignInComponent : BaseComponent, DiScopeHolder {
}

class DefaultSignInComponent(
    componentContext: ComponentContext,
//    private val onItemSelected: (id: Long) -> Unit,
) : SignInComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_SIGN_IN,
    ) {

//    override fun onItemClicked(id: Long) {
//        onItemSelected(id)
//    }

}

const val SCOPE_ID_SIGN_IN = "SCOPE_ID_SIGN_IN"