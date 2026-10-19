package dev.intelia.launcher.pulse

/**
 * P0 PULSE reliability: what the accessibility service shows is decided from facts read *now*, never from a flag a
 * broadcast once set. Android may drop, reorder or never deliver SCREEN_ON / SCREEN_OFF / USER_PRESENT (the
 * 2026-10-19 bug: USER_PRESENT from SystemUI never reached a not-exported receiver, so `locked` stayed true until the
 * service was rebound). Pure Kotlin, JVM tested.
 */
enum class PulseHealth {
    /** No service instance (accessibility off, unbound or the process died). */
    Disconnected,
    /** Service connected, but the owner's mode is not Everywhere. */
    Off,
    /** Screen off or keyguard up: no overlay above the lock screen, by design. */
    Locked,
    /** The lens should exist and is being added. */
    Attaching,
    /** The lens overlay is attached and touchable. */
    Active,
    /** WindowManager refused the lens; a bounded retry is pending or exhausted. */
    Error,
}

/** Everything the decision needs, read fresh from Android at each evaluation. */
data class PulseFacts(
    val connected: Boolean,
    val everywhere: Boolean,
    val interactive: Boolean,
    val keyguardLocked: Boolean,
    val lensAttached: Boolean = false,
    val lensFailed: Boolean = false,
)

object PulseLifecycle {
    /** The lens target exists only when connected, Everywhere, screen on and keyguard gone. */
    fun wantsLens(f: PulseFacts): Boolean = f.connected && f.everywhere && f.interactive && !f.keyguardLocked

    fun health(f: PulseFacts): PulseHealth = when {
        !f.connected -> PulseHealth.Disconnected
        !f.everywhere -> PulseHealth.Off
        !f.interactive || f.keyguardLocked -> PulseHealth.Locked
        f.lensAttached -> PulseHealth.Active
        f.lensFailed -> PulseHealth.Error
        else -> PulseHealth.Attaching
    }

    /**
     * Safety net after SCREEN_ON while the keyguard is up: re-read the keyguard at these delays in case the unlock
     * broadcast is late or missing. Bounded (≈ 1 min in total), cancelled by USER_PRESENT, SCREEN_OFF or unlock.
     */
    val UNLOCK_CHECKS_MS = longArrayOf(400, 800, 1_500, 3_000, 6_000, 12_000, 30_000)

    /** Retries after WindowManager refused the lens: few, growing, then the state stays [PulseHealth.Error]. */
    val ATTACH_RETRY_MS = longArrayOf(250, 1_000, 4_000)
}

/** Walks a fixed delay list once; [next] is null when exhausted, [reset] starts over. Not thread safe (main only). */
class Backoff(private val delays: LongArray) {
    private var i = 0
    fun next(): Long? = delays.getOrNull(i)?.also { i++ }
    fun reset() { i = 0 }
    val attempts: Int get() = i
}
