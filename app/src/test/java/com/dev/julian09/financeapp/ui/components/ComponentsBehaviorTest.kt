package com.dev.julian09.financeapp.ui.components

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dev.julian09.financeapp.domain.model.AuthField
import com.dev.julian09.financeapp.ui.feature.auth.LoginContent
import com.dev.julian09.financeapp.ui.feature.auth.LoginUiState
import com.dev.julian09.financeapp.ui.feature.settings.ServerState
import com.dev.julian09.financeapp.ui.feature.settings.SettingsContent
import com.dev.julian09.financeapp.ui.feature.settings.SettingsUiState
import com.dev.julian09.financeapp.ui.navigation.BottomBar
import com.dev.julian09.financeapp.ui.navigation.Tab
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ComponentsBehaviorTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `bottom bar marks the active tab and reports taps`() {
        var tapped: Tab? = null
        compose.setContent { CofferTheme { BottomBar(selected = Tab.History, onSelect = { tapped = it }) } }

        compose.onNodeWithText("Historial").assertIsSelected()
        compose.onNodeWithText("Inicio").assertIsNotSelected()
        compose.onNodeWithText("Cuentas").performClick()
        assertEquals(Tab.Accounts, tapped)
    }

    @Test
    fun `chip exposes its selection`() {
        compose.setContent { CofferTheme { AppChip("Este mes", selected = true, onClick = {}) } }
        compose.onNodeWithText("Este mes").assertIsSelected()
    }

    @Test
    fun `settings offers a retry only when the server is down`() {
        var retried = false
        compose.setContent {
            CofferTheme {
                SettingsContent(SettingsUiState("0.2.0", ServerState.Unavailable), onBack = {}, onRetry = { retried = true })
            }
        }
        compose.onNodeWithText("Servidor: no disponible").assertExists()
        compose.onNodeWithText("Versión 0.2.0").assertExists()
        compose.onNodeWithText("Reintentar").performClick()
        assertTrue(retried)
    }

    @Test
    fun `login shows the error under each empty field`() {
        compose.setContent {
            CofferTheme {
                LoginContent(
                    LoginUiState(
                        errors =
                            mapOf(
                                AuthField.Identifier to "Escribe tu usuario o correo",
                                AuthField.Password to "Escribe tu contraseña",
                            ),
                    ),
                    sessionExpired = true,
                    onIdentifier = {},
                    onPassword = {},
                    onSubmit = {},
                    onRegister = {},
                )
            }
        }
        compose.onNodeWithText("Escribe tu usuario o correo").assertExists()
        compose.onNodeWithText("Escribe tu contraseña").assertExists()
        compose.onNodeWithText("Tu sesión terminó. Inicia sesión de nuevo; tus datos siguen en este teléfono.").assertExists()
        compose.onNodeWithText("Continuar con Google").assertIsNotEnabled()
    }

    @Test
    fun `settings shows the server as available`() {
        compose.setContent { CofferTheme { SettingsContent(SettingsUiState("0.2.0", ServerState.Available), onBack = {}, onRetry = {}) } }
        compose.onNodeWithText("Servidor: disponible").assertExists()
        compose.onNodeWithText("Reintentar").assertDoesNotExist()
    }
}
