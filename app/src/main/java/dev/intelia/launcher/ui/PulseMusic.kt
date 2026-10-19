package dev.intelia.launcher.ui

import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxSize
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import android.os.SystemClock
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import dev.intelia.launcher.R
import dev.intelia.launcher.data.Motion
import dev.intelia.launcher.pulse.MediaWatch
import dev.intelia.launcher.pulse.Playing
import dev.intelia.launcher.pulse.clock
import dev.intelia.launcher.action.Action
import dev.intelia.launcher.action.MediaKey
import kotlinx.coroutines.delay

/**
 * Watches the playing session while this composable is in the tree and [on] is true (and notification access is
 * granted); returns it, or null. Event driven through [MediaWatch].
 */
@Composable
internal fun rememberPlaying(on: Boolean): Playing? {
    val context = LocalContext.current
    DisposableEffect(on) {
        if (on) MediaWatch.acquire(context)
        onDispose { if (on) MediaWatch.release() }
    }
    val now by MediaWatch.now.collectAsState()
    return now.takeIf { on }
}

/** The session's artwork, else the playing app's icon through public `LauncherApps`. */
@Composable
internal fun rememberArt(playing: Playing): ImageBitmap? {
    val context = LocalContext.current
    val art = playing.art
    return remember(art, playing.info.pkg) { art?.asImageBitmap() ?: appIcon(context, playing.info.pkg) }
}

private fun appIcon(context: Context, pkg: String): ImageBitmap? = try {
    context.getSystemService(LauncherApps::class.java)?.getActivityList(pkg, Process.myUserHandle())?.firstOrNull()
        ?.getIcon(context.resources.displayMetrics.densityDpi)?.toBitmap(96, 96)?.asImageBitmap()
} catch (_: SecurityException) {
    null
}

/** Three sound bars; they move only while playing and motion is on, else they stand still (paused = low). */
@Composable
internal fun MusicBars(playing: Boolean, color: Color, modifier: Modifier) {
    val animate = playing && LocalMotion.current != Motion.Off
    val phase = if (animate) {
        rememberInfiniteTransition(label = "bars").animateFloat(0f, 1f,
            infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "phase").value
    } else 0f
    Canvas(modifier) {
        val w = size.width / 5f
        val still = if (playing) floatArrayOf(.55f, .9f, .7f) else floatArrayOf(.25f, .25f, .25f)
        for (i in 0..2) {
            val f = if (animate) .3f + .7f * kotlin.math.abs(kotlin.math.sin((phase + i * .33f) * Math.PI.toFloat())) else still[i]
            val h = size.height * f
            drawRoundRect(color, Offset(w * (2 * i), (size.height - h) / 2f), Size(w, h), CornerRadius(w / 2f))
        }
    }
}

@Composable
internal fun MusicArt(art: ImageBitmap?, size: Dp, shape: androidx.compose.ui.graphics.Shape = CircleShape) {
    val ink = LocalInk.current
    Box(Modifier.size(size).clip(shape).background(ink.onNight.copy(alpha = .12f))) {
        if (art != null) Image(art, null, Modifier.size(size), contentScale = ContentScale.Crop)
    }
}

/**
 * Expanded music: artwork, title, artist, ⏮ ⏯ ⏭ and the timeline. Every control exists only when the session
 * supports its action; the timeline seeks only with `ACTION_SEEK_TO`, else it is shown read-only (or not at all
 * without a duration).
 */
