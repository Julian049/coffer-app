package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.model.HomeCard
import com.dev.julian09.financeapp.domain.port.AccountRepository
import java.util.UUID
import javax.inject.Inject

/** Personalizar inicio (S11-UI04, UI05): the cards of Home and the order of the accounts, saved together. */
class CustomizeHome
    @Inject
    constructor(
        private val updateSettings: UpdateSettings,
        private val accounts: AccountRepository,
    ) {
        suspend operator fun invoke(
            cards: List<HomeCard>,
            accountOrder: List<UUID>,
        ) {
            accounts.reorder(accountOrder)
            // UpdateSettings asks for the sync, which also takes the accounts
            updateSettings { it.copy(homeCards = cards) }
        }
    }
