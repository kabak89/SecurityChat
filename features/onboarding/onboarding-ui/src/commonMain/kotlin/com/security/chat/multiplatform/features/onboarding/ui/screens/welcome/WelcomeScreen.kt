package com.security.chat.multiplatform.features.onboarding.ui.screens.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.ui.Screen
import com.security.chat.multiplatform.common.core.ui.SingleEventEffect
import com.security.chat.multiplatform.common.icons.kit.DrawableRes
import com.security.chat.multiplatform.common.ui.kit.MAX_CONTENT_WIDTH_DP
import com.security.chat.multiplatform.common.ui.kit.components.ButtonContent
import com.security.chat.multiplatform.common.ui.kit.components.ButtonPrimary
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.onboarding.component.api.WelcomeComponent
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import securitychat.common.icons_kit.generated.resources.app_icon
import securitychat.common.localization.generated.resources.common_next
import securitychat.common.localization.generated.resources.onboarding_welcome_encryption
import securitychat.common.localization.generated.resources.onboarding_welcome_keys
import securitychat.common.localization.generated.resources.onboarding_welcome_privacy
import securitychat.common.localization.generated.resources.onboarding_welcome_storage
import securitychat.common.localization.generated.resources.onboarding_welcome_title

@Composable
internal fun WelcomeScreen(
    component: WelcomeComponent,
) {
    Screen(
        component = component,
        screenName = "WelcomeScreen",
    ) { _: WelcomeState, vm: WelcomeViewModel ->
        SingleEventEffect(vm.viewEvent) { event ->
            when (event) {
                WelcomeEvent.RequestNotificationPermission -> component.onContinue()
                WelcomeEvent.Finished -> component.onFinish()
            }
        }
        WelcomeScreenContent(onContinue = vm::onContinueClicked)
    }
}

@Composable
internal fun WelcomeScreenContent(
    onContinue: () -> Unit,
    scrollState: ScrollState = rememberScrollState(),
) {
    val animationModifier = Modifier
        .fillMaxWidth()
        .aspectRatio(16f / 9f)
    var viewportTop by remember { mutableFloatStateOf(0f) }
    var viewportHeight by remember { mutableIntStateOf(0) }
    var animationContentTop by remember { mutableFloatStateOf(0f) }
    var animationHeight by remember { mutableIntStateOf(0) }
    var exchangeContentTop by remember { mutableFloatStateOf(0f) }
    var exchangeHeight by remember { mutableIntStateOf(0) }
    var messageContentTop by remember { mutableFloatStateOf(0f) }
    var messageHeight by remember { mutableIntStateOf(0) }
    val animationProgress by remember(scrollState) {
        derivedStateOf {
            scrollAnimationProgress(
                animationTop = animationContentTop - scrollState.value,
                animationHeight = animationHeight,
                viewportHeight = viewportHeight,
            )
        }
    }
    val exchangeProgress by remember(scrollState) {
        derivedStateOf {
            scrollAnimationProgress(
                animationTop = exchangeContentTop - scrollState.value,
                animationHeight = exchangeHeight,
                viewportHeight = viewportHeight,
            )
        }
    }
    val messageProgress by remember(scrollState) {
        derivedStateOf {
            scrollAnimationProgress(
                animationTop = messageContentTop - scrollState.value,
                animationHeight = messageHeight,
                viewportHeight = viewportHeight,
            )
        }
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.backgroundPrimary)
            .systemBarsPadding()
            .onGloballyPositioned {
                viewportTop = it.positionInRoot().y
                viewportHeight = it.size.height
            }
            .testTag(WELCOME_VIEWPORT_TAG),
    ) {
        val minimumFinalSectionHeight = maxHeight
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = MAX_CONTENT_WIDTH_DP.dp)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(40.dp),
        ) {
            Text(
                modifier = Modifier.semantics { heading() },
                text = stringResource(StringRes.onboarding_welcome_title),
                color = AppTheme.colors.textPrimary,
                style = AppTheme.typography.title.copy(fontSize = 28.sp),
            )
            Image(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(160.dp)
                    .clip(AppTheme.shapes.roundedRectangle16),
                painter = painterResource(DrawableRes.app_icon),
                contentDescription = null,
            )
            Text(
                text = stringResource(StringRes.onboarding_welcome_keys),
                color = AppTheme.colors.textPrimary,
                style = AppTheme.typography.body,
            )
            KeyGenerationAnimation(
                progress = { animationProgress },
                modifier = animationModifier
                    .testTag(KEY_GENERATION_ANIMATION_TAG)
                    .onGloballyPositioned {
                        animationContentTop =
                            it.positionInRoot().y - viewportTop + scrollState.value
                        animationHeight = it.size.height
                    },
            )
            Text(
                text = stringResource(StringRes.onboarding_welcome_encryption),
                color = AppTheme.colors.textPrimary,
                style = AppTheme.typography.body,
            )
            KeyExchangeAnimation(
                progress = { exchangeProgress },
                modifier = animationModifier
                    .testTag(KEY_EXCHANGE_ANIMATION_TAG)
                    .onGloballyPositioned {
                        exchangeContentTop =
                            it.positionInRoot().y - viewportTop + scrollState.value
                        exchangeHeight = it.size.height
                    },
            )
            Text(
                text = stringResource(StringRes.onboarding_welcome_storage),
                color = AppTheme.colors.textPrimary,
                style = AppTheme.typography.body,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minimumFinalSectionHeight),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(40.dp)) {
                    MessageSendingAnimation(
                        progress = { messageProgress },
                        modifier = animationModifier
                            .testTag(MESSAGE_SENDING_ANIMATION_TAG)
                            .onGloballyPositioned {
                                messageContentTop =
                                    it.positionInRoot().y - viewportTop + scrollState.value
                                messageHeight = it.size.height
                            },
                    )
                    Text(
                        text = stringResource(StringRes.onboarding_welcome_privacy),
                        color = AppTheme.colors.textPrimary,
                        style = AppTheme.typography.body,
                    )
                }
                ButtonPrimary(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp)
                        .testTag(WELCOME_CONTINUE_BUTTON_TAG),
                    content = ButtonContent.Text(
                        text = stringResource(StringRes.common_next),
                    ),
                    onClicked = onContinue,
                )
            }
        }
    }
}

internal const val KEY_GENERATION_ANIMATION_TAG: String = "key_generation_animation"
internal const val KEY_EXCHANGE_ANIMATION_TAG: String = "key_exchange_animation"
internal const val MESSAGE_SENDING_ANIMATION_TAG: String = "message_sending_animation"
internal const val WELCOME_VIEWPORT_TAG: String = "welcome_viewport"
internal const val WELCOME_CONTINUE_BUTTON_TAG: String = "welcome_continue_button"

@Preview
@Composable
internal fun WelcomeScreenPreview() {
    AppTheme {
        WelcomeScreenContent(onContinue = {})
    }
}

@Preview
@Composable
internal fun WelcomeScreenEndPreview() {
    AppTheme {
        WelcomeScreenContent(
            onContinue = {},
            scrollState = rememberScrollState(initial = Int.MAX_VALUE),
        )
    }
}
