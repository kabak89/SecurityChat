package com.security.chat.multiplatform.features.call.ui.di

import com.security.chat.multiplatform.features.call.ui.screens.call.CallViewModel
import com.security.chat.multiplatform.features.call.ui.screens.permissions.PermissionsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

public val callUiModule: Module =
    module {
        viewModelOf(::CallViewModel)
        viewModelOf(::PermissionsViewModel)
    }
