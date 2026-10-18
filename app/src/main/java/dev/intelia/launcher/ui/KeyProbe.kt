package dev.intelia.launcher.ui

import dev.intelia.launcher.action.Gesture

/** One hardware key seen by the M4 diagnostics: its Android key code and whether it also came as a hold. */
data class KeyHit(val code: Int, val presses: Int = 1, val held: Boolean = false)

/**
 * M4 hardware-key diagnostics, pure: the keys that really reached Intelia, newest first. Only keys proven here can
 * ever become a [dev.intelia.launcher.action.Trigger.Key]; Power, Home and Recents never reach an app.
 */
object KeyProbe {
    const val MAX_SHOWN = 6

    /** Records a key down; [repeat] > 0 means Android repeats it, so the key can be held. */
    fun record(seen: List<KeyHit>, code: Int, repeat: Int): List<KeyHit> {
        val old = seen.firstOrNull { it.code == code }
        val hit = when {
            old == null -> KeyHit(code, held = repeat > 0)
            repeat > 0 -> old.copy(held = true)
            else -> old.copy(presses = old.presses + 1)
        }
        return (listOf(hit) + seen.filterNot { it.code == code }).take(MAX_SHOWN)
    }

    /** The gestures a seen key could carry: a press always, a hold only once Android repeated it. */
    fun gestures(hit: KeyHit): List<Gesture> = if (hit.held) listOf(Gesture.Tap, Gesture.Hold) else listOf(Gesture.Tap)
}
