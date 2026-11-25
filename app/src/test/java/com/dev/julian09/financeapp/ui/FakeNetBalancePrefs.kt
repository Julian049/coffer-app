package com.dev.julian09.financeapp.ui

import com.dev.julian09.financeapp.domain.model.NetChoices
import com.dev.julian09.financeapp.domain.port.NetBalancePrefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

class FakeNetBalancePrefs : NetBalancePrefs {
    val choices = MutableStateFlow(NetChoices())

    override fun observe(): Flow<NetChoices> = choices

    override suspend fun setPeriodOnly(
        cardId: UUID,
        periodOnly: Boolean,
    ) {
        choices.value = choices.value.let { it.copy(periodCards = if (periodOnly) it.periodCards + cardId else it.periodCards - cardId) }
    }

    override suspend fun setPurchaseIncluded(
        purchaseId: UUID,
        included: Boolean,
    ) {
        choices.value =
            choices.value.let {
                it.copy(excludedPurchases = if (included) it.excludedPurchases - purchaseId else it.excludedPurchases + purchaseId)
            }
    }
}
