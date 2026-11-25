package com.dev.julian09.financeapp.domain.port

import com.dev.julian09.financeapp.domain.model.NetChoices
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** 2.5: the finer choices of Saldo neto «Personalizado»; phone-local like [DraftPrefs] (PO, 2026-11-24). */
interface NetBalancePrefs {
    fun observe(): Flow<NetChoices>

    suspend fun setPeriodOnly(
        cardId: UUID,
        periodOnly: Boolean,
    )

    suspend fun setPurchaseIncluded(
        purchaseId: UUID,
        included: Boolean,
    )
}
