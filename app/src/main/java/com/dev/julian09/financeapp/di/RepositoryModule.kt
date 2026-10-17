package com.dev.julian09.financeapp.di

import com.dev.julian09.financeapp.data.clock.SystemClock
import com.dev.julian09.financeapp.data.repository.ServerStatusRepositoryImpl
import com.dev.julian09.financeapp.domain.port.Clock
import com.dev.julian09.financeapp.domain.port.ServerStatusRepository
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
    abstract fun bindServerStatus(impl: ServerStatusRepositoryImpl): ServerStatusRepository
}
