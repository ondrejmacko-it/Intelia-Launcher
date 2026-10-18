package dev.intelia.launcher.action

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Gestures a trigger surface can report; each surface offers only the ones it can really detect. */
@Serializable enum class Gesture { Tap, DoubleTap, TripleTap, Hold, SwipeLeft, SwipeRight, SwipeUp, SwipeDown, DoubleTapHold }

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

/**
 * One switchable ARC set: mixed apps, FLOWs, shortcuts and other actions; null is an empty slot.
 * Slots are in priority order: slot 0 sits in the middle of the arc, then left and right alternately (D6).
 */
@Serializable data class ArcSet(val id: String, val name: String = "", val slots: List<Action?> = emptyList())

/** How the shallow ARC looks and feels (M2): visible slots, radius (dp), opening angle (degrees), haptic tick per slot. */
@Serializable
data class ArcLook(
    val slots: Int = 5,
    val radius: Int = RADIUS_M,
    val span: Int = SPAN_M,
    val haptics: Boolean = true,
    /** Taps before the hold pick the set ARC opens on: hold = set 1, tap + hold = set 2, two taps + hold = set 3. */
    val tapSets: Boolean = true,
) {
    fun sanitized() = ArcLook(slots.coerceIn(MIN_SLOTS, MAX_VISIBLE), radius.coerceIn(RADIUS_S, RADIUS_L), span.coerceIn(SPAN_S, SPAN_L), haptics, tapSets)

    companion object {
        const val MIN_SLOTS = 3
        const val MAX_VISIBLE = 8
        const val RADIUS_S = 104
        const val RADIUS_M = 120
        const val RADIUS_L = 140
        const val SPAN_S = 90
        const val SPAN_M = 120
        const val SPAN_L = 150
    }
}

/**
 * How Global ARC under the camera looks, apart from LENS ARC: size, opening angle, slot tick and how much it dims
 * another app (percent). The slot count stays shared: it is how many slots every set holds.
 */
@Serializable
data class GlobalArcLook(
    val radius: Int = ArcLook.RADIUS_M,
    val span: Int = ArcLook.SPAN_M,
    val haptics: Boolean = true,
    val dim: Int = DIM_M,
) {
    fun sanitized() = GlobalArcLook(radius.coerceIn(ArcLook.RADIUS_S, ArcLook.RADIUS_L), span.coerceIn(ArcLook.SPAN_S, ArcLook.SPAN_L),
        haptics, dim.coerceIn(DIM_S, DIM_L))

    /** The [ArcLook] Global ARC's geometry runs on: LENS ARC's slot count with this size and angle. */
    fun over(lens: ArcLook): ArcLook = lens.copy(radius = radius, span = span, haptics = haptics)

    companion object {
        const val DIM_S = 30
        const val DIM_M = 45
        const val DIM_L = 65
    }
}

/**
 * The Control System configuration: ARC sets plus gesture bindings. Stored in settings as `controls`;
 * while that is absent, [migrate] derives it from the legacy `arc`, `arcFlows` and `arcShortcuts` lists
 * (which stay untouched), so existing content and order carry over.
 */
