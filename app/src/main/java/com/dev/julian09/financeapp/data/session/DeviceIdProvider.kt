package com.dev.julian09.financeapp.data.session

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Stable id of this installation, sent with every login and refresh; survives logout, not a reinstall. */
@Singleton
class DeviceIdProvider
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        val deviceId: String by lazy {
            val prefs = context.getSharedPreferences("device", Context.MODE_PRIVATE)
            prefs.getString(KEY, null) ?: UUID.randomUUID().toString().also { prefs.edit().putString(KEY, it).apply() }
        }

        private companion object {
            const val KEY = "device_id"
        }
    }
