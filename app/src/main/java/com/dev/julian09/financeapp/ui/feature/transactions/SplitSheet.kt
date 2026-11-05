package com.dev.julian09.financeapp.ui.feature.transactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dev.julian09.financeapp.domain.model.AccountBalance
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.usecase.SplitPart
import com.dev.julian09.financeapp.domain.usecase.halves
import com.dev.julian09.financeapp.ui.components.AppBottomSheet
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppTextField
import com.dev.julian09.financeapp.ui.components.PesosTransformation
import com.dev.julian09.financeapp.ui.components.SegmentedSelector
import com.dev.julian09.financeapp.ui.components.accountIcon
import com.dev.julian09.financeapp.ui.components.formatMoney
import com.dev.julian09.financeapp.ui.theme.CofferShapes
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import java.util.UUID

private class CustomPart(
    val id: Int,
    val digits: String = "",
    val accountId: UUID? = null,
)

/**
 * Hoja · Dividir (CAMBIOS-UI-2.4 §6.2): «Mitad» or «Personalizado». [accounts] are the chips where the money came back; the
 * button only wakes when the parts add up to [total] and each part that is not the user's has its account.
 */
@Composable
fun SplitSheet(
    total: Money,
    title: String,
    payerName: String,
    accounts: List<AccountBalance>,
    onConfirm: (List<SplitPart>) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = CofferTheme.colors
    var mode by remember { mutableStateOf(0) }
    var halfAccount by remember { mutableStateOf<UUID?>(null) }
    var nextId by remember { mutableStateOf(3) }
    var parts by remember { mutableStateOf(listOf(CustomPart(1), CustomPart(2))) }
    val (half, rest) = halves(total)
    val typed = parts.map { Money.ofPesos(it.digits.toLongOrNull() ?: 0) }
    val sum = typed.fold(Money.ZERO) { s, m -> s + m }
    val missing = total - sum
    val custom =
        parts.mapIndexed { i, p -> SplitPart(typed[i], p.accountId) }
    val ready =
        if (mode == 0) {
            halfAccount != null
        } else {
            missing == Money.ZERO && typed.all { it > Money.ZERO } && custom.drop(1).all { it.accountId != null }
        }
    AppBottomSheet(
        onDismiss = onDismiss,
        title = "Dividir",
        subtitle = "$title · ${formatMoney(total)} · $payerName",
        actions = {
            AppButton(
                "Dividir",
                onClick = { onConfirm(if (mode == 0) listOf(SplitPart(half), SplitPart(rest, halfAccount)) else custom) },
                enabled = ready,
                icon = Icons.Outlined.Check,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        SegmentedSelector(listOf("Mitad", "Personalizado"), mode, { mode = it }, Modifier.fillMaxWidth().height(40.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(CofferShapes.card)
                .background(colors.surface)
                .border(BorderStroke(1.dp, colors.border), CofferShapes.card),
        ) {
            if (mode == 0) {
                PartRow("Lo que pagué yo", formatMoney(half))
                HorizontalDivider(color = colors.border)
                PartRow("Lo que me pasaron", formatMoney(rest))
                AccountPicker(accounts, halfAccount) { halfAccount = it }
            } else {
                parts.forEachIndexed { i, p ->
                    if (i > 0) HorizontalDivider(color = colors.border)
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        AppTextField(
                            value = p.digits,
                            onValueChange = { v ->
                                val clean = v.filter(Char::isDigit).take(12)
                                // A part cannot go past what is left of the total
                                val others = typed.filterIndexed { j, _ -> j != i }.fold(Money.ZERO) { s, m -> s + m }
                                val ok = (clean.toLongOrNull() ?: 0).let { Money.ofPesos(it) + others <= total }
                                if (ok) parts = parts.map { if (it.id == p.id) CustomPart(it.id, clean, it.accountId) else it }
                            },
                            label = if (i == 0) "Lo que pagué yo" else "Parte ${i + 1}",
                            keyboardType = KeyboardType.Number,
                            visualTransformation = PesosTransformation(),
                        )
                        if (i > 0) {
                            AccountPicker(accounts, p.accountId) { acc ->
                                parts = parts.map { if (it.id == p.id) CustomPart(it.id, it.digits, acc) else it }
                            }
                        }
                    }
                }
                HorizontalDivider(color = colors.border)
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "+ Otra parte",
                        Modifier
                            .weight(1f)
                            .clickable(role = Role.Button) {
                                parts = parts + CustomPart(nextId++)
                            }.padding(vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primary,
                    )
                    Text("Falta ${formatMoney(missing)}", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun PartRow(
    label: String,
    value: String,
) {
    val colors = CofferTheme.colors
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
        Text(value, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
    }
}

/** The account chips (40 dp, sliding sideways) where the money came back. */
@Composable
private fun AccountPicker(
    accounts: List<AccountBalance>,
    selected: UUID?,
    onSelect: (UUID) -> Unit,
) {
    val colors = CofferTheme.colors
    LazyRow(
        Modifier.padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        items(accounts.filter { it.account.active && it.account.type.isPickable }, key = { it.account.id }) { item ->
            val on = item.account.id == selected
            Row(
                Modifier
                    .height(40.dp)
                    .clip(CofferShapes.pill)
                    .background(if (on) colors.primaryContainer else colors.surface)
                    .border(1.dp, if (on) colors.primary else colors.border, CofferShapes.pill)
                    .selectable(on, role = Role.RadioButton) { onSelect(item.account.id) }
                    .padding(start = 4.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier.size(32.dp).background(if (on) colors.surface else colors.iconContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        accountIcon(item.account.icon, item.account.type),
                        null,
                        tint = colors.iconOnContainer,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(item.account.name, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary, maxLines = 1)
            }
        }
    }
}
