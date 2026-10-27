package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.model.AuthFailure
import com.dev.julian09.financeapp.domain.model.AuthValidation
import com.dev.julian09.financeapp.domain.port.AuthRepository
import javax.inject.Inject

/** REQ-AUT-09: emails a 6-digit code; validated first, so an invalid email never reaches the network. */
class RequestPasswordReset
    @Inject
    constructor(
        private val repository: AuthRepository,
    ) {
        suspend operator fun invoke(email: String): Result<Unit> {
            val errors = AuthValidation.resetEmail(email)
            return if (errors.isNotEmpty()) Result.failure(AuthFailure.Invalid(errors)) else repository.requestPasswordReset(email)
        }
    }

/** REQ-AUT-09: the code sets the new password; every session, this one included, has to sign in again. */
class ConfirmPasswordReset
    @Inject
    constructor(
        private val repository: AuthRepository,
    ) {
        suspend operator fun invoke(
            email: String,
            code: String,
            newPassword: String,
        ): Result<Unit> {
            val errors = AuthValidation.resetConfirm(code, newPassword)
            return if (errors.isNotEmpty()) {
                Result.failure(AuthFailure.Invalid(errors))
            } else {
                repository.confirmPasswordReset(email, code, newPassword)
            }
        }
    }
