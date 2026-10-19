package com.dev.julian09.financeapp.ui.feature.importexport

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.julian09.financeapp.domain.model.AccountResolution
import com.dev.julian09.financeapp.domain.model.CategoryKind
import com.dev.julian09.financeapp.domain.model.CategoryRef
import com.dev.julian09.financeapp.domain.model.CategoryResolution
import com.dev.julian09.financeapp.domain.model.ImportField
import com.dev.julian09.financeapp.domain.model.ImportSource
import com.dev.julian09.financeapp.domain.model.RowProblem
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.usecase.AVAILABLE_ACCOUNT_TYPES
import com.dev.julian09.financeapp.ui.components.AppBottomSheet
import com.dev.julian09.financeapp.ui.components.AppButton
import com.dev.julian09.financeapp.ui.components.AppCard
import com.dev.julian09.financeapp.ui.components.AppChip
import com.dev.julian09.financeapp.ui.components.AppHeader
import com.dev.julian09.financeapp.ui.components.ButtonKind
import com.dev.julian09.financeapp.ui.components.MoneyKind
import com.dev.julian09.financeapp.ui.components.accountTypeLabel
import com.dev.julian09.financeapp.ui.components.formatMoney
import com.dev.julian09.financeapp.ui.feature.transactions.CircleIcon
import com.dev.julian09.financeapp.ui.feature.transactions.categoryLabel
import com.dev.julian09.financeapp.ui.feature.transactions.dateTimeLabel
import com.dev.julian09.financeapp.ui.theme.CofferTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ES_CO: Locale = Locale.forLanguageTag("es-CO")
private val SHORT_DATE = DateTimeFormatter.ofPattern("d MMM yyyy", ES_CO)

@Composable
fun ImportExportScreen(
    onBack: () -> Unit,
    viewModel: ImportExportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val open =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            // The import runs in a worker, maybe after the screen is gone: keep the right to read the file
            uri?.let { runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
            val name =
                uri?.let { u ->
                    context.contentResolver.query(u, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                        if (c.moveToFirst()) c.getString(0) else null
                    }
                } ?: "archivo.csv"
            viewModel.fileChosen(uri?.toString(), name)
        }
    val save =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
            viewModel.exportTo(uri?.toString())
        }
    LaunchedEffect(state.step) {
        if (state.step ==
            ImportStep.PICKING
        ) {
            open.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel", "application/octet-stream"))
        }
    }
    ImportExportContent(
        state,
        catalog,
        ImportExportActions(
            onBack = onBack,
            onStartImport = viewModel::startImport,
            onSource = viewModel::chooseSource,
            onCancel = viewModel::cancel,
            onMapping = viewModel::setMapping,
            onReview = viewModel::review,
            onAccount = viewModel::setAccount,
            onCategory = viewModel::setCategory,
            onCreateAll = viewModel::createAll,
            onSkipDuplicates = viewModel::setSkipDuplicates,
            onConfirm = viewModel::confirm,
            onExportChoice = viewModel::setExport,
            onCustomRange = viewModel::setCustomRange,
            onExport = { save.launch(viewModel.suggestedExportName()) },
        ),
    )
}

class ImportExportActions(
    val onBack: () -> Unit = {},
    val onStartImport: () -> Unit = {},
    val onSource: (ImportSource) -> Unit = {},
    val onCancel: () -> Unit = {},
    val onMapping: (ImportField, String?) -> Unit = { _, _ -> },
    val onReview: () -> Unit = {},
    val onAccount: (String, AccountResolution) -> Unit = { _, _ -> },
    val onCategory: (CategoryRef, CategoryResolution) -> Unit = { _, _ -> },
    val onCreateAll: () -> Unit = {},
    val onSkipDuplicates: (Boolean) -> Unit = {},
    val onConfirm: () -> Unit = {},
    val onExportChoice: (ExportChoice) -> Unit = {},
    val onCustomRange: (LocalDate, LocalDate) -> Unit = { _, _ -> },
    val onExport: () -> Unit = {},
)

