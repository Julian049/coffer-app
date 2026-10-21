package com.dev.julian09.financeapp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.dev.julian09.financeapp.data.local.LegacyDatabaseMigrator
import com.dev.julian09.financeapp.di.ApplicationScope
import com.dev.julian09.financeapp.domain.usecase.PurgeChangeHistory
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class FinanceApp :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var legacyMigrator: LegacyDatabaseMigrator

    @Inject
    lateinit var purgeChangeHistory: PurgeChangeHistory

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    // Workers are built by Hilt so SyncWorker gets its dependencies
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // Off the main thread; screens observe the database and update as soon as the MVP rows land
        appScope.launch {
            legacyMigrator.migrateIfNeeded()
            purgeChangeHistory()
        }
    }
}
