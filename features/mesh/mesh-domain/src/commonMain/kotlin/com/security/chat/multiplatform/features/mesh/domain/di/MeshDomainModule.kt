package com.security.chat.multiplatform.features.mesh.domain.di

import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.mesh.domain.MeshModel
import com.security.chat.multiplatform.features.mesh.domain.MeshModelImpl
import org.koin.core.module.Module
import org.koin.dsl.module

public val meshDomainModule: Module = module {
    single<MeshModel> {
        MeshModelImpl(
            meshRepo = get(),
            dispatcher = get<DispatcherProviderInterface>().Default,
            logManager = get(),
        )
    }
}
