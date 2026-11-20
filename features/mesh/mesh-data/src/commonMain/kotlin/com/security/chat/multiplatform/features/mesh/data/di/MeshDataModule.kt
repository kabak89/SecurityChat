package com.security.chat.multiplatform.features.mesh.data.di

import com.security.chat.multiplatform.common.background.work.backgroundWorker
import com.security.chat.multiplatform.common.core.component.SCOPE_ID_APP
import com.security.chat.multiplatform.features.mesh.data.BluetoothManager
import com.security.chat.multiplatform.features.mesh.data.BluetoothManagerImpl
import com.security.chat.multiplatform.features.mesh.data.BuildKonfig
import com.security.chat.multiplatform.features.mesh.data.ExchangeMeshMessagesWorker
import com.security.chat.multiplatform.features.mesh.data.LogManagerImpl
import com.security.chat.multiplatform.features.mesh.data.MESH_EXCHANGE_WORK
import com.security.chat.multiplatform.features.mesh.data.MESH_UPLOAD_WORK
import com.security.chat.multiplatform.features.mesh.data.MeshMessageCrypto
import com.security.chat.multiplatform.features.mesh.data.MeshMessageService
import com.security.chat.multiplatform.features.mesh.data.MeshRelayApi
import com.security.chat.multiplatform.features.mesh.data.MeshRepoImpl
import com.security.chat.multiplatform.features.mesh.data.UploadMeshMessagesWorker
import com.security.chat.multiplatform.features.mesh.data.storage.MeshLogStorage
import com.security.chat.multiplatform.features.mesh.data.storage.MeshMessageStorage
import com.security.chat.multiplatform.features.mesh.domain.LogManager
import com.security.chat.multiplatform.features.mesh.domain.repo.MeshRepo
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

public val meshDataModule: Module = module {
    singleOf(::MeshLogStorage)
    single<LogManager> {
        LogManagerImpl(
            storage = get(),
            timeProvider = get(),
            appScope = get(named(SCOPE_ID_APP)),
            isDebug = BuildKonfig.IS_DEBUG,
        )
    }
    singleOf(::BluetoothManagerImpl) bind BluetoothManager::class
    singleOf(::MeshRepoImpl) bind MeshRepo::class
    singleOf(::MeshMessageStorage)
    singleOf(::MeshMessageCrypto)
    singleOf(::MeshRelayApi)
    singleOf(::MeshMessageService)
    backgroundWorker(
        id = MESH_EXCHANGE_WORK,
        worker = ExchangeMeshMessagesWorker::class,
        create = { ExchangeMeshMessagesWorker(get()) },
    )
    backgroundWorker(
        id = MESH_UPLOAD_WORK,
        worker = UploadMeshMessagesWorker::class,
        create = { UploadMeshMessagesWorker(get()) },
    )
}
