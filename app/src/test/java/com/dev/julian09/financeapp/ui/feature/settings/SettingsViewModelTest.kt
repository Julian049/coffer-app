package com.dev.julian09.financeapp.ui.feature.settings

import com.dev.julian09.financeapp.domain.port.ServerStatusRepository
import com.dev.julian09.financeapp.domain.usecase.CheckServerStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private class FakeServer(
        var up: Boolean,
    ) : ServerStatusRepository {
        override suspend fun isAvailable() = up
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `reports the server state and retries`() {
        val server = FakeServer(up = false)
        val vm = SettingsViewModel(CheckServerStatus(server))
        assertEquals(ServerState.Unavailable, vm.state.value.server)

        server.up = true
        vm.refreshServer()
        assertEquals(ServerState.Available, vm.state.value.server)
    }
}
