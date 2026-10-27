package com.dev.julian09.financeapp.domain.model

/** Profile of the signed-in user (GET /api/v1/me). */
data class UserProfile(
    val id: String,
    val username: String,
    val displayName: String,
    val email: String,
    val currency: String,
    val googleLinked: Boolean,
    /** REQ-COR-01: where this user forwards bank receipts; null until the server sends it. */
    val forwardingAddress: String? = null,
    /** Gmail's code to accept the forward, once it arrived. */
    val forwardingConfirmation: String? = null,
)

/** What the app shows at start: sign-in, or the app. Expired keeps the local data (REQ-AUT-04, SEQ-03). */
sealed interface SessionState {
    data object Loading : SessionState

    data object SignedOut : SessionState

    data class Active(
        val user: UserProfile,
    ) : SessionState

    data class Expired(
        val user: UserProfile,
    ) : SessionState
}
