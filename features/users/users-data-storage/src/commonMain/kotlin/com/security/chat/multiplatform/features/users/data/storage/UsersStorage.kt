package com.security.chat.multiplatform.features.users.data.storage

import com.security.chat.multiplatform.common.core.db.DatabaseCreator
import com.security.chat.multiplatform.common.core.db.SecuredDatabaseDriverFactory
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import kotlinx.coroutines.withContext

public interface UsersStorage {
    public suspend fun getPublicKey(userId: String): String?
    public suspend fun setPublicKey(userId: String, publicKey: String)
}

internal class UsersStorageImpl(
    private val dispatcherProvider: DispatcherProviderInterface,
    private val driverFactory: SecuredDatabaseDriverFactory,
) : UsersStorage {

    private val dbCreator: DatabaseCreator<UsersDb> =
        DatabaseCreator(
            dispatcherProvider = dispatcherProvider,
            create = {
                UsersDb(
                    driver = driverFactory.createDriver(
                        databaseName = "users.db",
                        sqlSchema = UsersDb.Schema,
                        version = 1,
                    ),
                )
            },
        )

    override suspend fun getPublicKey(userId: String): String? {
        return withContext(dispatcherProvider.IO) {
            dbCreator.getDb().usersTableQueries.getById(userId).executeAsOneOrNull()?.publicKey
        }
    }

    override suspend fun setPublicKey(userId: String, publicKey: String) {
        withContext(dispatcherProvider.IO) {
            val usersTable = UsersTable(
                id = userId,
                publicKey = publicKey,
            )

            dbCreator.getDb().usersTableQueries.insert(usersTable)
        }
    }

}