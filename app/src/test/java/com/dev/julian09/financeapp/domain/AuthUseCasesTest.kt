package com.dev.julian09.financeapp.domain

import com.dev.julian09.financeapp.domain.model.AuthFailure
import com.dev.julian09.financeapp.domain.model.AuthField
import com.dev.julian09.financeapp.domain.model.LoginInput
import com.dev.julian09.financeapp.domain.model.RegisterInput
import com.dev.julian09.financeapp.domain.port.AuthRepository
import com.dev.julian09.financeapp.domain.usecase.Login
import com.dev.julian09.financeapp.domain.usecase.RegisterUser
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthUseCasesTest {
    private val repository = mockk<AuthRepository>(relaxed = true)

    @Test
    fun `an invalid registration never reaches the network`() =
        runTest {
            val result = RegisterUser(repository)(RegisterInput(" ", "no-es-correo", "Julian B", "corta"))

            val fields = (result.exceptionOrNull() as AuthFailure.Invalid).fields
            assertEquals(setOf(AuthField.DisplayName, AuthField.Email, AuthField.Username, AuthField.Password), fields.keys)
            coVerify(exactly = 0) { repository.register(any()) }
        }

    @Test
    fun `a valid registration goes to the repository`() =
        runTest {
            RegisterUser(repository)(RegisterInput("Julian", "julian@correo.com", "julian.b_9", "clave-segura"))
            coVerify(exactly = 1) { repository.register(any()) }
        }

    @Test
    fun `login needs an identifier and a password`() =
        runTest {
            val result = Login(repository)(LoginInput("jb", ""))

            assertEquals(setOf(AuthField.Identifier, AuthField.Password), (result.exceptionOrNull() as AuthFailure.Invalid).fields.keys)
            coVerify(exactly = 0) { repository.login(any()) }
        }
}
