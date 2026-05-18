package com.security.chat.multiplatform.features.push.data

import com.security.chat.multiplatform.common.core.network.NetworkManager
import com.security.chat.multiplatform.common.core.network.NetworkManagerFactory
import com.security.chat.multiplatform.common.core.network.entity.NetworkConfig
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.common.device.info.DeviceInfoManager
import com.security.chat.multiplatform.common.log.Log
import com.security.chat.multiplatform.common.settings.EncryptedSettings
import com.security.chat.multiplatform.features.chat.data.common.ChatDataHelper
import com.security.chat.multiplatform.features.chats.data.storage.ChatsStorage
import com.security.chat.multiplatform.features.push.data.entity.SyncedPushToken
import com.security.chat.multiplatform.features.push.data.network.RegisterDeviceTokenRequest
import com.security.chat.multiplatform.features.push.data.storage.PushStorage
import com.security.chat.multiplatform.features.push.domain.PushRepository
import com.security.chat.multiplatform.features.push.domain.entity.MessagesText
import com.security.chat.multiplatform.features.user.data.storage.UserStorage
import com.security.chat.multiplatform.features.users.data.storage.UsersStorage
import kotlinx.coroutines.withContext

public class PushRepositoryImpl(
    private val userStorage: UserStorage,
    private val networkConfig: NetworkConfig,
    private val networkManagerFactory: NetworkManagerFactory,
    private val deviceInfoManager: DeviceInfoManager,
    private val encryptedSettings: EncryptedSettings,
    private val dispatcherProviderInterface: DispatcherProviderInterface,
    private val chatsStorage: ChatsStorage,
    private val usersStorage: UsersStorage,
    private val chatDataHelper: ChatDataHelper,
    private val pushStorage: PushStorage,
    private val pushNotificationsManager: PushNotificationsManager,
) : PushRepository {

    private val networkManager: NetworkManager by lazy {
        networkManagerFactory.build(baseUrl = "${networkConfig.host}:${networkConfig.port}")
    }

    override suspend fun registerCurrentToken() {
        val token = runCatching { getToken() }
            .onFailure { Log.e(error = it, message = "Failed to retrieve push token") }
            .getOrNull()
            ?: return

        //TODO check push token after profile auth
        sendIfNeeded(token = token, force = true)
    }

    override suspend fun onTokenRefreshed(token: String) {
        sendIfNeeded(token = token, force = true)
    }

    override suspend fun getInterlocutorName(chatId: String): String? {
        val interlocutorId = chatsStorage.getChat(chatId)?.interlocutorId ?: return null
        return usersStorage.getUser(interlocutorId)?.name
    }

    override suspend fun processNewMessages(
        serializedMessages: String,
        chatId: String,
    ): MessagesText {
        val messagesTexts = chatDataHelper.processNewMessages(
            serializedMessages = serializedMessages,
            chatId = chatId,
        )
        return MessagesText(messagesTexts.joinToString(separator = "\n"))
    }

    override fun setShowNotificationsForChat(chatId: String, show: Boolean) {
        pushStorage.setShowNotificationsForChat(
            chatId = chatId,
            show = show,
        )
    }

    override fun isNotificationForChatMustBeShown(chatId: String): Boolean {
        return pushStorage.isNotificationForChatMustBeShown(chatId = chatId)
    }

    override fun clearNotificationsForChat(chatId: String) {
        pushNotificationsManager.clearNotificationsForChat(chatId)
    }

    private suspend fun sendIfNeeded(token: String, force: Boolean) {
        val userId = userStorage.getUserId()
        if (userId.isNullOrBlank()) {
            Log.d { "Push token registration skipped: user not authorised" }
            return
        }

        val lastSynced = getLastSyncedToken()
        val alreadyInSync = lastSynced != null &&
                lastSynced.userId == userId &&
                lastSynced.token == token
        if (!force && alreadyInSync) {
            Log.d { "Push token already in sync with backend, skipping" }
            return
        }

        runCatching {
            sendToken(
                userId = userId,
                token = token,
            )
        }
            .onSuccess {
                saveLastSyncedToken(
                    token = SyncedPushToken(userId = userId, token = token),
                )
                Log.d { "Push token successfully delivered to backend" }
            }
            .onFailure { error ->
                Log.e(error = error, message = "Failed to send push token to backend")
            }
    }

    private suspend fun sendToken(userId: String, token: String) {
        networkManager.runPost<RegisterDeviceTokenRequest, Unit>(
            relativePath = "/devices/token",
            request = RegisterDeviceTokenRequest(
                userId = userId,
                token = token,
                platform = deviceInfoManager.getPlatform(),
            ),
        )
    }

    private suspend fun getLastSyncedToken(): SyncedPushToken? {
        return withContext(dispatcherProviderInterface.IO) {
            val userId =
                encryptedSettings.getString(KEY_LAST_SYNCED_USER_ID) ?: return@withContext null
            val token =
                encryptedSettings.getString(KEY_LAST_SYNCED_PUSH_TOKEN) ?: return@withContext null
            SyncedPushToken(userId = userId, token = token)
        }
    }

    private suspend fun saveLastSyncedToken(token: SyncedPushToken?) {
        withContext(dispatcherProviderInterface.IO) {
            encryptedSettings.putString(
                key = KEY_LAST_SYNCED_USER_ID,
                value = token?.userId,
            )
            encryptedSettings.putString(
                key = KEY_LAST_SYNCED_PUSH_TOKEN,
                value = token?.token,
            )
        }
    }
}

private const val KEY_LAST_SYNCED_USER_ID = "KEY_LAST_SYNCED_PUSH_USER_ID"
private const val KEY_LAST_SYNCED_PUSH_TOKEN = "KEY_LAST_SYNCED_PUSH_TOKEN"