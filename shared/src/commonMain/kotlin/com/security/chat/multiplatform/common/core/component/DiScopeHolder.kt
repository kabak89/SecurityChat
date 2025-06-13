package com.security.chat.multiplatform.common.core.component

import org.koin.core.component.KoinComponent
import org.koin.core.scope.Scope

interface DiScopeHolder : KoinComponent {

    fun getDiScope(): Scope

}