@Serializable
data class Controls(
    val arcSets: List<ArcSet> = emptyList(),
    val bindings: List<Binding> = emptyList(),
    val arc: ArcLook = ArcLook(),
    /** Global ARC's own look (owner request 2026-10-18); older configs get the defaults. */
    val globalArc: GlobalArcLook = GlobalArcLook(),
    /** Key codes the M4 diagnostics proved reach Intelia; only these can carry a [Trigger.Key]. */
    val keys: List<Int> = emptyList(),
    /** D20 (APPROVED 2026-10-18): the PULSE service filters bound keys in every app; off until the owner turns it on. */
    val keysEverywhere: Boolean = false,
) {

    fun arcSet(id: String): ArcSet? = arcSets.firstOrNull { it.id == id }

    /** True when any ARC set holds [action]. */
    fun inArc(action: Action): Boolean = arcSets.any { action in it.slots }

    /** The first set always exists, so ARC has somewhere to put things. */
    private fun sets(): List<ArcSet> = arcSets.ifEmpty { listOf(ArcSet(MAIN_SET)) }

    private fun edit(id: String, transform: (List<Action?>) -> List<Action?>): Controls =
        copy(arcSets = sets().map { if (it.id == id) it.copy(slots = trim(transform(it.slots))) else it })

    /** Puts [action] (or an empty slot) at [index] of set [id]; the other slots keep their positions. */
    fun setSlot(id: String, index: Int, action: Action?): Controls {
        if (index !in 0 until arc.slots) return this
        return edit(id) { slots -> List(maxOf(slots.size, index + 1)) { i -> if (i == index) action else slots.getOrNull(i) } }
    }

    /** Swaps slot [index] with its neighbour [delta] places away (priority order). */
    fun moveSlot(id: String, index: Int, delta: Int): Controls {
        val j = index + delta
        if (index !in 0 until arc.slots || j !in 0 until arc.slots) return this
        return edit(id) { slots ->
            val list = MutableList(maxOf(slots.size, index + 1, j + 1)) { slots.getOrNull(it) }
            list[index] = list[j].also { list[j] = list[index] }
            list
        }
    }

    /** Adds [action] to the first free visible slot of the first set with room, or removes it from every set. */
    fun toggleInArc(action: Action, on: Boolean): Controls = if (!on) {
        copy(arcSets = arcSets.map { set -> set.copy(slots = trim(set.slots.map { if (it == action) null else it })) })
    } else if (inArc(action)) this else {
        val set = sets().firstOrNull { firstFree(it) != null } ?: return this
        setSlot(set.id, checkNotNull(firstFree(set)), action)
    }

    /** Whether [toggleInArc] has a free slot left anywhere. */
    fun arcFull(): Boolean = sets().none { firstFree(it) != null }

    private fun firstFree(set: ArcSet): Int? = (0 until arc.slots).firstOrNull { set.slots.getOrNull(it) == null }

    /** A new, empty set with a fresh id; null when [MAX_SETS] are in use. */
    fun addSet(): Controls? {
        val all = sets()
        if (all.size >= MAX_SETS) return null
        val id = generateSequence(all.size + 1) { it + 1 }.map { "set$it" }.first { id -> all.none { it.id == id } }
        return copy(arcSets = all + ArcSet(id))
    }

    /** Removes set [id]; the first set stays so ARC is never without one. */
    fun removeSet(id: String): Controls = if (sets().firstOrNull()?.id == id) this else copy(arcSets = sets().filterNot { it.id == id })

    fun withLook(look: ArcLook): Controls = copy(arc = look.sanitized())

    fun withGlobalLook(look: GlobalArcLook): Controls = copy(globalArc = look.sanitized())

    /** Slots beyond the last filled one carry nothing. */
    private fun trim(slots: List<Action?>): List<Action?> = slots.dropLastWhile { it == null }

    fun bound(trigger: Trigger): Action? = when (trigger) {
        is Trigger.ArcSlot -> arcSet(trigger.set)?.slots?.getOrNull(trigger.slot)
        else -> bindings.firstOrNull { it.trigger == trigger }?.action
    }

    /** Binds [action] to [trigger], replacing what it had; null unbinds. ARC slots are edited with [setSlot]. */
    fun bind(trigger: Trigger, action: Action?): Controls {
        if (trigger is Trigger.ArcSlot || (action is Action.Arc && !opensArc(trigger))) return this
        val rest = bindings.filterNot { it.trigger == trigger }
        return copy(bindings = if (action == null) rest else (rest + Binding(trigger, action)).takeLast(MAX_BINDINGS))
    }

    /** Every trigger [action] is bound to (ARC slots included), for "where else is this" hints and the global search. */
    fun triggersOf(action: Action): List<Trigger> =
        bindings.filter { it.action == action }.map { it.trigger } +
            arcSets.flatMap { set -> set.slots.mapIndexedNotNull { i, a -> if (a == action) Trigger.ArcSlot(set.id, i) else null } }

    /** Remembers a key code the diagnostics saw; newest first, capped. */
    fun withKey(code: Int): Controls =
        if (code !in 1..MAX_KEY_CODE || code in RESERVED_KEYS || keys.firstOrNull() == code) this else copy(keys = (listOf(code) + keys.filterNot { it == code }).take(MAX_KEYS))

    /** Forgets key [code] and every binding on it. */
    fun withoutKey(code: Int): Controls =
        copy(keys = keys.filterNot { it == code }, bindings = bindings.filterNot { (it.trigger as? Trigger.Key)?.code == code })

    /** Whether any gesture of key [code] is bound (the key filter lets every other key through untouched). */
    fun keyBound(code: Int): Boolean = bindings.any { (it.trigger as? Trigger.Key)?.code == code }

    /** The set Global ARC [arc] shows: its own while it exists, otherwise the first set (never none). */
    fun arcSetFor(arc: Action.Arc): ArcSet = arcSet(arc.set) ?: sets().first()

    /** The single validation gate for restored data: drops malformed actions, caps sizes, one binding per trigger. */
    fun sanitized(flowIds: Set<String>): Controls {
        // Global ARC never sits inside an ARC slot (an ARC cannot open itself) and only on a held PULSE gesture.
        fun ok(a: Action): Boolean = a.wellFormed() && (a !is Action.Flow || a.id in flowIds) && a !is Action.Arc
        val ids = HashSet<String>()
        val sets = arcSets.asSequence()
            .filter { Action.wellFormed(it.id) && ids.add(it.id) }
            .take(MAX_SETS)
            .map { set -> set.copy(name = set.name.filterNot(Char::isISOControl).take(MAX_NAME),
                slots = set.slots.take(MAX_SLOTS).map { a -> a?.takeIf(::ok) }) }
            .toList()
        val codes = keys.filter { it in 1..MAX_KEY_CODE && it !in RESERVED_KEYS }.distinct().take(MAX_KEYS)
        val seen = HashSet<Trigger>()
        val kept = bindings.asSequence()
            .filter { it.trigger !is Trigger.ArcSlot && it.trigger.valid() && seen.add(it.trigger) &&
                (ok(it.action) || (it.action is Action.Arc && it.action.wellFormed() && opensArc(it.trigger))) }
            .filter { b -> (b.trigger as? Trigger.Key)?.let { it.code in codes && it.gesture in KEY_GESTURES } ?: true }
            .take(MAX_BINDINGS).toList()
        return Controls(sets, kept, arc.sanitized(), globalArc.sanitized(), codes, keysEverywhere)
    }

    companion object {
        const val MAIN_SET = "main"
        const val MAX_SETS = 8
        const val MAX_SLOTS = 12
        const val MAX_BINDINGS = 64
        private const val MAX_NAME = 32
        const val MAX_KEYS = 8
        /** What a hardware key can carry (M6): press, double press, hold. */
        val KEY_GESTURES = listOf(Gesture.Tap, Gesture.DoubleTap, Gesture.Hold)

        /** Global ARC follows the finger after a hold, so only the held PULSE gestures can open it (M3.3). */
        fun opensArc(trigger: Trigger): Boolean =
            trigger is Trigger.Pulse && (trigger.gesture == Gesture.Hold || trigger.gesture == Gesture.DoubleTapHold)

        const val FLOW_SET = "flow"
        const val SHORTCUT_SET = "shortcuts"

        /**
         * Legacy ARC → one set per ring the owner used ([rings]): apps, FLOWs, shortcuts, each list in its stored order.
         * Shortcut keys are "package/id"; FLOWs are referenced by their stable id, so renaming keeps the binding.
         * Empty rings make no set (the first set always exists); the arc shows as many slots as the fullest set needs.
         */
        fun migrate(arc: List<String>, arcFlows: List<String>, arcShortcuts: List<String>, rings: Int = 3): Controls {
            val apps = ArcSet(MAIN_SET, slots = arc.map { Action.App(it) })
            val flows = ArcSet(FLOW_SET, slots = arcFlows.map { Action.Flow(it) })
            val shortcuts = ArcSet(SHORTCUT_SET, slots = arcShortcuts.mapNotNull { key ->
                key.indexOf('/').takeIf { it > 0 }?.let { Action.Shortcut(key.substring(0, it), key.substring(it + 1)) }
            })
            val sets = listOf(apps) + listOf(flows, shortcuts).take((rings - 1).coerceIn(0, 2)).filter { it.slots.isNotEmpty() }
            val widest = sets.maxOf { it.slots.size }
            return Controls(arcSets = sets, arc = ArcLook(slots = widest.coerceIn(5, ArcLook.MAX_VISIBLE)))
        }
    }
}

private fun Trigger.valid(): Boolean = when (this) {
    is Trigger.ArcSlot -> slot in 0 until Controls.MAX_SLOTS && Action.wellFormed(set)
    is Trigger.Key -> code in 1..MAX_KEY_CODE
    else -> true
}

private const val MAX_KEY_CODE = 400

/** Back, Home, Power and Recents are never remapped: the phone must stay usable whatever the bindings say. */
private val RESERVED_KEYS = setOf(3, 4, 26, 187)
