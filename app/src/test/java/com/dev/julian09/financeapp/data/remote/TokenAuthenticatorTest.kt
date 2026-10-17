package com.dev.julian09.financeapp.data.remote

import com.dev.julian09.financeapp.data.session.FakeSessionStore
import com.dev.julian09.financeapp.data.session.StoredSession
import com.dev.julian09.financeapp.data.session.TEST_USER
import kotlinx.coroutines.runBlocking
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.Request
import okhttp3.RequestBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class TokenAuthenticatorTest {
    private val server = MockWebServer()
    private val store = FakeSessionStore(StoredSession("old-access", "old-refresh-0123456789", TEST_USER))
    private val refreshes = AtomicInteger()
    private var refreshCode = 200

    @Before
    fun start() {
        server.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse =
                    when (request.url.encodedPath) {
                        "/api/v1/auth/refresh" -> {
                            refreshes.incrementAndGet()
                            Thread.sleep(100) // keeps simultaneous callers waiting on the lock
                            if (refreshCode == 200) {
                                json(200, authBody("new-access", "new-refresh-0123456789"))
                            } else {
                                json(401, """{"code":"FIN-AUTH-REVOKED"}""")
                            }
                        }
                        else ->
                            if (request.headers["Authorization"] == "Bearer new-access") {
                                json(200, "{}")
                            } else {
                                json(401, """{"code":"FIN-AUTH-INVALID"}""")
                            }
                    }
            }
        server.start()
    }

    @After
    fun stop() = server.close()

    @Test
    fun `401 refreshes once and retries with the new token`() {
        val network = TestNetwork(server, store)

        val response = network.client.newCall(get("/api/v1/me")).execute()

        assertEquals(200, response.code)
        assertEquals(1, refreshes.get())
        val saved = runBlocking { store.current() }!!
        assertEquals("new-access", saved.accessToken)
        assertEquals("new-refresh-0123456789", saved.refreshToken)
    }

    @Test
    fun `simultaneous 401s share a single refresh`() {
        val network = TestNetwork(server, store)
        val pool = Executors.newFixedThreadPool(4)
        val done = CountDownLatch(4)
        val codes = Collections.synchronizedList(mutableListOf<Int>())
        repeat(4) {
            pool.execute {
                codes +=
                    network.client
                        .newCall(get("/api/v1/me"))
                        .execute()
                        .use { it.code }
                done.countDown()
            }
        }
        assertTrue(done.await(10, TimeUnit.SECONDS))
        pool.shutdown()

        assertEquals(listOf(200, 200, 200, 200), codes.toList())
        assertEquals(1, refreshes.get())
    }

    @Test
    fun `a revoked refresh expires the session and keeps the profile`() {
        refreshCode = 401
        val network = TestNetwork(server, store)

        val response = network.client.newCall(get("/api/v1/me")).execute()

        assertEquals(401, response.code)
        val saved = runBlocking { store.current() }!!
        assertTrue(saved.expired)
        assertEquals(TEST_USER, saved.user)
    }

    @Test
    fun `public auth routes never carry the bearer token`() {
        val network = TestNetwork(server, store)
        network.client
            .newCall(
                Request
                    .Builder()
                    .url(server.url("/api/v1/auth/login"))
                    .post(RequestBody.EMPTY)
                    .build(),
            ).execute()
            .close()

        val recorded = server.takeRequest()
        assertEquals(null, recorded.headers["Authorization"])
    }

    private fun get(path: String) = Request.Builder().url(server.url(path)).build()

    private fun json(
        code: Int,
        body: String,
    ) = MockResponse
        .Builder()
        .code(code)
        .addHeader("Content-Type", "application/json")
        .body(body)
        .build()
}
