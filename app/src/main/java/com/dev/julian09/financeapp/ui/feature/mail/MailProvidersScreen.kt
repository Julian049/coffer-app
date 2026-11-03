package com.dev.julian09.financeapp.ui.feature.mail

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.R
import com.dev.julian09.financeapp.domain.model.Account
import com.dev.julian09.financeapp.domain.model.AccountType
import com.dev.julian09.financeapp.domain.model.MailProvider
import com.dev.julian09.financeapp.domain.model.MailProviderKind
import com.dev.julian09.financeapp.domain.usecase.MailProviderLine
import com.dev.julian09.financeapp.domain.usecase.MailProvidersOverview
import com.dev.julian09.financeapp.ui.components.AppBottomSheet
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.ButtonKind
import com.dev.julian09.financeapp.ui.components.accountIcon
import com.dev.julian09.financeapp.ui.components.accountTypeShortLabel
import com.dev.julian09.financeapp.ui.theme.CofferShapes
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import java.util.UUID

/** The logo of each provider in the catalog; a provider without one shows its initial (changes-ui-2.3 §3). */
@DrawableRes
internal fun mailProviderLogo(source: String): Int? =
    when (source) {
        "UBER" -> R.drawable.mail_logo_uber
        "BANCOLOMBIA", "BANCOLOMBIA_TC" -> R.drawable.mail_logo_bancolombia
        "RAPPICUENTA" -> R.drawable.mail_logo_rappicuenta
        "RAPPICARD" -> R.drawable.mail_logo_rappicard
        else -> null
    }

@Composable
fun MailProvidersScreen(
    onBack: () -> Unit,
    viewModel: MailProvidersViewModel = hiltViewModel(),
) {
    val overview by viewModel.overview.collectAsStateWithLifecycle()
    val sheet by viewModel.sheet.collectAsStateWithLifecycle()
    MailProvidersContent(overview, onBack, viewModel::open)
    val current = overview
    sheet?.let { s ->
        if (current != null) {
            MailProviderSheetContent(
                s,
                current.choicesFor(s.provider),
                onChoose = viewModel::choose,
                onDigits = viewModel::digits,
                onSave = viewModel::confirm,
                onUnlink = viewModel::unlink,
                onDismiss = viewModel::dismiss,
            )
        }
    }
}

