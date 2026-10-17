package dev.intelia.launcher.action

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Gestures a trigger surface can report; each surface offers only the ones it can really detect. */
@Serializable enum class Gesture { Tap, DoubleTap, TripleTap, Hold, SwipeLeft, SwipeRight, SwipeUp, SwipeDown }

/**
 * Where an [Action] is started from (Control System V1). Typed and open for new surfaces; every trigger runs
 * only on an explicit user gesture, never while typing in Search.
 */
@Serializable
sealed interface Trigger {
    /** A slot of an ARC set (the ARC itself picks the set; slots are bound through [ArcSet.slots]). */
    @Serializable @SerialName("arc") data class ArcSlot(val set: String, val slot: Int) : Trigger

    @Serializable @SerialName("pulse") data class Pulse(val gesture: Gesture) : Trigger

    @Serializable @SerialName("home") data class Home(val gesture: Gesture) : Trigger

    @Serializable @SerialName("lens") data class Lens(val gesture: Gesture) : Trigger

    /** A hardware key by its Android key code, only once diagnostics proved it can be captured (M4). */
    @Serializable @SerialName("key") data class Key(val code: Int, val gesture: Gesture) : Trigger
}

@Serializable data class Binding(val trigger: Trigger, val action: Action)

/** One switchable ARC set: mixed apps, FLOWs, shortcuts and other actions; null is an empty slot. */
@Serializable data class ArcSet(val id: String, val name: String = "", val slots: List<Action?> = emptyList())

/**
 * The Control System configuration: ARC sets plus gesture bindings. Stored in settings as `controls`;
 * while that is absent, [migrate] derives it from the legacy `arc`, `arcFlows` and `arcShortcuts` lists
 * (which stay untouched), so existing content and order carry over.
 */
@Serializable
data class Controls(val arcSets: List<ArcSet> = emptyList(), val bindings: List<Binding> = emptyList()) {

    fun arcSet(id: String): ArcSet? = arcSets.firstOrNull { it.id == id }

    fun bound(trigger: Trigger): Action? = when (trigger) {
        is Trigger.ArcSlot -> arcSet(trigger.set)?.slots?.getOrNull(trigger.slot)
        else -> bindings.firstOrNull { it.trigger == trigger }?.action
    }

    /** The single validation gate for restored data: drops malformed actions, caps sizes, one binding per trigger. */
    fun sanitized(flowIds: Set<String>): Controls {
        fun ok(a: Action): Boolean = a.wellFormed() && (a !is Action.Flow || a.id in flowIds)
        val ids = HashSet<String>()
        val sets = arcSets.asSequence()
            .filter { Action.wellFormed(it.id) && ids.add(it.id) }
            .take(MAX_SETS)
            .map { set -> set.copy(name = set.name.filterNot(Char::isISOControl).take(MAX_NAME),
                slots = set.slots.take(MAX_SLOTS).map { a -> a?.takeIf(::ok) }) }
            .toList()
        val seen = HashSet<Trigger>()
        val kept = bindings.asSequence()
            .filter { it.trigger !is Trigger.ArcSlot && it.trigger.valid() && ok(it.action) && seen.add(it.trigger) }
            .take(MAX_BINDINGS).toList()
        return Controls(sets, kept)
    }

    companion object {
        const val MAIN_SET = "main"
        const val MAX_SETS = 8
        const val MAX_SLOTS = 12
        const val MAX_BINDINGS = 64
        private const val MAX_NAME = 32

        /**
         * Legacy ARC → one set, in ring order (apps, then FLOWs, then shortcuts), each list in its stored order.
         * Shortcut keys are "package/id"; FLOWs are referenced by their stable id, so renaming keeps the binding.
         */
        fun migrate(arc: List<String>, arcFlows: List<String>, arcShortcuts: List<String>): Controls {
            val slots = arc.map { Action.App(it) } +
                arcFlows.map { Action.Flow(it) } +
                arcShortcuts.mapNotNull { key -> key.indexOf('/').takeIf { it > 0 }?.let { Action.Shortcut(key.substring(0, it), key.substring(it + 1)) } }
            return Controls(arcSets = listOf(ArcSet(MAIN_SET, slots = slots)))
        }
    }
}

private fun Trigger.valid(): Boolean = when (this) {
    is Trigger.ArcSlot -> slot in 0 until Controls.MAX_SLOTS && Action.wellFormed(set)
    is Trigger.Key -> code in 1..MAX_KEY_CODE
    else -> true
}

private const val MAX_KEY_CODE = 400
