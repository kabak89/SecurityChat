package com.security.chat.multiplatform.features.authorize.ui.screens.authorize

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.security.chat.multiplatform.features.authorize.component.AuthorizeComponent
import com.security.chat.multiplatform.features.authorize.ui.screens.signin.SignInScreen
import com.security.chat.multiplatform.features.authorize.ui.screens.signup.SignUpScreen

@Composable
fun AuthorizeScreen(component: AuthorizeComponent) {
    Children(
        stack = component.childStack,
        animation = predictiveBackAnimation(
            backHandler = component.backHandler,
            fallbackAnimation = stackAnimation(slide()),
            onBack = component::onBackClicked,
        ),
    ) {
        when (val child = it.instance) {
            is AuthorizeComponent.Child.SignUp -> SignUpScreen(component = child.component)
            is AuthorizeComponent.Child.SignIn -> SignInScreen(component = child.component)
        }
    }
}