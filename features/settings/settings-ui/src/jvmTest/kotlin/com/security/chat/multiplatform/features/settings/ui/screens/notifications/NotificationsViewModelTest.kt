package com.security.chat.multiplatform.features.settings.ui.screens.notifications

import androidx.lifecycle.ViewModelStore
import com.security.chat.multiplatform.common.permission.PermissionsManager
import com.security.chat.multiplatform.common.permission.entity.AllowanceResult
import com.security.chat.multiplatform.common.permission.entity.Permission
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NotificationsViewModelTest {

    private val permissionsManager = FakePermissionsManager()
    private val viewModelStore = ViewModelStore()
    private lateinit var viewModel: NotificationsViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = NotificationsViewModel(permissionsManager)
        viewModelStore.put("notifications", viewModel)
    }

    @AfterEach
    fun tearDown() {
        viewModelStore.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun `opening the screen reads permission without requesting it`() =
        runTest {
            val events = observeViewModel()
            assertNull(viewModel.viewState.value.isPermissionAllowed)

            viewModel.onViewActive()

            assertEquals(false, viewModel.viewState.value.isPermissionAllowed)
            assertTrue(events.isEmpty())
            assertNull(viewModel.viewState.value.alertDialogDescriptor)
        }

    @Test
    fun `allowed notifications do not trigger another request`() =
        runTest {
            permissionsManager.allowance = AllowanceResult.Allowed
            val events = observeViewModel()

            viewModel.onViewActive()
            viewModel.onAllowClicked()

            assertEquals(true, viewModel.viewState.value.isPermissionAllowed)
            assertTrue(events.isEmpty())
        }

    @Test
    fun `allowing notifications updates the status and ignores repeated taps`() =
        runTest {
            val events = observeViewModel()
            viewModel.onViewActive()

            viewModel.onAllowClicked()
            viewModel.onAllowClicked()

            assertEquals(listOf(NotificationsEvent.RequestPermission), events)
            assertTrue(viewModel.viewState.value.permissionRequestInProgress)

            viewModel.onPermissionResult(AllowanceResult.Allowed)

            assertEquals(true, viewModel.viewState.value.isPermissionAllowed)
            assertFalse(viewModel.viewState.value.permissionRequestInProgress)
            assertNull(viewModel.viewState.value.alertDialogDescriptor)
        }

    @Test
    fun `denial shows an explanation and dismissing it allows another attempt`() =
        runTest {
            val events = observeViewModel()
            viewModel.onViewActive()
            viewModel.onAllowClicked()

            viewModel.onPermissionResult(AllowanceResult.Restricted)

            assertEquals(false, viewModel.viewState.value.isPermissionAllowed)
            assertFalse(viewModel.viewState.value.permissionRequestInProgress)
            val dialog = assertNotNull(viewModel.viewState.value.alertDialogDescriptor)
            dialog.dismissAction()
            assertNull(viewModel.viewState.value.alertDialogDescriptor)
            assertEquals(0, permissionsManager.settingsOpened)

            viewModel.onAllowClicked()

            assertEquals(
                listOf(NotificationsEvent.RequestPermission, NotificationsEvent.RequestPermission),
                events,
            )
        }

    @Test
    fun `settings can enable notifications and the status refreshes on return`() =
        runTest {
            observeViewModel()
            viewModel.onViewActive()
            viewModel.onAllowClicked()
            viewModel.onPermissionResult(AllowanceResult.Restricted)
            val dialog = assertNotNull(viewModel.viewState.value.alertDialogDescriptor)

            assertNotNull(dialog.positiveAction).invoke()

            assertEquals(1, permissionsManager.settingsOpened)
            assertNull(viewModel.viewState.value.alertDialogDescriptor)
            viewModel.onViewInactive()
            permissionsManager.allowance = AllowanceResult.Allowed
            viewModel.onViewActive()

            assertEquals(true, viewModel.viewState.value.isPermissionAllowed)
        }

    @Test
    fun `revoking notifications in settings updates the status on return`() =
        runTest {
            permissionsManager.allowance = AllowanceResult.Allowed
            observeViewModel()
            viewModel.onViewActive()

            viewModel.onViewInactive()
            permissionsManager.allowance = AllowanceResult.Restricted
            viewModel.onViewActive()

            assertEquals(false, viewModel.viewState.value.isPermissionAllowed)
        }

    @Test
    fun `a pending permission check cannot overwrite the system dialog result`() =
        runTest {
            observeViewModel()
            viewModel.onViewActive()
            viewModel.onAllowClicked()
            val pendingCheck = CompletableDeferred<AllowanceResult>()
            permissionsManager.pendingCheck = pendingCheck
            viewModel.onViewActive()

            viewModel.onPermissionResult(AllowanceResult.Allowed)
            pendingCheck.complete(AllowanceResult.Restricted)

            assertEquals(true, viewModel.viewState.value.isPermissionAllowed)
            assertNull(viewModel.viewState.value.alertDialogDescriptor)
        }

    private fun TestScope.observeViewModel(): List<NotificationsEvent> {
        val events = mutableListOf<NotificationsEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.viewState.collect()
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.viewEvent.collect { events += it }
        }
        return events
    }

    private class FakePermissionsManager : PermissionsManager {

        var allowance: AllowanceResult = AllowanceResult.Restricted
        var pendingCheck: CompletableDeferred<AllowanceResult>? = null
        var settingsOpened = 0

        override fun isPermissionAllowed(permission: Permission): AllowanceResult =
            error("Notification status must be checked asynchronously")

        override suspend fun checkPermission(permission: Permission): AllowanceResult {
            assertEquals(Permission.Notifications, permission)
            return pendingCheck?.await() ?: allowance
        }

        override fun openAppSettings() {
            settingsOpened++
        }
    }
}
