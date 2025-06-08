package com.security.chat.multiplatform.features.authorize.domain

import com.security.chat.multiplatform.common.core.domain.BaseModel
import com.security.chat.multiplatform.common.core.domain.ScopedModel
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.authorize.domain.entity.AuthResult
import com.security.chat.multiplatform.features.authorize.domain.entity.SignInStateInfo
import com.security.chat.multiplatform.features.authorize.domain.repo.SignInRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import ru.kode.remo.Task0

interface SignInModel : ScopedModel {

    val authorize: Task0<Unit>

    fun setUsername(userName: String)
    fun setPassword(password: String)
    fun getAuthResultFlow(): Flow<AuthResult?>
    fun getStateFlow(): Flow<SignInStateInfo>

}

class SignInModelImpl(
    private val signInRepo: SignInRepo,
    dispatcherProvider: DispatcherProviderInterface,
    coroutineScope: CoroutineScope,
) : SignInModel,
    BaseModel(
        dispatcher = dispatcherProvider.Default,
        coroutineScope = coroutineScope,
    ) {

    private val stateFlow = MutableStateFlow(State())

    override val authorize: Task0<Unit> =
        task { ->
            val username = stateFlow.value.username
            val password = stateFlow.value.password

            val result = signInRepo.authorize(
                username = username,
                password = password,
            )

            stateFlow.update { it.copy(authResult = result) }
        }

    override fun setUsername(userName: String) {
        stateFlow.update { it.copy(username = userName) }
    }

    override fun setPassword(password: String) {
        stateFlow.update { it.copy(password = password) }
    }

    override fun getAuthResultFlow(): Flow<AuthResult?> {
        return stateFlow
            .map { it.authResult }
            .distinctUntilChanged()
    }

    override fun getStateFlow(): Flow<SignInStateInfo> {
        return stateFlow
            .map {
                SignInStateInfo(
                    username = it.username,
                    password = it.password,
                )
            }
            .distinctUntilChanged()
    }

    private data class State(
        val username: String = "",
        val password: String = "",
        val authResult: AuthResult? = null,
    )

}