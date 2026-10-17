package com.dev.julian09.financeapp.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import com.dev.julian09.financeapp.data.session.EncryptedSessionStore
import com.dev.julian09.financeapp.data.session.KeystoreCipher
import com.dev.julian09.financeapp.data.session.SessionFile
import com.dev.julian09.financeapp.data.session.SessionFileSerializer
import com.dev.julian09.financeapp.data.session.SessionStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SessionModule {
    @Binds
    abstract fun bindSessionStore(impl: EncryptedSessionStore): SessionStore

    companion object {
        @Provides
        @Singleton
        fun provideSessionDataStore(
            @ApplicationContext context: Context,
            cipher: KeystoreCipher,
            json: Json,
        ): DataStore<SessionFile> =
            DataStoreFactory.create(
                serializer = SessionFileSerializer(cipher, json),
                // Undecryptable file (key gone after reinstall/restore) = signed out, never a crash
                corruptionHandler = ReplaceFileCorruptionHandler { SessionFile() },
                produceFile = { context.dataStoreFile("session.bin") },
            )
    }
}
