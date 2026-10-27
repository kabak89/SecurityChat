package com.security.chat.multiplatform.features.call.ui.screens.permissions

import androidx.lifecycle.ViewModelStore
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.ui.entity.PrintableText
import com.security.chat.multiplatform.common.permission.PermissionsManager
import com.security.chat.multiplatform.common.permission.entity.AllowanceResult
import com.security.chat.multiplatform.common.permission.entity.Permission
import com.security.chat.multiplatform.features.call.component.api.PermissionsComponent
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
import org.koin.core.scope.Scope
import securitychat.common.localization.generated.resources.permissions_local_network_restricted_message
import securitychat.common.localization.generated.resources.permissions_local_network_restricted_title
import securitychat.common.localization.generated.resources.permissions_microphone_restricted_message
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PermissionsViewModelTest {

    private val permissionsManager = FakePermissionsManager()
    private val component = FakePermissionsComponent()
    private lateinit var viewModel: PermissionsViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = PermissionsViewModel(component, permissionsManager)
        component.viewModelStore.put("permissions", viewModel)
    }

    @AfterEach
    fun tearDown() {
        component.viewModelStore.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun `call opens once when all permissions are already granted`() = runTest {
        permissionsManager.restricted.clear()
        val events = observeViewModel()
        viewModel.onViewActive()
        viewModel.onViewActive()
        viewModel.onAllowClicked()
        assertEquals(1, component.grantedCount)
        assertFalse(viewModel.viewState.value.isPermissionRequired)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `microphone and local network are requested in order before opening the call`() = runTest {
        val events = observeViewModel()
        viewModel.onViewActive()
        assertTrue(events.isEmpty())
        assertEquals(
            listOf(Permission.RecordAudio, Permission.LocalNetwork),
            viewModel.viewState.value.requiredPermissions,
        )
        viewModel.onAllowClicked()
        viewModel.onAllowClicked()
        assertEquals(listOf(PermissionsEvent.RequestPermission(Permission.RecordAudio)), events)
        grant(Permission.RecordAudio)
        assertEquals(
            listOf(
                PermissionsEvent.RequestPermission(Permission.RecordAudio),
                PermissionsEvent.RequestPermission(Permission.LocalNetwork),
            ),
            events,
        )
        assertEquals(listOf(Permission.LocalNetwork), viewModel.viewState.value.requiredPermissions)
        assertEquals(0, component.grantedCount)
        grant(Permission.LocalNetwork)
        assertEquals(1, component.grantedCount)
        assertFalse(viewModel.viewState.value.isPermissionRequired)
    }

    @Test
    fun `existing microphone access does not skip the local network request`() = runTest {
        permissionsManager.restricted -= Permission.RecordAudio
        val events = observeViewModel()
        viewModel.onViewActive()
        assertEquals(0, component.grantedCount)
        viewModel.onAllowClicked()
        assertEquals(listOf(PermissionsEvent.RequestPermission(Permission.LocalNetwork)), events)
        grant(Permission.LocalNetwork)
        assertEquals(1, component.grantedCount)
    }

    @Test
    fun `platforms without a local network permission only request the microphone`() = runTest {
        permissionsManager.restricted -= Permission.LocalNetwork
        val events = observeViewModel()
        viewModel.onViewActive()
        assertEquals(listOf(Permission.RecordAudio), viewModel.viewState.value.requiredPermissions)
        viewModel.onAllowClicked()
        grant(Permission.RecordAudio)
        assertEquals(listOf(PermissionsEvent.RequestPermission(Permission.RecordAudio)), events)
        assertEquals(1, component.grantedCount)
    }

    @Test
    fun `local network denial offers settings and rechecks permissions on return`() = runTest {
        permissionsManager.restricted -= Permission.RecordAudio
        observeViewModel()
        viewModel.onAllowClicked()
        viewModel.onPermissionResult(Permission.LocalNetwork, AllowanceResult.Restricted)
        val dialog = assertNotNull(viewModel.viewState.value.alertDialogDescriptor)
        assertEquals(
            StringRes.permissions_local_network_restricted_title,
            assertIs<PrintableText.StringRes>(dialog.content.title).resId,
        )
        assertEquals(
            StringRes.permissions_local_network_restricted_message,
            assertIs<PrintableText.StringRes>(dialog.content.message).resId,
        )
        assertEquals(0, component.grantedCount)
        assertEquals(0, permissionsManager.settingsOpened)
        assertNotNull(dialog.positiveAction).invoke()
        assertEquals(1, permissionsManager.settingsOpened)
        viewModel.onViewInactive()
        permissionsManager.restricted -= Permission.LocalNetwork
        viewModel.onViewActive()
        viewModel.onViewActive()
        assertEquals(1, component.grantedCount)
        assertNull(viewModel.viewState.value.alertDialogDescriptor)
    }

    @Test
    fun `microphone denial does not request the local network or open the call`() = runTest {
        val events = observeViewModel()
        viewModel.onAllowClicked()
        viewModel.onPermissionResult(Permission.RecordAudio, AllowanceResult.Restricted)
        val dialog = assertNotNull(viewModel.viewState.value.alertDialogDescriptor)
        assertEquals(
            StringRes.permissions_microphone_restricted_message,
            assertIs<PrintableText.StringRes>(dialog.content.message).resId,
        )
        assertEquals(listOf(PermissionsEvent.RequestPermission(Permission.RecordAudio)), events)
        assertEquals(0, component.grantedCount)
        assertNotNull(dialog.negativeAction).invoke()
        assertEquals(1, component.backCount)
        assertEquals(0, permissionsManager.settingsOpened)
    }

    @Test
    fun `declining local network access on the screen shows the relevant explanation`() = runTest {
        permissionsManager.restricted -= Permission.RecordAudio
        val events = observeViewModel()
        viewModel.onViewActive()
        viewModel.showPermissionRestrictedDialog()
        val dialog = assertNotNull(viewModel.viewState.value.alertDialogDescriptor)
        assertEquals(
            StringRes.permissions_local_network_restricted_message,
            assertIs<PrintableText.StringRes>(dialog.content.message).resId,
        )
        dialog.dismissAction()
        assertEquals(1, component.backCount)
        assertEquals(0, component.grantedCount)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `stale permission callbacks do not advance the permission flow`() = runTest {
        val events = observeViewModel()
        viewModel.onAllowClicked()
        viewModel.onPermissionResult(Permission.LocalNetwork, AllowanceResult.Allowed)
        assertEquals(listOf(PermissionsEvent.RequestPermission(Permission.RecordAudio)), events)
        assertEquals(0, component.grantedCount)
        assertNull(viewModel.viewState.value.alertDialogDescriptor)
    }

    private fun grant(permission: Permission) {
        permissionsManager.restricted -= permission
        viewModel.onPermissionResult(permission, AllowanceResult.Allowed)
    }

    private fun TestScope.observeViewModel(): List<PermissionsEvent> {
        val events = mutableListOf<PermissionsEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.viewState.collect()
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.viewEvent.collect { events += it }
        }
        return events
    }

    private class FakePermissionsManager : PermissionsManager {
        val restricted = mutableSetOf(Permission.RecordAudio, Permission.LocalNetwork)
        var settingsOpened = 0

        override fun isPermissionAllowed(permission: Permission): AllowanceResult =
            if (permission in restricted) AllowanceResult.Restricted else AllowanceResult.Allowed

        override fun openAppSettings() {
            settingsOpened++
        }
    }

    private class FakePermissionsComponent : PermissionsComponent {
        override val viewModelStore = ViewModelStore()
        var grantedCount = 0
        var backCount = 0

        override fun getDiScope(): Scope = error("DI scope is not used by permission tests")

        override fun onBackClicked() {
            backCount++
        }

        override fun onPermissionsGranted() {
            grantedCount++
        }
    }
}
