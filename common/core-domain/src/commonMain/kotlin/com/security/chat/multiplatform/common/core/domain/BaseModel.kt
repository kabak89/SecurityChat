package com.security.chat.multiplatform.common.core.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.kode.remo.ReactiveModel as ReMo

public open class BaseModel(
    dispatcher: CoroutineDispatcher,
    coroutineScope: CoroutineScope,
    errorMapper: ((Throwable) -> Throwable)? = null,
) : ScopedModel, ReMo(
    dispatcher = dispatcher,
    errorMapper = errorMapper ?: { error ->
        println(error)
        error
    },
) {

    init {
        println("ewqeqweqw ${this@BaseModel::class.simpleName} init")
        start(parentScope = coroutineScope)
    }

    override fun onPostStart() {
        uncaughtExceptions
            .onEach {
                println(it)
                println("Uncaught exception in ${this@BaseModel::class.simpleName}")
            }
            .launchIn(scope)
    }

}