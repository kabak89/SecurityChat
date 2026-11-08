package com.security.chat.multiplatform.features.chat.ui.screens.groupchat

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.security.chat.multiplatform.common.core.ui.entity.UiLceState
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.chat.ui.screens.groupchat.entity.ChatInfoUM
import com.security.chat.multiplatform.features.chat.ui.screens.groupchat.entity.MessageQuoteUM
import com.security.chat.multiplatform.features.chat.ui.screens.groupchat.entity.MessageStatusUM
import com.security.chat.multiplatform.features.chat.ui.screens.groupchat.entity.MessageUM
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class GroupChatReplyInteractionTest {
    @Test
    fun `tapping a quote loads older history and scrolls to the original`() {
        assertQuoteJumps(fromComposer = false)
    }

    @Test
    fun `tapping a quote in the reply panel scrolls to the original`() {
        assertQuoteJumps(fromComposer = true)
    }

    private fun assertQuoteJumps(fromComposer: Boolean) {
        runSkikoComposeUiTest(size = Size(width = 412f, height = 900f)) {
            val quote = MessageQuoteUM.Text(messageId = "original", text = "Quoted text")
            val answer = MessageUM.Outgoing.Text(
                id = "answer",
                text = "Reply",
                datetimeText = "12:10",
                status = MessageStatusUM.Sent,
                quote = quote.takeUnless { fromComposer },
            )
            val pages = MutableStateFlow(PagingData.from<MessageUM>(listOf(answer)))
            var requestedId: String? = null
            setContent {
                AppTheme {
                    GroupChatContent(
                        modifier = Modifier.fillMaxSize(),
                        state = GroupChatState(
                            message = "",
                            syncState = UiLceState.NotStarted,
                            alertDialogDescriptor = null,
                            fullscreenImage = null,
                            chatInfo = ChatInfoUM.empty(),
                            messageMenuId = null,
                            deletingMessageId = null,
                            replyQuote = quote.takeIf { fromComposer },
                        ),
                        messages = pages.collectAsLazyPagingItems(),
                        events = emptyFlow(),
                        onBackClicked = {},
                        onMessageEdited = {},
                        onImagePicked = {},
                        onSendMessageClicked = {},
                        onSyncClicked = {},
                        onMoreClicked = {},
                        onCallClicked = {},
                        onImageClicked = {},
                        onFullscreenImageDismissed = {},
                        onLoadMessagesAround = { id ->
                            requestedId = id
                            pages.value = PagingData.from(
                                List(25) { index ->
                                    answer.copy(
                                        id = "old-$index",
                                        text = "History $index",
                                        quote = null,
                                    )
                                } + answer.copy(
                                    id = "original",
                                    text = "Original row",
                                    quote = null,
                                ),
                            )
                        },
                    )
                }
            }

            onNodeWithText("Quoted text", useUnmergedTree = true).performTouchInput { click() }
            onNodeWithText("Original row", useUnmergedTree = true).assertIsDisplayed()
            runOnIdle { assertEquals("original", requestedId) }
        }
    }
}
