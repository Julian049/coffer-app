package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.model.LegalDocs
import com.dev.julian09.financeapp.domain.port.LegalConsentStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** True while [userId] has not accepted the current version of the terms and privacy policy. */
class ObserveNeedsLegalConsent
    @Inject
    constructor(
        private val store: LegalConsentStore,
    ) {
        operator fun invoke(userId: String): Flow<Boolean> = store.acceptedVersion(userId).map { it != LegalDocs.VERSION }
    }

class AcceptLegalTerms
    @Inject
    constructor(
        private val store: LegalConsentStore,
    ) {
        suspend operator fun invoke(userId: String) = store.accept(userId, LegalDocs.VERSION)
    }
