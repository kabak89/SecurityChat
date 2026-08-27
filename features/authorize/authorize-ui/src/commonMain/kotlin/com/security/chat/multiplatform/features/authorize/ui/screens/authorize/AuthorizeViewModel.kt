package com.security.chat.multiplatform.features.authorize.ui.screens.authorize

import androidx.lifecycle.viewModelScope
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.common.core.ui.BaseViewModel
import com.security.chat.multiplatform.features.authorize.ui.screens.authorize.entity.Connection
import com.security.chat.multiplatform.features.authorize.ui.screens.authorize.entity.Node
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

internal class AuthorizeViewModel(
    private val dispatcherProvider: DispatcherProviderInterface,
) : BaseViewModel<AuthorizeState, AuthorizeEvent>() {

    override fun onPostStart() {
        super.onPostStart()

        flow {
            while (true) {
                delay(1.seconds)
                val currentTime =
                    currentViewState.currentTime + CHANGES_STEP.inWholeMilliseconds

                updateState { oldState ->
                    oldState.copy(
                        currentTime = currentTime,
                    )
                }

                emit(Unit)
            }
        }
            .collectWhenViewActive()
            .onEach {
                if (isNeeded(CHANCE_TO_CREATE_NODE)) {
                    createNewNode()
                }

                updateConnections()

                if (isNeeded(CHANCE_TO_CREATE_CONNECTION)) {
                    createNewConnection()
                }
            }
            .flowOn(dispatcherProvider.Default)
            .launchIn(viewModelScope)
    }

    override fun createInitialState(): AuthorizeState {
        return AuthorizeState(
            nodes = persistentListOf(),
            currentTime = 0,
            connections = persistentListOf(),
        )
    }

    private fun updateConnections() {
        val currentConnections = currentViewState.connections

        val newConnections = currentConnections
            .map { connection ->
                val newLifeTime = connection.lifeTime + CHANGES_STEP.inWholeMilliseconds

                val messageProgress = when {
                    newLifeTime > MIN_LIFETIME_TO_MESSAGE.inWholeMilliseconds
                            && connection.messageProgress == null
                        -> {
                        val newDirection = Connection.Direction.entries.random()
                        val newCurrentMessageProgress = 0f

                        Connection.MessageProgress(
                            direction = newDirection,
                            progress = newCurrentMessageProgress,
                        )
                    }

                    newLifeTime > MIN_LIFETIME_TO_MESSAGE.inWholeMilliseconds
                            && connection.messageProgress != null
                            && connection.messageProgress.progress < 1f
                        -> {
                        val newProgress =
                            (connection.messageProgress.progress + MESSAGE_PROGRESS_PER_STEP)
                                .coerceAtMost(1f)

                        Connection.MessageProgress(
                            direction = connection.messageProgress.direction,
                            progress = newProgress,
                        )
                    }

                    else -> {
                        null
                    }
                }

                connection.copy(
                    lifeTime = newLifeTime,
                    messageProgress = messageProgress,
                )
            }
            .toPersistentList()

        updateState { it.copy(connections = newConnections) }
    }

    private fun createNewConnection() {
        if (currentViewState.nodes.size < 2) return
        val start = currentViewState.nodes.random()
        val end = (currentViewState.nodes - start).random()
        var alreadyExists = false
        val currentConnections = currentViewState.connections

        currentConnections.forEach { connection ->
            if ((connection.start == start && connection.end == end) ||
                (connection.end == start && connection.start == end)
            ) {
                alreadyExists = true
                return@forEach
            }
        }

        if (alreadyExists) return

        val newConnection = Connection(
            id = Uuid.random().toString(),
            start = start,
            end = end,
            lifeTime = 0,
            messageProgress = null,
        )

        val newConnections = currentConnections.adding(newConnection)
        updateState { it.copy(connections = newConnections) }
    }

    private fun createNewNode() {
        val currentNodes = currentViewState.nodes
        if (currentNodes.size > MAX_NODES_COUNT) return

        val newNode = Node(
            xRelative = Random.nextFloat(),
            yRelative = Random.nextFloat(),
        )

        val newNodes = currentNodes.adding(newNode)
        updateState { it.copy(nodes = newNodes) }
    }
}

private fun isNeeded(chance: Float): Boolean {
    check(chance > 0 && chance < 1f)
    return Random.nextFloat() <= chance
}

internal val CHANGES_STEP = 1.seconds

private const val CHANCE_TO_CREATE_NODE = 0.7f
private const val CHANCE_TO_CREATE_CONNECTION = 0.4f

private val MIN_LIFETIME_TO_MESSAGE = 3.seconds
private const val MESSAGE_PROGRESS_PER_STEP = 0.2f

private const val MAX_NODES_COUNT = 50
