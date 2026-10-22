package com.dev.julian09.financeapp.ui.feature.settings

/** Open-source pieces Coffer is built with and the license each one carries (full text: docs/10-legal/third-party-notices.md). */
data class Notice(
    val name: String,
    val license: String,
)

val THIRD_PARTY_NOTICES =
    listOf(
        Notice("Kotlin, kotlinx.coroutines y kotlinx.serialization", "Apache-2.0"),
        Notice("AndroidX: Compose, Material 3, Lifecycle, Room, WorkManager, DataStore, Credential Manager, Hilt", "Apache-2.0"),
        Notice("Material Icons", "Apache-2.0"),
        Notice("Dagger y Hilt", "Apache-2.0"),
        Notice("OkHttp y Retrofit", "Apache-2.0"),
        Notice("SQLCipher para Android (Zetetic)", "BSD-style"),
        Notice("Servicios de Google Play (inicio con Google)", "Términos del SDK de Android"),
    )
