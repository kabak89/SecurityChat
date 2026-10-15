package com.security.chat.multiplatform.common.core.network

import com.security.chat.multiplatform.common.core.error.NetworkError
import com.security.chat.multiplatform.common.core.network.entity.AccessToken
import com.security.chat.multiplatform.common.core.network.entity.NetworkConfig
import com.security.chat.multiplatform.common.core.network.entity.RefreshRequest
import com.security.chat.multiplatform.common.core.network.entity.RefreshResponse
import com.security.chat.multiplatform.common.core.network.entity.RefreshToken
import com.security.chat.multiplatform.common.core.network.entity.SocketAuthEnvelope
import com.security.chat.multiplatform.common.core.network.entity.SocketAuthState
import com.security.chat.multiplatform.common.core.network.entity.Tokens
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.common.settings.EncryptedSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TokenManagerTest {
    @Test
    fun `socket lease expiry does not cancel rotation or saving the token pair`() = runTest {
        withFixture { f ->
            val response = CompletableDeferred<Unit>()
            f.handler = { response.await(); f.success() }
            f.manager.saveTokens(oldTokens)
            var closed = false
            var updateCount = 0
            val worker = SocketAuthorizationClient(
                tokenManager = f.manager,
                json = Json,
                timeSource = testScheduler.timeSource,
                send = { updateCount++ },
                close = { closed = true },
            )
            val job = backgroundScope.launch { worker.run() }
            worker.receive(
                Json.encodeToString(
                    SocketAuthEnvelope(
                        "auth.state",
                        Json.encodeToString(SocketAuthState(20_000L, 20_000L)),
                    ),
                ),
            )
            runCurrent()
            assertEquals(1, f.requests.size)
            advanceTimeBy(20_000L)
            runCurrent()
            assertTrue(closed)
            assertTrue(job.isCompleted)
            assertEquals(oldTokens, f.manager.getTokens())
            assertFalse(f.settings.getString("KEY_PENDING_REFRESH").isNullOrBlank())
            response.complete(Unit)
            runCurrent()
            assertEquals(newTokens, f.manager.getTokens())
            assertNull(f.settings.getString("KEY_PENDING_REFRESH"))
            assertEquals(1, f.requests.size)
            assertEquals(0, updateCount)
            assertEquals(0, f.logoutCount)
        }
    }

    @Test
    fun `cancelled waiter does not interrupt rotation or saving the token pair`() = runTest {
        withFixture { f ->
            val response = CompletableDeferred<Unit>()
            f.handler = { response.await(); f.success() }
            f.manager.saveTokens(oldTokens)
            val caller = backgroundScope.async { f.manager.refreshTokens() }
            runCurrent()
            assertEquals(1, f.requests.size)
            caller.cancelAndJoin()
            response.complete(Unit)
            runCurrent()
            assertEquals(newTokens, f.manager.getTokens())
            assertEquals(0, f.logoutCount)
            assertNull(f.settings.getString("KEY_PENDING_REFRESH"))
            assertNull(f.settings.getString("KEY_ACCESS_TOKEN"))
            assertNull(f.settings.getString("KEY_REFRESH_TOKEN"))
        }
    }

    @Test
    fun `parallel callers share one refresh even when one cancels`() = runTest {
        withFixture { f ->
            val response = CompletableDeferred<Unit>()
            f.handler = { response.await(); f.success() }
            f.manager.saveTokens(oldTokens)
            val first = backgroundScope.async { f.manager.refreshTokens() }
            val second = backgroundScope.async { f.manager.refreshTokens() }
            runCurrent()
            assertEquals(1, f.requests.size)
            first.cancelAndJoin()
            response.complete(Unit)
            assertEquals(newTokens, second.await())
            assertEquals(1, f.requests.size)
        }
    }

    @Test
    fun `lost response retries the same request and saves the cached server result`() = runTest {
        withFixture { f ->
            var accepted: RefreshRequest? = null
            f.handler = { request ->
                if (accepted == null) {
                    accepted = request
                    Reply(HttpStatusCode.ServiceUnavailable, "Response lost after rotation")
                } else {
                    assertEquals(accepted, request)
                    f.success()
                }
            }
            f.manager.saveTokens(oldTokens)
            assertEquals(newTokens, f.manager.refreshTokens())
            assertEquals(2, f.requests.size)
            assertTrue(f.requests.first().requestId.isNotBlank())
            assertEquals(0, f.logoutCount)
        }
    }

    @Test
    fun `HTTP timeout retries with the same id instead of invalidating the session`() = runTest {
        withFixture { f ->
            f.handler = {
                if (f.requests.size == 1) delay(35_000)
                f.success()
            }
            f.manager.saveTokens(oldTokens)
            val caller = backgroundScope.async { f.manager.refreshTokens() }
            runCurrent()
            advanceTimeBy(35_001)
            runCurrent()
            assertEquals(newTokens, caller.await())
            assertEquals(2, f.requests.size)
            assertEquals(f.requests[0], f.requests[1])
            assertEquals(0, f.logoutCount)
        }
    }

    @Test
    fun `pending request survives a manager restart after transient failures`() = runTest {
        withFixture { f ->
            f.handler = { Reply(HttpStatusCode.ServiceUnavailable, "Unavailable") }
            f.manager.saveTokens(oldTokens)
            assertFailsWith<NetworkError> { f.manager.refreshTokens() }
            val pending = f.requests.first()
            assertEquals(3, f.requests.size)
            assertTrue(f.requests.all { it == pending })
            assertEquals(0, f.logoutCount)
            f.handler = { f.success() }
            assertEquals(newTokens, f.newManager().refreshTokens())
            assertEquals(pending, f.requests.last())
        }
    }

    @Test
    fun `logout prevents a late refresh response from restoring credentials`() = runTest {
        withFixture { f ->
            val response = CompletableDeferred<Unit>()
            f.handler = { response.await(); f.success() }
            f.manager.saveTokens(oldTokens)
            val caller = backgroundScope.async { f.manager.refreshTokens() }
            runCurrent()
            f.manager.clearTokens()
            response.complete(Unit)
            assertNull(caller.await())
            assertNull(f.settings.getString("KEY_TOKENS"))
            assertNull(f.settings.getString("KEY_PENDING_REFRESH"))
            assertEquals(0, f.logoutCount)
        }
    }

    @Test
    fun `late rejection from a previous login cannot log out the new login`() = runTest {
        withFixture { f ->
            val response = CompletableDeferred<Unit>()
            f.handler = { response.await(); Reply(HttpStatusCode.Unauthorized, "Revoked") }
            f.manager.saveTokens(oldTokens)
            val caller = backgroundScope.async { f.manager.refreshTokens() }
            runCurrent()
            f.manager.saveTokens(newTokens)
            response.complete(Unit)
            assertNull(caller.await())
            assertEquals(newTokens, f.manager.getTokens())
            assertEquals(0, f.logoutCount)
        }
    }

    @Test
    fun `invalid refresh token still reports logout without retries`() = runTest {
        withFixture { f ->
            f.handler = { Reply(HttpStatusCode.Unauthorized, "Revoked") }
            f.manager.saveTokens(oldTokens)
            assertNull(f.manager.refreshTokens())
            assertEquals(1, f.requests.size)
            assertEquals(1, f.logoutCount)
            assertNull(f.settings.getString("KEY_PENDING_REFRESH"))
        }
    }

    @Test
    fun `legacy token fields migrate to one stored pair`() = runTest {
        withFixture { f ->
            f.settings.putString("KEY_ACCESS_TOKEN", oldTokens.accessToken.value)
            f.settings.putString("KEY_REFRESH_TOKEN", oldTokens.refreshToken.value)
            assertEquals(oldTokens, f.manager.getTokens())
            assertFalse(f.settings.getString("KEY_TOKENS").isNullOrBlank())
            assertNull(f.settings.getString("KEY_ACCESS_TOKEN"))
            assertNull(f.settings.getString("KEY_REFRESH_TOKEN"))
            assertEquals(oldTokens, f.newManager().getTokens())
        }
    }

    private suspend fun TestScope.withFixture(block: suspend (Fixture) -> Unit) {
        val fixture = Fixture(this)
        try {
            block(fixture)
        } finally {
            fixture.appScope.cancel()
            fixture.client.close()
            fixture.engine.close()
        }
    }

    private class Fixture(private val scope: TestScope) {
        val settings = MemorySettings()
        val requests = mutableListOf<RefreshRequest>()
        var logoutCount = 0
        var handler: suspend (RefreshRequest) -> Reply = { success() }
        private val dispatcher = StandardTestDispatcher(scope.testScheduler)
        val appScope = CoroutineScope(
            scope.backgroundScope.coroutineContext +
                    SupervisorJob(scope.backgroundScope.coroutineContext[Job]),
        )
        val engine = MockEngine(
            MockEngineConfig().apply {
                this.dispatcher = this@Fixture.dispatcher
                addHandler { request ->
                    val body = (request.body as OutgoingContent.ByteArrayContent)
                        .bytes().decodeToString()
                    val payload = Json.decodeFromString<RefreshRequest>(body)
                    requests += payload
                    val reply = handler(payload)
                    respond(
                        reply.body, reply.status,
                        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            },
        )
        val client = HttpClient(engine) {
            expectSuccess = true
            install(ContentNegotiation) { json(Json) }
            HttpResponseValidator {
                handleResponseExceptionWithRequest { error, _ ->
                    if (error is ResponseException) {
                        throw NetworkError(error.response.status.value)
                    }
                }
            }
        }
        val manager = newManager()

        fun newManager(): TokenManager =
            TokenManagerImpl(
                settings,
                object : LogoutErrorAlerter {
                    override fun logout() {
                        logoutCount++
                    }
                },
                object : DispatcherProviderInterface {
                    override val Default = dispatcher
                    override val IO = dispatcher
                    override val Unconfined = dispatcher
                    override val Main = Dispatchers.Main
                },
                object : NetworkManagerFactory {
                    override fun build(
                        baseUrl: String,
                        needAuthorization: Boolean,
                    ): NetworkManager =
                        NetworkManager(client, baseUrl)
                },
                NetworkConfig("http://localhost", 80), appScope, Json,
            )

        fun success(): Reply =
            Reply(
                HttpStatusCode.OK,
                Json.encodeToString(
                    RefreshResponse(
                        "me",
                        newTokens.accessToken.value,
                        newTokens.refreshToken.value,
                    ),
                ),
            )
    }

    private data class Reply(val status: HttpStatusCode, val body: String)

    private class MemorySettings : EncryptedSettings {
        private val values = mutableMapOf<String, Any?>()
        override fun putString(key: String, value: String?) {
            values[key] = value
        }

        override fun getString(key: String): String? = values[key] as? String
        override fun putLong(key: String, value: Long?) {
            values[key] = value
        }

        override fun getLong(key: String): Long? = values[key] as? Long
        override fun putDouble(key: String, value: Double?) {
            values[key] = value
        }

        override fun getDouble(key: String): Double? = values[key] as? Double
        override fun putBoolean(key: String, value: Boolean?) {
            values[key] = value
        }

        override fun getBoolean(key: String): Boolean? = values[key] as? Boolean
    }

    private companion object {
        val oldTokens = Tokens(AccessToken("old-access"), RefreshToken("old-refresh"))
        val newTokens = Tokens(AccessToken("new-access"), RefreshToken("new-refresh"))
    }
}
