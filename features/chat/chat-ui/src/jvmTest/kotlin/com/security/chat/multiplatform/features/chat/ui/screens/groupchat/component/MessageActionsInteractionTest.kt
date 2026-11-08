package com.security.chat.multiplatform.features.chat.ui.screens.groupchat.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.chat.ui.screens.groupchat.entity.MessageQuoteUM
import com.security.chat.multiplatform.features.chat.ui.screens.groupchat.entity.MessageStatusUM
import com.security.chat.multiplatform.features.chat.ui.screens.groupchat.entity.MessageUM
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class MessageActionsInteractionTest {
    @Test
    fun `tapping selectable outgoing text opens the message menu`() =
        runSkikoComposeUiTest(size = Size(width = 412f, height = 900f)) {
            var menuClicks = 0
            setContent {
                AppTheme {
                    MessageActionsComponent(
                        modifier = Modifier.fillMaxWidth(),
                        expanded = false,
                        enabled = true,
                        onClicked = { menuClicks++ },
                        onDismiss = {},
                        onReplyClicked = {},
                        onDeleteClicked = {},
                    ) {
                        OutgoingMessageComponent(
                            modifier = Modifier.fillMaxWidth(),
                            message = MessageUM.Outgoing.Text(
                                id = "text",
                                text = "hello",
                                datetimeText = "12:10",
                                status = MessageStatusUM.Sent,
                            ),
                        )
                    }
                }
            }
            onNodeWithText(text = "hello", useUnmergedTree = true).performTouchInput {
                click(Offset(x = width - 4f, y = height / 2f))
            }
            runOnIdle { assertEquals(1, menuClicks) }
        }


    @Test
    fun `image tap opens the photo and container tap opens the message menu`() {
        runSkikoComposeUiTest(size = Size(width = 412f, height = 900f)) {
            var imageClicks = 0
            var menuClicks = 0
            var deleteClicks = 0
            var expanded by mutableStateOf(false)
            setContent {
                AppTheme {
                    MessageActionsComponent(
                        modifier = Modifier.fillMaxWidth().testTag("message"),
                        expanded = expanded,
                        enabled = true,
                        onClicked = { menuClicks++; expanded = true },
                        onDismiss = { expanded = false },
                        onReplyClicked = {},
                        onDeleteClicked = { deleteClicks++; expanded = false },
                    ) {
                        OutgoingImageMessageComponent(
                            modifier = Modifier.fillMaxWidth(),
                            message = MessageUM.Outgoing.Image(
                                id = "image",
                                text = "image",
                                datetimeText = "12:10",
                                filePath = "",
                                status = MessageStatusUM.Sent,
                            ),
                            onClicked = { imageClicks++ },
                        )
                    }
                }
            }
            onNodeWithTag("message").performTouchInput { click(Offset(width - 40f, 40f)) }
            runOnIdle {
                assertEquals(1, imageClicks)
                assertEquals(0, menuClicks)
            }
            onNodeWithTag("message").performTouchInput { click(Offset(4f, 4f)) }
            onNodeWithTag(MESSAGE_DELETE_ACTION_TAG).performClick()
            runOnIdle {
                assertEquals(1, imageClicks)
                assertEquals(1, menuClicks)
                assertEquals(1, deleteClicks)
            }
        }
    }

    @Test
    fun `incoming message offers reply and tapping the quote jumps without opening the menu`() {
        runSkikoComposeUiTest(size = Size(width = 412f, height = 900f)) {
            var expanded by mutableStateOf(false)
            var replyClicks = 0
            var jumpedTo: String? = null
            setContent {
                AppTheme {
                    MessageActionsComponent(
                        modifier = Modifier.fillMaxWidth().testTag("message"),
                        expanded = expanded,
                        enabled = true,
                        onClicked = { expanded = true },
                        onDismiss = { expanded = false },
                        onReplyClicked = {
                            replyClicks++
                            expanded = false
                        },
                    ) {
                        IncomingMessageComponent(
                            modifier = Modifier.fillMaxWidth(),
                            message = MessageUM.Incoming.Text(
                                id = "incoming",
                                text = "An answer",
                                datetimeText = "12:10",
                                senderName = "John",
                                quote = MessageQuoteUM.Text(
                                    messageId = "original",
                                    text = "Original message",
                                ),
                            ),
                            showSenderName = false,
                            onQuoteClicked = { jumpedTo = it },
                        )
                    }
                }
            }
            onNodeWithText("Original message", useUnmergedTree = true).performTouchInput { click() }
            onNodeWithTag(MESSAGE_REPLY_ACTION_TAG).assertDoesNotExist()
            runOnIdle { assertEquals("original", jumpedTo) }

            onNodeWithTag("message").performTouchInput { click(Offset(width - 4f, 4f)) }
            onNodeWithTag(MESSAGE_DELETE_ACTION_TAG).assertDoesNotExist()
            onNodeWithTag(MESSAGE_REPLY_ACTION_TAG).performClick()
            runOnIdle { assertEquals(1, replyClicks) }
        }
    }
}
