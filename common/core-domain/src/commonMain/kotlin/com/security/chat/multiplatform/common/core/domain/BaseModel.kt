package com.security.chat.multiplatform.common.core.domain

import com.security.chat.multiplatform.common.log.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.kode.remo.ReactiveModel as ReMo

public open class BaseModel(
    dispatcher: CoroutineDispatcher,
    errorMapper: ((Throwable) -> Throwable)? = null,
) : ScopedModel, ReMo(
    dispatcher = dispatcher,
    errorMapper = errorMapper ?: { error ->
        Log.e(error)
        error
    },
) {

    init {
        Log.d { "${this@BaseModel::class.qualifiedName} init" }
    }

    override fun onPostStart() {
        uncaughtExceptions
            .onEach {
                Log.e(it, "Uncaught exception in ${this@BaseModel::class.simpleName}")
            }
            .launchIn(scope)
    }

}