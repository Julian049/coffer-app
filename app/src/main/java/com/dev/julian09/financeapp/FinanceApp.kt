package com.dev.julian09.financeapp

import android.app.Application
import com.dev.julian09.financeapp.data.local.LegacyDatabaseMigrator
import com.dev.julian09.financeapp.di.ApplicationScope
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class FinanceApp : Application() {
    @Inject
    lateinit var legacyMigrator: LegacyDatabaseMigrator

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        // Off the main thread; screens observe the database and update as soon as the MVP rows land
        appScope.launch { legacyMigrator.migrateIfNeeded() }
    }
}
