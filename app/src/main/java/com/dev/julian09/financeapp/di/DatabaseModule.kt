package com.dev.julian09.financeapp.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.dev.julian09.financeapp.data.local.DatabaseKeyProvider
import com.dev.julian09.financeapp.data.local.FinanceDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        keys: DatabaseKeyProvider,
    ): FinanceDatabase {
        System.loadLibrary("sqlcipher")
        return Room
            .databaseBuilder(context, FinanceDatabase::class.java, FinanceDatabase.NAME)
            .openHelperFactory(SupportOpenHelperFactory(keys.passphrase()))
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .build()
    }

    @Provides
    fun provideAccountDao(db: FinanceDatabase) = db.accounts()

    @Provides
    fun provideCategoryDao(db: FinanceDatabase) = db.categories()

    @Provides
    fun provideTagDao(db: FinanceDatabase) = db.tags()

    @Provides
    fun provideTransactionDao(db: FinanceDatabase) = db.transactions()

    @Provides
    fun provideSettingsDao(db: FinanceDatabase) = db.settings()

    @Provides
    fun provideSyncMetaDao(db: FinanceDatabase) = db.syncMeta()
}
