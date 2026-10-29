package com.security.chat.multiplatform

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.permission.PermissionsManager
import com.security.chat.multiplatform.common.permission.entity.AllowanceResult
import com.security.chat.multiplatform.common.permission.entity.Permission
import com.security.chat.multiplatform.fake.successfulSignInModule
import com.security.chat.multiplatform.testutil.pauseForVideo
import com.security.chat.multiplatform.testutil.runAppUiTest
import com.security.chat.multiplatform.testutil.signIn
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import org.jetbrains.compose.resources.getString
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import securitychat.common.localization.generated.resources.common_next
import securitychat.common.localization.generated.resources.notification_permission_description
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class)
@Tag("ui")
class OnboardingTest {

    @Test
    fun `scrolling onboarding and continuing opens notification permission screen`() {
        val permissionModule = module {
            single<PermissionsManager> {
                mock {
                    every {
                        isPermissionAllowed(Permission.Notifications)
                    } returns AllowanceResult.Restricted
                }
            }
        }

        runAppUiTest(
            testModules = listOf(successfulSignInModule(), permissionModule),
            videoName = "onboarding-notification-permission",
        ) {
            signIn()

            pauseForVideo(1.seconds)

            val nextLabel = getString(StringRes.common_next)
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText(nextLabel).fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText(nextLabel).performScrollTo()
            pauseForVideo(1.seconds)
            onNodeWithText(nextLabel).performClick()
            pauseForVideo(1.seconds)

            val permissionDescription = getString(StringRes.notification_permission_description)
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText(permissionDescription).fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText(permissionDescription).assertIsDisplayed()
        }
    }
}
