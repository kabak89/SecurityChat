package com.security.chat.multiplatform.features.users.data.storage

import com.security.chat.multiplatform.common.core.db.DatabaseCreator
import com.security.chat.multiplatform.common.core.db.SecuredDatabaseDriverFactory
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.users.data.storage.entity.UserSM
import com.security.chat.multiplatform.features.users.data.storage.mapper.toSM
import com.security.chat.multiplatform.features.users.data.storage.mapper.toTable
import kotlinx.coroutines.withContext

public interface UsersStorage {
    public suspend fun saveUser(user: UserSM)
    public suspend fun getUser(id: String): UserSM?
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

    override suspend fun saveUser(user: UserSM) {
        withContext(dispatcherProvider.IO) {
            dbCreator.getDb().usersTableQueries.insert(user.toTable())
        }
    }

    override suspend fun getUser(id: String): UserSM? {
        return withContext(dispatcherProvider.IO) {
            dbCreator.getDb().usersTableQueries.getById(id).executeAsOneOrNull()?.toSM()
        }
    }
}