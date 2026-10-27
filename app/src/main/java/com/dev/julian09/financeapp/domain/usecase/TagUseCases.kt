package com.dev.julian09.financeapp.domain.usecase

import com.dev.julian09.financeapp.domain.model.LedgerError
import com.dev.julian09.financeapp.domain.model.TagColors
import com.dev.julian09.financeapp.domain.port.SyncScheduler
import com.dev.julian09.financeapp.domain.port.TagRepository
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

private const val TAG_NAME_MAX = 50

/** Unique ignoring case, like the server's uq_tag_name; a leading # is not part of the name. */
internal fun cleanTagName(name: String) = name.trim().removePrefix("#").trim()

internal suspend fun tagNameError(
    name: String,
    repository: TagRepository,
    except: UUID? = null,
): LedgerError? =
    when {
        name.isEmpty() -> LedgerError.NAME_REQUIRED
        name.length > TAG_NAME_MAX -> LedgerError.NAME_TOO_LONG
        repository.observeAll().first().any { it.id != except && it.name.equals(name, ignoreCase = true) } -> LedgerError.NAME_TAKEN
        else -> null
    }

/** S9-DOM02 (REQ-ETQ-01). */
class RenameTag
    @Inject
    constructor(
        private val repository: TagRepository,
        private val scheduler: SyncScheduler,
    ) {
        suspend operator fun invoke(
            id: UUID,
            name: String,
        ): LedgerError? {
            val clean = cleanTagName(name)
            tagNameError(clean, repository, except = id)?.let { return it }
            repository.rename(id, clean)
            scheduler.requestSync()
            return null
        }
    }

/** S9-DOM02: the movements stay, without a tag. */
class DeleteTag
    @Inject
    constructor(
        private val repository: TagRepository,
        private val scheduler: SyncScheduler,
    ) {
        suspend operator fun invoke(id: UUID) {
            repository.delete(id)
            scheduler.requestSync()
        }
    }

/** REQ-ETQ-04: pinned tags show their month total on Home. */
class PinTag
    @Inject
    constructor(
        private val repository: TagRepository,
        private val scheduler: SyncScheduler,
    ) {
        suspend operator fun invoke(
            id: UUID,
            pinned: Boolean,
        ) {
            repository.setPinned(id, pinned)
            scheduler.requestSync()
        }
    }

/** REQ-ETQ-05: only the 6 colors of [TagColors]. */
class SetTagColor
    @Inject
    constructor(
        private val repository: TagRepository,
        private val scheduler: SyncScheduler,
    ) {
        suspend operator fun invoke(
            id: UUID,
            color: String,
        ) {
            require(TagColors.entries.any { it.hex == color }) { "Color fuera de la paleta: $color" }
            repository.setColor(id, color)
            scheduler.requestSync()
        }
    }
