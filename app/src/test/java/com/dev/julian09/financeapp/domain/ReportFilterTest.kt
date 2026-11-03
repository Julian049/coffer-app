package com.dev.julian09.financeapp.domain

import com.dev.julian09.financeapp.domain.model.DeferredPurchase
import com.dev.julian09.financeapp.domain.model.Money
import com.dev.julian09.financeapp.domain.model.RateType
import com.dev.julian09.financeapp.domain.model.Transaction
import com.dev.julian09.financeapp.domain.model.TransactionType
import com.dev.julian09.financeapp.domain.usecase.ReportFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class ReportFilterTest {
    private val cash = UUID.randomUUID()
    private val bank = UUID.randomUUID()
    private val card = UUID.randomUUID()

    private fun movement(
        type: TransactionType,
        account: UUID,
        to: UUID? = null,
    ) = Transaction(UUID.randomUUID(), type, Money.ofPesos(1_000), Instant.EPOCH, account, destinationAccountId = to)

    private fun plan(card: UUID) =
        DeferredPurchase(
            UUID.randomUUID(),
            card,
            UUID.randomUUID(),
            Money.ofPesos(1_200),
            12,
            BigDecimal("0.02"),
            RateType.MV,
            false,
            LocalDate.EPOCH,
        )

    @Test
    fun sinFiltroEntraTodo() {
        val f = ReportFilter()
        assertTrue(f.keeps(movement(TransactionType.EXPENSE, cash)))
        assertTrue(f.keeps(movement(TransactionType.INCOME, bank)))
        assertEquals(1, f.deferred(listOf(plan(card))).size)
    }

    @Test
    fun lasCuentasElegidasCuentanComoOrigenOComoDestino() {
        val f = ReportFilter(accountIds = setOf(bank))
        assertTrue(f.keeps(movement(TransactionType.EXPENSE, bank)))
        assertTrue(f.keeps(movement(TransactionType.TRANSFER, cash, to = bank)))
        assertFalse(f.keeps(movement(TransactionType.EXPENSE, cash)))
        // a deferred purchase belongs to its card
        assertEquals(0, f.deferred(listOf(plan(card))).size)
        assertEquals(1, ReportFilter(accountIds = setOf(card)).deferred(listOf(plan(card))).size)
    }

    @Test
    fun soloIngresosDejaFueraLosGastosYLasComprasDiferidas() {
        val f = ReportFilter(type = TransactionType.INCOME)
        assertTrue(f.keeps(movement(TransactionType.INCOME, cash)))
        assertFalse(f.keeps(movement(TransactionType.EXPENSE, cash)))
        assertEquals(0, f.deferred(listOf(plan(card))).size)
        assertEquals(1, ReportFilter(type = TransactionType.EXPENSE).deferred(listOf(plan(card))).size)
    }
}
