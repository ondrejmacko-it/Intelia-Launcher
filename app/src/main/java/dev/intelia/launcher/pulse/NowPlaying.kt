package dev.intelia.launcher.pulse

/**
 * M3.2 Music PULSE: what one media session shows, pure so the rules are tested on the JVM. Built from a
 * `MediaController` in [MediaWatch]; [actions] are the session's own `PlaybackState` action bits, and a control
 * is offered only when the session supports it.
 */
data class NowPlaying(
    val pkg: String,
    val title: String,
    val artist: String,
    val state: Int,
    val actions: Long,
    val durationMs: Long,
    val positionMs: Long,
    /** `elapsedRealtime` when [positionMs] was reported. */
    val updatedAt: Long,
    val speed: Float,
) {
    val playing: Boolean get() = state == STATE_PLAYING || state == STATE_BUFFERING
    /** Compact appears only while something plays or sits paused; stopped, error or none shows nothing. */
    val visible: Boolean get() = playing || state == STATE_PAUSED
    val canPrev: Boolean get() = actions and ACTION_SKIP_TO_PREVIOUS != 0L
    val canNext: Boolean get() = actions and ACTION_SKIP_TO_NEXT != 0L
    val canToggle: Boolean get() = actions and (ACTION_PLAY_PAUSE or if (playing) ACTION_PAUSE else ACTION_PLAY) != 0L
    val canSeek: Boolean get() = actions and ACTION_SEEK_TO != 0L && durationMs > 0

    /** Position extrapolated to [now] (elapsedRealtime) while playing, clamped to the track. */
    fun positionAt(now: Long): Long {
        val moved = if (state == STATE_PLAYING && positionMs >= 0) positionMs + ((now - updatedAt).coerceAtLeast(0) * speed).toLong() else positionMs
        return if (durationMs > 0) moved.coerceIn(0, durationMs) else moved.coerceAtLeast(0)
    }

    companion object {
        // android.media.session.PlaybackState values, kept here so the model stays pure.
        const val STATE_STOPPED = 1
        const val STATE_PAUSED = 2
        const val STATE_PLAYING = 3
        const val STATE_BUFFERING = 6
        const val ACTION_PAUSE = 2L
        const val ACTION_PLAY = 4L
        const val ACTION_SKIP_TO_PREVIOUS = 16L
        const val ACTION_SKIP_TO_NEXT = 32L
        const val ACTION_SEEK_TO = 256L
        const val ACTION_PLAY_PAUSE = 512L

        /** The session PULSE follows: the first playing one in the system's order, else the first visible one. */
        fun pick(sessions: List<NowPlaying>): NowPlaying? = sessions.firstOrNull { it.playing } ?: sessions.firstOrNull { it.visible }
    }
}

/** "3:07" or "1:02:05". */
fun clock(ms: Long): String {
    val s = (ms.coerceAtLeast(0) / 1000).toInt()
    val h = s / 3600
    val m = s / 60 % 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}
