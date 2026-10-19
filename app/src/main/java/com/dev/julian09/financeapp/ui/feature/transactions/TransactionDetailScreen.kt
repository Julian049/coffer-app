package com.dev.julian09.financeapp.ui.feature.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.ButtonKind
import com.dev.julian09.financeapp.ui.components.ConfirmDialog
import com.dev.julian09.financeapp.ui.components.MoneyKind
import com.dev.julian09.financeapp.ui.components.MoneyText
import com.dev.julian09.financeapp.ui.components.TransferIcon
import com.dev.julian09.financeapp.ui.components.categoryIcon
import com.dev.julian09.financeapp.ui.components.formatMoney
import com.dev.julian09.financeapp.ui.theme.CofferShapes
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TransactionDetailScreen(
    onBack: () -> Unit,
    onEdit: (TransactionDetail) -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel(),
) {
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    LaunchedEffect(deleted) { if (deleted) onBack() }
    TransactionDetailContent(detail, onBack, onEdit, viewModel::confirmDelete)
}

private val CREATED = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-CO"))

fun kindOf(type: TransactionType) =
    when (type) {
        TransactionType.EXPENSE -> MoneyKind.Expense
        TransactionType.INCOME -> MoneyKind.Income
        TransactionType.TRANSFER -> MoneyKind.Transfer
    }

private fun typeLabel(type: TransactionType) =
    when (type) {
        TransactionType.EXPENSE -> "Gasto"
        TransactionType.INCOME -> "Ingreso"
        TransactionType.TRANSFER -> "Transferencia"
    }

/** Detalle de transacción (S5-UI04); deleting asks first and is direct (REQ-TRX-07). Restoring arrives in S11. */
@Composable
fun TransactionDetailContent(
    detail: TransactionDetail?,
    onBack: () -> Unit,
    onEdit: (TransactionDetail) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
) {
    val colors = CofferTheme.colors
    var confirming by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxSize()) {
        AppHeader(title = "Detalle", onBack = onBack)
        val d = detail ?: return@Column
        val t = d.transaction
        val title = t.note ?: d.categoryLabel?.substringAfterLast(" > ") ?: typeLabel(t.type)
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AppCard(Modifier.fillMaxWidth(), highlight = true) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(Modifier.size(48.dp).background(colors.iconContainer, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(
                            if (t.type == TransactionType.TRANSFER) TransferIcon else categoryIcon(d.category?.icon),
                            contentDescription = null,
                            tint = colors.iconOnContainer,
                        )
                    }
                    Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, textAlign = TextAlign.Center)
                    MoneyText(t.amount, kind = kindOf(t.type), style = MaterialTheme.typography.displaySmall)
                    Surface(shape = CofferShapes.pill, color = colors.background) {
                        Text(
                            typeLabel(t.type),
                            Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
            AppCard {
                if (t.type == TransactionType.TRANSFER) {
                    Line("Origen", d.accountName)
                    Line("Destino", d.destinationName ?: "Cuenta")
                } else {
                    Line("Categoría", d.categoryLabel ?: "Sin categoría")
                    Line("Cuenta", d.accountName)
                }
                Line("Fecha y hora", dateTimeLabel(t.occurredAt, today, zone))
                if (t.installments > 1) Line("Cuotas", "${t.installments} cuotas")
                paymentKindLabel(t.paymentKind)?.let { Line("Tipo de pago", it) }
                d.tagName?.let { Line("Etiqueta", "#$it") }
                t.note?.let { Line("Nota", it) }
            }
            if (t.principal != null) PaymentBreakdownCard(t)
            val created =
                t.createdAt
                    ?.let {
                        " · creada el " +
                            CREATED
                                .format(
                                    it.atZone(zone),
                                ).replace(".", "")
                    }.orEmpty()
            Text(
                (if (t.pendingSync) "Pendiente de sincronizar" else "Sincronizada") + created,
                Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            // A plan or a payment split cannot follow an edit: they are deleted and registered again
            val editable = t.installments <= 1 && t.paymentKind == null
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (editable) {
                    AppButton(
                        "Editar",
                        onClick = { onEdit(d) },
                        kind = ButtonKind.Secondary,
                        icon = Icons.Outlined.Edit,
                        modifier = Modifier.weight(1f),
                    )
                }
                AppButton(
                    "Eliminar",
                    onClick = { confirming = true },
                    kind = ButtonKind.Danger,
                    icon = Icons.Outlined.DeleteOutline,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (confirming) {
            ConfirmDialog(
                title = "¿Eliminar esta transacción?",
                message = "$title · ${formatMoney(t.amount, kindOf(t.type))}.",
                confirmText = "Eliminar",
                onConfirm = {
                    confirming = false
                    onDelete()
                },
                onDismiss = { confirming = false },
                destructive = true,
            )
        }
    }
}

@Composable
private fun Line(
    label: String,
    value: String,
) {
    val colors = CofferTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary, textAlign = TextAlign.End)
    }
}

private fun paymentKindLabel(kind: String?) =
    when (kind) {
        "CARD_MIN" -> "Pago mínimo"
        "CARD_TOTAL" -> "Pago total"
        else -> null
    }

/** Detalle de transacción · Pago de tarjeta (S7-UI03): only the principal lowers the debt (REQ-TC-11). */
@Composable
private fun PaymentBreakdownCard(t: Transaction) {
    val colors = CofferTheme.colors
    AppCard {
        Text("Desglose del pago", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
        Line("Capital", formatMoney(t.principal ?: Money.ZERO))
        Line("Intereses", formatMoney(t.interest ?: Money.ZERO))
        Line("Otros cargos", formatMoney(t.fees ?: Money.ZERO))
        Line("Total", formatMoney(t.amount))
    }
    Text(
        "Intereses y cargos suman en la categoría «Intereses y cargos financieros» en la fecha de pago.",
        style = MaterialTheme.typography.bodySmall,
        color = colors.textSecondary,
    )
}
