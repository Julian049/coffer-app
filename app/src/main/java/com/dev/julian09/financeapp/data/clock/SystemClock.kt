package com.dev.julian09.financeapp.data.clock

import com.dev.julian09.financeapp.domain.port.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class SystemClock
    @Inject
    constructor() : Clock {
        override fun now(): Instant = Instant.now()

        override fun today(): LocalDate = LocalDate.now(zone())

        override fun zone(): ZoneId = ZoneId.systemDefault()
    }
