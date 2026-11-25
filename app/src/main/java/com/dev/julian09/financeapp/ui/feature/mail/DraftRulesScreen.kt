package com.dev.julian09.financeapp.ui.feature.mail

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.model.RuleLevel
import com.dev.julian09.financeapp.domain.model.RuleOrigin
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.ReorderableColumn
import com.dev.julian09.financeapp.ui.components.formatMoney
import com.dev.julian09.financeapp.ui.components.iconOf
import com.dev.julian09.financeapp.ui.theme.CofferShapes
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import java.util.UUID

@Composable
fun DraftRulesScreen(
    onBack: () -> Unit,
    onNewRule: () -> Unit,
    onOpenRule: (UUID) -> Unit,
    viewModel: DraftRulesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DraftRulesContent(state, onBack, onNewRule, onOpenRule, viewModel::move)
}

/** Registro automático (Figma `257:689`, changes-ui-2.5 §2). */
@Composable
fun DraftRulesContent(
    state: DraftRulesState,
    onBack: () -> Unit,
    onNewRule: () -> Unit,
    onOpenRule: (UUID) -> Unit,
    onMove: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CofferTheme.colors
    Column(modifier.fillMaxSize()) {
        AppHeader(title = "Registro automático", onBack = onBack)
        if (!state.loaded) return@Column
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Si un movimiento que llega por correo cumple una regla, se registra directo en tu historial, sin pasar por «Por revisar».",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = colors.textSecondary,
            )
            NewRuleButton(onNewRule)
            if (state.sure.isEmpty() && state.probable.isEmpty()) {
                Text(
                    "Aún no tienes reglas. Crea una, o deja que Coffer la cree cuando un movimiento se repita.",
                    Modifier.fillMaxWidth().padding(top = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
                return@Column
            }
            if (state.sure.isNotEmpty()) {
                GroupLabel("Se registran directo · ${state.sure.size}")
                RulesCard {
                    ReorderableColumn(state.sure, key = { it.rule.id }, onMove = onMove) { row, handle ->
                        if (row != state.sure.first()) HorizontalDivider(thickness = 1.dp, color = colors.border)
                        RuleRow(row, state, onOpenRule) { handle() }
                    }
                }
            }
            if (state.probable.isNotEmpty()) {
                GroupLabel("Quedan por revisar · ${state.probable.size}")
                RulesCard {
                    state.probable.forEachIndexed { i, row ->
                        if (i > 0) HorizontalDivider(thickness = 1.dp, color = colors.border)
                        RuleRow(row, state, onOpenRule) {
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary)
                        }
                    }
                }
            }
            Footnotes(state)
        }
    }
}

/** 48 dp, white with a thin border, «+ Nueva regla» in walnut. */
@Composable
private fun NewRuleButton(onClick: () -> Unit) {
    val colors = CofferTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .height(48.dp)
            .clip(CofferShapes.card)
            .background(colors.surface)
            .border(1.dp, colors.border, CofferShapes.card)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Add, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
        Text(
            "Nueva regla",
            Modifier.padding(start = 6.dp),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
            color = colors.primary,
        )
    }
}

@Composable
private fun GroupLabel(text: String) =
    Text(text, Modifier.padding(top = 16.dp), style = MaterialTheme.typography.bodySmall, color = CofferTheme.colors.textSecondary)

@Composable
private fun RulesCard(content: @Composable () -> Unit) {
    val colors = CofferTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(CofferShapes.card)
            .background(colors.surface)
            .border(1.dp, colors.border, CofferShapes.card),
    ) { content() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RuleRow(
    row: DraftRuleRow,
    state: DraftRulesState,
    onOpen: (UUID) -> Unit,
    trailing: @Composable () -> Unit,
) {
    val colors = CofferTheme.colors
    val rule = row.rule
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Editar regla", onClick = { onOpen(rule.id) })
            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RuleCircle(
            row.category?.let { iconOf(it, state.categories) },
            sure = rule.level == RuleLevel.SURE,
            byCoffer = rule.origin == RuleOrigin.LEARNED,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    rule.note,
                    Modifier.align(Alignment.CenterVertically),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                )
                row.category?.let {
                    Text(
                        it.name,
                        Modifier
                            .align(Alignment.CenterVertically)
                            .background(colors.quickStrip, RoundedCornerShape(50))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
                        color = colors.textSecondary,
                    )
                }
            }
            Text(conditionsLine(row), style = MaterialTheme.typography.bodySmall, color = colors.textPrimary)
        }
        trailing()
    }
}

// «El Corral · $ 28.000 · Bancolombia»: what the rule asks for in the primary text, «Cualquier …» in the secondary one
@Composable
private fun conditionsLine(row: DraftRuleRow) =
    buildAnnotatedString {
        val secondary = SpanStyle(color = CofferTheme.colors.textSecondary)
        val rule = row.rule
        append(rule.merchant)
        withStyle(secondary) { append("  ·  ") }
        if (rule.amount != null) append(formatMoney(rule.amount, decimals = 0)) else withStyle(secondary) { append("Cualquier monto") }
        withStyle(secondary) { append("  ·  ") }
        when {
            rule.accountId == null -> withStyle(secondary) { append("Cualquier cuenta") }
            else -> append(row.account?.name ?: "Cuenta eliminada")
        }
    }

/**
 * The rule's category in a 40 dp circle: walnut with a white icon when it registers by itself, clay when it waits in
 * «Por revisar», as in «Por revisar» itself; Coffer's rules carry the spark badge at the bottom right.
 */
@Composable
internal fun RuleCircle(
    icon: ImageVector?,
    sure: Boolean,
    byCoffer: Boolean,
) {
    val colors = CofferTheme.colors
    Box(Modifier.size(40.dp)) {
        Box(
            Modifier.size(40.dp).background(if (sure) colors.primary else colors.iconContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            icon?.let {
                Icon(
                    it,
                    contentDescription = null,
                    tint = if (sure) colors.onPrimary else colors.iconOnContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        if (byCoffer) {
            CofferBadge(
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp),
            )
        }
    }
}

/** «Creada por Coffer»: an 18 dp walnut circle with a white spark and a 2 dp ring in the surface color. */
@Composable
internal fun CofferBadge(modifier: Modifier = Modifier) {
    val colors = CofferTheme.colors
    Box(
        modifier
            .size(22.dp)
            .background(colors.surface, CircleShape)
            .padding(2.dp)
            .background(colors.primary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Outlined.AutoAwesome, contentDescription = "Creada por Coffer", tint = colors.onPrimary, modifier = Modifier.size(10.dp))
    }
}

@Composable
private fun Footnotes(state: DraftRulesState) {
    val colors = CofferTheme.colors
    val style = MaterialTheme.typography.bodySmall
    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if ((state.sure + state.probable).any { it.rule.origin == RuleOrigin.LEARNED }) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CofferBadge()
                Text(
                    "La creó Coffer porque ese movimiento se repite. Puedes editarla o borrarla.",
                    style = style,
                    color = colors.textSecondary,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.DragIndicator, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(22.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Si un movimiento cumple dos reglas, se usa la de más arriba. Arrastra para cambiar el orden.",
                    style = style,
                    color = colors.textSecondary,
                )
                if (state.probable.isNotEmpty()) {
                    Text(
                        "Las que quedan por revisar solo las crea Coffer: llenan el título y la categoría, y tú confirmas. " +
                            "Ábrela para que entre directo.",
                        style = style,
                        color = colors.textSecondary,
                    )
                }
            }
        }
    }
}