/** Importar y exportar (S8-UI01, S8-UI02): the two cards, or the step of the import in progress. */
@Composable
fun ImportExportContent(
    state: ImportExportState,
    catalog: ImportCatalog,
    actions: ImportExportActions,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    Column(modifier.fillMaxSize()) {
        AppHeader(title = "Importar y exportar", onBack = actions.onBack)
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (state.step) {
                ImportStep.MAPPING -> MappingStep(state, actions, zone)
                ImportStep.REVIEW -> ReviewStep(state, catalog, actions)
                ImportStep.LOADING -> Loading("Leyendo ${state.fileName}…")
                else -> Cards(state, catalog, actions, zone)
            }
        }
    }
    if (state.step == ImportStep.SOURCE) SourceSheet(actions)
}

@Composable
private fun Loading(text: String) {
    val colors = CofferTheme.colors
    AppCard(Modifier.fillMaxWidth()) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        LinearProgressIndicator(Modifier.fillMaxWidth(), color = colors.primary, trackColor = colors.border)
    }
}

@Composable
private fun Cards(
    state: ImportExportState,
    catalog: ImportCatalog,
    actions: ImportExportActions,
    zone: ZoneId,
) {
    val colors = CofferTheme.colors
    val status = catalog.status
    AppCard(Modifier.fillMaxWidth()) {
        CircleIcon(Icons.Outlined.FileUpload)
        Text("Importar movimientos", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
        Text(
            "Desde el CSV que exporta Cashew. Podrás revisar las columnas, ver una vista previa y decidir qué hacer con las " +
                "cuentas, las categorías nuevas y los duplicados.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
        if (status.running) {
            Text(
                if (status.processed == 0) "Importando…" else "Importando… ${status.processed} filas",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
            )
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = colors.primary, trackColor = colors.border)
        } else {
            AppButton(
                "Seleccionar archivo",
                onClick = actions.onStartImport,
                icon = Icons.Outlined.Description,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    ExportCard(state, actions)
    state.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary) }
    status.last?.let { last ->
        AppCard(Modifier.fillMaxWidth()) {
            Text("Última importación", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            Text(
                "${SHORT_DATE.format(last.at.atZone(zone)).replace(".", "")} · ${last.fileName}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
            Figure("Importadas", last.imported.toString(), colors.income)
            Figure("Omitidas por duplicadas", last.skipped.toString(), colors.textPrimary)
            Figure("Con error", last.errors.toString(), if (last.errors > 0) colors.expense else colors.textPrimary)
        }
    }
}

@Composable
private fun Figure(
    label: String,
    value: String,
    tint: androidx.compose.ui.graphics.Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = CofferTheme.colors.textSecondary)
        Text(value, style = MaterialTheme.typography.titleSmall, color = tint)
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ExportCard(
    state: ImportExportState,
    actions: ImportExportActions,
) {
    val colors = CofferTheme.colors
    var picking by rememberSaveable { mutableStateOf(false) }
    AppCard(Modifier.fillMaxWidth()) {
        CircleIcon(Icons.Outlined.FileDownload)
        Text("Exportar a CSV", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
        Text("Descarga tus transacciones en un archivo CSV.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        Text("Rango de fechas (opcional)", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppChip("Todo", selected = state.export == ExportChoice.ALL, onClick = { actions.onExportChoice(ExportChoice.ALL) })
            AppChip(
                "Este mes",
                selected = state.export == ExportChoice.THIS_MONTH,
                onClick = { actions.onExportChoice(ExportChoice.THIS_MONTH) },
            )
            AppChip(
                "Mes anterior",
                selected = state.export == ExportChoice.LAST_MONTH,
                onClick = { actions.onExportChoice(ExportChoice.LAST_MONTH) },
            )
            val custom =
                if (state.export == ExportChoice.CUSTOM && state.customFrom != null && state.customTo != null) {
                    "${SHORT_DATE.format(state.customFrom).replace(".", "")} – ${SHORT_DATE.format(state.customTo).replace(".", "")}"
                } else {
                    "Personalizado"
                }
            AppChip(custom, selected = state.export == ExportChoice.CUSTOM, onClick = { picking = true })
        }
        AppButton(
            "Exportar CSV",
            onClick = actions.onExport,
            kind = ButtonKind.Secondary,
            icon = Icons.Outlined.FileDownload,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    if (picking) {
        val range = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    val from = range.selectedStartDateMillis
                    val to = range.selectedEndDateMillis ?: from
                    // The picker works in UTC midnights
                    if (from != null && to != null) {
                        actions.onCustomRange(
                            Instant.ofEpochMilli(from).atZone(ZoneOffset.UTC).toLocalDate(),
                            Instant.ofEpochMilli(to).atZone(ZoneOffset.UTC).toLocalDate(),
                        )
                    }
                    picking = false
                }) { Text("Listo") }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancelar") } },
        ) { DateRangePicker(state = range, modifier = Modifier.padding(top = 8.dp)) }
    }
}

/** "¿De dónde importas?" (PO, 2026-10-19): only Cashew in v1.0.0. */
@Composable
private fun SourceSheet(actions: ImportExportActions) {
    val colors = CofferTheme.colors
    AppBottomSheet(onDismiss = actions.onCancel, title = "¿De dónde importas?", subtitle = "Elige la app de la que viene el archivo.") {
        AppCard(Modifier.fillMaxWidth(), onClick = { actions.onSource(ImportSource.CASHEW) }) {
            Text("Cashew", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            Text(
                "El CSV que exporta Cashew desde Ajustes → Exportar datos.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
        AppButton("Cancelar", onClick = actions.onCancel, kind = ButtonKind.Secondary, modifier = Modifier.fillMaxWidth())
    }
}

private fun fieldLabel(field: ImportField) =
    when (field) {
        ImportField.DATE -> "Fecha"
        ImportField.AMOUNT -> "Monto (negativo = gasto)"
        ImportField.ACCOUNT -> "Cuenta"
        ImportField.CATEGORY -> "Categoría"
        ImportField.SUBCATEGORY -> "Subcategoría"
        ImportField.TAG -> "Etiqueta (presupuesto en Cashew)"
        ImportField.TITLE -> "Título"
        ImportField.NOTE -> "Nota"
    }

/** REQ-IMP-02: each field takes a column of the file, suggested from Cashew's names. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MappingStep(
    state: ImportExportState,
    actions: ImportExportActions,
    zone: ZoneId,
) {
    val colors = CofferTheme.colors
    val preview = state.preview ?: return
    Text("Asociar columnas", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
    Text(
        "${state.fileName} · revisa qué columna alimenta cada dato. Las demás columnas se ignoran.",
        style = MaterialTheme.typography.bodySmall,
        color = colors.textSecondary,
    )
    ImportField.entries.forEach { field ->
        AppCard(Modifier.fillMaxWidth()) {
            Text(fieldLabel(field), style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val current = preview.mapping[field]
                AppChip("Ninguna", selected = current == null, onClick = { actions.onMapping(field, null) })
                preview.headers.filter { it.isNotBlank() }.forEach { h ->
                    AppChip(h, selected = current == h, onClick = { actions.onMapping(field, h) })
                }
            }
        }
    }
    if (preview.sample.isNotEmpty()) {
        Text("Vista previa", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
        preview.sample.take(5).forEach { row ->
            AppCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            row.note ?: row.category ?: "Transferencia",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary,
                        )
                        Text(
                            listOfNotNull(row.account + (row.destination?.let { " → $it" } ?: ""), row.category, row.tag?.let { "#$it" })
                                .joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                        Text(
                            dateTimeLabel(row.occurredAt, LocalDate.now(zone), zone),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                    Text(
                        formatMoney(
                            row.amount,
                            when (row.type) {
                                TransactionType.INCOME -> MoneyKind.Income
                                TransactionType.EXPENSE -> MoneyKind.Expense
                                TransactionType.TRANSFER -> MoneyKind.Transfer
                            },
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary,
                    )
                }
            }
        }
    }
    AppButton("Continuar", onClick = actions.onReview, modifier = Modifier.fillMaxWidth())
    AppButton("Cancelar", onClick = actions.onCancel, kind = ButtonKind.Secondary, modifier = Modifier.fillMaxWidth())
}

private fun problemLabel(problem: RowProblem) =
    when (problem) {
        RowProblem.DATE -> "fecha inválida"
        RowProblem.AMOUNT -> "monto inválido o en cero"
        RowProblem.ACCOUNT -> "sin cuenta"
        RowProblem.CATEGORY -> "sin categoría"
    }

/** REQ-IMP-03 to 05: what will happen, the unknown names and the duplicates, before importing. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReviewStep(
    state: ImportExportState,
    catalog: ImportCatalog,
    actions: ImportExportActions,
) {
    val colors = CofferTheme.colors
    val preview = state.preview ?: return
    var associating by rememberSaveable { mutableStateOf<Int?>(null) }
    Text("Antes de importar", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
    AppCard(Modifier.fillMaxWidth()) {
        Figure("Listas para importar", preview.valid.toString(), colors.income)
        Figure(
            "Con error (no se importan)",
            preview.errorCount.toString(),
            if (preview.errorCount >
                0
            ) {
                colors.expense
            } else {
                colors.textPrimary
            },
        )
        Figure("Posibles duplicadas", preview.duplicates.toString(), colors.textPrimary)
        preview.errors.take(5).forEach {
            Text("Fila ${it.line}: ${problemLabel(it.problem)}", style = MaterialTheme.typography.bodySmall, color = colors.expense)
        }
    }
    if (preview.duplicates > 0) {
        AppCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Omitir duplicados", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                    Text(
                        "Misma fecha y hora, monto, cuenta y tipo que un movimiento que ya tienes. Recomendado.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                }
                Switch(
                    checked = state.skipDuplicates,
                    onCheckedChange = actions.onSkipDuplicates,
                    colors = SwitchDefaults.colors(checkedTrackColor = colors.primary, checkedThumbColor = colors.onPrimary),
                )
            }
        }
    }
    if (state.accounts.isNotEmpty() || state.categories.isNotEmpty()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Nombres nuevos", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            TextButton(onClick = actions.onCreateAll) { Text("Crear todas", color = colors.primary) }
        }
    }
    state.accounts.forEach { (name, resolution) ->
        AppCard(Modifier.fillMaxWidth()) {
            Text("Cuenta «$name»", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            Text("Crear como", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AVAILABLE_ACCOUNT_TYPES.forEach { type ->
                    AppChip(
                        accountTypeLabel(type),
                        selected = (resolution as? AccountResolution.Create)?.type == type,
                        onClick = { actions.onAccount(name, AccountResolution.Create(type)) },
                    )
                }
            }
            Text(
                "Para una tarjeta de crédito, créala antes en Cuentas con su cupo y sus fechas, y elígela aquí.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
            if (catalog.accounts.isNotEmpty()) {
                Text("o usar una que ya tienes", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    catalog.accounts.forEach { a ->
                        AppChip(
                            a.name,
                            selected = (resolution as? AccountResolution.Use)?.accountId == a.id,
                            onClick = { actions.onAccount(name, AccountResolution.Use(a.id)) },
                        )
                    }
                }
            }
        }
    }
    state.categories.entries.forEachIndexed { index, (ref, resolution) ->
        val used = (resolution as? CategoryResolution.Use)?.categoryId?.let { id -> catalog.categories.firstOrNull { it.id == id } }
        AppCard(Modifier.fillMaxWidth()) {
            Text(
                "Categoría «${ref.name}${ref.subcategory?.let {
                    " > $it"
                } ?: ""}» · ${if (ref.kind == CategoryKind.EXPENSE) "gasto" else "ingreso"}",
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppChip(
                    "Crear",
                    selected = resolution == CategoryResolution.Create,
                    onClick = { actions.onCategory(ref, CategoryResolution.Create) },
                )
                AppChip(
                    used?.let { "Usar: " + categoryLabel(it, catalog.categories) } ?: "Asociar a una existente",
                    selected = used != null,
                    onClick = { associating = index },
                )
            }
        }
    }
    val toImport = preview.valid - if (state.skipDuplicates) preview.duplicates else 0
    AppButton(
        if (toImport == 1) "Importar 1 movimiento" else "Importar $toImport movimientos",
        onClick = actions.onConfirm,
        enabled = toImport > 0,
        modifier = Modifier.fillMaxWidth(),
    )
    AppButton("Cancelar", onClick = actions.onCancel, kind = ButtonKind.Secondary, modifier = Modifier.fillMaxWidth())
    associating?.let { index ->
        val ref = state.categories.keys.elementAtOrNull(index) ?: return@let
        AppBottomSheet(onDismiss = { associating = null }, title = "Asociar «${ref.name}»") {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val ofKind = catalog.categories.filter { it.kind == ref.kind }
                ofKind.filter { it.parentId == null }.forEach { main ->
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        (listOf(main) + ofKind.filter { it.parentId == main.id }).forEach { c ->
                            AppChip(categoryLabel(c, ofKind), selected = false, onClick = {
                                actions.onCategory(ref, CategoryResolution.Use(c.id))
                                associating = null
                            })
                        }
                    }
                }
            }
        }
    }
}
