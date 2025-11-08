package com.security.chat.multiplatform.features.chat.ui.screens.personalchat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.chat.component.PersonalChatComponent
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import securitychat.common.icons_kit.generated.resources.Res
import securitychat.common.icons_kit.generated.resources.ic_back
import securitychat.common.icons_kit.generated.resources.ic_send

@Composable
internal fun PersonalChatScreen(
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
        parameters = { parametersOf(component) },
    )

    val state = vm.viewState.collectAsStateWithLifecycle().value

    PersonalChatContent(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
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
            .background(AppTheme.colors.backgroundPrimary)
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
        val textSecondaryColor = AppTheme.colors.textSecondary
        val placeholderColor = remember { textSecondaryColor.copy(alpha = 0.3f) }

        TextField(
            modifier = Modifier
                .weight(1f),
            value = message,
            maxLines = 3,
            onValueChange = onMessageEdited,
            placeholder = {
                Text("Message")
            },
            colors = TextFieldDefaults.colors().copy(
                focusedContainerColor = AppTheme.colors.backgroundSecondary,
                unfocusedContainerColor = AppTheme.colors.backgroundSecondary,
                focusedTextColor = AppTheme.colors.textSecondary,
                unfocusedTextColor = AppTheme.colors.textSecondary,
                cursorColor = AppTheme.colors.element,
                focusedPlaceholderColor = placeholderColor,
                unfocusedPlaceholderColor = placeholderColor,
                focusedIndicatorColor = AppTheme.colors.element,
                unfocusedIndicatorColor = AppTheme.colors.element,
            ),
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
                    color = AppTheme.colors.element,
                )
            } else {
                IconButton(
                    modifier = Modifier
                        .size(48.dp),
                    onClick = onSendMessageClicked,
                    content = {
                        Icon(
                            modifier = Modifier
                                .size(32.dp),
                            imageVector = vectorResource(Res.drawable.ic_send),
                            tint = AppTheme.colors.element,
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
                        tint = AppTheme.colors.element,
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
            color = AppTheme.colors.textPrimary,
        )
        Spacer(
            modifier = Modifier
                .weight(0.2f),
        )
    }
}