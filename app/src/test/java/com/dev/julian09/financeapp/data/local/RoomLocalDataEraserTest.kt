package com.dev.julian09.financeapp.data.local

import com.dev.julian09.financeapp.data.local.entity.SyncMetaEntity
import com.dev.julian09.financeapp.data.sync.SyncMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** INC-01: another account never sees the data the phone kept at sign-out. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomLocalDataEraserTest {
    private val ledger = LedgerFixture()
    private val eraser = RoomLocalDataEraser(ledger.db, Dispatchers.Unconfined)

    @After
    fun close() = ledger.db.close()

    @Test
    fun otraCuentaBorraLosDatosDelTelefono() =
        runBlocking {
            ledger.db.syncMeta().put(SyncMetaEntity(SyncMeta.OWNER, "cuenta-a"))
            ledger.category("Comida")

            assertTrue(eraser.eraseIfOwnedByAnother("cuenta-b"))
            assertEquals(0, ledger.db.categories().count())
        }

    @Test
    fun laMismaCuentaConservaSusDatos() =
        runBlocking {
            ledger.db.syncMeta().put(SyncMetaEntity(SyncMeta.OWNER, "cuenta-a"))
            ledger.category("Comida")

            assertFalse(eraser.eraseIfOwnedByAnother("cuenta-a"))
            assertEquals(1, ledger.db.categories().count())
        }

    @Test
    fun sinDuenoNoBorraNada() =
        runBlocking {
            ledger.category("Comida")

            assertFalse(eraser.eraseIfOwnedByAnother("cuenta-a"))
            assertEquals(1, ledger.db.categories().count())
        }
}
