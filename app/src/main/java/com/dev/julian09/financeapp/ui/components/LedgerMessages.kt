package com.dev.julian09.financeapp.ui.components

import com.dev.julian09.financeapp.domain.model.LedgerError

/** What a refused ledger write tells the user; never technical text (RNF-07). */
fun LedgerError.message(): String =
    when (this) {
        LedgerError.NAME_REQUIRED -> "Escribe un nombre"
        LedgerError.NAME_TOO_LONG -> "El nombre es muy largo"
        LedgerError.NAME_TAKEN -> "Ya tienes una con ese nombre"
        LedgerError.TYPE_NOT_AVAILABLE -> "Este tipo de cuenta estará disponible pronto"
        LedgerError.TARGET_REQUIRED -> "Elige a dónde pasan los movimientos"
        LedgerError.TARGET_INVALID -> "Elige otra opción"
        LedgerError.CONFIRMATION_MISMATCH -> "El nombre no coincide"
        LedgerError.ONE_LEVEL_ONLY -> "Las subcategorías no pueden tener subcategorías"
        LedgerError.KIND_MISMATCH -> "La subcategoría debe ser del mismo tipo que su categoría"
        LedgerError.SYSTEM_CATEGORY_IN_USE -> "La app usa esta categoría para los intereses de tus tarjetas y préstamos"
        LedgerError.AMOUNT_REQUIRED -> "El monto debe ser mayor que cero"
        LedgerError.ACCOUNT_REQUIRED -> "Elige una cuenta"
        LedgerError.CATEGORY_REQUIRED -> "Elige una categoría"
        LedgerError.SAME_ACCOUNT -> "El destino debe ser otra cuenta"
        LedgerError.NOTE_TOO_LONG -> "La nota es muy larga"
    }
