package com.dev.julian09.financeapp.legacy.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dev.julian09.financeapp.ui.theme.ExpenseRed
import com.dev.julian09.financeapp.ui.theme.IncomeGreen
import com.dev.julian09.financeapp.ui.theme.PendingSync
import com.dev.julian09.financeapp.ui.theme.SyncedCloud
import com.dev.julian09.financeapp.ui.theme.TextPrimary
import com.dev.julian09.financeapp.ui.theme.TextSecondary

@Composable
fun TransactionItem(
    title: String,
    date: String,
    status: Boolean,
    value: Double,
) {
    Row(
        modifier =
            Modifier
                .padding(all = 8.dp)
                .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                title,
                Modifier.padding(bottom = 4.dp),
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
            )
            Row {
                Text(date, color = TextSecondary)
            }
        }

        var color: Color?
        if (!status) {
            color = PendingSync
        } else {
            color = SyncedCloud
        }

        Box(
            modifier =
                Modifier
                    .padding(top = 4.dp)
                    .size(9.dp)
                    .background(color = color, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {}
        var valueToAssign: String
        var colorToValue: Color
        if (value > 0) {
            valueToAssign = "+$value"
            colorToValue = IncomeGreen
        } else {
            valueToAssign = "$value"
            colorToValue = ExpenseRed
        }
        Text(
            valueToAssign,
            fontWeight = FontWeight.Bold,
            color = colorToValue,
        )
    }
}

@Preview(showBackground = true, name = "Gasto Negativo")
@Composable
fun PreviewTransactionNegativeFalse() {
    // Escenario: Un gasto común (egreso)
    TransactionItem("Compra Supermercado", "20-03-2026", false, -450.50)
}

@Preview(showBackground = true, name = "Ingreso Positivo")
@Composable
fun PreviewTransactionPositiveTrue() {
    // Escenario: Un ingreso de dinero (nómina/regalo)
    TransactionItem("Salario Quincenal", "15-03-2026", true, 2500.0)
}

@Preview(showBackground = true, name = "Reembolso (Positivo/False)")
@Composable
fun PreviewTransactionPositiveFalse() {
    // Escenario: Dinero que entra pero quizás marcado como "no verificado" o un ajuste
    TransactionItem("Reembolso Amazon", "22-03-2026", false, 89.99)
}

@Preview(showBackground = true, name = "Cobro Pendiente (Negativo/True)")
@Composable
fun PreviewTransactionNegativeTrue() {
    // Escenario: Un cargo que está marcado como "completado" o "prioritario"
    TransactionItem("Suscripción Netflix", "01-03-2026", true, -15.99)
}
