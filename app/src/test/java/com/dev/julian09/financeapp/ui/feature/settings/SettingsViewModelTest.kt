package com.dev.julian09.financeapp.ui.feature.settings

import com.dev.julian09.financeapp.domain.model.SessionState
import com.dev.julian09.financeapp.domain.port.ServerStatusRepository
import com.dev.julian09.financeapp.domain.usecase.CheckServerStatus
import com.dev.julian09.financeapp.domain.usecase.Logout
import com.dev.julian09.financeapp.domain.usecase.ObserveSession
import com.dev.julian09.financeapp.domain.usecase.RefreshProfile
import com.dev.julian09.financeapp.ui.FakeAuthRepository
import com.dev.julian09.financeapp.ui.FakeSyncScheduler
import com.dev.julian09.financeapp.ui.PROFILE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    private val auth = FakeAuthRepository().apply { session.value = SessionState.Active(PROFILE) }
    private val server = FakeServer(up = false)

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() =
        SettingsViewModel(CheckServerStatus(server), ObserveSession(auth), RefreshProfile(auth), Logout(auth, scheduler))

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
}
