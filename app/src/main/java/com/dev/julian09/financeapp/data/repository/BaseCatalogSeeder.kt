package com.dev.julian09.financeapp.data.repository

import com.dev.julian09.financeapp.data.local.FinanceDatabase
import com.dev.julian09.financeapp.data.local.entity.CategoryEntity
import com.dev.julian09.financeapp.data.local.entity.SettingsEntity
import com.dev.julian09.financeapp.domain.port.Clock
import java.util.UUID
import javax.inject.Inject

/**
 * REQ-CAT-01: base categories (detailed-design §6.9.10) and default settings, only for a new account — the initial
 * download brought none. New rows are PENDING, so the next push backs them up.
 */
class BaseCatalogSeeder
    @Inject
    constructor(
        private val db: FinanceDatabase,
        private val clock: Clock,
    ) {
        /** Returns true when it created something that must be pushed. */
        suspend fun ensureBaseCatalog(userId: UUID): Boolean {
            var created = false
            val now = clock.now()
            if (db.categories().count() == 0) {
                val rows = mutableListOf<CategoryEntity>()
                BASE.forEachIndexed { index, base ->
                    val parent =
                        CategoryEntity(
                            id = UUID.randomUUID(),
                            name = base.name,
                            kind = base.kind,
                            isBase = true,
                            systemKey = base.systemKey,
                            sortOrder = index,
                            createdAt = now,
                            updatedAt = now,
                        )
                    rows += parent
                    base.children.forEachIndexed { childIndex, child ->
                        rows +=
                            CategoryEntity(
                                id = UUID.randomUUID(),
                                parentId = parent.id,
                                name = child,
                                kind = base.kind,
                                isBase = true,
                                sortOrder = childIndex,
                                createdAt = now,
                                updatedAt = now,
                            )
                    }
                }
                db.categories().upsert(rows)
                created = true
            }
            if (db.settings().count() == 0) {
                // One settings row per user, identified by the user id (server ck_settings_id)
                db.settings().upsert(
                    SettingsEntity(
                        id = userId,
                        reminderLeadDays = 5,
                        notificationsEnabled = true,
                        homeBalanceView = "AVAILABLE",
                        amountDecimals = 0,
                        homeCards = "[]",
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
                created = true
            }
            return created
        }

        private class Base(
            val name: String,
            val kind: String,
            val children: List<String> = emptyList(),
            val systemKey: String? = null,
        )

        private companion object {
            // Approved by the PO in the S3 planning (detailed-design §6.9.10)
            val BASE =
                listOf(
                    Base("Comida", "EXPENSE", listOf("Restaurantes", "Supermercado", "Cafeterías")),
                    Base("Transporte", "EXPENSE", listOf("Taxi / Apps", "Gasolina", "Mantenimiento")),
                    Base("Hogar", "EXPENSE", listOf("Servicios públicos", "Arriendo", "Mantenimiento")),
                    Base("Entretenimiento", "EXPENSE", listOf("Streaming", "Salidas", "Cine")),
                    Base("Salud", "EXPENSE", listOf("Farmacia", "Consultas")),
                    Base("Educación", "EXPENSE"),
                    Base("Intereses y cargos financieros", "EXPENSE", systemKey = "FINANCIAL_CHARGES"),
                    Base("Otros gastos", "EXPENSE"),
                    Base("Salario", "INCOME"),
                    Base("Otros ingresos", "INCOME"),
                )
        }
    }
