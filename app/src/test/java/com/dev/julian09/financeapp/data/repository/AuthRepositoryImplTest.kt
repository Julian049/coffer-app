package com.dev.julian09.financeapp.data.repository

import com.dev.julian09.financeapp.data.remote.TestNetwork
import com.dev.julian09.financeapp.data.remote.authBody
import com.dev.julian09.financeapp.data.remote.testJson
import com.dev.julian09.financeapp.data.session.FakeSessionStore
import com.dev.julian09.financeapp.data.session.StoredSession
import com.dev.julian09.financeapp.data.session.TEST_USER
import com.dev.julian09.financeapp.domain.model.AuthFailure
import com.dev.julian09.financeapp.domain.model.AuthField
import com.dev.julian09.financeapp.domain.model.LoginInput
import com.dev.julian09.financeapp.domain.model.RegisterInput
import com.dev.julian09.financeapp.domain.model.SessionState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AuthRepositoryImplTest {
    private val server = MockWebServer()
    private val store = FakeSessionStore()
    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun start() {
        server.start()
        val network = TestNetwork(server, store)
        repository = AuthRepositoryImpl(network.authApi, store, network.device, testJson)
    }

    @After
    fun stop() = server.close()

    @Test
    fun `login saves the session`() =
        runTest {
            server.enqueue(json(200, authBody("a", "r-0123456789012345678")))

            val result = repository.login(LoginInput("julianb", "clave-segura"))

            assertEquals("julianb", result.getOrThrow().username)
            assertEquals(SessionState.Active(TEST_USER.toDomain()), repository.observeSession().first())
        }

    @Test
    fun `server problems become failures the screens understand`() =
        runTest {
            server.enqueue(json(401, """{"code":"FIN-AUTH-INVALID","detail":"Usuario o contraseña incorrectos."}"""))
            assertEquals(AuthFailure.WrongCredentials, repository.login(LoginInput("julianb", "mala")).exceptionOrNull())

            server.enqueue(json(429, """{"code":"FIN-RATE-LIMIT"}""").newBuilder().addHeader("Retry-After", "42").build())
            assertEquals(AuthFailure.TooManyAttempts(42), repository.login(LoginInput("julianb", "mala")).exceptionOrNull())

            server.enqueue(json(409, """{"code":"FIN-CONFLICT","detail":"Ese correo ya está registrado."}"""))
            val taken = repository.register(RegisterInput("Julian", "j@correo.com", "julianb", "clave-segura")).exceptionOrNull()
            assertEquals(AuthFailure.Taken(AuthField.Email, "Ese correo ya está registrado."), taken)

            server.enqueue(json(400, """{"code":"FIN-VALIDATION","errors":[{"field":"username","message":"Muy corto"}]}"""))
            val invalid = repository.register(RegisterInput("Julian", "j@correo.com", "jb", "clave-segura")).exceptionOrNull()
            assertEquals(AuthFailure.Invalid(mapOf(AuthField.Username to "Muy corto")), invalid)

            server.enqueue(json(500, """{"code":"FIN-INTERNAL"}"""))
            assertEquals(AuthFailure.Unexpected, repository.login(LoginInput("julianb", "x")).exceptionOrNull())
        }

    @Test
    fun `no connection is its own failure`() =
        runTest {
            server.enqueue(MockResponse.Builder().onRequestStart(SocketEffect.CloseSocket()).build())
            assertEquals(AuthFailure.NoConnection, repository.login(LoginInput("julianb", "x")).exceptionOrNull())
        }

    @Test
    fun `logout clears the session even when the server cannot be reached`() =
        runTest {
            store.save(StoredSession("a", "r-0123456789012345678", TEST_USER))
            server.close()

            repository.logout()

            assertNull(store.current())
            assertEquals(SessionState.SignedOut, repository.observeSession().first())
        }

    private fun json(
        code: Int,
        body: String,
    ) = MockResponse
        .Builder()
        .code(code)
        .addHeader("Content-Type", "application/problem+json")
        .body(body)
        .build()
}
