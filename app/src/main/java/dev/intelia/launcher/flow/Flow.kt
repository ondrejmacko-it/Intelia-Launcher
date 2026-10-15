package dev.intelia.launcher.flow

import kotlinx.serialization.Serializable
import java.text.Normalizer
import java.util.Locale

/**
 * A saved command: one exact name that opens 1–[Flow.MAX_TARGETS] apps in order.
 * Matching never executes anything; running requires an explicit user action.
 */
@Serializable
data class FlowAlias(val id: String, val name: String, val targets: List<String>)

enum class FlowError { Empty, ControlCharacter, TooLong, Duplicate, NoTargets, TooManyTargets, InvalidTarget }

object Flow {
    const val MAX_NAME = 64
    const val MAX_TARGETS = 4
    private const val MAX_TARGET_KEY = 512
    private val whitespace = Regex("[\\s\\p{Z}]+")

    fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKC)
        .replace(whitespace, " ").trim().lowercase(Locale.ROOT)

    fun validate(alias: FlowAlias, others: List<FlowAlias> = emptyList()): FlowError? {
        if (alias.name.codePoints().anyMatch(Character::isISOControl)) return FlowError.ControlCharacter
        val normalized = normalize(alias.name)
        if (normalized.isEmpty()) return FlowError.Empty
        if (normalized.codePointCount(0, normalized.length) > MAX_NAME) return FlowError.TooLong
        if (others.any { it.id != alias.id && normalize(it.name) == normalized }) return FlowError.Duplicate
        if (alias.targets.isEmpty()) return FlowError.NoTargets
        if (alias.targets.size > MAX_TARGETS) return FlowError.TooManyTargets
        if (alias.id.isBlank() || alias.targets.distinct().size != alias.targets.size ||
            alias.targets.any { it.isBlank() || it.length > MAX_TARGET_KEY || it.codePoints().anyMatch(Character::isISOControl) }
        ) return FlowError.InvalidTarget
        return null
    }

    /** Exact match first, then name prefixes; for preview only. */
    fun suggest(aliases: List<FlowAlias>, query: String): List<FlowAlias> {
        val normalized = normalize(query)
        if (normalized.isEmpty()) return emptyList()
        val exact = aliases.filter { normalize(it.name) == normalized }
        val prefix = aliases.filter { it !in exact && normalize(it.name).startsWith(normalized) }
        return exact + prefix
    }

    fun exact(aliases: List<FlowAlias>, query: String): FlowAlias? {
        val normalized = normalize(query)
        return if (normalized.isEmpty()) null else aliases.firstOrNull { normalize(it.name) == normalized }
    }

    /** All targets must resolve; a partial sequence is never dispatched. */
    fun <T : Any> resolve(alias: FlowAlias, lookup: (String) -> T?): List<T>? =
        alias.targets.map { lookup(it) ?: return null }
}
