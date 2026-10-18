package com.dev.julian09.financeapp.data.local

import android.content.Context
import com.dev.julian09.financeapp.data.session.KeystoreCipher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 256-bit random passphrase for SQLCipher, stored only wrapped by a Keystore AES-GCM key (ADR-0008), in
 * noBackupFilesDir. If it cannot be unwrapped (Keystore wiped) the local database is unreadable: it is deleted and
 * rebuilt from the cloud (HU-06).
 */
@Singleton
class DatabaseKeyProvider
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val cipher: KeystoreCipher,
    ) {
        private val keyFile get() = File(context.noBackupFilesDir, "finance.db.key")

        fun passphrase(): ByteArray {
            if (keyFile.exists()) {
                try {
                    return cipher.decrypt(keyFile.readBytes(), KeystoreCipher.DATABASE_KEY)
                } catch (e: GeneralSecurityException) {
                    // Without the old key the old file is noise; start clean and let the initial download refill it
                    context.deleteDatabase(FinanceDatabase.NAME)
                }
            }
            val key = ByteArray(32).also(SecureRandom()::nextBytes)
            keyFile.writeBytes(cipher.encrypt(key, KeystoreCipher.DATABASE_KEY))
            return key
        }
    }