@Composable
internal fun MusicCard(playing: Playing, onOpen: (() -> Unit)? = null) {
    val ink = LocalInk.current
    val sandbox = LocalSandbox.current
    val info = playing.info
    val art = rememberArt(playing)
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    // A UI clock for the timeline while the card is visible; the media state itself comes from callbacks.
    LaunchedEffect(info.playing, info.updatedAt) {
        while (info.playing) {
            now = SystemClock.elapsedRealtime(); delay(1000)
        }
        now = SystemClock.elapsedRealtime()
    }
    val openLabel = stringResource(R.string.pulse_open_player)
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        // S3: the artwork and title open the playing app; the controls below stay where the thumb expects them.
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .then(if (onOpen != null) Modifier.clickable(role = Role.Button, onClickLabel = openLabel, onClick = onOpen) else Modifier),
            verticalAlignment = Alignment.CenterVertically) {
            MusicArt(art, 56.dp, RoundedCornerShape(14.dp))
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(info.title.ifEmpty { stringResource(R.string.pulse_music_now) }, style = InkText.body, color = ink.onNight,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (info.artist.isNotEmpty()) Text(info.artist, style = InkText.small, color = ink.onNight.copy(alpha = .66f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (info.durationMs > 0) Timeline(info.positionAt(now), info.durationMs, info.canSeek) { ms ->
            if (sandbox == null) MediaWatch.controls()?.seekTo(ms)
        }
        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            if (info.canPrev) MusicButton(Glyph.Prev, R.string.pulse_prev) { sandbox?.invoke(Action.Media(MediaKey.Previous)) ?: MediaWatch.controls()?.skipToPrevious() }
            if (info.canToggle) MusicButton(if (info.playing) Glyph.Pause else Glyph.Play, R.string.pulse_play_pause) {
                sandbox?.invoke(Action.Media(MediaKey.PlayPause)) ?: MediaWatch.controls()?.let { if (info.playing) it.pause() else it.play() }
            }
            if (info.canNext) MusicButton(Glyph.Next, R.string.pulse_next) { sandbox?.invoke(Action.Media(MediaKey.Next)) ?: MediaWatch.controls()?.skipToNext() }
        }
    }
}

@Composable
private fun MusicButton(glyph: Glyph, label: Int, onClick: () -> Unit) {
    val ink = LocalInk.current
    val text = stringResource(label)
    Box(Modifier.size(52.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = text, onClick = onClick)
        .semantics { contentDescription = text }, contentAlignment = Alignment.Center) {
        HomeGlyph(glyph, null, tint = ink.onNight, size = 22.dp)
    }
}

/** A thread with a bead; tap or drag seeks when [seekable], the times sit underneath. */
@Composable
private fun Timeline(position: Long, duration: Long, seekable: Boolean, onSeek: (Long) -> Unit) {
    val ink = LocalInk.current
    var drag by remember { mutableStateOf<Float?>(null) }
    val fraction = drag ?: (position.toFloat() / duration).coerceIn(0f, 1f)
    val label = stringResource(R.string.pulse_seek)
    val touch = if (!seekable) Modifier else Modifier
        .pointerInput(duration) { detectTapGestures { o -> onSeek((o.x / size.width).coerceIn(0f, 1f).times(duration).toLong()) } }
        .pointerInput(duration) {
            detectHorizontalDragGestures(
                onDragStart = { o -> drag = (o.x / size.width).coerceIn(0f, 1f) },
                onDragEnd = { drag?.let { onSeek((it * duration).toLong()) }; drag = null },
                onDragCancel = { drag = null },
            ) { change, _ -> drag = (change.position.x / size.width).coerceIn(0f, 1f) }
        }
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(28.dp).then(touch).semantics {
            contentDescription = label
            stateDescription = "${clock(position)} / ${clock(duration)}"
            progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
            if (seekable) setProgress { v -> onSeek((v.coerceIn(0f, 1f) * duration).toLong()); true }
        }) {
            val y = size.height / 2f
            drawLine(ink.onNight.copy(alpha = .22f), Offset(0f, y), Offset(size.width, y), 2.dp.toPx())
            drawLine(ink.accent, Offset(0f, y), Offset(size.width * fraction, y), 2.dp.toPx())
            if (seekable) drawCircle(ink.accent, 6.dp.toPx(), Offset(size.width * fraction, y))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(clock(drag?.let { (it * duration).toLong() } ?: position), style = InkText.small, color = ink.onNight.copy(alpha = .66f))
            Text(clock(duration), style = InkText.small, color = ink.onNight.copy(alpha = .66f))
        }
    }
}

/** The compact music capsule (M3.2), drawn the same on Home and above apps; [gauge] is the battery ring while charging. */
@Composable
internal fun MusicCompact(shown: Playing, mc: MusicCapsule, lensRadius: Float, opacity: Int, gauge: Int?) {
    val ink = LocalInk.current
    Box(Modifier.size(mc.width.dp, mc.height.dp).clip(RoundedCornerShape(50)).background(ink.night.copy(alpha = opacity / 100f))) {
        MusicBars(shown.info.playing, ink.accent, Modifier.offset(x = 7.dp).align(Alignment.CenterStart).size(12.dp, mc.height.dp * .5f))
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(mc.lensX.dp.toPx(), size.height / 2f)
            val r = lensRadius + 3.dp.toPx()
            drawCircle(ink.onNight.copy(alpha = .18f), r, c, style = Stroke(1.5.dp.toPx()))
            if (gauge != null) drawArc(ink.accent, -90f, 360f * gauge / 100f, false, Offset(c.x - r, c.y - r),
                Size(2 * r, 2 * r), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        }
        Box(Modifier.align(Alignment.CenterEnd).padding(end = 3.dp)) { MusicArt(rememberArt(shown), (mc.height - 6f).dp) }
    }
}
