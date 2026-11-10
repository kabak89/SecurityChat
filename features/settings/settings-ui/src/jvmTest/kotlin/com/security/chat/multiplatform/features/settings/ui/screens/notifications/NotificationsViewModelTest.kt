package com.security.chat.multiplatform.features.settings.ui.screens.notifications

import androidx.lifecycle.ViewModelStore
import com.security.chat.multiplatform.common.device.info.DeviceInfoManager
import com.security.chat.multiplatform.common.device.info.entity.Platform
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NotificationsViewModelTest {

    private val permissionsManager = FakePermissionsManager()
    private val viewModelStore = ViewModelStore()
    private lateinit var viewModel: NotificationsViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        createViewModel()
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
            assertNull(viewModel.viewState.value.allowedPermissions[Permission.Notifications])

            viewModel.onViewActive()

            assertEquals(
                false,
                viewModel.viewState.value.allowedPermissions[Permission.Notifications],
            )
            assertTrue(events.isEmpty())
            assertNull(viewModel.viewState.value.restrictedPermission)
        }

    @Test
    fun `allowed notifications do not trigger another request`() =
        runTest {
            permissionsManager.allowance = AllowanceResult.Allowed
            val events = observeViewModel()

            viewModel.onViewActive()
            viewModel.onAllowClicked(Permission.Notifications)

            assertEquals(
                true,
                viewModel.viewState.value.allowedPermissions[Permission.Notifications],
            )
            assertTrue(events.isEmpty())
        }

    @Test
    fun `allowing notifications updates the status and ignores repeated taps`() =
        runTest {
            val events = observeViewModel()
            viewModel.onViewActive()

            viewModel.onAllowClicked(Permission.Notifications)
            viewModel.onAllowClicked(Permission.Notifications)

            assertEquals(
                listOf(NotificationsEvent.RequestPermission(Permission.Notifications)),
                events,
            )
            assertEquals(
                Permission.Notifications,
                viewModel.viewState.value.permissionRequestInProgress,
            )

            viewModel.onPermissionResult(Permission.Notifications, AllowanceResult.Allowed)

            assertEquals(
                true,
                viewModel.viewState.value.allowedPermissions[Permission.Notifications],
            )
            assertNull(viewModel.viewState.value.permissionRequestInProgress)
            assertNull(viewModel.viewState.value.restrictedPermission)
        }

    @Test
    fun `denial shows an explanation and dismissing it allows another attempt`() =
        runTest {
            val events = observeViewModel()
            viewModel.onViewActive()
            viewModel.onAllowClicked(Permission.Notifications)

            viewModel.onPermissionResult(Permission.Notifications, AllowanceResult.Restricted)

            assertEquals(
                false,
                viewModel.viewState.value.allowedPermissions[Permission.Notifications],
            )
            assertNull(viewModel.viewState.value.permissionRequestInProgress)
            assertEquals(Permission.Notifications, viewModel.viewState.value.restrictedPermission)
            viewModel.onDismissDialog()
            assertNull(viewModel.viewState.value.restrictedPermission)
            assertEquals(0, permissionsManager.settingsOpened)

            viewModel.onAllowClicked(Permission.Notifications)

            assertEquals(
                listOf(
                    NotificationsEvent.RequestPermission(Permission.Notifications),
                    NotificationsEvent.RequestPermission(Permission.Notifications),
                ),
                events,
            )
        }

    @Test
    fun `settings can enable notifications and the status refreshes on return`() =
        runTest {
            observeViewModel()
            viewModel.onViewActive()
            viewModel.onAllowClicked(Permission.Notifications)
            viewModel.onPermissionResult(Permission.Notifications, AllowanceResult.Restricted)
            assertEquals(Permission.Notifications, viewModel.viewState.value.restrictedPermission)

            viewModel.onOpenSettingsClicked()

            assertEquals(1, permissionsManager.settingsOpened)
            assertNull(viewModel.viewState.value.restrictedPermission)
            viewModel.onViewInactive()
            permissionsManager.allowance = AllowanceResult.Allowed
            viewModel.onViewActive()

            assertEquals(
                true,
                viewModel.viewState.value.allowedPermissions[Permission.Notifications],
            )
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

            assertEquals(
                false,
                viewModel.viewState.value.allowedPermissions[Permission.Notifications],
            )
        }

    @Test
    fun `a pending permission check cannot overwrite the system dialog result`() =
        runTest {
            observeViewModel()
            viewModel.onViewActive()
            viewModel.onAllowClicked(Permission.Notifications)
            val pendingCheck = CompletableDeferred<AllowanceResult>()
            permissionsManager.pendingCheck = pendingCheck
            viewModel.onViewActive()

            viewModel.onPermissionResult(Permission.Notifications, AllowanceResult.Allowed)
            pendingCheck.complete(AllowanceResult.Restricted)

            assertEquals(
                true,
                viewModel.viewState.value.allowedPermissions[Permission.Notifications],
            )
            assertNull(viewModel.viewState.value.restrictedPermission)
        }

    @Test
    fun `granting Bluetooth starts mesh and keeps notification status separate`() = runTest {
        val events = observeViewModel()
        viewModel.onViewActive()
        viewModel.onAllowClicked(Permission.Bluetooth)
        viewModel.onAllowClicked(Permission.Notifications)
        viewModel.onPermissionResult(Permission.Notifications, AllowanceResult.Allowed)
        assertEquals(Permission.Bluetooth, viewModel.viewState.value.permissionRequestInProgress)

        viewModel.onPermissionResult(Permission.Bluetooth, AllowanceResult.Allowed)

        assertEquals(true, viewModel.viewState.value.allowedPermissions[Permission.Bluetooth])
        assertEquals(false, viewModel.viewState.value.allowedPermissions[Permission.Notifications])
        assertEquals(
            listOf(
                NotificationsEvent.RequestPermission(Permission.Bluetooth),
                NotificationsEvent.BluetoothAllowed,
            ),
            events,
        )
    }

    @Test
    fun `granting Bluetooth in system settings refreshes the status and starts mesh`() = runTest {
        val events = observeViewModel()
        permissionsManager.allowance = AllowanceResult.Allowed
        viewModel.onViewActive()
        viewModel.onAllowClicked(Permission.Bluetooth)
        viewModel.onPermissionResult(Permission.Bluetooth, AllowanceResult.Restricted)
        assertEquals(Permission.Bluetooth, viewModel.viewState.value.restrictedPermission)

        viewModel.onOpenSettingsClicked()
        viewModel.onViewInactive()
        permissionsManager.bluetoothAllowance = AllowanceResult.Allowed
        viewModel.onViewActive()
        viewModel.onViewActive()

        assertEquals(1, permissionsManager.settingsOpened)
        assertNull(viewModel.viewState.value.restrictedPermission)
        assertEquals(true, viewModel.viewState.value.allowedPermissions[Permission.Bluetooth])
        assertEquals(
            listOf(
                NotificationsEvent.RequestPermission(Permission.Bluetooth),
                NotificationsEvent.BluetoothAllowed,
            ),
            events,
        )
    }

    @Test
    fun `background scanning requires foreground location and restarts mesh when granted`() =
        runTest {
            permissionsManager.requiresBackgroundLocation = true
            createViewModel()
            val events = observeViewModel()
            val backgroundLocation = Permission.BluetoothBackgroundLocation
            viewModel.onViewActive()

            viewModel.onAllowClicked(backgroundLocation)
            assertTrue(events.isEmpty())

            permissionsManager.bluetoothAllowance = AllowanceResult.Allowed
            viewModel.onViewActive()
            viewModel.onAllowClicked(backgroundLocation)
            viewModel.onPermissionResult(backgroundLocation, AllowanceResult.Allowed)

            assertEquals(true, viewModel.viewState.value.allowedPermissions[backgroundLocation])
            assertEquals(
                listOf(
                    NotificationsEvent.BluetoothAllowed,
                    NotificationsEvent.RequestPermission(backgroundLocation),
                    NotificationsEvent.BluetoothAllowed,
                ),
                events,
            )
        }

    private fun createViewModel() {
        viewModel = NotificationsViewModel(
            permissionsManager = permissionsManager,
            deviceInfoManager = object : DeviceInfoManager {
                override fun getPlatform(): Platform = Platform.Android

                override fun getDeviceName(): String = "Test device"
            },
        )
        viewModelStore.put("notifications", viewModel)
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
        var bluetoothAllowance: AllowanceResult = AllowanceResult.Restricted
        var requiresBackgroundLocation = false
        var pendingCheck: CompletableDeferred<AllowanceResult>? = null
        var settingsOpened = 0

        override fun isPermissionAllowed(permission: Permission): AllowanceResult =
            error("Notification status must be checked asynchronously")

        override suspend fun checkPermission(permission: Permission): AllowanceResult {
            return when (permission) {
                Permission.Notifications -> pendingCheck?.await() ?: allowance
                Permission.Bluetooth -> bluetoothAllowance
                Permission.BluetoothBackgroundLocation -> AllowanceResult.Restricted
                else -> error("Unexpected permission: $permission")
            }
        }

        override fun openAppSettings() {
            settingsOpened++
        }

        override fun bluetoothScanRequiresBackgroundLocation(): Boolean = requiresBackgroundLocation
    }
}
