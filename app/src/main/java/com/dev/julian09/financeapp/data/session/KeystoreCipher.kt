package com.dev.julian09.financeapp.data.session

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/** AES-256-GCM with a key that never leaves Android Keystore (RNF-09). Output: 12-byte IV + ciphertext with tag. */
@Singleton
class KeystoreCipher
    @Inject
    constructor() {
        fun encrypt(
            plain: ByteArray,
            alias: String = SESSION_KEY,
        ): ByteArray {
            val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key(alias)) }
            return cipher.iv + cipher.doFinal(plain)
        }

        fun decrypt(
            data: ByteArray,
            alias: String = SESSION_KEY,
        ): ByteArray {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(alias), GCMParameterSpec(TAG_BITS, data, 0, IV_BYTES))
            return cipher.doFinal(data, IV_BYTES, data.size - IV_BYTES)
        }

        private fun key(alias: String): SecretKey {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
            val spec =
                KeyGenParameterSpec
                    .Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).apply { init(spec) }.generateKey()
        }

        companion object {
            /** One key per purpose (ADR-0008): session tokens and the database passphrase never share a key. */
            const val SESSION_KEY = "coffer_session_key"
            const val DATABASE_KEY = "coffer_database_key"
            private const val ANDROID_KEYSTORE = "AndroidKeyStore"
            private const val TRANSFORMATION = "AES/GCM/NoPadding"
            private const val IV_BYTES = 12
            private const val TAG_BITS = 128
        }
    }
