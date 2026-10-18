package dev.intelia.launcher.action

/**
 * The hardware-key remap state machine (M6, D20), pure so it is unit tested on the JVM. The PULSE service feeds it
 * key downs, ups and its two timers and turns the [Effect]s into actions; it never sees text, only key codes, and
 * it keeps nothing after a gesture ends.
 *
 * A key with no binding is never touched. A bound key is taken whole; whatever the owner did not bind falls back to
 * the key's own job ([Effect.Replay]): a single press of a volume key with only a double press bound still changes
 * the volume, only one double-tap timeout later. Without a bound double press a press runs at once.
 *
 * @param bound whether [Gesture] of a key code has a binding.
 */
class KeyRemap(private val bound: (Int, Gesture) -> Boolean) {
    sealed interface Effect {
        data class Run(val code: Int, val gesture: Gesture) : Effect
        /** Do what the key does without Intelia (one step for a volume key). */
        data class Replay(val code: Int) : Effect
        /** Start the hold timer (long-press timeout); [hold] comes back unless [CancelHold]. */
        data class StartHold(val code: Int) : Effect
        data object CancelHold : Effect
        /** Start the second-press timer (double-tap timeout); [tapTimeout] comes back unless [CancelTap]. */
        data class StartTap(val code: Int) : Effect
        data object CancelTap : Effect
    }

    private var down: Int? = null
    private var held = false
    private var waiting: Int? = null

    /** Whether the filter takes this key at all; everything else goes on to the system unchanged. */
    fun takes(code: Int): Boolean = Controls.KEY_GESTURES.any { bound(code, it) }

    fun down(code: Int, repeat: Int): List<Effect> {
        if (!takes(code)) return emptyList()
        if (repeat > 0) {
            // A held key: once the hold ran, the repeats are swallowed; with no hold bound they keep their own job.
            return if (down == code && !held && !bound(code, Gesture.Hold)) listOf(Effect.Replay(code)) else emptyList()
        }
        val out = mutableListOf<Effect>()
        // Another key in between ends the wait for a second press as a single press.
        waiting?.takeIf { it != code }?.let { out += Effect.CancelTap; out += single(it); waiting = null }
        down = code
        held = false
        if (bound(code, Gesture.Hold)) out += Effect.StartHold(code)
        return out
    }

    fun hold(code: Int): List<Effect> {
        if (down != code || held) return emptyList()
        held = true
        val out = mutableListOf<Effect>()
        if (waiting == code) { out += Effect.CancelTap; waiting = null }
        out += Effect.Run(code, Gesture.Hold)
        return out
    }

    fun up(code: Int): List<Effect> {
        if (down != code) return emptyList()
        down = null
        if (held) { held = false; return emptyList() }
        val out = mutableListOf<Effect>(Effect.CancelHold)
        when {
            waiting == code -> { out += Effect.CancelTap; waiting = null; out += Effect.Run(code, Gesture.DoubleTap) }
            bound(code, Gesture.DoubleTap) -> { waiting = code; out += Effect.StartTap(code) }
            else -> out += single(code)
        }
        return out
    }

    fun tapTimeout(code: Int): List<Effect> {
        if (waiting != code) return emptyList()
        waiting = null
        return listOf(single(code))
    }

    /** Ends everything (service off, screen off): no gesture is left half done and nothing runs late. */
    fun reset(): List<Effect> {
        down = null; held = false; waiting = null
        return listOf(Effect.CancelHold, Effect.CancelTap)
    }

    private fun single(code: Int): Effect = if (bound(code, Gesture.Tap)) Effect.Run(code, Gesture.Tap) else Effect.Replay(code)
}
