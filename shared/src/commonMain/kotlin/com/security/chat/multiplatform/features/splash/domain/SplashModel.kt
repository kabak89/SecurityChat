package com.security.chat.multiplatform.features.splash.domain

import com.security.chat.multiplatform.common.core.domain.BaseModel
import com.security.chat.multiplatform.common.core.domain.ScopedModel
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.splash.domain.entity.UserState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import ru.kode.remo.Task0
import kotlin.time.Duration.Companion.seconds

interface SplashModel : ScopedModel {
    val fetchUserState: Task0<Unit>

    fun getUserStateFlow(): Flow<UserState>
}

class SplashModelImpl(
    dispatcherProvider: DispatcherProviderInterface,
    coroutineScope: CoroutineScope,
) : SplashModel,
    BaseModel(
        dispatcher = dispatcherProvider.Default,
        coroutineScope = coroutineScope,
    ) {

    private val stateFlow = MutableStateFlow(State())

    override val fetchUserState: Task0<Unit> =
        task { ->
            //TODO
            delay(2.seconds)
            stateFlow.update { it.copy(userState = UserState.NotAuthorized) }
        }

    override fun getUserStateFlow(): Flow<UserState> {
        return stateFlow
            .map { it.userState }
            .distinctUntilChanged()
    }

    private data class State(
        val userState: UserState = UserState.Unknown,
    )

}