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
 */
internal fun Modifier.pulseTouch(bound: (Gesture) -> Boolean, swipes: Boolean, onGesture: (Gesture) -> Unit): Modifier = composed {
    val isBound by rememberUpdatedState(bound)
    val fire by rememberUpdatedState(onGesture)
    val consume by rememberUpdatedState(swipes)
    pointerInput(Unit) {
        val rec = PulseGestures({ isBound(it) }, PulseGestures.Timing(viewConfiguration.doubleTapTimeoutMillis,
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
                apply(rec.down(first.position.x / density, first.position.y / density))
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.changes.count { it.pressed } > 1) { apply(rec.cancel()); rec.reset(); break }
                    val c = event.changes.firstOrNull { it.id == first.id } ?: run { apply(rec.cancel()); rec.reset(); break }
                    val x = c.position.x / density
                    val y = c.position.y / density
                    if (!c.pressed) {
                        c.consume()
                        apply(rec.up(x, y))
                        break
                    }
                    if (c.isConsumed) { apply(rec.cancel()); rec.reset(); break }
                    apply(rec.move(x, y))
                    if (consume || rec.holding) c.consume()
                }
            }
        }
    }
}
