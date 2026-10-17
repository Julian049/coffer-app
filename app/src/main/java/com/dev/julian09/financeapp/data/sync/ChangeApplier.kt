package com.dev.julian09.financeapp.data.sync

import androidx.room.withTransaction
import com.dev.julian09.financeapp.data.local.FinanceDatabase
import com.dev.julian09.financeapp.data.local.entity.SyncMetaEntity
import com.dev.julian09.financeapp.data.local.entity.SyncState
import com.dev.julian09.financeapp.data.remote.dto.ChangeDto
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * Applies a pulled page with last-write-wins on the client (SEQ-09) and moves the cursor in the same transaction, so a
 * cut in the middle resumes from the last saved page without downloading it again.
 */
class ChangeApplier
    @Inject
    constructor(
        private val db: FinanceDatabase,
        private val tables: SyncTables,
    ) {
        suspend fun apply(
            changes: List<ChangeDto>,
            nextCursor: Long,
        ) = db.withTransaction {
            // shortcut: entities of later sprints are skipped; their sprint resets the cursor to 0 to fetch them
            changes.forEach { change -> tables.byEntity[change.entity]?.let { applyOne(it, change) } }
            db.syncMeta().put(SyncMetaEntity(SyncMeta.PULL_CURSOR, nextCursor.toString()))
        }

        private suspend fun <E> applyOne(
            table: SyncTable<E>,
            change: ChangeDto,
        ) {
            val local = table.syncInfo(UUID.fromString(change.id))
            val incomingAt = Instant.parse(change.updatedAt)
            // A local edit not yet sent and at least as recent wins; it goes up in the next push
            if (local != null && local.syncState != SyncState.SYNCED && !local.updatedAt.isBefore(incomingAt)) return
            table.upsert(table.decode(change))
        }
    }

/** Keys of sync_meta (data-model §5.1). */
object SyncMeta {
    const val PULL_CURSOR = "pull_cursor"
    const val LAST_SYNC_AT = "last_sync_at"
    const val INITIAL_SYNC_DONE = "initial_sync_done"

    /** User whose data is in this database; another user's session never syncs it (no cross-account leak). */
    const val OWNER = "owner_user_id"
}
