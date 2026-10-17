package com.dev.julian09.financeapp.ui.feature.sync

import com.dev.julian09.financeapp.domain.model.InitialSyncProgress
import com.dev.julian09.financeapp.ui.FakeSyncScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InitialDownloadViewModelTest {
    private val scheduler = FakeSyncScheduler()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `starts the download once and follows its progress`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = InitialDownloadViewModel(scheduler)
            backgroundScope.launch { vm.progress.collect {} }
            assertEquals(1, scheduler.initialRequests.size)

            val step = InitialSyncProgress(received = 40, total = 100, byEntity = mapOf("account" to 5L, "transaction" to 35L))
            scheduler.progress.value = scheduler.initialRequests[0] to step
            assertEquals(step, vm.progress.value)
        }

    @Test
    fun `retry starts a new work and follows that one`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = InitialDownloadViewModel(scheduler)
            backgroundScope.launch { vm.progress.collect {} }
            scheduler.progress.value = scheduler.initialRequests[0] to InitialSyncProgress(failed = true)
            assertEquals(true, vm.progress.value.failed)

            vm.retry()
            assertEquals(2, scheduler.initialRequests.size)
            // Late updates of the failed work are ignored
            scheduler.progress.value = scheduler.initialRequests[0] to InitialSyncProgress(failed = true, received = 9)
            assertEquals(0L, vm.progress.value.received)
            scheduler.progress.value = scheduler.initialRequests[1] to InitialSyncProgress(received = 3)
            assertEquals(InitialSyncProgress(received = 3), vm.progress.value)
        }
}
