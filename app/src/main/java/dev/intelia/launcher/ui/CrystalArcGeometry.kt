package dev.intelia.launcher.ui

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Geometry of the Crystal arc (DESIGN_SYSTEM 4.4/4.5), in dp with the arc centre at the origin and y down.
 * Pure Kotlin so the B3 gesture can share it with the renderer.
 */
object CrystalArcGeometry {
    const val START_DEG = 8f
    const val END_DEG = 172f
    const val SLOTS = 5
    /** Selected slot moves this far outwards (dp); its dot sits [DOT_INSET] inside the arc. */
    const val SELECTED_LIFT = 3f
    const val DOT_INSET = 30f

    /** Slot angles in degrees, first slot first: 150/120/90/60/30 (right hand) or mirrored. */
    fun slotAngles(count: Int = SLOTS, mirrored: Boolean = false): List<Float> {
        val n = count.coerceIn(1, SLOTS)
        val step = 30f
        val first = 90f + step * (n - 1) / 2f
        val angles = List(n) { first - step * it }
        return if (mirrored) angles.reversed() else angles
    }

    /** Point at [deg] (0 = right, 90 = up) on a circle of [radius] around the origin; y grows downwards. */
    fun point(deg: Float, radius: Float): Pair<Float, Float> {
        val rad = deg * PI / 180.0
        return (radius * cos(rad)).toFloat() to (-radius * sin(rad)).toFloat()
    }

    /** End handles sit just past the arc's ends: left (previous set) and right (next set). */
    const val HANDLE_LEFT_DEG = 180f
    const val HANDLE_RIGHT_DEG = 0f
}
