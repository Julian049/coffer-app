package com.dev.julian09.financeapp.ui.feature.mail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.AccountType
import com.dev.julian09.financeapp.domain.model.MailProviders
import com.dev.julian09.financeapp.domain.model.MailRule
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.usecase.MailProviderLine
import com.dev.julian09.financeapp.domain.usecase.MailProvidersOverview
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.UUID

/** changes-ui-2.3 §3: Proveedores de correo (196:437) and Hoja · Asociar tarjeta de crédito (196:655), at 390 × 844. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class MailProvidersScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val visa =
        Account(UUID(1, 1), "Tarjeta Visa", AccountType.CREDIT_CARD, Money.ZERO, institution = "Bancolombia", lastDigits = "4821")
    private val master = Account(UUID(1, 2), "Tarjeta Mastercard", AccountType.CREDIT_CARD, Money.ZERO, institution = "Bancolombia")
    private val savings = Account(UUID(1, 3), "Bancolombia", AccountType.SAVINGS, Money.ZERO, institution = "Bancolombia")

    private val overview =
        MailProvidersOverview(
            MailProviders.all.map { p ->
                val (rule, account) =
                    when (p.source) {
                        "UBER" -> MailRule(UUID(2, 1), p.source, null, visa.id) to visa
                        "BANCOLOMBIA" -> MailRule(UUID(2, 2), p.source, null, savings.id) to savings
                        "BANCOLOMBIA_TC" -> MailRule(UUID(2, 3), p.source, "4821", visa.id) to visa
                        else -> null to null
                    }
                MailProviderLine(p, rule, account)
            },
            listOf(visa, master, savings),
        )

    @Test
    fun providersLight() = capture("mail_providers_light", dark = false) { MailProvidersContent(overview, onBack = {}, onOpen = {}) }

    @Test
    fun providersDark() = capture("mail_providers_dark", dark = true) { MailProvidersContent(overview, onBack = {}, onOpen = {}) }

    @Test
    fun cardSheetLight() = capture("mail_card_sheet_light", dark = false) { CardSheet() }

    @Test
    fun cardSheetDark() = capture("mail_card_sheet_dark", dark = true) { CardSheet() }

    @Test
    fun theSummaryNamesTwoAndCountsTheRest() {
        assertEquals("3 de 5 asociados · Uber, Bancolombia y más", overview.summary)
        assertEquals(listOf(visa, master), overview.choicesFor(MailProviders.of("RAPPICARD")!!))
        assertEquals(listOf(savings), overview.choicesFor(MailProviders.of("RAPPICUENTA")!!))
    }

    @Composable
    private fun CardSheet() {
        val provider = MailProviders.of("BANCOLOMBIA_TC")!!
        val sheet = MailProviderSheet(provider, accountId = visa.id, digits = "4821", associated = true)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            MailProviderSheetBody(sheet, overview.choicesFor(provider), onChoose = {}, onDigits = {})
            MailProviderSheetActions(sheet, onSave = {}, onUnlink = {})
        }
    }

    private fun capture(
        name: String,
        dark: Boolean,
        content: @Composable () -> Unit,
    ) {
        compose.setContent {
            CofferTheme(darkTheme = dark) {
                Column(Modifier.fillMaxSize().background(CofferTheme.colors.background)) { content() }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }
}
