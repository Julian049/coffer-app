package com.dev.julian09.financeapp.ui.feature.transactions

import com.dev.julian09.financeapp.domain.model.TransactionType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.UUID

/** 2.2: the main button says «Siguiente» until only saving is left. */
class ReadyToSaveTest {
    private val id = UUID.randomUUID()

    private fun form(
        type: TransactionType = TransactionType.EXPENSE,
        amountDigits: String = "",
        accountId: UUID? = null,
        categoryId: UUID? = null,
        destinationId: UUID? = null,
        editingId: UUID? = null,
        draftId: UUID? = null,
    ) = TransactionFormState(
        type = type,
        amountDigits = amountDigits,
        categoryId = categoryId,
        accountId = accountId,
        destinationId = destinationId,
        occurredAt = Instant.EPOCH,
        editingId = editingId,
        draftId = draftId,
    )

    @Test
    fun `a new expense needs the amount, the account and the category`() {
        assertFalse(readyToSave(form(amountDigits = "5000", accountId = id)))
        assertFalse(readyToSave(form(accountId = id, categoryId = id)))
        assertTrue(readyToSave(form(amountDigits = "5000", accountId = id, categoryId = id)))
    }

    @Test
    fun `a transfer needs the destination instead of the category`() {
        val transfer = form(type = TransactionType.TRANSFER, amountDigits = "5000", accountId = id)
        assertFalse(readyToSave(transfer))
        assertTrue(readyToSave(form(TransactionType.TRANSFER, "5000", id, destinationId = id)))
    }

    @Test
    fun `editing a movement or reviewing a draft keeps the usual button`() {
        assertTrue(readyToSave(form(editingId = id)))
        assertTrue(readyToSave(form(draftId = id)))
    }
}
