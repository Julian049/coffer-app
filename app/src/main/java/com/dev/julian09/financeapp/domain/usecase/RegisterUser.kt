package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.model.AuthFailure
import com.dev.julian09.financeapp.domain.model.AuthValidation
import com.dev.julian09.financeapp.domain.model.RegisterInput
import com.dev.julian09.financeapp.domain.model.UserProfile
import com.dev.julian09.financeapp.domain.port.AuthRepository
import javax.inject.Inject

class RegisterUser
    @Inject
    constructor(
        private val repository: AuthRepository,
    ) {
        /** Validates first: an invalid form never reaches the network. */
        suspend operator fun invoke(input: RegisterInput): Result<UserProfile> {
            val errors = AuthValidation.register(input)
            return if (errors.isNotEmpty()) Result.failure(AuthFailure.Invalid(errors)) else repository.register(input)
        }
    }
