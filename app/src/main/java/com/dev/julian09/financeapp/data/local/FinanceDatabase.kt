package com.dev.julian09.financeapp.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dev.julian09.financeapp.data.local.dao.AccountDao
import com.dev.julian09.financeapp.data.local.dao.CardDao
import com.dev.julian09.financeapp.data.local.dao.CategoryDao
import com.dev.julian09.financeapp.data.local.dao.ChangeLogDao
import com.dev.julian09.financeapp.data.local.dao.LoanDao
import com.dev.julian09.financeapp.data.local.dao.RecurringDao
import com.dev.julian09.financeapp.data.local.dao.SettingsDao
import com.dev.julian09.financeapp.data.local.dao.SyncMetaDao
import com.dev.julian09.financeapp.data.local.dao.TagDao
import com.dev.julian09.financeapp.data.local.dao.TransactionDao
import com.dev.julian09.financeapp.data.local.entity.AccountEntity
import com.dev.julian09.financeapp.data.local.entity.CategoryEntity
import com.dev.julian09.financeapp.data.local.entity.ChangeLogEntity
import com.dev.julian09.financeapp.data.local.entity.CreditCardDetailsEntity
import com.dev.julian09.financeapp.data.local.entity.DeferredPurchaseEntity
import com.dev.julian09.financeapp.data.local.entity.InstallmentEntity
import com.dev.julian09.financeapp.data.local.entity.LoanDetailsEntity
import com.dev.julian09.financeapp.data.local.entity.RecurringExpenseEntity
import com.dev.julian09.financeapp.data.local.entity.SettingsEntity
import com.dev.julian09.financeapp.data.local.entity.SyncMetaEntity
import com.dev.julian09.financeapp.data.local.entity.TagEntity
import com.dev.julian09.financeapp.data.local.entity.TransactionEntity

/**
 * finance.db, encrypted with SQLCipher (ADR-0008). Version 7 with exported schemas (app/schemas); every later change
 * is a tested Migration, never a destructive fallback (data-model §5.2).
 */
@Database(
    entities = [
        SettingsEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        TagEntity::class,
        TransactionEntity::class,
        SyncMetaEntity::class,
        ChangeLogEntity::class,
        RecurringExpenseEntity::class,
        CreditCardDetailsEntity::class,
        DeferredPurchaseEntity::class,
        InstallmentEntity::class,
        LoanDetailsEntity::class,
    ],
    version = 8,
    exportSchema = true,
    // 1 → 2 adds change_log (S5), 2 → 3 recurring_expenses (S6), 3 → 4 the card tables (S7), 4 → 5 categories.excludeFromReports (S9), 5 → 6 loan_details (S10), 6 → 7 transactions.concept and settings.netBalanceMode (S12), 7 → 8 settings.loanPaymentCategoryId and accounts.includeInNet (S15);
    // FinanceDatabaseMigrationTest checks them
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
    ],
)
@TypeConverters(Converters::class)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun accounts(): AccountDao

    abstract fun categories(): CategoryDao

    abstract fun tags(): TagDao

    abstract fun transactions(): TransactionDao

    abstract fun settings(): SettingsDao

    abstract fun syncMeta(): SyncMetaDao

    abstract fun changeLog(): ChangeLogDao

    abstract fun recurring(): RecurringDao

    abstract fun cards(): CardDao

    abstract fun loans(): LoanDao

    companion object {
        const val NAME = "finance.db"
    }
}
