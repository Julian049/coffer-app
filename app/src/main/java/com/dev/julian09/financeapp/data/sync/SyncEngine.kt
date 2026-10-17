package com.dev.julian09.financeapp.data.sync

import androidx.room.withTransaction
import com.dev.julian09.financeapp.data.local.FinanceDatabase
import com.dev.julian09.financeapp.data.local.entity.SyncMetaEntity
import com.dev.julian09.financeapp.data.remote.SyncApi
import com.dev.julian09.financeapp.data.remote.dto.ChangeDto
import com.dev.julian09.financeapp.data.remote.dto.PushRequestDto
import com.dev.julian09.financeapp.data.repository.BaseCatalogSeeder
import com.dev.julian09.financeapp.data.session.DeviceIdProvider
import com.dev.julian09.financeapp.data.session.SessionStore
import com.dev.julian09.financeapp.domain.port.Clock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/** The local data belongs to another account than the signed-in one: syncing would mix two people's money. */
class ForeignDataException : IllegalStateException("Local data belongs to another account")

/**
 * One sync round (SEQ-02, 08, 09): push the outbox, pull everything newer than the cursor, and on the very first round
 * of an account create the base catalog. Network and server errors propagate; [SyncWorker] decides retry or stop.
 */
class SyncEngine
    @Inject
    constructor(
        private val db: FinanceDatabase,
        private val tables: SyncTables,
        private val applier: ChangeApplier,
        private val api: SyncApi,
        private val store: SessionStore,
        private val deviceId: DeviceIdProvider,
        private val seeder: BaseCatalogSeeder,
        private val clock: Clock,
    ) {
        private class Sent(
            val table: SyncTable<*>,
            val updatedAt: Instant,
        )

        /** [onProgress] gets (received, total, received per entity); total is only known on the initial download. */
        suspend fun sync(onProgress: suspend (Long, Long?, Map<String, Long>) -> Unit = { _, _, _ -> }) =
            // The one-off, periodic and initial works are unique per name, not across names: one round at a time
            LOCK.withLock {
                val session = store.current()?.takeUnless { it.expired } ?: return@withLock
                claimOwnership(session.user.id)
                push()
                pull(onProgress)
                if (db.syncMeta().get(SyncMeta.INITIAL_SYNC_DONE) != "true") {
                    if (seeder.ensureBaseCatalog(UUID.fromString(session.user.id))) push()
                    db.syncMeta().put(SyncMetaEntity(SyncMeta.INITIAL_SYNC_DONE, "true"))
                }
                db.syncMeta().put(SyncMetaEntity(SyncMeta.LAST_SYNC_AT, clock.now().toString()))
            }

        private suspend fun claimOwnership(userId: String) {
            when (db.syncMeta().get(SyncMeta.OWNER)) {
                null -> db.syncMeta().put(SyncMetaEntity(SyncMeta.OWNER, userId))
                userId -> Unit
                else -> throw ForeignDataException()
            }
        }

        private suspend fun push() {
            // Bounded: a row edited while being sent stays PENDING and goes in the next batch
            repeat(MAX_PUSH_ROUNDS) {
                val sent = LinkedHashMap<String, Sent>()
                val changes = mutableListOf<ChangeDto>()
                for (table in tables.all) {
                    if (changes.size >= PUSH_BATCH) break
                    collect(table, PUSH_BATCH - changes.size, changes, sent)
                }
                if (changes.isEmpty()) return
                val results = api.push(PushRequestDto(deviceId.deviceId, changes)).results
                db.withTransaction {
                    results.forEach { result ->
                        val s = sent[result.id] ?: return@forEach
                        val id = UUID.fromString(result.id)
                        when (result.status) {
                            "APPLIED", "STALE" -> s.table.markSynced(id, s.updatedAt)
                            else -> s.table.markFailed(id, s.updatedAt, result.code ?: "REJECTED")
                        }
                    }
                }
            }
        }

        private suspend fun <E> collect(
            table: SyncTable<E>,
            limit: Int,
            into: MutableList<ChangeDto>,
            sent: MutableMap<String, Sent>,
        ) {
            table.pending(limit).forEach { row ->
                into += table.encode(row)
                sent[table.idOf(row).toString()] = Sent(table, table.updatedAtOf(row))
            }
        }

        private suspend fun pull(onProgress: suspend (Long, Long?, Map<String, Long>) -> Unit) {
            var cursor = db.syncMeta().get(SyncMeta.PULL_CURSOR)?.toLongOrNull() ?: 0
            var received = 0L
            val byEntity = mutableMapOf<String, Long>()
            var total: Long? = null
            do {
                val page = api.pull(cursor, PULL_PAGE)
                applier.apply(page.changes, page.nextCursor)
                received += page.changes.size
                page.changes.forEach { byEntity.merge(it.entity, 1, Long::plus) }
                // remaining only comes with since=0, the start of the initial download
                if (total == null && page.remaining != null) total = page.changes.size + page.remaining
                onProgress(received, total, byEntity.toMap())
                cursor = page.nextCursor
            } while (page.hasMore)
        }

        companion object {
            const val PUSH_BATCH = 200
            const val PULL_PAGE = 500
            private const val MAX_PUSH_ROUNDS = 50
            private val LOCK = Mutex()
        }
    }
