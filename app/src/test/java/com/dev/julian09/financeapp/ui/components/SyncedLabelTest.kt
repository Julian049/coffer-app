package com.dev.julian09.financeapp.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class SyncedLabelTest {
    private val now = Instant.parse("2026-10-17T12:00:00Z")

    @Test
    fun `reads how long ago the last sync was`() {
        assertEquals("Sincronizado", syncedLabel(null, now))
        assertEquals("Sincronizado · ahora", syncedLabel(now.minusSeconds(30), now))
        assertEquals("Sincronizado · hace 5 min", syncedLabel(now.minusSeconds(5 * 60), now))
        assertEquals("Sincronizado · hace 3 h", syncedLabel(now.minusSeconds(3 * 3600 + 59 * 60), now))
        assertEquals("Sincronizado · hace 2 d", syncedLabel(now.minusSeconds(2 * 86400), now))
    }
}
