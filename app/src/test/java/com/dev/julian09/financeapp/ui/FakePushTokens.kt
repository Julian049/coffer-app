package com.dev.julian09.financeapp.ui

import com.dev.julian09.financeapp.domain.port.PushTokens

class FakePushTokens : PushTokens {
    var registered = 0
    var unregistered = 0

    override suspend fun register() {
        registered++
    }

    override suspend fun unregister() {
        unregistered++
    }
}
