package com.dev.julian09.financeapp.di

import android.content.Context
import androidx.room.Room
import com.dev.julian09.financeapp.legacy.data.TransactionRepositoryImpl
import com.dev.julian09.financeapp.legacy.data.local.AppDatabase
import com.dev.julian09.financeapp.legacy.data.local.TransactionDao
import com.dev.julian09.financeapp.legacy.data.remote.ApiService
import com.dev.julian09.financeapp.legacy.domain.repository.TransactionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** MVP wiring, kept until the encrypted database and sync replace it in Sprint 3. */
@Module
@InstallIn(SingletonComponent::class)
object LegacyModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, "finance_db").build()

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides
    @Singleton
    fun provideTransactionRepository(
        apiService: ApiService,
        transactionDao: TransactionDao,
    ): TransactionRepository = TransactionRepositoryImpl(transactionDao, apiService)
}
