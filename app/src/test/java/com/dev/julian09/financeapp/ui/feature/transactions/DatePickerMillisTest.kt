package com.dev.julian09.financeapp.ui.feature.transactions

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class DatePickerMillisTest {
    private val bogota = ZoneId.of("America/Bogota")

    @Test
    fun `late evening movement opens the picker on its own local day`() {
        // 2026-03-05 22:30 Bogota is already 2026-03-06 in UTC: the day must not shift.
        val at = LocalDateTime.of(2026, 3, 5, 22, 30).atZone(bogota).toInstant()
        val expected = LocalDateTime.of(2026, 3, 5, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        assertEquals(expected, datePickerMillis(at, bogota))
    }

    @Test
    fun `picker millis follow the movement and not today`() {
        val old = Instant.parse("2020-01-02T15:00:00Z")
        assertEquals(Instant.parse("2020-01-02T00:00:00Z").toEpochMilli(), datePickerMillis(old, bogota))
    }
}
