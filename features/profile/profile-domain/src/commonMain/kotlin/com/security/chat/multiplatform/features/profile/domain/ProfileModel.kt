package com.security.chat.multiplatform.features.profile.domain

import com.security.chat.multiplatform.common.core.domain.BaseModel
import com.security.chat.multiplatform.common.core.domain.ScopedModel
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.profile.domain.entity.Profile
import com.security.chat.multiplatform.features.profile.domain.entity.UpdateProfileParams
import com.security.chat.multiplatform.features.profile.domain.repo.ProfileRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import ru.kode.remo.Task0

public interface ProfileModel : ScopedModel {
    public val fetchUserInfo: Task0<Unit>
    public val updateUserInfo: Task0<Unit>

    public fun getProfileFlow(): Flow<Profile?>
    public fun setName(name: String)
    public fun getIsUpdateAvailableFlow(): Flow<Boolean>
}

internal class ProfileModelImpl(
    private val profileRepo: ProfileRepo,
    dispatcherProvider: DispatcherProviderInterface,
) : ProfileModel,
    BaseModel(
        dispatcher = dispatcherProvider.Default,
    ) {

    private val stateFlow = MutableStateFlow<State>(State())

    override val fetchUserInfo: Task0<Unit> =
        task { ->
            val cachedProfile = profileRepo.getProfile()
            stateFlow.update { it.copy(profile = cachedProfile) }
            profileRepo.fetchUserInfo()
            val profile = profileRepo.getProfile()
            stateFlow.update {
                it.copy(
                    profile = profile,
                    newName = null,
                )
            }
        }

    override val updateUserInfo: Task0<Unit> =
        task { ->
            val newName = requireNotNull(stateFlow.value.newName).trim()
            val params = UpdateProfileParams(
                name = newName,
            )
            profileRepo.updateProfile(params)
            val profile = profileRepo.getProfile()
            stateFlow.update { it.copy(profile = profile) }
        }

    override fun getProfileFlow(): Flow<Profile?> {
        return stateFlow
            .map { state ->
                if (state.newName != null) {
                    state.profile?.copy(name = state.newName)
                } else {
                    state.profile
                }
            }
            .distinctUntilChanged()
    }

    override fun setName(name: String) {
        stateFlow.update { it.copy(newName = name) }
    }

    override fun getIsUpdateAvailableFlow(): Flow<Boolean> {
        return stateFlow
            .map {
                it.newName != null &&
                        it.profile != null &&
                        it.newName.trim() != it.profile.name &&
                        it.newName.isNotBlank()
            }
            .distinctUntilChanged()
    }

    private data class State(
        val profile: Profile? = null,
        val newName: String? = null,
    )
}
