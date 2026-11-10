package com.security.chat.multiplatform.features.mesh.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.security.chat.multiplatform.common.core.db.SecuredDatabaseDriverFactory
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.common.core.time.TimeProvider
import com.security.chat.multiplatform.features.mesh.data.storage.MeshLogStorage
import com.security.chat.multiplatform.features.mesh.data.storage.MeshLogsDb
import com.security.chat.multiplatform.features.mesh.domain.entity.MeshLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class LogManagerImplTest {

    @Test
    fun `debug messages update the observed database with their original timestamps`() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            MeshLogsDb.Schema.create(driver).await()
            val driverFactory = TestDriverFactory(driver)
            var now = Instant.fromEpochMilliseconds(100)
            val manager = LogManagerImpl(
                storage = createStorage(driverFactory),
                timeProvider = object : TimeProvider {
                    override fun now(): Instant = now
                },
                appScope = backgroundScope,
                isDebug = true,
            )
            val snapshots = mutableListOf<List<MeshLog>>()
            val collection =
                manager.getLogsFlow().onEach { snapshots += it }.launchIn(backgroundScope)
            try {
                runCurrent()
                assertTrue(snapshots.last().isEmpty())

                manager.d { "BLE scan started" }
                now = Instant.fromEpochMilliseconds(200)
                manager.e("BLE scan failed", IllegalStateException("Bluetooth off"))
                runCurrent()

                assertEquals(
                    listOf(
                        MeshLog(
                            id = 2,
                            recordedAtMillis = 200,
                            message = "BLE scan failed\nBluetooth off",
                        ),
                        MeshLog(id = 1, recordedAtMillis = 100, message = "BLE scan started"),
                    ),
                    snapshots.last(),
                )
                assertEquals(1, driverFactory.openCount)
            } finally {
                collection.cancelAndJoin()
            }
        } finally {
            driver.close()
        }
    }

    @Test
    fun `non-debug logging never opens the database`() = runTest {
        val driverFactory = TestDriverFactory(driver = null)
        val manager = LogManagerImpl(
            storage = createStorage(driverFactory),
            timeProvider = object : TimeProvider {
                override fun now(): Instant = error("Non-debug logs must not capture a timestamp")
            },
            appScope = backgroundScope,
            isDebug = false,
        )

        manager.d { "BLE scan started" }
        manager.e("BLE scan failed")
        assertTrue(manager.getLogsFlow().first().isEmpty())
        runCurrent()

        assertEquals(0, driverFactory.openCount)
    }

    private fun TestScope.createStorage(driverFactory: SecuredDatabaseDriverFactory): MeshLogStorage {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val dispatcherProvider = object : DispatcherProviderInterface {
            override val Default = dispatcher
            override val IO = dispatcher
            override val Unconfined = dispatcher
            override val Main = Dispatchers.Main
        }
        return MeshLogStorage(dispatcherProvider, driverFactory)
    }

    private class TestDriverFactory(private val driver: SqlDriver?) : SecuredDatabaseDriverFactory {

        var openCount = 0
            private set

        override fun createDriver(
            databaseName: String,
            sqlSchema: SqlSchema<QueryResult.AsyncValue<Unit>>,
            version: Int,
        ): SqlDriver {
            openCount++
            return checkNotNull(driver)
        }
    }
}
