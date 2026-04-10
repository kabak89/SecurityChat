package com.security.chat.multiplatform.features.profile.ui.di

import com.security.chat.multiplatform.features.profile.ui.screens.main.ProfileMainViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

public val profileUiModule: Module =
    module {
        viewModelOf(::ProfileMainViewModel)
    }
