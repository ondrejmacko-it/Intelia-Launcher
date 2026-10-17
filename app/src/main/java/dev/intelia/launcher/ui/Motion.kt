package dev.intelia.launcher.ui

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset

/**
 * Veil 3.0 motion (prototype §Pohyb, owner 2026-10-17: Calm keeps the brisker durations, about 3/5 of the prototype's).
 * Every duration still goes through [motionMs], so Motion Off and the system's Reduce motion make it 0 ms.
 */
object VeilMotion {
    /** The prototype's `--ease`: a quick start that settles with about 1 % overshoot. */
    val ease = CubicBezierEasing(.2f, .9f, .25f, 1.04f)

    /** Studio → a detail page and back (prototype 300 ms). */
    const val PAGE_MS = 180

    /**
     * How a full-screen page change moves: +1 slides a level deeper in, −1 slides back out, 0 fades. Depth 0 is Home
     * (with its Search and drawer layers); only a step between two pages above it is a slide.
     */
    fun slide(fromDepth: Int, toDepth: Int): Int = when {
        fromDepth <= 0 || toDepth <= 0 || fromDepth == toDepth -> 0
        toDepth > fromDepth -> 1
        else -> -1
    }

    /**
     * Deeper: the new page comes in from the end over the old one, which drifts a third back and fades. Back: the
     * page on top leaves the way it came and the one below returns from its third. Interruptible like any transition.
     */
    fun page(direction: Int, ms: Int): ContentTransform {
        val move = tween<IntOffset>(ms, easing = ease)
        val fade = tween<Float>(ms / 2)
        return if (direction >= 0) {
            ContentTransform(slideInHorizontally(move) { it } + fadeIn(fade), slideOutHorizontally(move) { -it / 3 } + fadeOut(fade),
                targetContentZIndex = 1f)
        } else {
            ContentTransform(slideInHorizontally(move) { -it / 3 } + fadeIn(fade), slideOutHorizontally(move) { it } + fadeOut(fade),
                targetContentZIndex = -1f)
        }
    }
}
