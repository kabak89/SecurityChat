package com.security.chat.multiplatform.features.authorize.domain

import com.security.chat.multiplatform.common.core.domain.BaseModel
import com.security.chat.multiplatform.common.core.domain.ScopedModel
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.authorize.domain.entity.SignInStateInfo
import com.security.chat.multiplatform.features.authorize.domain.repo.SignInRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kode.remo.Task0

public interface SignInModel : ScopedModel {

    public val signIn: Task0<Unit>

    public fun setUsername(userName: String)
    public fun setPassword(password: String)
    public fun getStateFlow(): Flow<SignInStateInfo>
}

internal class SignInModelImpl(
    private val signInRepo: SignInRepo,
    dispatcherProvider: DispatcherProviderInterface,
) : SignInModel,
    BaseModel(
        dispatcher = dispatcherProvider.Default,
    ) {

    private val stateFlow = MutableStateFlow(State())

    override val signIn: Task0<Unit> =
        task { ->
            val username = stateFlow.value.username
            val password = stateFlow.value.password

            signInRepo.signIn(
                username = username,
                password = password,
            )
        }

    override fun onPostStart() {
        super.onPostStart()

        scope.launch {
            val onboardingPassed = signInRepo.isOnboardingPassed()
            stateFlow.update { it.copy(isOnboardingPassed = onboardingPassed) }
        }
    }

    override fun setUsername(userName: String) {
        stateFlow.update { it.copy(username = userName) }
    }

    override fun setPassword(password: String) {
        stateFlow.update { it.copy(password = password) }
    }

    override fun getStateFlow(): Flow<SignInStateInfo> {
        return stateFlow
            .map {
                val username = it.username
                val password = it.password

                SignInStateInfo(
                    username = username,
                    password = password,
                    isSignInEnabled = username.isNotBlank() && password.isNotBlank(),
                    isOnboardingPassed = it.isOnboardingPassed,
                )
            }
            .distinctUntilChanged()
    }

    private data class State(
        val username: String = "",
        val password: String = "",
        val isOnboardingPassed: Boolean = false,
    )
}