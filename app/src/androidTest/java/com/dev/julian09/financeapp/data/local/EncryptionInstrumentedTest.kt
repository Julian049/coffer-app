package com.dev.julian09.financeapp.data.local

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dev.julian09.financeapp.data.local.entity.SyncMetaEntity
import com.dev.julian09.financeapp.data.session.KeystoreCipher
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * RNF-08: the database file is unreadable without its key. Run by hand on a device or an emulator with API 31–36:
 * ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.dev.julian09.financeapp.data.local.EncryptionInstrumentedTest
 */
@RunWith(AndroidJUnit4::class)
class EncryptionInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "encryption-test.db"

    @Test
    fun theFileIsEncryptedAndReadableOnlyWithTheKey() =
        runBlocking {
            context.deleteDatabase(name)
            System.loadLibrary("sqlcipher")
            val key = DatabaseKeyProvider(context, KeystoreCipher()).passphrase()
            val db =
                Room
                    .databaseBuilder(context, FinanceDatabase::class.java, name)
                    .openHelperFactory(SupportOpenHelperFactory(key))
                    .build()
            db.syncMeta().put(SyncMetaEntity("probe", "texto-secreto"))
            assertEquals("texto-secreto", db.syncMeta().get("probe"))
            db.close()

            val file = context.getDatabasePath(name)
            val header = file.readBytes().copyOf(16).decodeToString()
            assertFalse("plain SQLite header", header.startsWith("SQLite format 3"))
            assertFalse("value in clear text", file.readBytes().decodeToString().contains("texto-secreto"))
            // Plain SQLite (no key) must not be able to read it
            val opened = runCatching { SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { it.version } }
            assertTrue("opened without the key", opened.exceptionOrNull() is SQLiteException)
            context.deleteDatabase(name)
            File(context.noBackupFilesDir, "finance.db.key").delete()
        }
}
