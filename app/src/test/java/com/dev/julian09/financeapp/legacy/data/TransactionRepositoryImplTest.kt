package com.dev.julian09.financeapp.legacy.data

import com.dev.julian09.financeapp.legacy.data.local.TransactionDao
import com.dev.julian09.financeapp.legacy.data.remote.ApiService
import com.dev.julian09.financeapp.legacy.domain.model.Transaction
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TransactionRepositoryImplTest {
    private lateinit var repository: TransactionRepositoryImpl
    private val dao: TransactionDao = mockk(relaxed = true)
    private val api: ApiService = mockk()

    @Before
    fun onBefore() {
        repository = TransactionRepositoryImpl(dao, api)
    }

    @Test
    fun `getTotalAmount deberia sumar correctamente todos los valores`() =
        runBlocking {
            val transactions =
                listOf(
                    Transaction("1", "Sueldo", 1000.0, true, "", "", true),
                    Transaction("2", "Renta", -500.0, false, "", "", true),
                    Transaction("3", "Comida", -100.0, false, "", "", true),
                )

            val result = repository.getTotalAmount(transactions)

            assertEquals(400.0, result, 0.0)
        }
}
