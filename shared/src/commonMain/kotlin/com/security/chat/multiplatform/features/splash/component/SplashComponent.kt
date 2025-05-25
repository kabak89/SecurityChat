package com.security.chat.multiplatform.features.splash.component

import com.arkivanov.decompose.ComponentContext

interface SplashComponent {
    fun onItemClicked(id: Long)
    fun onAuthorizeClicked()
}

class DefaultSplashComponent(
    private val onItemSelected: (id: Long) -> Unit,
    private val goToAuthorize: () -> Unit,
    componentContext: ComponentContext,
) : SplashComponent, ComponentContext by componentContext {

    override fun onItemClicked(id: Long) {
        onItemSelected(id)
    }

    override fun onAuthorizeClicked() {
        goToAuthorize()
    }
}