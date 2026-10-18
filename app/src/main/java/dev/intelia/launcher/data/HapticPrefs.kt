package dev.intelia.launcher.data

import kotlinx.serialization.Serializable

/** Where a haptic answer comes from; each may get its own strength later (D24). */
@Serializable enum class HapticSurface { Arc, Pulse, Gestures, Keys }

/**
 * Haptic strength (D24). [strength] 0–100 is the global level; null = not chosen yet, taken from the old [Haptics]
 * choice so an existing install feels the same. [per] holds later per-surface levels (no UI yet).
 */
@Serializable
data class HapticPrefs(
    val strength: Int? = null,
    val per: Map<HapticSurface, Int> = emptyMap(),
) {
    fun sanitized(): HapticPrefs = HapticPrefs(strength?.coerceIn(0, Haptic.MAX), per.mapValues { it.value.coerceIn(0, Haptic.MAX) })
}

/** Pure rules of D24: which level applies and how a device plays it. */
object Haptic {
    const val MAX = 100

    /** The four stops a device without amplitude control offers, and the level each one stores. */
    enum class Step(val level: Int) { Off(0), Light(25), Medium(55), Strong(85) }

    enum class Kind { Tick, Confirm }

    sealed interface Plan {
        data object None : Plan
        /** One pulse of [ms] at [amplitude] 1–255 (devices with amplitude control). */
        data class Amplitude(val ms: Long, val amplitude: Int) : Plan
        /** One of the system's own effects for [step] (every device). */
        data class System(val step: Step, val kind: Kind) : Plan
    }

    /** The old three-way choice as a level, on the matching stop, so an existing install keeps its feel and its name. */
    fun fromLegacy(h: Haptics): Int = when (h) {
        Haptics.Off -> Step.Off.level
        Haptics.Soft -> Step.Light.level
        Haptics.Strong -> Step.Strong.level
    }

    /** The closest old choice, written next to [HapticPrefs.strength] for older readers. */
    fun toLegacy(level: Int): Haptics = when {
        level <= 0 -> Haptics.Off
        level <= 55 -> Haptics.Soft
        else -> Haptics.Strong
    }

    /** The level for [surface] (null = global): its own level, else the global one, else the old choice. */
    fun level(prefs: HapticPrefs, legacy: Haptics, surface: HapticSurface? = null): Int =
        (surface?.let(prefs.per::get) ?: prefs.strength ?: fromLegacy(legacy)).coerceIn(0, MAX)

    /** The stop a level falls in: 0 off, then thirds. */
    fun step(level: Int): Step = when {
        level <= 0 -> Step.Off
        level <= 33 -> Step.Light
        level <= 66 -> Step.Medium
        else -> Step.Strong
    }

    /**
     * How to play [kind] at [level]: nothing at 0 or with the system's touch feedback off, a pulse whose amplitude
     * follows the level where the motor can do it, else the system effect of the level's stop. A confirm is longer and
     * a little stronger than a tick.
     */
    fun plan(level: Int, kind: Kind, amplitudeControl: Boolean, systemOn: Boolean = true): Plan {
        val l = level.coerceIn(0, MAX)
        if (l == 0 || !systemOn) return Plan.None
        if (!amplitudeControl) return Plan.System(step(l), kind)
        val base = MIN_AMPLITUDE + (255 - MIN_AMPLITUDE) * l / MAX
        return when (kind) {
            Kind.Tick -> Plan.Amplitude(TICK_MS, base)
            Kind.Confirm -> Plan.Amplitude(CONFIRM_MS, (base + 40).coerceAtMost(255))
        }
    }

    const val TICK_MS = 12L
    const val CONFIRM_MS = 30L
    /** The weakest pulse a motor still makes felt; level 1 starts here, not at 0. */
    const val MIN_AMPLITUDE = 20
}
