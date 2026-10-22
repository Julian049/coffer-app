package com.dev.julian09.financeapp.data.sync

import com.dev.julian09.financeapp.data.local.FixedClock
import com.dev.julian09.financeapp.data.local.entity.AccountEntity
import com.dev.julian09.financeapp.data.local.entity.SyncMetaEntity
import com.dev.julian09.financeapp.data.local.entity.SyncState
import com.dev.julian09.financeapp.data.local.inMemoryDatabase
import com.dev.julian09.financeapp.data.remote.SyncApi
import com.dev.julian09.financeapp.data.remote.testJson
import com.dev.julian09.financeapp.data.repository.BaseCatalogSeeder
import com.dev.julian09.financeapp.data.session.DeviceIdProvider
import com.dev.julian09.financeapp.data.session.FakeSessionStore
import com.dev.julian09.financeapp.data.session.StoredSession
import com.dev.julian09.financeapp.data.session.TEST_USER
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.Instant
import java.util.UUID

/** S3-QA03: one sync round against a scripted server (MockWebServer) and a real Room schema. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SyncEngineTest {
    private val server = MockWebServer()
    private val db = inMemoryDatabase()
    private val clock = FixedClock()
    private val user = TEST_USER.copy(id = UUID.randomUUID().toString())
    private val store = FakeSessionStore(StoredSession("access", "refresh-0123456789012345", user))
    private val pushes = mutableListOf<JsonObject>()
    private val pullSince = mutableListOf<String?>()
    private var pulls: (String?) -> MockResponse = { emptyPage(it?.toLong() ?: 0) }
    private var pushReply: (JsonObject) -> MockResponse = ::allApplied
    private lateinit var engine: SyncEngine

    @Before
    fun start() {
        server.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse =
                    if (request.url.encodedPath.endsWith("/push")) {
                        val body = testJson.parseToJsonElement(request.body!!.utf8()).jsonObject
                        pushes += body
                        pushReply(body)
                    } else {
                        val since = request.url.queryParameter("since")
                        pullSince += since
                        pulls(since)
                    }
            }
        server.start()
        val api =
            Retrofit
                .Builder()
                .baseUrl(server.url("/"))
                .client(OkHttpClient())
                .addConverterFactory(testJson.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(SyncApi::class.java)
        val tables = SyncTables(db)
        val device = mockk<DeviceIdProvider> { every { deviceId } returns "device-test-01" }
        engine = SyncEngine(db, tables, ChangeApplier(db, tables), api, store, device, BaseCatalogSeeder(db, clock), clock)
    }

    @After
    fun stop() {
        server.close()
        db.close()
    }

    @Test
    fun initialDownloadComesInPagesWithProgressAndMovesTheCursor() =
        runBlocking {
            val account = UUID.randomUUID()
            val category = UUID.randomUUID()
            pulls = { since ->
                when (since) {
                    "0" -> page(listOf(accountChange(account), categoryChange(category)), next = 2, hasMore = true, remaining = 1)
                    else -> page(listOf(transactionChange(UUID.randomUUID(), account, category)), next = 3, hasMore = false)
                }
            }
            val progress = mutableListOf<Pair<Long, Long?>>()
            var lastByEntity = emptyMap<String, Long>()

            engine.sync { received, total, byEntity ->
                progress += received to total
                lastByEntity = byEntity
            }

            assertEquals(listOf("0", "2"), pullSince)
            assertEquals(listOf(2L to 3L, 3L to 3L), progress)
            assertEquals(mapOf("account" to 1L, "category" to 1L, "transaction" to 1L), lastByEntity)
            assertEquals("3", db.syncMeta().get(SyncMeta.PULL_CURSOR))
            val tx =
                db
                    .transactions()
                    .observeRecent(10)
                    .first()
                    .single()
            assertEquals(SyncState.SYNCED, tx.syncState)
            assertEquals(28000_0000L, tx.amount)
            assertEquals("true", db.syncMeta().get(SyncMeta.INITIAL_SYNC_DONE))
        }

    @Test
    fun deletedRowsCountTowardsTheTotalButNotAsDownloadedData() =
        runBlocking {
            val account = UUID.randomUUID()
            val category = UUID.randomUUID()
            val deleted =
                transactionChange(
                    UUID.randomUUID(),
                    account,
                    category,
                ).replace("\"deletedAt\":null", "\"deletedAt\":\"2026-10-17T10:00:00Z\"")
            pulls = { page(listOf(accountChange(account), categoryChange(category), deleted), next = 3, hasMore = false, remaining = 0) }
            var last: Triple<Long, Long?, Map<String, Long>>? = null

            engine.sync { received, total, byEntity -> last = Triple(received, total, byEntity) }

            assertEquals(Triple(3L, 3L, mapOf("account" to 1L, "category" to 1L)), last)
        }

    @Test
    fun aNewAccountGetsTheBaseCatalogAndBacksItUp() =
        runBlocking {
            engine.sync()

            // 10 categories + 14 subcategories (detailed-design §6.9.10) and the settings row
            assertEquals(25, pushes.single()["changes"]!!.jsonArray.size)
            val categories = db.categories().observeAll().first()
            assertEquals(24, categories.size)
            assertTrue(categories.all { it.syncState == SyncState.SYNCED && it.isBase })
            assertEquals(1, categories.count { it.systemKey == "FINANCIAL_CHARGES" })

            engine.sync()
            assertEquals("seeded only once", 24, db.categories().count())
        }

    @Test
    fun pushResultsMarkRowsSyncedOrFailed() =
        runBlocking {
            initialSyncDone()
            val ok = pendingAccount("Bancolombia")
            val bad = pendingAccount("Rechazada")
            pushReply = { body ->
                val results =
                    body["changes"]!!.jsonArray.joinToString(",") { c ->
                        val id = c.jsonObject["id"]!!.jsonPrimitive.content
                        if (id == bad.toString()) {
                            """{"id":"$id","status":"REJECTED","code":"FIN-VALIDATION","detail":"x"}"""
                        } else {
                            """{"id":"$id","status":"APPLIED","version":7}"""
                        }
                    }
                json("""{"results":[$results]}""")
            }

            engine.sync()

            assertEquals(SyncState.SYNCED, db.accounts().syncInfo(ok)!!.syncState)
            assertEquals(SyncState.FAILED, db.accounts().syncInfo(bad)!!.syncState)
            assertEquals(0, db.accounts().pending(10).size)
        }

    @Test
    fun aRowEditedWhileBeingSentStaysPendingForTheNextPush() =
        runBlocking {
            initialSyncDone()
            val id = pendingAccount("Antes")
            var first = true
            pushReply = { body ->
                if (first) {
                    first = false
                    // The user edits the account while the request is in flight
                    runBlocking { db.accounts().upsert(listOf(account(id, "Después", clock.now().plusSeconds(60)))) }
                }
                allApplied(body)
            }

            engine.sync()

            assertEquals(2, pushes.size)
            assertEquals(
                "Después",
                pushes[1]["changes"]!!
                    .jsonArray[0]
                    .jsonObject["data"]!!
                    .jsonObject["name"]!!
                    .jsonPrimitive.content,
            )
            assertEquals(SyncState.SYNCED, db.accounts().syncInfo(id)!!.syncState)
        }

    @Test
    fun aCutInTheMiddleResumesFromTheLastSavedPage() =
        runBlocking {
            val account = UUID.randomUUID()
            pulls = { since ->
                if (since ==
                    "0"
                ) {
                    page(listOf(accountChange(account)), next = 5, hasMore = true, remaining = 10)
                } else {
                    MockResponse.Builder().code(503).build()
                }
            }
            try {
                engine.sync()
                fail("a 503 must reach the worker")
            } catch (e: HttpException) {
                assertEquals(503, e.code())
            }
            assertEquals("5", db.syncMeta().get(SyncMeta.PULL_CURSOR))

            pulls = { since -> emptyPage(since!!.toLong()) }
            engine.sync()
            assertEquals("5", pullSince.last())
        }

    @Test
    fun anotherAccountsDatabaseIsNeverSynced() =
        runBlocking {
            db.syncMeta().put(SyncMetaEntity(SyncMeta.OWNER, UUID.randomUUID().toString()))
            pendingAccount("Ajena")

            try {
                engine.sync()
                fail("must refuse")
            } catch (e: ForeignDataException) {
                assertTrue(pushes.isEmpty() && pullSince.isEmpty())
            }
        }

    @Test
    fun withoutSessionNothingHappens() =
        runBlocking {
            store.clear()
            engine.sync()
            assertTrue(pushes.isEmpty() && pullSince.isEmpty())
            assertNull(db.syncMeta().get(SyncMeta.OWNER))
        }

    private suspend fun initialSyncDone() {
        db.syncMeta().put(SyncMetaEntity(SyncMeta.INITIAL_SYNC_DONE, "true"))
    }

    private suspend fun pendingAccount(name: String): UUID {
        val id = UUID.randomUUID()
        db.accounts().upsert(listOf(account(id, name, clock.now())))
        return id
    }

    private fun account(
        id: UUID,
        name: String,
        at: Instant,
    ) = AccountEntity(id = id, name = name, type = "SAVINGS", initialBalance = 0, createdAt = at, updatedAt = at)

    private fun allApplied(body: JsonObject): MockResponse {
        val results = body["changes"]!!.jsonArray.joinToString(",") { """{"id":${it.jsonObject["id"]},"status":"APPLIED","version":1}""" }
        return json("""{"results":[$results]}""")
    }

    private fun emptyPage(since: Long) = page(emptyList(), next = since, hasMore = false, remaining = if (since == 0L) 0 else null)

    private fun page(
        changes: List<String>,
        next: Long,
        hasMore: Boolean,
        remaining: Long? = null,
    ) = json(
        """{"changes":[${changes.joinToString(
            ",",
        )}],"nextCursor":$next,"hasMore":$hasMore${remaining?.let { ",\"remaining\":$it" } ?: ""}}""",
    )

    private fun accountChange(id: UUID) =
        change(
            "account",
            id,
            """{"name":"Bancolombia","type":"SAVINGS","initialBalance":"1250000.0000","includeInTotal":true,"active":true,"sortOrder":0}""",
        )

    private fun categoryChange(id: UUID) =
        change("category", id, """{"parentId":null,"name":"Comida","kind":"EXPENSE","isBase":true,"sortOrder":0}""")

    private fun transactionChange(
        id: UUID,
        account: UUID,
        category: UUID,
    ) = change(
        "transaction",
        id,
        """{"type":"EXPENSE","amount":"28000.0000","occurredAt":"2026-10-16T12:00:00Z","accountId":"$account","categoryId":"$category","installments":1}""",
    )

    private fun change(
        entity: String,
        id: UUID,
        data: String,
    ) =
        """{"entity":"$entity","id":"$id","createdAt":"2026-10-16T10:00:00Z","updatedAt":"2026-10-16T10:00:00Z","deletedAt":null,"data":$data,"version":1}"""

    private fun json(body: String) =
        MockResponse
            .Builder()
            .code(200)
            .addHeader("Content-Type", "application/json")
            .body(body)
            .build()
}
