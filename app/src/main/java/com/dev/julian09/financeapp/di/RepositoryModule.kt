package com.dev.julian09.financeapp.di

import com.dev.julian09.financeapp.data.clock.SystemClock
import com.dev.julian09.financeapp.data.reminder.NotificationPublisher
import com.dev.julian09.financeapp.data.reminder.ReminderSchedulerImpl
import com.dev.julian09.financeapp.data.repository.AccountRepositoryImpl
import com.dev.julian09.financeapp.data.repository.AuthRepositoryImpl
import com.dev.julian09.financeapp.data.repository.CategoryRepositoryImpl
import com.dev.julian09.financeapp.data.repository.RecurringRepositoryImpl
import com.dev.julian09.financeapp.data.repository.ServerStatusRepositoryImpl
import com.dev.julian09.financeapp.data.repository.SettingsRepositoryImpl
import com.dev.julian09.financeapp.data.repository.TagRepositoryImpl
import com.dev.julian09.financeapp.data.repository.TransactionRepositoryImpl
import com.dev.julian09.financeapp.data.sync.SyncSchedulerImpl
import com.dev.julian09.financeapp.domain.port.AccountRepository
import com.dev.julian09.financeapp.domain.port.AuthRepository
import com.dev.julian09.financeapp.domain.port.CategoryRepository
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.port.RecurringRepository
import com.dev.julian09.financeapp.domain.port.ReminderNotifier
import com.dev.julian09.financeapp.domain.port.ReminderScheduler
import com.dev.julian09.financeapp.domain.port.ServerStatusRepository
import com.dev.julian09.financeapp.domain.port.SettingsRepository
import com.dev.julian09.financeapp.domain.port.SyncScheduler
import com.dev.julian09.financeapp.domain.port.TagRepository
import com.dev.julian09.financeapp.domain.port.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Binds domain ports to their data implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindClock(impl: SystemClock): Clock

    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun bindServerStatus(impl: ServerStatusRepositoryImpl): ServerStatusRepository

    @Binds
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    abstract fun bindTagRepository(impl: TagRepositoryImpl): TagRepository

    @Binds
    abstract fun bindRecurringRepository(impl: RecurringRepositoryImpl): RecurringRepository

    @Binds
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    abstract fun bindReminderNotifier(impl: NotificationPublisher): ReminderNotifier

    @Binds
    abstract fun bindReminderScheduler(impl: ReminderSchedulerImpl): ReminderScheduler

    @Binds
    abstract fun bindSyncScheduler(impl: SyncSchedulerImpl): SyncScheduler
}
