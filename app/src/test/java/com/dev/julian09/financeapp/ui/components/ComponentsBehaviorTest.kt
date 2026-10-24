package com.dev.julian09.financeapp.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.dev.julian09.financeapp.domain.finance.FoxMood
import com.dev.julian09.financeapp.domain.finance.FoxState
import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.AccountType
import com.dev.julian09.financeapp.domain.model.AuthField
import com.dev.julian09.financeapp.domain.model.Category
import com.dev.julian09.financeapp.domain.model.CategoryKind
import com.dev.julian09.financeapp.domain.model.CategoryRef
import com.dev.julian09.financeapp.domain.model.CategoryResolution
import com.dev.julian09.financeapp.domain.model.ImportPreview
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.usecase.HomeDashboard
import com.dev.julian09.financeapp.ui.feature.auth.LoginContent
import com.dev.julian09.financeapp.ui.feature.auth.LoginUiState
import com.dev.julian09.financeapp.ui.feature.home.BalanceView
import com.dev.julian09.financeapp.ui.feature.home.HomeContent
import com.dev.julian09.financeapp.ui.feature.home.HomeUiState
import com.dev.julian09.financeapp.ui.feature.importexport.ImportCatalog
import com.dev.julian09.financeapp.ui.feature.importexport.ImportExportActions
import com.dev.julian09.financeapp.ui.feature.importexport.ImportExportContent
import com.dev.julian09.financeapp.ui.feature.importexport.ImportExportState
import com.dev.julian09.financeapp.ui.feature.importexport.ImportStep
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
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ComponentsBehaviorTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the eye hides and shows the balance of Home`() {
        val dashboard =
            HomeDashboard(
                available = Money.ofPesos(1_000_000),
                monthlyChange = null,
                previousMonth = YearMonth.of(2026, 9),
                accounts = emptyList(),
                recent = emptyList(),
                fox = FoxMood(FoxState.NORMAL),
            )
        compose.setContent { CofferTheme { HomeContent(HomeUiState(dashboard, emptyList()), {}, {}, {}) } }
        compose.onNodeWithText("$1.000.000").assertIsDisplayed()
        compose.onNodeWithContentDescription("Ocultar el saldo").performClick()
        compose.onNodeWithText("$ ••••••").assertIsDisplayed()
        compose.onNodeWithContentDescription("Mostrar el saldo").performClick()
        compose.onNodeWithText("$1.000.000").assertIsDisplayed()
    }

    @Test
    fun `the transaction type tabs keep every label on one line at 360 dp`() {
        // BUG-01: «Transferencia» with its icon wrapped its last letter
        compose.setContent {
            CofferTheme {
                androidx.compose.foundation.layout.Box(
                    androidx.compose.ui.Modifier
                        .width(328.dp),
                ) {
                    SegmentedSelector(
                        listOf("Gasto", "Ingreso", "Transferencia"),
                        selectedIndex = 2,
                        onSelect = {},
                        modifier =
                            androidx.compose.ui.Modifier
                                .fillMaxWidth(),
                        icons =
                            listOf(
                                androidx.compose.material.icons.Icons.Outlined.ArrowDownward,
                                androidx.compose.material.icons.Icons.Outlined.ArrowUpward,
                                androidx.compose.material.icons.Icons.Outlined.SwapHoriz,
                            ),
                    )
                }
            }
        }
        val one =
            compose
                .onNodeWithText("Gasto")
                .fetchSemanticsNode()
                .size.height
        assertEquals(
            one,
            compose
                .onNodeWithText("Transferencia")
                .fetchSemanticsNode()
                .size.height,
        )
    }

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
    fun `a long bottom sheet scrolls to its button`() {
        // #110: with many categories Nueva categoría pushed "Crear categoría" off the screen
        var saved = false
        compose.setContent {
            CofferTheme {
                AppBottomSheet(onDismiss = {}, title = "Nueva categoría") {
                    repeat(40) { Text("Subcategoría de Categoría $it") }
                    AppButton("Crear categoría", onClick = { saved = true })
                }
            }
        }
        compose
            .onNodeWithText("Crear categoría")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        assertTrue(saved)
    }

    @Test
    fun `associating an imported category with an existing one opens the picker`() {
        // #114: the picker nested a second vertical scroll inside the sheet and the app closed
        val dining = CategoryRef(CategoryKind.EXPENSE, "Dining")
        val food = Category(UUID(2, 1), "Comida", CategoryKind.EXPENSE)
        var chosen: CategoryResolution? = null
        compose.setContent {
            CofferTheme {
                ImportExportContent(
                    ImportExportState(
                        step = ImportStep.REVIEW,
                        fileName = "cashew.csv",
                        preview =
                            ImportPreview(
                                headers = listOf("amount"),
                                mapping = emptyMap(),
                                sample = emptyList(),
                                valid = 1,
                                errors = emptyList(),
                                errorCount = 0,
                                duplicates = 0,
                                unknownAccounts = emptyList(),
                                unknownCategories = listOf(dining),
                            ),
                        categories = mapOf(dining to CategoryResolution.Create),
                    ),
                    ImportCatalog(categories = listOf(food)),
                    ImportExportActions(onCategory = { _, resolution -> chosen = resolution }),
                    zone = ZoneOffset.UTC,
                )
            }
        }
        compose.onNodeWithText("Asociar a una existente").performScrollTo().performClick()
        compose.onNodeWithText("Comida").assertIsDisplayed().performClick()
        assertEquals(CategoryResolution.Use(food.id), chosen)
    }

    @Test
    fun `tapping the balance shows both views and picks the one for Home`() {
        // HU-04: $1.000.000 available and $800.000 of card due this period
        var chosen: BalanceView? = null
        val dashboard =
            HomeDashboard(
                available = Money.ofPesos(1_000_000),
                monthlyChange = null,
                previousMonth = YearMonth.of(2026, 9),
                accounts = emptyList(),
                recent = emptyList(),
                fox = FoxMood(FoxState.NORMAL),
                net = Money.ofPesos(200_000),
            )
        compose.setContent {
            CofferTheme {
                HomeContent(HomeUiState(dashboard, emptyList()), {}, {}, {}, onBalanceView = { chosen = it })
            }
        }
        compose.onNodeWithText("Saldo total disponible").performClick()
        compose.onNodeWithText("Saldo neto").assertIsDisplayed()
        compose.onNodeWithText("$200.000").assertIsDisplayed()
        // Hoja · Detalle del saldo (S15-UI11): a tap on the card picks it, with no «Listo»
        compose.onNodeWithText("Saldo neto").performScrollTo().performClick()
        assertEquals(BalanceView.NET, chosen)
    }

    @Test
    fun `the import warns about opening balances that the history would add again`() {
        // #67: a card created with its debt gets it twice once the purchases behind it are imported
        val visa = Account(UUID(1, 1), "Visa", AccountType.CREDIT_CARD, Money.ofPesos(800_000), includeInTotal = false, active = true)
        val cash = Account(UUID(1, 2), "Efectivo", AccountType.CASH, Money.ZERO, includeInTotal = true, active = true)
        compose.setContent {
            CofferTheme {
                ImportExportContent(
                    ImportExportState(
                        step = ImportStep.REVIEW,
                        fileName = "cashew.csv",
                        preview =
                            ImportPreview(
                                headers = listOf("amount"),
                                mapping = emptyMap(),
                                sample = emptyList(),
                                valid = 1,
                                errors = emptyList(),
                                errorCount = 0,
                                duplicates = 0,
                                unknownAccounts = emptyList(),
                                unknownCategories = emptyList(),
                            ),
                    ),
                    ImportCatalog(accounts = listOf(visa, cash)),
                    ImportExportActions(),
                    zone = ZoneOffset.UTC,
                )
            }
        }
        compose.onNodeWithText("Revisa los saldos iniciales").assertIsDisplayed()
        compose.onNodeWithText("Visa", substring = true).assertIsDisplayed()
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
