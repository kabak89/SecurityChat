package com.security.chat.multiplatform.features.authorize.domain

import com.security.chat.multiplatform.common.core.domain.BaseModel
import com.security.chat.multiplatform.common.core.domain.ScopedModel
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.authorize.domain.entity.SignUpResult
import com.security.chat.multiplatform.features.authorize.domain.entity.SignUpStateInfo
import com.security.chat.multiplatform.features.authorize.domain.repo.SignUpRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import ru.kode.remo.Task0

interface SignUpModel : ScopedModel {
    val signUp: Task0<Unit>

    fun setUsername(userName: String)
    fun setPassword(password: String)
    fun setPasswordRepeat(passwordRepeat: String)
    fun getStateFlow(): Flow<SignUpStateInfo>
    fun getResultFlow(): Flow<SignUpResult?>
}

internal class SignUpModelImpl(
    private val signUpRepo: SignUpRepo,
    dispatcherProvider: DispatcherProviderInterface,
    coroutineScope: CoroutineScope,
) : SignUpModel,
    BaseModel(
        dispatcher = dispatcherProvider.Default,
        coroutineScope = coroutineScope,
    ) {

    private val stateFlow = MutableStateFlow(State())

    override val signUp: Task0<Unit> =
        task { ->
            if (!stateFlow.value.formFilled) return@task

            val result = signUpRepo.signUp(
                username = stateFlow.value.username.trim(),
                password = stateFlow.value.password,
            )

            stateFlow.update { it.copy(result = result) }
        }

    override fun setUsername(userName: String) {
        stateFlow.update { it.copy(username = userName) }
    }

    override fun setPassword(password: String) {
        stateFlow.update { it.copy(password = password) }
    }

    override fun setPasswordRepeat(passwordRepeat: String) {
        stateFlow.update { it.copy(passwordRepeat = passwordRepeat) }
    }

    override fun getStateFlow(): Flow<SignUpStateInfo> {
        return stateFlow
            .map { state ->
                SignUpStateInfo(
                    username = state.username,
                    password = state.password,
                    passwordRepeat = state.passwordRepeat,
                    formFilled = state.formFilled,
                )
            }
            .distinctUntilChanged()
    }

    override fun getResultFlow(): Flow<SignUpResult?> {
        return stateFlow
            .map { it.result }
            .distinctUntilChanged()
    }

    private data class State(
        val username: String = "",
        val password: String = "",
        val passwordRepeat: String = "",
        val result: SignUpResult? = null,
    ) {
        val formFilled: Boolean =
            username.isNotBlank() &&
                    password.isNotBlank() &&
                    passwordRepeat.isNotBlank() &&
                    (password == passwordRepeat)
    }

}