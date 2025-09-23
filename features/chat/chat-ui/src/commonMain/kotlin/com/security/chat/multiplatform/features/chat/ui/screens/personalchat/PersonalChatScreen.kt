package com.security.chat.multiplatform.features.chat.ui.screens.personalchat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
    )
}

@Composable
private fun PersonalChatContent(
    modifier: Modifier,
    state: PersonalChatState,
    events: Flow<PersonalChatEvent>,
    onBackClicked: () -> Unit,
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