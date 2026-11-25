package com.dev.julian09.financeapp.data.session

import android.content.Context
import com.dev.julian09.financeapp.domain.model.NetChoices
import com.dev.julian09.financeapp.domain.port.NetBalancePrefs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Stays on the phone (PO, 2026-11-24): a new install starts with every card by its whole debt and every purchase in. */
@Singleton
class SharedPrefsNetBalancePrefs
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : NetBalancePrefs {
        private val prefs = context.getSharedPreferences("net_balance", Context.MODE_PRIVATE)
        private val choices = MutableStateFlow(NetChoices(read(PERIOD_CARDS), read(EXCLUDED_PURCHASES)))

        override fun observe(): Flow<NetChoices> = choices

        override suspend fun setPeriodOnly(
            cardId: UUID,
            periodOnly: Boolean,
        ) = save(choices.value.copy(periodCards = choices.value.periodCards.toggled(cardId, periodOnly)))

        override suspend fun setPurchaseIncluded(
            purchaseId: UUID,
            included: Boolean,
        ) = save(choices.value.copy(excludedPurchases = choices.value.excludedPurchases.toggled(purchaseId, !included)))

        private fun Set<UUID>.toggled(
            id: UUID,
            on: Boolean,
        ) = if (on) this + id else this - id

        private fun save(next: NetChoices) {
            prefs
                .edit()
                .putStringSet(PERIOD_CARDS, next.periodCards.map(UUID::toString).toSet())
                .putStringSet(EXCLUDED_PURCHASES, next.excludedPurchases.map(UUID::toString).toSet())
                .apply()
            choices.value = next
        }

        private fun read(key: String) =
            prefs
                .getStringSet(key, emptySet())
                .orEmpty()
                .mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() }
                .toSet()

        private companion object {
            const val PERIOD_CARDS = "period_cards"
            const val EXCLUDED_PURCHASES = "excluded_purchases"
        }
    }
