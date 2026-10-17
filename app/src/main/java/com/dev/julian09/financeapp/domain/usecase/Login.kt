package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.model.AuthFailure
import com.dev.julian09.financeapp.domain.model.AuthValidation
import com.dev.julian09.financeapp.domain.model.LoginInput
import com.dev.julian09.financeapp.domain.model.UserProfile
import com.dev.julian09.financeapp.domain.port.AuthRepository
import javax.inject.Inject

class Login
    @Inject
    constructor(
        private val repository: AuthRepository,
    ) {
        /** Validates first: an invalid form never reaches the network. */
        suspend operator fun invoke(input: LoginInput): Result<UserProfile> {
            val errors = AuthValidation.login(input)
            return if (errors.isNotEmpty()) Result.failure(AuthFailure.Invalid(errors)) else repository.login(input)
        }
    }
