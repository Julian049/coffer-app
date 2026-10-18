package com.dev.julian09.financeapp.data.sync

import androidx.work.ListenableWorker
import com.dev.julian09.financeapp.data.local.entity.AccountEntity
import com.dev.julian09.financeapp.data.local.entity.SyncState
import com.dev.julian09.financeapp.data.local.inMemoryDatabase
import com.dev.julian09.financeapp.data.remote.dto.ChangeDto
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.time.Instant
import java.util.UUID

/** S3-QA03: last-write-wins on the client (SEQ-09) and which failures the worker retries. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ChangeApplierTest {
    private val db = inMemoryDatabase()
    private val applier = ChangeApplier(db, SyncTables(db))
    private val id = UUID.randomUUID()
    private val t10 = Instant.parse("2026-10-16T10:00:00Z")

    @After
    fun close() = db.close()

    @Test
    fun aNewRowIsInsertedAsSynced() =
        runBlocking {
            applier.apply(listOf(change("Del servidor", t10)), nextCursor = 9)
            assertEquals("Del servidor" to SyncState.SYNCED, local())
            assertEquals("9", db.syncMeta().get(SyncMeta.PULL_CURSOR))
        }

    @Test
    fun aSyncedRowIsReplaced() =
        runBlocking {
            save("Viejo", t10.plusSeconds(600), SyncState.SYNCED)
            applier.apply(listOf(change("Del servidor", t10)), nextCursor = 1)
            assertEquals("Del servidor" to SyncState.SYNCED, local())
        }

    @Test
    fun anOlderPendingEditLoses() =
        runBlocking {
            save("Local viejo", t10, SyncState.PENDING)
            applier.apply(listOf(change("Del servidor", t10.plusSeconds(60))), nextCursor = 1)
            assertEquals("Del servidor" to SyncState.SYNCED, local())
        }

    @Test
    fun aNewerOrEqualPendingEditWinsAndWillBePushed() =
        runBlocking {
            save("Local nuevo", t10, SyncState.PENDING)
            applier.apply(listOf(change("Del servidor", t10)), nextCursor = 1)
            assertEquals("Local nuevo" to SyncState.PENDING, local())
        }

    @Test
    fun deletionsArriveAsSoftDeletes() =
        runBlocking {
            applier.apply(listOf(change("Borrada", t10, deletedAt = t10)), nextCursor = 1)
            val deletedAt =
                db.query("SELECT deletedAt FROM accounts WHERE id = ?", arrayOf(id.toString())).use {
                    it.moveToFirst()
                    it.getLong(0)
                }
            assertEquals(t10.toEpochMilli(), deletedAt)
        }

    @Test
    fun theWorkerRetriesOnlyWhatCanRecover() {
        fun http(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody(null)))
        assertEquals(ListenableWorker.Result.retry(), SyncWorker.resultFor(IOException("sin red")))
        assertEquals(ListenableWorker.Result.retry(), SyncWorker.resultFor(http(503)))
        assertEquals(ListenableWorker.Result.retry(), SyncWorker.resultFor(http(429)))
        assertEquals(ListenableWorker.Result.failure(), SyncWorker.resultFor(http(401)))
        assertEquals(ListenableWorker.Result.failure(), SyncWorker.resultFor(ForeignDataException()))
    }

    private suspend fun save(
        name: String,
        at: Instant,
        state: SyncState,
    ) = db.accounts().upsert(
        listOf(
            AccountEntity(id = id, name = name, type = "CASH", initialBalance = 0, createdAt = at, updatedAt = at, syncState = state),
        ),
    )

    private suspend fun local(): Pair<String, SyncState> {
        val info = db.accounts().syncInfo(id)!!
        val name =
            db.query("SELECT name FROM accounts WHERE id = ?", arrayOf(id.toString())).use {
                it.moveToFirst()
                it.getString(0)
            }
        return name to info.syncState
    }

    private fun change(
        name: String,
        updatedAt: Instant,
        deletedAt: Instant? = null,
    ) = ChangeDto(
        entity = "account",
        id = id.toString(),
        createdAt = t10.toString(),
        updatedAt = updatedAt.toString(),
        deletedAt = deletedAt?.toString(),
        data =
            buildJsonObject {
                put("name", name)
                put("type", "CASH")
                put("initialBalance", "0")
                put("includeInTotal", true)
                put("active", true)
                put("sortOrder", 0)
            },
    )
}
