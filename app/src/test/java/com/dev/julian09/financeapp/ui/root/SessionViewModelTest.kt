package com.dev.julian09.financeapp.ui.root

import com.dev.julian09.financeapp.domain.model.SessionState
import com.dev.julian09.financeapp.domain.usecase.ObserveSession
import com.dev.julian09.financeapp.ui.FakeAuthRepository
import com.dev.julian09.financeapp.ui.FakeReminderScheduler
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
class SessionViewModelTest {
    private val scheduler = FakeSyncScheduler()
    private val reminders = FakeReminderScheduler()
    private val auth = FakeAuthRepository()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `syncs only with a session whose initial download finished`() {
        SessionViewModel(ObserveSession(auth), scheduler, reminders)
        assertEquals(0, scheduler.syncRequests)

        auth.session.value = SessionState.Active(PROFILE)
        assertEquals(0, scheduler.syncRequests) // the download screen runs its own work
        assertFalse(scheduler.periodic)
        assertFalse(reminders.scheduled)

        scheduler.initialDone.value = true
        assertEquals(1, scheduler.syncRequests)
        assertTrue(scheduler.periodic)
        assertTrue(reminders.scheduled)
    }
}
