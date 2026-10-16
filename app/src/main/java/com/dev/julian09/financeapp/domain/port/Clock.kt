package com.dev.julian09.financeapp.domain.port

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Time source of the domain; tests inject a fixed one instead of reading the system clock. */
interface Clock {
    fun now(): Instant

    fun today(): LocalDate

    fun zone(): ZoneId
}
