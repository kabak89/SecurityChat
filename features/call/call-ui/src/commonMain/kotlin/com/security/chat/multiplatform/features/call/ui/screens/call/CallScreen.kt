package com.security.chat.multiplatform.features.call.ui.screens.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.ui.Screen
import com.security.chat.multiplatform.common.icons.kit.DrawableRes
import com.security.chat.multiplatform.common.ui.kit.MAX_CONTENT_WIDTH_DP
import com.security.chat.multiplatform.common.ui.kit.components.ButtonContent
import com.security.chat.multiplatform.common.ui.kit.components.ButtonPrimary
import com.security.chat.multiplatform.common.ui.kit.components.CenterContent
import com.security.chat.multiplatform.common.ui.kit.components.SideContent
import com.security.chat.multiplatform.common.ui.kit.components.ToolbarComponent
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.call.component.api.CallMainComponent
import com.security.chat.multiplatform.features.call.ui.screens.call.entity.CallParticipantUM
import com.security.chat.multiplatform.features.call.ui.screens.call.entity.CallStatusUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource
import securitychat.common.icons_kit.generated.resources.ic_back
import securitychat.common.localization.generated.resources.call_end
import securitychat.common.localization.generated.resources.call_no_online_participants
import securitychat.common.localization.generated.resources.call_online_participants
import securitychat.common.localization.generated.resources.call_start
import securitychat.common.localization.generated.resources.call_status_active
import securitychat.common.localization.generated.resources.call_status_connecting
import securitychat.common.localization.generated.resources.call_title

@Composable
internal fun CallScreen(
    component: CallMainComponent,
) {
    Screen(
        component = component,
        screenName = "CallScreen",
    ) { state: CallState, vm: CallViewModel ->
        CallContent(
            state = state,
            onBackClicked = component::onBackClicked,
            onCallButtonClicked = vm::onCallButtonClicked,
        )
    }
}

@Composable
private fun CallContent(
    state: CallState,
    onBackClicked: () -> Unit,
    onCallButtonClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.backgroundPrimary)
            .systemBarsPadding(),
    ) {
        ToolbarComponent(
            modifier = Modifier.fillMaxWidth(),
            startContent = SideContent.Button(
                icon = DrawableRes.ic_back,
                onClicked = onBackClicked,
            ),
            centerContent = CenterContent.Title(
                text = stringResource(StringRes.call_title),
            ),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = MAX_CONTENT_WIDTH_DP.dp)
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(StringRes.call_online_participants),
                color = AppTheme.colors.textPrimary,
                style = AppTheme.typography.title2,
            )
            Spacer(Modifier.height(16.dp))
            if (state.onlineParticipants.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(StringRes.call_no_online_participants),
                        color = AppTheme.colors.textSuppressed,
                        style = AppTheme.typography.body,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.onlineParticipants, key = { it.id }) { participant ->
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            text = participant.name,
                            color = AppTheme.colors.textPrimary,
                            style = AppTheme.typography.body,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                modifier = Modifier.fillMaxWidth().heightIn(min = 24.dp),
                text = when (state.callStatus) {
                    CallStatusUM.Idle -> ""
                    CallStatusUM.Connecting -> stringResource(StringRes.call_status_connecting)
                    CallStatusUM.Active -> stringResource(StringRes.call_status_active)
                },
                color = AppTheme.colors.textSecondary,
                style = AppTheme.typography.body,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            ButtonPrimary(
                modifier = Modifier.fillMaxWidth(),
                content = ButtonContent.Text(
                    text = if (state.callStatus == CallStatusUM.Active) {
                        stringResource(StringRes.call_end)
                    } else {
                        stringResource(StringRes.call_start)
                    },
                ),
                enabled = state.isCallButtonEnabled,
                onClicked = onCallButtonClicked,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Preview
@Composable
internal fun CallScreenPreview() {
    CallPreview(
        state = CallState(
            onlineParticipants = previewParticipants(),
            callStatus = CallStatusUM.Idle,
        ),
    )
}

@Preview
@Composable
internal fun CallScreenEmptyPreview() {
    CallPreview(
        state = CallState(
            onlineParticipants = persistentListOf(),
            callStatus = CallStatusUM.Idle,
        ),
    )
}

@Preview
@Composable
internal fun CallScreenConnectingPreview() {
    CallPreview(
        state = CallState(
            onlineParticipants = previewParticipants(),
            callStatus = CallStatusUM.Connecting,
        ),
    )
}

@Composable
private fun CallPreview(state: CallState) {
    AppTheme {
        CallContent(
            state = state,
            onBackClicked = {},
            onCallButtonClicked = {},
        )
    }
}

private fun previewParticipants(): ImmutableList<CallParticipantUM> =
    persistentListOf(
        CallParticipantUM(id = "1", name = "alex"),
        CallParticipantUM(id = "2", name = "maria"),
    )
