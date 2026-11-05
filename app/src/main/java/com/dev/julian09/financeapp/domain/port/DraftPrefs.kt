package com.dev.julian09.financeapp.domain.port

import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.UUID

/** 2.4: two phone-local choices around drafts; like [CalendarAccountsStore], not ledger data, so they do not sync. */
interface DraftPrefs {
    /** «Guardar solos los repetidos» (Conectar correo); on until the user turns it off. */
    fun observeAutoSave(): Flow<Boolean>

    suspend fun setAutoSave(on: Boolean)

    /** Movements saved on their own that the user has not been told about yet (Hoja · Movimientos registrados). */
    fun observeRegistered(): Flow<List<UUID>>

    suspend fun addRegistered(transactionId: UUID)

    /** The user opened the sheet: the notice goes away. */
    suspend fun clearRegistered()

    /** 2.4-07: «this merchant in this account is that recurring expense» (key = `learningKey`), learnt when the user says yes. */
    fun observeRecurringLinks(): Flow<Map<String, UUID>>

    suspend fun linkRecurring(
        key: String,
        recurringId: UUID,
    )

    /** Deshacer and Dividir restart what is learned of a merchant: only movements made after [at] count (key = `learningKey`). */
    fun observeResets(): Flow<Map<String, Instant>>

    suspend fun resetLearning(
        key: String,
        at: Instant,
    )
}
