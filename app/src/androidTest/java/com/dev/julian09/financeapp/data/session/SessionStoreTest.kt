package com.dev.julian09.financeapp.data.session

import androidx.datastore.core.DataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Real Android Keystore + DataStore. Run by hand on a device or emulator (decision of the S2 planning):
 * ./gradlew connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class SessionStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val file = File(context.filesDir, "session-test.bin").also { it.delete() }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val store =
        EncryptedSessionStore(
            DataStoreFactory.create(SessionFileSerializer(KeystoreCipher(), Json), scope = scope, produceFile = { file }),
        )
    private val session =
        StoredSession("access-token-plain", "refresh-token-plain", StoredUser("u-1", "julianb", "Julian", "j@c.com", "COP"))

    @After
    fun tearDown() {
        scope.cancel()
        file.delete()
    }

    @Test
    fun savesEncryptedAndReadsBack() =
        runBlocking {
            store.save(session)

            assertEquals(session, store.current())
            val raw = file.readBytes().decodeToString()
            assertFalse("token stored in clear text", raw.contains("refresh-token-plain"))
            assertFalse("username stored in clear text", raw.contains("julianb"))
        }

    @Test
    fun expiringKeepsTheProfileAndClearForgetsEverything() =
        runBlocking {
            store.save(session)
            store.markExpired()
            assertTrue(store.current()!!.expired)
            assertEquals(session.user, store.current()!!.user)

            store.clear()
            assertNull(store.current())
        }
}
