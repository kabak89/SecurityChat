package com.security.chat.multiplatform

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.fake.successfulSignInModule
import com.security.chat.multiplatform.testutil.runAppUiTest
import com.security.chat.multiplatform.testutil.signIn
import org.jetbrains.compose.resources.getString
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import securitychat.common.localization.generated.resources.onboarding_welcome_title

@OptIn(ExperimentalTestApi::class)
@Tag("ui")
class AppStartupTest {

    @Test
    fun `successful sign in opens onboarding`() {
        runAppUiTest(
            testModules = listOf(successfulSignInModule()),
            videoName = "sign-in-onboarding",
        ) {
            signIn()

            val welcomeTitle = getString(StringRes.onboarding_welcome_title)
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText(welcomeTitle).fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText(welcomeTitle).assertIsDisplayed()
        }
    }
}
