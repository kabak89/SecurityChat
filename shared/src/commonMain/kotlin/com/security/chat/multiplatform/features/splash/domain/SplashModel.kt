package com.security.chat.multiplatform.features.splash.domain

import com.security.chat.multiplatform.common.core.domain.BaseModel
import com.security.chat.multiplatform.common.core.domain.ScopedModel
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.splash.domain.entity.UserState
import com.security.chat.multiplatform.features.splash.domain.repo.SplashRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import ru.kode.remo.Task0

interface SplashModel : ScopedModel {
    val fetchUserState: Task0<Unit>

    fun getUserStateFlow(): Flow<UserState>
}

class SplashModelImpl(
    private val splashRepo: SplashRepo,
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
            val isUserAuthorized = splashRepo.isUserAuthorized()
            val userState = if (isUserAuthorized) {
                UserState.Authorized
            } else {
                UserState.NotAuthorized
            }
            stateFlow.update { it.copy(userState = userState) }
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