package com.dev.julian09.financeapp.domain.port

/** Wipes everything the user owns on this phone: the account no longer exists, so nothing is worth keeping. */
interface LocalDataEraser {
    suspend fun eraseAll()

    /** INC-01: the phone keeps the data at sign-out (REQ-AUT-06), but another account must never see it; true when erased. */
    suspend fun eraseIfOwnedByAnother(userId: String): Boolean
}
