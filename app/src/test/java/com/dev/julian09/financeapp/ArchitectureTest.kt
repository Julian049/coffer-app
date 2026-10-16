package com.dev.julian09.financeapp

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.functions
import com.lemonappdev.konsist.api.ext.list.properties
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.Test

/** Layer rules of detailed-design §6.4. The MVP under `legacy` is out of scope until Sprint 3 removes it. */
class ArchitectureTest {
    private val domain = Konsist.scopeFromPackage("$BASE.domain..", sourceSetName = "main")
    private val ui = Konsist.scopeFromPackage("$BASE.ui..", sourceSetName = "main")

    @Test
    fun `domain does not depend on Android or data`() {
        domain.files.assertFalse { file ->
            file.imports.any { it.name.startsWith("android.") || it.name.startsWith("androidx.") || it.name.startsWith("$BASE.data") }
        }
    }

    // ADR-0004: money is never a binary floating point number
    @Test
    fun `domain has no Double or Float`() {
        domain.files.properties(includeNested = true).assertFalse { it.type?.name in FLOATING }
        domain.files.functions(includeNested = true).assertFalse { fn ->
            fn.returnType?.name in FLOATING || fn.parameters.any { it.type.name in FLOATING }
        }
    }

    @Test
    fun `ui does not import data`() {
        ui.files.assertFalse { file -> file.imports.any { it.name.startsWith("$BASE.data") } }
    }

    private companion object {
        const val BASE = "com.dev.julian09.financeapp"
        val FLOATING = setOf("Double", "Float", "Double?", "Float?")
    }
}
