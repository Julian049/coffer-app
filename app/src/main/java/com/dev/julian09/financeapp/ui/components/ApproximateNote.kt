package com.dev.julian09.financeapp.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dev.julian09.financeapp.ui.theme.CofferTheme

const val CARD_APPROXIMATE_NOTE =
    "Los valores son aproximados, no los uses como el cobro exacto. El banco puede mover el corte o el pago uno o dos días " +
        "(por ejemplo, si cae en fin de semana) y calcular distinto; confirma siempre en tu extracto."

const val LOAN_APPROXIMATE_NOTE =
    "La cuota es una estimación según los datos que ingresaste. El banco puede facturar valores o fechas distintos; " +
        "confirma siempre con tu plan de pagos."

/** The small closing note of the forms and payment sheets of cards and loans: what the app shows is an estimate. */
@Composable
fun ApproximateNote(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = CofferTheme.colors.textSecondary, modifier = modifier)
}
