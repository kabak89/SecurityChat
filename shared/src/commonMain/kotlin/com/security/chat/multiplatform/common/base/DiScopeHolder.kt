package com.security.chat.multiplatform.common.base

import org.koin.core.component.KoinComponent
import org.koin.core.scope.Scope

interface DiScopeHolder : KoinComponent {

    fun getDiScope(): Scope

}