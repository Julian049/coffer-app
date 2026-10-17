package com.dev.julian09.financeapp.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.dev.julian09.financeapp.domain.port.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** Same schema without SQLCipher (its native library does not load in Robolectric); encryption has its own device test. */
fun inMemoryDatabase(): FinanceDatabase =
    Room
        .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), FinanceDatabase::class.java)
        .allowMainThreadQueries()
        .build()

class FixedClock(
    private val instant: Instant = Instant.parse("2026-10-16T12:00:00Z"),
) : Clock {
    override fun now() = instant

    override fun today(): LocalDate = LocalDate.ofInstant(instant, zone())

    override fun zone(): ZoneId = ZoneOffset.UTC
}
