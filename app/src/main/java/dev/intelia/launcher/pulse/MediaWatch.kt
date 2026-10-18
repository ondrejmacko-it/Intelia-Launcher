package dev.intelia.launcher.pulse

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What PULSE shows for the followed session: the pure model and the artwork (null → the app's icon). */
class Playing(val info: NowPlaying, val art: Bitmap?)

/**
 * D21: the notification listener exists only so [MediaSessionManager.getActiveSessions] may be called. It reads no
 * notification, stores nothing and does nothing when bound.
 */
class PulseMediaListener : NotificationListenerService() {
    override fun onListenerConnected() = MediaWatch.refresh()
}

/**
 * M3.2: the playing media session, event driven (session list listener + [MediaController.Callback], no polling).
 * Watched only while someone holds it: Home while started, the PULSE service while its compact capsule or menu can
 * show. Without the owner's notification-access switch it reports nothing and never asks.
 */
object MediaWatch {
    private val main = Handler(Looper.getMainLooper())
    private val _now = MutableStateFlow<Playing?>(null)
    val now: StateFlow<Playing?> = _now.asStateFlow()

    private var app: Context? = null
    private var holders = 0
    private var manager: MediaSessionManager? = null
    private var controllers: List<MediaController> = emptyList()
    private var followed: MediaController? = null

    fun allowed(context: Context): Boolean = context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

    /** Android's notification-access page for Intelia's listener (the list on older versions). */
    fun openSettings(context: Context) {
        val component = ComponentName(context, PulseMediaListener::class.java)
        val detail = if (Build.VERSION.SDK_INT >= 30) Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component.flattenToString()) else null
        for (intent in listOfNotNull(detail, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return
            } catch (_: android.content.ActivityNotFoundException) {
            } catch (_: SecurityException) {
            }
        }
    }

    fun acquire(context: Context) {
        app = context.applicationContext
        if (holders++ == 0) start()
    }

    fun release() {
        if (holders == 0) return
        if (--holders == 0) stop()
    }

    /** The listener got bound (access just switched on): start watching if someone is waiting. */
    fun refresh() {
        main.post { if (holders > 0 && manager == null) start() }
    }

    /** Transport for the followed session; null when nothing is followed. */
    fun controls(): MediaController.TransportControls? = followed?.transportControls

    private val sessionsChanged = MediaSessionManager.OnActiveSessionsChangedListener { list -> follow(list.orEmpty()) }

    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onSessionDestroyed() = follow(controllers - followed)
    }

    private fun start() {
        val context = app ?: return
        if (!allowed(context)) return
        val m = context.getSystemService(MediaSessionManager::class.java) ?: return
        val component = ComponentName(context, PulseMediaListener::class.java)
        try {
            m.addOnActiveSessionsChangedListener(sessionsChanged, component, main)
            manager = m
            follow(m.getActiveSessions(component))
        } catch (_: SecurityException) {
            manager = null
        }
    }

    private fun stop() {
        manager?.removeOnActiveSessionsChangedListener(sessionsChanged)
        manager = null
        controllers.forEach { it.unregisterCallback(callback) }
        controllers = emptyList()
        followed = null
        _now.value = null
    }

    private fun follow(list: List<MediaController?>) {
        controllers.forEach { it.unregisterCallback(callback) }
        controllers = list.filterNotNull()
        // Every session gets the callback, so a pause in one and play in another moves PULSE over without polling.
        controllers.forEach { it.registerCallback(callback, main) }
        publish()
    }

    private fun publish() {
        val infos = controllers.map { it to it.info() }
        val pick = NowPlaying.pick(infos.map { it.second })
        followed = infos.firstOrNull { it.second === pick }?.first
        val meta = followed?.metadata
        _now.value = pick?.let { Playing(it, meta?.art()) }
    }

    private fun MediaController.info(): NowPlaying {
        val s = playbackState
        val m = metadata
        return NowPlaying(
            pkg = packageName,
            title = m?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: m?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE).orEmpty(),
            artist = m?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: m?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE).orEmpty(),
            state = s?.state ?: PlaybackState.STATE_NONE,
            actions = s?.actions ?: 0L,
            durationMs = m?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
            positionMs = s?.position ?: 0L,
            updatedAt = s?.lastPositionUpdateTime?.takeIf { it > 0 } ?: SystemClock.elapsedRealtime(),
            speed = s?.playbackSpeed?.takeIf { it.isFinite() } ?: 1f,
        )
    }

    private fun MediaMetadata.art(): Bitmap? = getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
        ?: getBitmap(MediaMetadata.METADATA_KEY_ART) ?: getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
}