/** Proveedores de correo (Figma 196:437, REQ-COR-11). */
@Composable
fun MailProvidersContent(
    overview: MailProvidersOverview?,
    onBack: () -> Unit,
    onOpen: (MailProvider) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CofferTheme.colors
    Column(modifier.fillMaxSize()) {
        AppHeader(title = "Proveedores de correo", onBack = onBack)
        if (overview == null) return@Column
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Cuando llega un correo de un proveedor, el movimiento queda por revisar en la cuenta que asocies.",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = colors.textSecondary,
            )
            val (linked, unlinked) = overview.lines.partition { it.associated }
            ProviderGroup("Asociados · ${linked.size}", linked, onOpen)
            ProviderGroup("Sin asociar · ${unlinked.size}", unlinked, onOpen)
            Text(
                "Por ahora estos son los proveedores disponibles. Iremos sumando más bancos y apps.",
                Modifier.fillMaxWidth().padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            // TODO(backlog 2.3, PO 2026-11-03): where a suggestion goes (a form or the support email) is still to be decided
            Text(
                "¿Falta el tuyo? Sugiere un proveedor · Próximamente",
                Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(top = 12.dp),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// Rows run edge to edge, split by thin lines (Figma: one card per group, never a card per row)
@Composable
private fun ListCard(content: @Composable () -> Unit) {
    val colors = CofferTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(CofferShapes.card)
            .background(colors.surface)
            .border(1.dp, colors.border, CofferShapes.card),
    ) { content() }
}

@Composable
private fun ProviderGroup(
    label: String,
    lines: List<MailProviderLine>,
    onOpen: (MailProvider) -> Unit,
) {
    if (lines.isEmpty()) return
    val colors = CofferTheme.colors
    Text(label, Modifier.padding(top = 16.dp), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
    ListCard {
        lines.forEachIndexed { i, line ->
            if (i > 0) HorizontalDivider(thickness = 1.dp, color = colors.border)
            ProviderRow(line) { onOpen(line.provider) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProviderRow(
    line: MailProviderLine,
    onClick: () -> Unit,
) {
    val colors = CofferTheme.colors
    val p = line.provider
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = "Asociar ${p.name}",
                onClick = onClick,
            ).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ProviderLogo(p, 40)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    p.name,
                    Modifier.align(Alignment.CenterVertically),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                )
                KindPill(p.kind, Modifier.align(Alignment.CenterVertically))
            }
            val account = line.account
            if (account != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    line.rule?.lastDigits?.let { Text("*$it", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary) }
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = "a",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(12.dp),
                    )
                    Icon(
                        accountIcon(account.icon, account.type),
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        account.name,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = colors.textPrimary,
                        maxLines = 1,
                    )
                }
            } else {
                Text(
                    if (p.asksCard) "Elige la tarjeta y sus últimos 4 dígitos" else "Elige en qué cuenta se registran",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary)
    }
}

/** The logo in a circle; card providers carry a small card badge at the bottom right. */
@Composable
private fun ProviderLogo(
    p: MailProvider,
    size: Int,
) {
    val colors = CofferTheme.colors
    Box(Modifier.size(size.dp)) {
        val logo = mailProviderLogo(p.source)
        if (logo != null) {
            Image(painterResource(logo), contentDescription = null, modifier = Modifier.size(size.dp).clip(CircleShape))
        } else {
            Box(Modifier.size(size.dp).background(colors.iconContainer, CircleShape), contentAlignment = Alignment.Center) {
                Text(p.name.take(1), style = MaterialTheme.typography.titleSmall, color = colors.iconOnContainer)
            }
        }
        if (p.asksCard) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .size(20.dp)
                    .background(colors.surface, CircleShape)
                    .border(1.dp, colors.border, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.CreditCard, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
private fun KindPill(
    kind: MailProviderKind,
    modifier: Modifier = Modifier,
) {
    val colors = CofferTheme.colors
    Row(
        modifier.background(colors.quickStrip, RoundedCornerShape(50)).padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            if (kind == MailProviderKind.BANK) Icons.Outlined.AccountBalance else Icons.Outlined.Storefront,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(11.dp),
        )
        Text(
            if (kind == MailProviderKind.BANK) "Banco" else "Comercio",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
            color = colors.textSecondary,
        )
    }
}

/** Hoja · Asociar proveedor (196:519) and Hoja · Asociar tarjeta de crédito (196:655). */
@Composable
internal fun MailProviderSheetContent(
    sheet: MailProviderSheet,
    choices: List<Account>,
    onChoose: (UUID) -> Unit,
    onDigits: (String) -> Unit,
    onSave: () -> Unit,
    onUnlink: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(onDismiss = onDismiss, actions = { MailProviderSheetActions(sheet, onSave, onUnlink) }) {
        MailProviderSheetBody(sheet, choices, onChoose, onDigits)
    }
}

@Composable
internal fun MailProviderSheetActions(
    sheet: MailProviderSheet,
    onSave: () -> Unit,
    onUnlink: () -> Unit,
) {
    AppButton("Guardar", onClick = onSave, icon = Icons.Outlined.Check, enabled = sheet.canSave, modifier = Modifier.fillMaxWidth())
    if (sheet.associated) {
        AppButton("Quitar asociación", onClick = onUnlink, kind = ButtonKind.Danger, modifier = Modifier.fillMaxWidth())
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MailProviderSheetBody(
    sheet: MailProviderSheet,
    choices: List<Account>,
    onChoose: (UUID) -> Unit,
    onDigits: (String) -> Unit,
) {
    val colors = CofferTheme.colors
    val p = sheet.provider
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        ProviderLogo(p, 48)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(p.name, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), color = colors.textPrimary)
                KindPill(p.kind, Modifier.align(Alignment.CenterVertically))
            }
            Text(p.covers, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
    }
    Text(
        if (p.asksCard) "¿Qué tarjeta es?" else "¿En qué cuenta se registran?",
        Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodySmall,
        color = colors.textSecondary,
    )
    if (choices.isEmpty()) {
        Text(
            if (p.asksCard) {
                "No tienes tarjetas de crédito activas. Créala en Cuentas y vuelve aquí."
            } else {
                "No tienes cuentas activas para asociar."
            },
            Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textPrimary,
        )
    } else {
        ListCard {
            choices.forEachIndexed { i, account ->
                if (i > 0) HorizontalDivider(thickness = 1.dp, color = colors.border)
                AccountChoice(account, account.id == sheet.accountId) { onChoose(account.id) }
            }
        }
    }
    if (p.asksCard) {
        Text(
            "Últimos 4 dígitos de la tarjeta",
            Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
        DigitBoxes(sheet.digits, onDigits)
        Text(
            "Con estos dígitos reconocemos los correos de esta tarjeta. Se toman de la cuenta; puedes cambiarlos.",
            Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun AccountChoice(
    account: Account,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = CofferTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (selected) colors.primaryContainer else colors.surface)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(32.dp).background(if (selected) colors.surface else colors.iconContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                accountIcon(account.icon, account.type),
                contentDescription = null,
                tint = colors.iconOnContainer,
                modifier = Modifier.size(16.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                account.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) colors.onPrimaryContainer else colors.textPrimary,
                maxLines = 1,
            )
            Text(accountDetail(account), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, maxLines = 1)
        }
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = colors.primary, unselectedColor = colors.borderHighlight),
        )
    }
}

// «Crédito · Bancolombia · *4821»: the type, the bank and the number, whatever the account has
private fun accountDetail(account: Account) =
    listOfNotNull(
        if (account.type == AccountType.CREDIT_CARD) "Crédito" else accountTypeShortLabel(account.type),
        account.institution?.takeIf { it.isNotBlank() },
        account.lastDigits?.let { "*$it" },
    ).joinToString(" · ")

/** «•••• ••••» and four 48 × 56 boxes over one hidden field, so the keyboard and paste behave as one input. */
@Composable
private fun DigitBoxes(
    digits: String,
    onDigits: (String) -> Unit,
) {
    val colors = CofferTheme.colors
    BasicTextField(
        value = digits,
        onValueChange = onDigits,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Últimos 4 dígitos de la tarjeta" },
        decorationBox = {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("•••• ••••", Modifier.padding(end = 6.dp), style = MaterialTheme.typography.titleMedium, color = colors.textSecondary)
                repeat(4) { i ->
                    val active = i == digits.length.coerceAtMost(3)
                    Box(
                        Modifier
                            .size(width = 48.dp, height = 56.dp)
                            .background(colors.surface, RoundedCornerShape(12.dp))
                            .border(if (active) 1.5.dp else 1.dp, if (active) colors.primary else colors.border, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            digits.getOrNull(i)?.toString().orEmpty(),
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Medium),
                            color = colors.textPrimary,
                        )
                    }
                }
            }
        },
    )
}
