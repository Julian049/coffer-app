package com.dev.julian09.financeapp.ui.feature.settings

import com.dev.julian09.financeapp.domain.model.AuthFailure
import com.dev.julian09.financeapp.domain.model.SessionState
import com.dev.julian09.financeapp.domain.port.ServerStatusRepository
import com.dev.julian09.financeapp.domain.usecase.CheckServerStatus
import com.dev.julian09.financeapp.domain.usecase.DeleteAccount
import com.dev.julian09.financeapp.domain.usecase.Logout
import com.dev.julian09.financeapp.domain.usecase.ObserveSession
import com.dev.julian09.financeapp.domain.usecase.RefreshProfile
import com.dev.julian09.financeapp.ui.FakeAuthRepository
import com.dev.julian09.financeapp.ui.FakeLocalDataEraser
import com.dev.julian09.financeapp.ui.FakeReminderScheduler
import com.dev.julian09.financeapp.ui.FakeSyncScheduler
import com.dev.julian09.financeapp.ui.PROFILE
import com.dev.julian09.financeapp.ui.feature.auth.formMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private class FakeServer(
        var up: Boolean,
    ) : ServerStatusRepository {
        override suspend fun isAvailable() = up
    }

    private val scheduler = FakeSyncScheduler()
    private val reminders = FakeReminderScheduler()
    private val eraser = FakeLocalDataEraser()
    private val auth = FakeAuthRepository().apply { session.value = SessionState.Active(PROFILE) }
    private val server = FakeServer(up = false)

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() =
        SettingsViewModel(
            CheckServerStatus(server),
            ObserveSession(auth),
            RefreshProfile(auth),
            Logout(auth, scheduler, FakeReminderScheduler()),
            DeleteAccount(auth, scheduler, reminders, eraser),
        )

    @Test
    fun `reports the server state and retries`() {
        val vm = viewModel()
        assertEquals(ServerState.Unavailable, vm.state.value.server)

        server.up = true
        vm.refreshServer()
        assertEquals(ServerState.Available, vm.state.value.server)
    }

    @Test
    fun `shows the profile of the session`() {
        assertEquals(PROFILE, viewModel().state.value.user)
    }

    @Test
    fun `logout asks first and then signs out`() {
        val vm = viewModel()
        vm.requestLogout()
        assertTrue(vm.state.value.confirmingLogout)
        assertFalse(auth.loggedOut)

        vm.confirmLogout()
        assertTrue(auth.loggedOut)
        assertTrue("sync stops on sign-out", scheduler.cancelled)
        assertFalse(vm.state.value.confirmingLogout)
    }

    @Test
    fun `deleting the account erases this phone only after the server accepted`() {
        val vm = viewModel()
        vm.requestDeleteAccount()
        assertEquals(DeleteAccountUi(), vm.state.value.deleteAccount)

        auth.deleteResult = Result.failure(AuthFailure.ConfirmationFailed)
        vm.confirmDeleteAccount(password = "mala")
        assertFalse("nothing is wiped when the proof fails", eraser.erased)
        assertFalse(auth.sessionCleared)
        assertEquals(
            AuthFailure.ConfirmationFailed.formMessage(),
            vm.state.value.deleteAccount
                ?.error,
        )

        auth.deleteResult = Result.success(Unit)
        vm.confirmDeleteAccount(password = "clave-segura")
        assertEquals(listOf<Pair<String?, String?>>("mala" to null, "clave-segura" to null), auth.deleteCalls)
        assertTrue(eraser.erased)
        assertTrue(scheduler.cancelled)
        assertTrue(reminders.cancelled)
        assertTrue(auth.sessionCleared)
        assertNull(vm.state.value.deleteAccount)
    }

    @Test
    fun `a google confirmation sends the token and a closed picker is not an error`() {
        val vm = viewModel()
        vm.requestDeleteAccount()
        vm.deleteAccountFailed(null)
        assertNull(
            vm.state.value.deleteAccount
                ?.error,
        )

        vm.confirmDeleteAccount(idToken = "google-token")
        assertEquals(listOf<Pair<String?, String?>>(null to "google-token"), auth.deleteCalls)
        assertTrue(eraser.erased)
    }
}
