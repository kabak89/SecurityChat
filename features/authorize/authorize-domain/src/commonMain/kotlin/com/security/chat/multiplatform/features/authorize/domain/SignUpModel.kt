package com.security.chat.multiplatform.features.authorize.domain

import com.security.chat.multiplatform.common.core.domain.BaseModel
import com.security.chat.multiplatform.common.core.domain.ScopedModel
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.common.log.Log
import com.security.chat.multiplatform.features.authorize.domain.entity.SignUpStateInfo
import com.security.chat.multiplatform.features.authorize.domain.repo.SignUpRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kode.remo.Task0

public interface SignUpModel : ScopedModel {
    public val signUp: Task0<Unit>

    public fun setUsername(userName: String)
    public fun getStateFlow(): Flow<SignUpStateInfo>
}

internal class SignUpModelImpl(
    private val signUpRepo: SignUpRepo,
    dispatcherProvider: DispatcherProviderInterface,
) : SignUpModel,
    BaseModel(
        dispatcher = dispatcherProvider.Default,
    ) {

    private val stateFlow = MutableStateFlow(State())

    override val signUp: Task0<Unit> =
        task { ->
            if (!stateFlow.value.formFilled) {
                Log.e("form is not filled")
                return@task
            }

            signUpRepo.signUp(
                username = stateFlow.value.username.trim(),
            )
        }

    override fun onPostStart() {
        super.onPostStart()

        scope.launch {
            val onboardingPassed = signUpRepo.isOnboardingPassed()
            stateFlow.update { it.copy(isOnboardingPassed = onboardingPassed) }
        }
    }

    override fun setUsername(userName: String) {
        stateFlow.update { it.copy(username = userName) }
    }

    override fun getStateFlow(): Flow<SignUpStateInfo> {
        return stateFlow
            .map { state ->
                SignUpStateInfo(
                    username = state.username,
                    formFilled = state.formFilled,
                    isOnboardingPassed = state.isOnboardingPassed,
                )
            }
            .distinctUntilChanged()
    }

    private data class State(
        val username: String = "",
        val isOnboardingPassed: Boolean = false,
    ) {
        val formFilled: Boolean = username.isNotBlank()
    }

}