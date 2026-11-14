package com.security.chat.multiplatform.common.background.work

import com.security.chat.multiplatform.common.background.work.BackgroundWorkManager.Type
import com.security.chat.multiplatform.common.core.component.SCOPE_ID_APP
import com.security.chat.multiplatform.common.core.network.ConnectivityObserver
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.koin.core.KoinApplication
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds

class BackgroundWorkManagerTest {

    private lateinit var app: KoinApplication

    @AfterTest
    fun closeKoin() {
        app.close()
    }

    @Test
    fun `one time work waits for the network and runs once with the original input`() = runTest {
        val online = MutableStateFlow(false)
        val received = mutableListOf<Map<String, String>>()
        val manager = manager(onlineState = online) { TestWorker { received += it } }
        val conditions = BackgroundWorkConditions(
            schedule = BackgroundWorkSchedule.OneShot,
            requiresInternet = true,
        )

        val input = mutableMapOf("chatId" to "chat-1", "messages" to "first")
        manager.enqueue(
            key = "local",
            worker = TestWorker::class,
            conditions = conditions.copy(requiresInternet = false),
            type = Type.Silent,
            policy = BackgroundWorkPolicy.KEEP,
            priority = BackgroundWorkPriority.Normal,
            input = mapOf("messages" to "local"),
        )
        manager.enqueue(
            key = "chat-1",
            worker = TestWorker::class,
            conditions = conditions,
            type = Type.Silent,
            policy = BackgroundWorkPolicy.KEEP,
            priority = BackgroundWorkPriority.Normal,
            input = input,
        )
        input["messages"] = "changed after enqueue"
        manager.enqueue(
            key = "chat-1",
            worker = TestWorker::class,
            conditions = conditions.copy(requiresInternet = false),
            type = Type.Silent,
            policy = BackgroundWorkPolicy.KEEP,
            priority = BackgroundWorkPriority.Normal,
            input = mapOf("messages" to "second"),
        )
        runCurrent()
        val local = listOf(mapOf("messages" to "local"))
        assertEquals(local, received)

        online.value = true
        runCurrent()
        val expected = local + mapOf("chatId" to "chat-1", "messages" to "first")
        assertEquals(expected, received)

        advanceTimeBy(30.seconds)
        assertEquals(expected, received)
    }

    @Test
    fun `periodic work creates a fresh worker for each run`() = runTest {
        var instances = 0
        val runs = mutableListOf<Pair<Int, Map<String, String>>>()
        val manager = manager {
            val instance = ++instances
            TestWorker { runs += instance to it }
        }
        val conditions = BackgroundWorkConditions(
            schedule = BackgroundWorkSchedule.Repeatable(interval = 10.seconds),
            requiresInternet = false,
        )

        val input = mapOf("source" to "periodic")
        assertFailsWith<IllegalArgumentException> {
            manager.enqueue(
                key = "mesh_sync",
                worker = TestWorker::class,
                conditions = conditions,
                type = Type.Silent,
                policy = BackgroundWorkPolicy.KEEP,
                priority = BackgroundWorkPriority.Expedited(legacyForeground = Type.Silent),
                input = input,
            )
        }
        manager.enqueue(
            key = "mesh_sync",
            worker = TestWorker::class,
            conditions = conditions,
            type = Type.Foreground(notificationText = "Running periodic work"),
            policy = BackgroundWorkPolicy.KEEP,
            priority = BackgroundWorkPriority.Normal,
            input = input,
        )
        advanceTimeBy(20.seconds)
        runCurrent()
        val expected = listOf(1 to input, 2 to input, 3 to input)
        assertEquals(expected, runs)

        manager.cancel("mesh_sync")
        advanceTimeBy(20.seconds)
        assertEquals(expected, runs)
    }

    @Test
    fun `cancel before the first run allows the worker to be enqueued again`() = runTest {
        val received = mutableListOf<Map<String, String>>()
        val manager = manager {
            TestWorker {
                received += it
                awaitCancellation()
            }
        }
        val conditions = BackgroundWorkConditions(
            schedule = BackgroundWorkSchedule.OneShot,
            requiresInternet = false,
        )

        manager.enqueue(
            key = "one-shot",
            worker = TestWorker::class,
            conditions = conditions,
            type = Type.Silent,
            policy = BackgroundWorkPolicy.KEEP,
            priority = BackgroundWorkPriority.Normal,
        )
        manager.cancel("one-shot")
        assertEquals(emptyList(), received)

        manager.enqueue(
            key = "one-shot",
            worker = TestWorker::class,
            conditions = conditions,
            type = Type.Silent,
            policy = BackgroundWorkPolicy.KEEP,
            priority = BackgroundWorkPriority.Normal,
        )
        runCurrent()
        assertEquals(listOf(emptyMap()), received)
        manager.cancel("one-shot")
    }

