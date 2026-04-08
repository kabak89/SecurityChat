package com.security.chat.multiplatform.features.chats.ui.screens.addchat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
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
import com.security.chat.multiplatform.common.core.ui.SingleEventEffect
import com.security.chat.multiplatform.features.chats.component.api.AddChatComponent
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import securitychat.common.icons_kit.generated.resources.Res
import securitychat.common.icons_kit.generated.resources.ic_back

@Composable
public fun AddChatScreen(
    component: AddChatComponent,
) {
    try {
        if (component.getDiScope().closed) return
    } catch (e: Exception) {
        println(e)
        return
    }

    val vm: AddChatViewModel = koinViewModel(
        viewModelStoreOwner = component,
        scope = component.getDiScope(),
    )

    val state = vm.viewState.collectAsStateWithLifecycle().value

    AddChatContent(
        modifier = Modifier
            .fillMaxSize(),
        state = state,
        events = vm.viewEvent,
        onBackClicked = component::onBackClicked,
        onUsernameTextChanged = vm::onUsernameTextChanged,
        onFindClicked = vm::onFindClicked,
        onChatCreated = component::onChatCreated,
    )
}

@Composable
private fun AddChatContent(
    modifier: Modifier = Modifier,
    state: AddChatState,
    events: Flow<AddChatEvent>,
    onBackClicked: () -> Unit,
    onUsernameTextChanged: (String) -> Unit,
    onFindClicked: () -> Unit,
    onChatCreated: (id: String) -> Unit,
) {
    SingleEventEffect(
        sideEffectFlow = events,
        collector = { event ->
            when (event) {
                is AddChatEvent.ChatCreated -> onChatCreated(event.id)
            }
        },
    )

    Column(
        modifier = modifier
            .background(Color.White)
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding(),
    ) {
        ToolbarComponent(
            modifier = Modifier
                .fillMaxWidth(),
            onBackClicked = onBackClicked,
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            value = state.username,
            onValueChange = onUsernameTextChanged,
            placeholder = {
                Text("Username")
            },
            enabled = !state.isLoading,
        )
        Spacer(Modifier.weight(1f))
        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(alignment = Alignment.CenterHorizontally),
            )
        } else {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onClick = onFindClicked,
                content = {
                    Text("Find")
                },
            )
        }
        Spacer(Modifier.height(16.dp))
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
            text = "Find user",
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.weight(0.2f))
    }
}