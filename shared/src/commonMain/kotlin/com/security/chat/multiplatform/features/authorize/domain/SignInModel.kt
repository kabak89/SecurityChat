package com.security.chat.multiplatform.features.authorize.domain

import com.security.chat.multiplatform.common.core.domain.BaseModel
import com.security.chat.multiplatform.common.core.domain.ScopedModel
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.authorize.domain.entity.SignInResult
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

    val signIn: Task0<Unit>

    fun setUsername(userName: String)
    fun setPassword(password: String)
    fun getAuthResultFlow(): Flow<SignInResult?>
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

    override val signIn: Task0<Unit> =
        task { ->
            val username = stateFlow.value.username
            val password = stateFlow.value.password

            val result = signInRepo.signIn(
                username = username,
                password = password,
            )

            stateFlow.update { it.copy(signInResult = result) }
        }

    override fun setUsername(userName: String) {
        stateFlow.update { it.copy(username = userName) }
    }

    override fun setPassword(password: String) {
        stateFlow.update { it.copy(password = password) }
    }

    override fun getAuthResultFlow(): Flow<SignInResult?> {
        return stateFlow
            .map { it.signInResult }
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
        val signInResult: SignInResult? = null,
    )

}