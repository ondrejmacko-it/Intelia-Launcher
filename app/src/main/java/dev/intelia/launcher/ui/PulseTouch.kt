package dev.intelia.launcher.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import dev.intelia.launcher.action.Gesture
import dev.intelia.launcher.pulse.PulseGestures
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * M3.1: the capsule's touches on Home through the shared [PulseGestures] recognizer (the PULSE service feeds the
 * same one from its overlay view). [bound] is read at touch time; moves are consumed only while a swipe is bound,
 * so an unbound capsule leaves drags to Home (swipe down → notifications).
 *
 * M3.3: after a hold the finger keeps reporting through [onHeld] (local dp, also outside the target) and the press
 * ends with [onHeldEnd]: true when it lifted (release runs), false when it was cancelled (nothing runs).
 */
internal fun Modifier.pulseTouch(bound: (Gesture) -> Boolean, swipes: Boolean, onGesture: (Gesture) -> Unit,
    onHeld: (Float, Float) -> Unit = { _, _ -> }, onHeldEnd: (Boolean) -> Unit = {}, doubleTapMs: Int = 0,
    onDown: () -> Unit = {}): Modifier = composed {
    val down by rememberUpdatedState(onDown)
    val isBound by rememberUpdatedState(bound)
    val fire by rememberUpdatedState(onGesture)
    val consume by rememberUpdatedState(swipes)
    val held by rememberUpdatedState(onHeld)
    val heldEnd by rememberUpdatedState(onHeldEnd)
    pointerInput(doubleTapMs) {
        // P0.2: the owner's double-tap window (0 = system timeout).
        val window = if (doubleTapMs > 0) doubleTapMs.toLong() else viewConfiguration.doubleTapTimeoutMillis
        val rec = PulseGestures({ isBound(it) }, PulseGestures.Timing(window,
            viewConfiguration.longPressTimeoutMillis, viewConfiguration.touchSlop / density))
        coroutineScope {
            var timer: Job? = null
            fun apply(out: PulseGestures.Out) {
                out.run?.let(fire)
                if (out.timer == PulseGestures.KEEP) return
                timer?.cancel()
                timer = out.timer?.let { ms -> launch { delay(ms); timer = null; apply(rec.timeout()) } }
            }
            awaitEachGesture {
                val first = awaitFirstDown(requireUnconsumed = false)
                // The press is the capsule's: Home's own long press (the editor) and taps must not see it.
                first.consume()
                down()
                apply(rec.down(first.position.x / density, first.position.y / density))
                fun drop() { val was = rec.holding; apply(rec.cancel()); rec.reset(); if (was) heldEnd(false) }
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.changes.count { it.pressed } > 1) { drop(); break }
                    val c = event.changes.firstOrNull { it.id == first.id } ?: run { drop(); break }
                    val x = c.position.x / density
                    val y = c.position.y / density
                    if (!c.pressed) {
                        c.consume()
                        val was = rec.holding
                        apply(rec.up(x, y))
                        if (was) heldEnd(true)
                        break
                    }
                    if (c.isConsumed) { drop(); break }
                    apply(rec.move(x, y))
                    if (rec.holding) held(x, y)
                    if (consume || rec.holding) c.consume()
                }
            }
        }
    }
}
