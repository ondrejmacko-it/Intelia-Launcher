package dev.intelia.launcher.ui

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import dev.intelia.launcher.data.Haptic

/**
 * D24: plays [Haptic.plan] on this phone. With amplitude control a one-shot pulse (`VIBRATE`, touch usage, so the
 * system's touch-intensity setting applies); without it the system effect of the level's stop on [View]. Android's
 * touch feedback switch off = nothing on either path.
 */
class HapticEngine private constructor(private val context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        context.getSystemService(Vibrator::class.java)
    }

    /** True when the motor can play a chosen strength; else Studio offers the four stops. */
    val amplitudeControl: Boolean = vibrator?.let { it.hasVibrator() && it.hasAmplitudeControl() } == true

    /** Android's "touch feedback" switch; read on each play, so a change applies at once. */
    @Suppress("DEPRECATION")
    fun systemOn(): Boolean = Settings.System.getInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0

    fun play(view: View?, level: Int, kind: Haptic.Kind) {
        when (val p = Haptic.plan(level, kind, amplitudeControl, systemOn())) {
            Haptic.Plan.None -> Unit
            is Haptic.Plan.Amplitude -> pulse(p)
            is Haptic.Plan.System -> view?.performHapticFeedback(constant(p))
        }
    }

    private fun pulse(p: Haptic.Plan.Amplitude) {
        val v = vibrator ?: return
        val effect = VibrationEffect.createOneShot(p.ms, p.amplitude)
        try {
            if (Build.VERSION.SDK_INT >= 33) v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH))
            else v.vibrate(effect)
        } catch (_: SecurityException) {
            // VIBRATE missing (should not happen, it is in the manifest): stay silent rather than crash a gesture.
        }
    }

    private fun constant(p: Haptic.Plan.System): Int = when (p.step) {
        Haptic.Step.Off -> HapticFeedbackConstants.NO_HAPTICS
        Haptic.Step.Light -> if (p.kind == Haptic.Kind.Tick) HapticFeedbackConstants.CLOCK_TICK else HapticFeedbackConstants.KEYBOARD_TAP
        Haptic.Step.Medium -> if (p.kind == Haptic.Kind.Tick) HapticFeedbackConstants.KEYBOARD_TAP else HapticFeedbackConstants.VIRTUAL_KEY
        Haptic.Step.Strong -> if (p.kind == Haptic.Kind.Tick) HapticFeedbackConstants.VIRTUAL_KEY else HapticFeedbackConstants.LONG_PRESS
    }

    companion object {
        @Volatile private var one: HapticEngine? = null

        fun of(context: Context): HapticEngine = one ?: synchronized(this) {
            one ?: HapticEngine(context.applicationContext).also { one = it }
        }
    }
}
