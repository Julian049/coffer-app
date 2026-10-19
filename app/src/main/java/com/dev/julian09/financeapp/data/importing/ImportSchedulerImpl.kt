package com.dev.julian09.financeapp.data.importing

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.dev.julian09.financeapp.data.local.FinanceDatabase
import com.dev.julian09.financeapp.data.local.entity.SyncMetaEntity
import com.dev.julian09.financeapp.domain.model.AccountResolution
import com.dev.julian09.financeapp.domain.model.AccountType
import com.dev.julian09.financeapp.domain.model.CategoryKind
import com.dev.julian09.financeapp.domain.model.CategoryRef
import com.dev.julian09.financeapp.domain.model.CategoryResolution
import com.dev.julian09.financeapp.domain.model.ImportField
import com.dev.julian09.financeapp.domain.model.ImportPlan
import com.dev.julian09.financeapp.domain.model.ImportSource
import com.dev.julian09.financeapp.domain.model.ImportSummary
import com.dev.julian09.financeapp.domain.port.ImportScheduler
import com.dev.julian09.financeapp.domain.port.ImportStatus
import com.dev.julian09.financeapp.domain.usecase.RunImport
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** The plan travels through sync_meta, not WorkManager's Data (10 KB at most). */
@Serializable
internal data class PlanDto(
    val source: String,
    val uri: String,
    val fileName: String,
    val mapping: Map<String, String?>,
    val accounts: List<AccountDto>,
    val categories: List<CategoryDto>,
    val skipDuplicates: Boolean,
)

@Serializable
internal data class AccountDto(
    val name: String,
    val createAs: String? = null,
    val useId: String? = null,
)

@Serializable
internal data class CategoryDto(
    val kind: String,
    val name: String,
    val subcategory: String? = null,
    val useId: String? = null,
)

@Serializable
internal data class SummaryDto(
    val fileName: String,
    val at: String,
    val imported: Int,
    val skipped: Int,
    val errors: Int,
)

internal fun ImportPlan.toDto() =
    PlanDto(
        source.name,
        uri,
        fileName,
        mapping.mapKeys { it.key.name },
        accounts.map { (name, r) ->
            when (r) {
                is AccountResolution.Create -> AccountDto(name, createAs = r.type.name)
                is AccountResolution.Use -> AccountDto(name, useId = r.accountId.toString())
            }
        },
        categories.map { (ref, r) ->
            CategoryDto(ref.kind.name, ref.name, ref.subcategory, (r as? CategoryResolution.Use)?.categoryId?.toString())
        },
        skipDuplicates,
    )

internal fun PlanDto.toPlan() =
    ImportPlan(
        source = ImportSource.valueOf(source),
        uri = uri,
        fileName = fileName,
        mapping = mapping.mapKeys { ImportField.valueOf(it.key) },
        accounts =
            accounts.associate { a ->
                a.name to
                    (
                        a.useId?.let { AccountResolution.Use(UUID.fromString(it)) }
                            ?: AccountResolution.Create(AccountType.valueOf(a.createAs!!))
                    )
            },
        categories =
            categories.associate { c ->
                CategoryRef(CategoryKind.valueOf(c.kind), c.name, c.subcategory) to
                    (c.useId?.let { CategoryResolution.Use(UUID.fromString(it)) } ?: CategoryResolution.Create)
            },
        skipDuplicates = skipDuplicates,
    )

private object ImportMeta {
    const val PLAN = "import_plan"
    const val LAST = "last_import"
    const val WORK = "import"
    const val PROGRESS = "processed"
}

@Singleton
class ImportSchedulerImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val db: FinanceDatabase,
        private val json: Json,
    ) : ImportScheduler {
        private val workManager get() = WorkManager.getInstance(context)

        override suspend fun start(plan: ImportPlan) {
            db.syncMeta().put(SyncMetaEntity(ImportMeta.PLAN, json.encodeToString(PlanDto.serializer(), plan.toDto())))
            // KEEP: a second tap while one runs does not import the file twice
            workManager.enqueueUniqueWork(ImportMeta.WORK, ExistingWorkPolicy.KEEP, OneTimeWorkRequestBuilder<ImportWorker>().build())
        }

        override fun observe(): Flow<ImportStatus> =
            combine(workManager.getWorkInfosForUniqueWorkFlow(ImportMeta.WORK), db.syncMeta().observe(ImportMeta.LAST)) { infos, last ->
                val running = infos.firstOrNull { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
                ImportStatus(
                    running = running != null,
                    processed = running?.progress?.getInt(ImportMeta.PROGRESS, 0) ?: 0,
                    last =
                        last?.let { text ->
                            runCatching { json.decodeFromString(SummaryDto.serializer(), text) }.getOrNull()?.let {
                                ImportSummary(it.fileName, Instant.parse(it.at), it.imported, it.skipped, it.errors)
                            }
                        },
                )
            }
    }

/** SEQ-13 §13.1: runs the plan saved by ImportSchedulerImpl and keeps its summary for Importar y exportar. */
@HiltWorker
class ImportWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val runImport: RunImport,
        private val db: FinanceDatabase,
        private val json: Json,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val text = db.syncMeta().get(ImportMeta.PLAN) ?: return Result.failure()
            val plan = json.decodeFromString(PlanDto.serializer(), text).toPlan()
            val summary = runImport(plan) { setProgress(workDataOf(ImportMeta.PROGRESS to it)) }
            val dto = SummaryDto(summary.fileName, summary.at.toString(), summary.imported, summary.skipped, summary.errors)
            db.syncMeta().put(SyncMetaEntity(ImportMeta.LAST, json.encodeToString(SummaryDto.serializer(), dto)))
            return Result.success()
        }
    }
