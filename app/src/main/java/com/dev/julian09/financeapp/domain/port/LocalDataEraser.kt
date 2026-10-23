package com.dev.julian09.financeapp.domain.port

/** Wipes everything the user owns on this phone: the account no longer exists, so nothing is worth keeping. */
interface LocalDataEraser {
    suspend fun eraseAll()
}
