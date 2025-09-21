package com.security.chat.multiplatform.features.chat.ui.screens.personalchat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.security.chat.multiplatform.features.chat.component.PersonalChatComponent
import kotlinx.coroutines.flow.Flow
import org.koin.compose.viewmodel.koinViewModel

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
    )
}

@Composable
private fun PersonalChatContent(
    modifier: Modifier,
    state: PersonalChatState,
    events: Flow<PersonalChatEvent>,
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
        )
    }
}

@Composable
private fun ToolbarComponent(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.weight(0.2f))
        Text(
            modifier = Modifier
                .weight(0.6f),
            text = "Chat",
            textAlign = TextAlign.Center,
        )
        Row(
            modifier = Modifier
                .weight(0.2f),
            horizontalArrangement = Arrangement.End,
        ) {
//            IconButton(
//                modifier = Modifier
//                    .size(48.dp),
//                onClick = onAddClicked,
//                content = {
//                    Icon(
//                        imageVector = vectorResource(Res.drawable.ic_add),
//                        tint = Color.Black,
//                        contentDescription = null,
//                    )
//                },
//            )
        }
    }
}