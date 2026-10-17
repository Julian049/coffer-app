package com.dev.julian09.financeapp.ui.feature.auth

import com.dev.julian09.financeapp.domain.model.AuthFailure
import com.dev.julian09.financeapp.domain.model.AuthField
import com.dev.julian09.financeapp.domain.usecase.Login
import com.dev.julian09.financeapp.domain.usecase.RegisterUser
import com.dev.julian09.financeapp.ui.FakeAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelsTest {
    private val repository = FakeAuthRepository()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `login shows field errors without calling the server`() {
        val vm = LoginViewModel(Login(repository))
        vm.submit()

        assertEquals(setOf(AuthField.Identifier, AuthField.Password), vm.state.value.errors.keys)
        assertEquals(0, repository.calls)
    }

    @Test
    fun `wrong credentials keep the identifier and clear the password`() {
        repository.next = Result.failure(AuthFailure.WrongCredentials)
        val vm = LoginViewModel(Login(repository))
        vm.onIdentifier("julian@correo.com")
        vm.onPassword("mala-clave")
        vm.submit()

        with(vm.state.value) {
            assertEquals("Usuario o contraseña incorrectos.", formError)
            assertEquals("julian@correo.com", identifier)
            assertEquals("", password)
            assertFalse(loading)
        }
    }

    @Test
    fun `too many attempts says how long to wait`() {
        repository.next = Result.failure(AuthFailure.TooManyAttempts(42))
        val vm = LoginViewModel(Login(repository))
        vm.onIdentifier("julianb")
        vm.onPassword("x")
        vm.submit()

        assertEquals("Demasiados intentos. Espera 42 s y vuelve a intentarlo.", vm.state.value.formError)
    }

    @Test
    fun `editing a field clears its error`() {
        val vm = LoginViewModel(Login(repository))
        vm.submit()
        vm.onIdentifier("julianb")

        assertEquals(setOf(AuthField.Password), vm.state.value.errors.keys)
    }

    @Test
    fun `expired session pre-fills the username`() {
        val vm = LoginViewModel(Login(repository))
        vm.prefill("julianb")
        assertEquals("julianb", vm.state.value.identifier)
    }

    @Test
    fun `register puts a taken email next to the email field`() {
        repository.next = Result.failure(AuthFailure.Taken(AuthField.Email, "Ese correo ya está registrado."))
        val vm = RegisterViewModel(RegisterUser(repository))
        vm.onChange(AuthField.DisplayName, "Julian")
        vm.onChange(AuthField.Email, "julian@correo.com")
        vm.onChange(AuthField.Username, "JulianB ")
        vm.onChange(AuthField.Password, "clave-segura")
        vm.submit()

        assertEquals("julianb", vm.state.value.username)
        assertEquals(mapOf(AuthField.Email to "Ese correo ya está registrado."), vm.state.value.errors)
        assertNull(vm.state.value.formError)
    }

    @Test
    fun `no connection is a form message`() {
        repository.next = Result.failure(AuthFailure.NoConnection)
        val vm = RegisterViewModel(RegisterUser(repository))
        listOf(
            AuthField.DisplayName to "Julian",
            AuthField.Email to "j@c.co",
            AuthField.Username to "julianb",
            AuthField.Password to "clave-segura",
        ).forEach { (field, value) -> vm.onChange(field, value) }
        vm.submit()

        assertEquals("Sin conexión. Revisa tu internet y vuelve a intentarlo.", vm.state.value.formError)
    }
}
