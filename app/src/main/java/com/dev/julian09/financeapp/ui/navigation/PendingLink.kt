package com.dev.julian09.financeapp.ui.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

/** The recurring expense, card or loan of a tapped notice (S6-DAT02, S7, S10): MainActivity sets it, Home opens its sheet and clears it. */
object PendingLink {
    val recurringId = MutableStateFlow<UUID?>(null)

    val cardId = MutableStateFlow<UUID?>(null)

    val loanId = MutableStateFlow<UUID?>(null)
}
