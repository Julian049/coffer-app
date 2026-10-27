package com.dev.julian09.financeapp.domain.port

import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** Hoja · Cuentas del calendario (REQ-CAL-03): the accounts the user picked on this phone; null = never picked, use the default. */
interface CalendarAccountsStore {
    fun observe(): Flow<Set<UUID>?>

    suspend fun save(ids: Set<UUID>)
}
