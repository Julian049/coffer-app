package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.model.UserProfile
import com.dev.julian09.financeapp.domain.port.AuthRepository
import javax.inject.Inject

/** GET /me for the Settings profile card; the stored profile is shown meanwhile and offline. */
class RefreshProfile
    @Inject
    constructor(
        private val repository: AuthRepository,
    ) {
        suspend operator fun invoke(): Result<UserProfile> = repository.refreshProfile()
    }
