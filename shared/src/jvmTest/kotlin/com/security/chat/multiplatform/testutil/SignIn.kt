package com.security.chat.multiplatform.testutil

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.security.chat.multiplatform.common.core.localization.StringRes
import org.jetbrains.compose.resources.getString
import securitychat.common.localization.generated.resources.sign_in_sign_in_label
import java.security.KeyPairGenerator
import java.util.Base64

@OptIn(ExperimentalTestApi::class)
internal suspend fun SkikoComposeUiTest.signIn() {
    val keyPair = KeyPairGenerator.getInstance("RSA")
        .apply { initialize(2048) }
        .generateKeyPair()
    val privateKey = Base64.getEncoder().encodeToString(keyPair.private.encoded)

    waitUntil(timeoutMillis = 5_000) {
        onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size == 1
    }
    val signInLabel = getString(StringRes.sign_in_sign_in_label)
    onNode(hasSetTextAction()).performTextInput(privateKey)
    onNodeWithText(signInLabel).performClick()
}
