package com.dev.julian09.financeapp.ui

import com.dev.julian09.financeapp.domain.port.LocalDataEraser

class FakeLocalDataEraser : LocalDataEraser {
    var erased = false

    override suspend fun eraseAll() {
        erased = true
    }

    val checked = mutableListOf<String>()

    override suspend fun eraseIfOwnedByAnother(userId: String): Boolean {
        checked += userId
        return false
    }
}
