package com.dev.julian09.financeapp.data.session

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.security.GeneralSecurityException
import javax.inject.Inject
import javax.inject.Singleton

/** File content of the session DataStore; null session = signed out. */
@Serializable
data class SessionFile(
    val session: StoredSession? = null,
)

/**
 * Typed DataStore whose bytes are AES-GCM encrypted with a Keystore key. If the file cannot be decrypted
 * (key lost after a reinstall or a restore to another phone) the session is simply gone: the user signs in again.
 */
class SessionFileSerializer(
    private val cipher: KeystoreCipher,
    private val json: Json,
) : Serializer<SessionFile> {
    override val defaultValue = SessionFile()

    override suspend fun readFrom(input: InputStream): SessionFile {
        val bytes = input.readBytes()
        if (bytes.isEmpty()) return defaultValue
        return try {
            json.decodeFromString(SessionFile.serializer(), cipher.decrypt(bytes).decodeToString())
        } catch (e: GeneralSecurityException) {
            throw CorruptionException("Session cannot be decrypted", e)
        }
    }

    override suspend fun writeTo(
        t: SessionFile,
        output: OutputStream,
    ) {
        output.write(cipher.encrypt(json.encodeToString(SessionFile.serializer(), t).encodeToByteArray()))
    }
}

@Singleton
class EncryptedSessionStore
    @Inject
    constructor(
        private val dataStore: DataStore<SessionFile>,
    ) : SessionStore {
        override val session: Flow<StoredSession?> = dataStore.data.map { it.session }

        override suspend fun current(): StoredSession? = session.first()

        override suspend fun save(session: StoredSession) {
            dataStore.updateData { SessionFile(session) }
        }

        override suspend fun markExpired() {
            dataStore.updateData { file -> file.copy(session = file.session?.copy(expired = true)) }
        }

        override suspend fun clear() {
            dataStore.updateData { SessionFile() }
        }
    }
