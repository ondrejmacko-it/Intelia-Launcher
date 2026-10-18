package dev.intelia.launcher.pulse

import dev.intelia.launcher.action.Gesture
import kotlin.math.abs
import kotlin.math.hypot

/**
 * PULSE 2.0 (M3.1): the one gesture recognizer for the capsule, used by Home and by the PULSE service alike.
 * Pure and deterministic: the caller feeds touches in dp and owns a single timer; every [Out] says whether a gesture
 * is recognised now ([Out.run]) and how the timer must stand afterwards ([Out.timer]: null = no timer, else (re)start
 * it with that many ms and call [timeout] when it fires).
 *
 * A shorter gesture never fires while a longer one bound by the owner can still follow: a tap waits for a second tap
 * only when [Gesture.DoubleTap] or [Gesture.DoubleTapHold] is bound, a press waits for a hold only when the hold is
 * bound. A second finger, a cancel or an unbound swipe runs nothing.
 */
class PulseGestures(private val bound: (Gesture) -> Boolean, private val timing: Timing = Timing()) {
    data class Timing(val doubleTapMs: Long = 300, val holdMs: Long = 400, val slopDp: Float = 8f, val swipeDp: Float = 36f)

    data class Out(val run: Gesture? = null, val timer: Long? = null)

    private enum class Phase { Idle, Down, Between, SecondDown, Drag, Held, Dead }

    private var phase = Phase.Idle
    private var second = false
    private var x0 = 0f
    private var y0 = 0f

    /** True after a hold was recognised and the finger is still down (Global ARC follows the finger from here, M3.3). */
    val holding: Boolean get() = phase == Phase.Held

    /** True while a touch or a pending second tap is in progress. */
    val busy: Boolean get() = phase != Phase.Idle

    fun down(x: Float, y: Float): Out {
        x0 = x; y0 = y
        return when (phase) {
            // After a cancel the lifting finger may never be reported (the system took the touch), so a new down
            // starts afresh; a second finger arrives as a cancel from the caller, never as a down here.
            Phase.Idle, Phase.Dead -> { phase = Phase.Down; second = false; Out(timer = if (bound(Gesture.Hold)) timing.holdMs else null) }
            Phase.Between -> { phase = Phase.SecondDown; second = true; Out(timer = if (bound(Gesture.DoubleTapHold)) timing.holdMs else null) }
            // A down while a press is in progress is a second finger: nothing runs.
            else -> cancel()
        }
    }

    fun move(x: Float, y: Float): Out {
        if ((phase == Phase.Down || phase == Phase.SecondDown) && hypot(x - x0, y - y0) > timing.slopDp) {
            phase = Phase.Drag
            return Out()
        }
        return Out(timer = KEEP)
    }

    fun up(x: Float, y: Float): Out {
        val was = phase
        phase = Phase.Idle
        return when (was) {
            Phase.Down -> if (bound(Gesture.DoubleTap) || bound(Gesture.DoubleTapHold)) {
                phase = Phase.Between
                Out(timer = timing.doubleTapMs)
            } else Out(Gesture.Tap)
            // Two quick taps with only "double tap + hold" bound stay one tap: nothing is lost, nothing extra runs.
            Phase.SecondDown -> Out(if (bound(Gesture.DoubleTap)) Gesture.DoubleTap else Gesture.Tap)
            Phase.Drag -> Out(if (second) null else swipe(x - x0, y - y0)?.takeIf(bound))
            else -> Out()
        }
    }

    fun timeout(): Out = when (phase) {
        Phase.Down -> { phase = Phase.Held; Out(Gesture.Hold) }
        Phase.SecondDown -> { phase = Phase.Held; Out(Gesture.DoubleTapHold) }
        Phase.Between -> { phase = Phase.Idle; Out(Gesture.Tap) }
        else -> Out()
    }

    /** Pointer cancelled, a second finger, the screen locked or the window went away: drop everything. */
    fun cancel(): Out {
        phase = if (phase == Phase.Idle || phase == Phase.Between) Phase.Idle else Phase.Dead
        return Out()
    }

    /** Clean slate without waiting for the finger to lift (service torn down, mode switched off). */
    fun reset() { phase = Phase.Idle }

    /** A clear swipe in screen dp; diagonals and short moves are none. */
    fun swipe(dx: Float, dy: Float): Gesture? = when {
        maxOf(abs(dx), abs(dy)) < timing.swipeDp -> null
        abs(dy) >= abs(dx) * 1.5f -> if (dy > 0) Gesture.SwipeDown else Gesture.SwipeUp
        abs(dx) >= abs(dy) * 1.5f -> if (dx < 0) Gesture.SwipeLeft else Gesture.SwipeRight
        else -> null
    }

    companion object {
        /** [Out.timer] for "leave the timer as it is" (moves within the slop). */
        const val KEEP = -1L

        /**
         * Every gesture the capsule can carry, in the order Studio lists them. No swipe up: above the camera there is
         * less room than [Timing.swipeDp], so offering it would promise a gesture nobody can make.
         */
        val GESTURES = listOf(Gesture.Tap, Gesture.DoubleTap, Gesture.Hold, Gesture.DoubleTapHold,
            Gesture.SwipeDown, Gesture.SwipeLeft, Gesture.SwipeRight)
    }
}

/** Where PULSE lives (M3.1): nowhere, only on Home, or above every app through the PULSE service. */
enum class PulseMode { Off, Home, Everywhere }

/**
 * The visible stage of PULSE 2.0. Hidden: nothing is drawn, only the small lens target exists. Compact: an event is
 * active (charging, alarm, later media). Expanded: the capsule is open. Arc: Global ARC follows the finger (M3.3).
 */
enum class PulseStage { Hidden, Compact, Expanded, Arc }

fun pulseStage(event: Boolean, expanded: Boolean, arc: Boolean): PulseStage = when {
    arc -> PulseStage.Arc
    expanded -> PulseStage.Expanded
    event -> PulseStage.Compact
    else -> PulseStage.Hidden
}