    @Test
    fun `one shot work runs in order per key and independently across keys`() = runTest {
        val firstMayFinish = CompletableDeferred<Unit>()
        val started = mutableListOf<String>()
        val manager = manager {
            TestWorker { input ->
                val message = input.getValue("message")
                started += message
                if (message == "first") firstMayFinish.await()
            }
        }
        val priority = BackgroundWorkPriority.Expedited(legacyForeground = Type.Silent)

        val first = manager.enqueueOneShot(
            key = "chat-1",
            worker = TestWorker::class,
            requiresInternet = false,
            type = Type.Silent,
            policy = BackgroundWorkPolicy.APPEND_OR_REPLACE,
            priority = priority,
            input = mapOf("message" to "first"),
        )
        val second = manager.enqueueOneShot(
            key = "chat-1",
            worker = TestWorker::class,
            requiresInternet = false,
            type = Type.Silent,
            policy = BackgroundWorkPolicy.APPEND_OR_REPLACE,
            priority = priority,
            input = mapOf("message" to "second"),
        )
        val other = manager.enqueueOneShot(
            key = "chat-2",
            worker = TestWorker::class,
            requiresInternet = false,
            type = Type.Silent,
            policy = BackgroundWorkPolicy.APPEND_OR_REPLACE,
            priority = priority,
            input = mapOf("message" to "other chat"),
        )
        runCurrent()
        first.await()
        second.await()
        other.await()
        assertEquals(listOf("first", "other chat"), started)

        firstMayFinish.complete(Unit)
        runCurrent()
        assertEquals(listOf("first", "other chat", "second"), started)
    }

    @Test
    fun `new work starts a fresh queue after a failure`() = runTest {
        val started = mutableListOf<String>()
        val manager = manager {
            TestWorker { input ->
                val message = input.getValue("message")
                started += message
                check(message != "first") { "The first run failed" }
            }
        }
        val conditions = BackgroundWorkConditions(
            schedule = BackgroundWorkSchedule.OneShot,
            requiresInternet = false,
        )

        for (message in listOf("first", "queued")) {
            manager.enqueue(
                key = "chat-1",
                worker = TestWorker::class,
                conditions = conditions,
                type = Type.Silent,
                policy = BackgroundWorkPolicy.APPEND_OR_REPLACE,
                priority = BackgroundWorkPriority.Normal,
                input = mapOf("message" to message),
            )
        }
        runCurrent()
        assertEquals(listOf("first"), started)

        manager.enqueue(
            key = "chat-1",
            worker = TestWorker::class,
            conditions = conditions,
            type = Type.Silent,
            policy = BackgroundWorkPolicy.APPEND_OR_REPLACE,
            priority = BackgroundWorkPriority.Normal,
            input = mapOf("message" to "new"),
        )
        runCurrent()
        assertEquals(listOf("first", "new"), started)
    }

    @Test
    fun `keep pending combines offline requests into one run`() = runTest {
        val online = MutableStateFlow(false)
        val started = mutableListOf<String>()
        val manager = manager(onlineState = online) {
            TestWorker { started += it.getValue("message") }
        }

        manager.enqueueKeepingPending(message = "first", requiresInternet = true)
        runCurrent()
        repeat(10) {
            manager.enqueueKeepingPending(message = "extra", requiresInternet = true)
        }
        runCurrent()
        assertEquals(emptyList(), started)

        online.value = true
        runCurrent()

        assertEquals(listOf("first"), started)
    }

    @Test
    fun `keep pending adds just one successor per active run`() = runTest {
        val firstMayFinish = CompletableDeferred<Unit>()
        val secondMayFinish = CompletableDeferred<Unit>()
        val started = mutableListOf<String>()
        val manager = manager {
            TestWorker {
                val message = it.getValue("message")
                started += message
                when (message) {
                    "first" -> firstMayFinish.await()
                    "second" -> secondMayFinish.await()
                }
            }
        }

        manager.enqueueKeepingPending(message = "first")
        runCurrent()
        manager.enqueueKeepingPending(message = "second")
        repeat(10) { manager.enqueueKeepingPending(message = "extra") }
        runCurrent()
        assertEquals(listOf("first"), started)

        firstMayFinish.complete(Unit)
        runCurrent()
        assertEquals(listOf("first", "second"), started)

        manager.enqueueKeepingPending(message = "third")
        repeat(10) { manager.enqueueKeepingPending(message = "extra") }
        secondMayFinish.complete(Unit)
        runCurrent()
        assertEquals(listOf("first", "second", "third"), started)
    }

    private suspend fun BackgroundWorkManager.enqueueKeepingPending(
        message: String,
        requiresInternet: Boolean = false,
    ) {
        enqueueOneShot(
            key = "chat",
            worker = TestWorker::class,
            requiresInternet = requiresInternet,
            type = Type.Silent,
            policy = BackgroundWorkPolicy.KEEP_PENDING,
            priority = BackgroundWorkPriority.Normal,
            input = mapOf("message" to message),
        ).await()
    }

    private fun TestScope.manager(
        onlineState: StateFlow<Boolean> = MutableStateFlow(true),
        create: () -> TestWorker,
    ): BackgroundWorkManager {
        app = koinApplication {
            modules(
                backgroundWorkModule,
                module {
                    single<CoroutineScope>(named(SCOPE_ID_APP)) { backgroundScope }
                    single<ConnectivityObserver> {
                        object : ConnectivityObserver {
                            override val isOnline = onlineState
                        }
                    }
                    backgroundWorker(
                        id = "test-worker",
                        worker = TestWorker::class,
                        create = { create() },
                    )
                },
            )
        }
        return app.koin.get()
    }

    private class TestWorker(
        private val task: suspend (Map<String, String>) -> Unit,
    ) : BackgroundWorker {
        override suspend fun run(input: Map<String, String>) = task(input)
    }
}
