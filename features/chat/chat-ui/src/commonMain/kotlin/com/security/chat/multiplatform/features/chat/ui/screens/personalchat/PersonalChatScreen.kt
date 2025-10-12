package com.security.chat.multiplatform.features.chat.ui.screens.personalchat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.security.chat.multiplatform.features.chat.component.PersonalChatComponent
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import securitychat.common.icons_kit.generated.resources.Res
import securitychat.common.icons_kit.generated.resources.ic_back
import securitychat.common.icons_kit.generated.resources.ic_send

@Composable
public fun PersonalChatScreen(
    component: PersonalChatComponent,
) {
    try {
        if (component.getDiScope().closed) return
    } catch (e: Exception) {
        println(e)
        return
    }

    val vm: PersonalChatViewModel = koinViewModel(
        viewModelStoreOwner = component,
        scope = component.getDiScope(),
    )

    val state = vm.viewState.collectAsStateWithLifecycle().value

    PersonalChatContent(
        modifier = Modifier
            .fillMaxSize(),
        state = state,
        events = vm.viewEvent,
        onBackClicked = component::onExitClicked,
        onMessageEdited = vm::onMessageEdited,
        onSendMessageClicked = vm::onSendMessageClicked,
    )
}

@Composable
private fun PersonalChatContent(
    modifier: Modifier,
    state: PersonalChatState,
    events: Flow<PersonalChatEvent>,
    onBackClicked: () -> Unit,
    onMessageEdited: (String) -> Unit,
    onSendMessageClicked: () -> Unit,
) {
    Column(
        modifier = modifier
            .background(Color.White)
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        ToolbarComponent(
            modifier = Modifier
                .fillMaxWidth(),
            onBackClicked = onBackClicked,
        )
        LazyColumn(
            modifier = Modifier
                .weight(1f),
            content = {
                //TODO
            },
        )
        EditMessageComponent(
            modifier = Modifier
                .fillMaxWidth(),
            message = state.message,
            sendingInProgress = state.sendingMessageInProgress,
            onMessageEdited = onMessageEdited,
            onSendMessageClicked = onSendMessageClicked,
        )
    }
}

@Composable
private fun EditMessageComponent(
    modifier: Modifier = Modifier,
    message: String,
    sendingInProgress: Boolean,
    onMessageEdited: (String) -> Unit,
    onSendMessageClicked: () -> Unit,
) {
    Row(
        modifier = modifier,
    ) {
        TextField(
            modifier = Modifier
                .weight(1f),
            value = message,
            maxLines = 3,
            onValueChange = onMessageEdited,
            placeholder = {
                Text("Message")
            },
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .align(alignment = Alignment.CenterVertically),
        ) {
            if (sendingInProgress) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(48.dp),
                )
            } else {
                IconButton(
                    modifier = Modifier
                        .size(48.dp),
                    onClick = onSendMessageClicked,
                    content = {
                        Icon(
                            imageVector = vectorResource(Res.drawable.ic_send),
                            tint = Color.Black,
                            contentDescription = null,
                        )
                    },
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
    }
}

@Composable
private fun ToolbarComponent(
    modifier: Modifier = Modifier,
    onBackClicked: () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(0.2f),
            horizontalArrangement = Arrangement.Start,
        ) {
            IconButton(
                modifier = Modifier
                    .size(48.dp),
                onClick = onBackClicked,
                content = {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_back),
                        tint = Color.Black,
                        contentDescription = null,
                    )
                },
            )
        }
        Text(
            modifier = Modifier
                .weight(0.6f),
            text = "Chat",
            textAlign = TextAlign.Center,
        )
        Spacer(
            modifier = Modifier
                .weight(0.2f),
        )
    }
}