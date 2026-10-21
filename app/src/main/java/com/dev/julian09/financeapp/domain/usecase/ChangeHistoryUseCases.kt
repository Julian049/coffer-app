package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.model.ChangeAction
import com.dev.julian09.financeapp.domain.model.ChangeEntry
import com.dev.julian09.financeapp.domain.model.isEditable
import com.dev.julian09.financeapp.domain.port.ChangeLogRepository
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.port.SyncScheduler
import com.dev.julian09.financeapp.domain.port.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Duration
import javax.inject.Inject

/** REQ-HIS-04: entries live 90 days, not configurable. */
val HISTORY_DAYS: Duration = Duration.ofDays(90)

/** An entry of Historial de cambios and what it offers. */
data class ChangeLine(
    val entry: ChangeEntry,
    val canRestore: Boolean = false,
    val canRevert: Boolean = false,
)

/**
 * S11-DOM01: the last 90 days, newest first. Only the latest change of a movement can be undone, so a restore or a
 * revert never overwrites a later edit; plans and payment splits are not editable, so they are not undone either.
 */
class ObserveChangeHistory
    @Inject
    constructor(
        private val repository: ChangeLogRepository,
        private val clock: Clock,
    ) {
        operator fun invoke(): Flow<List<ChangeLine>> =
            repository.observeSince(clock.now() - HISTORY_DAYS).map { entries ->
                val latest = entries.groupBy { it.transactionId }.mapValues { (_, list) -> list.first().id }
                entries.map { e ->
                    val last = latest[e.transactionId] == e.id
                    ChangeLine(
                        e,
                        canRestore = last && e.action == ChangeAction.DELETE && e.before?.isEditable() == true,
                        canRevert =
                            last && e.action == ChangeAction.UPDATE && e.before?.isEditable() == true && e.after?.isEditable() == true,
                    )
                }
            }
    }

/** REQ-HIS-03: a deleted movement comes back as it was, if its accounts and category still allow it. */
class RestoreTransaction
    @Inject
    constructor(
        private val repository: TransactionRepository,
        private val validator: TransactionValidator,
        private val scheduler: SyncScheduler,
    ) {
        suspend operator fun invoke(entry: ChangeEntry): TransactionErrors {
            val movement = entry.before ?: return emptyMap()
            validator.check(movement).takeIf { it.isNotEmpty() }?.let { return it }
            if (repository.restore(movement.id)) scheduler.requestSync()
            return emptyMap()
        }
    }

/** REQ-HIS-03: an edit goes back to the values before it, logged as a new UPDATE. */
class RevertEdit
    @Inject
    constructor(
        private val update: UpdateTransaction,
    ) {
        suspend operator fun invoke(entry: ChangeEntry): TransactionErrors = entry.before?.let { update(it) }.orEmpty()
    }

/** S11-DOM01: on every start, entries older than 90 days leave the phone. */
class PurgeChangeHistory
    @Inject
    constructor(
        private val repository: ChangeLogRepository,
        private val clock: Clock,
    ) {
        suspend operator fun invoke() = repository.purgeBefore(clock.now() - HISTORY_DAYS)
    }
