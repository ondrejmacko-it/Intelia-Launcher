package dev.intelia.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.intelia.launcher.R
import dev.intelia.launcher.data.LauncherSettings
import dev.intelia.launcher.data.PulseLens
import java.util.Locale
import kotlin.math.roundToInt

private const val STEP = .5f

private fun Float.snap(): Float = (this / STEP).roundToInt() * STEP

/**
 * Lens calibration for PULSE. The ground is night-dark so the real camera hole disappears into it;
 * an accent dot is drawn where Intelia believes the lens is. Move it until the camera swallows the
 * dot and only the hairline ring around it remains. Nothing is stored until Save.
 */
@Composable
fun LensCalibration(saved: PulseLens?, onSave: (PulseLens?) -> Unit, onBack: () -> Unit) {
    val ink = LocalInk.current
    val density = LocalDensity.current
    val screenWidth = LocalConfiguration.current.screenWidthDp.toFloat()
    val detected = rememberDetectedHole()
    val auto = detected?.let { with(density) { PulseLens(it.x.toDp().value, it.y.toDp().value, it.radius.toDp().value) } }
    var x by rememberSaveable { mutableStateOf(saved?.x ?: -1f) }
    var y by rememberSaveable { mutableStateOf(saved?.y ?: -1f) }
    var r by rememberSaveable { mutableStateOf(saved?.r ?: -1f) }
    /** True once the dot came from the user or a saved value; the cutout read never overrides it then. */
    var settled by rememberSaveable { mutableStateOf(saved != null) }
    // The cutout is known a frame after the first composition; until then, or without one, a guess.
    LaunchedEffect(auto) {
        val start = auto ?: if (x < 0f) PulseLens(screenWidth / 2f, 24f, 11f) else return@LaunchedEffect
        if (!settled || x < 0f) { x = start.x.snap(); y = start.y.snap(); r = start.r.snap() }
        if (auto != null) settled = true
    }
    SystemBarIcons(darkIcons = false)
    val on = ink.onNight

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (x >= 0f) Canvas(Modifier.fillMaxSize()) {
            val c = Offset(x.dp.toPx(), y.dp.toPx())
            val rr = r.dp.toPx()
            // A thread from the lens down to the controls, the target dot and its hairline ring.
            drawLine(on.copy(alpha = .18f), Offset(c.x, c.y + rr + 10.dp.toPx()), Offset(c.x, 132.dp.toPx()), 1.dp.toPx(), StrokeCap.Round)
            drawCircle(ink.accent, rr, c)
            drawCircle(on.copy(alpha = .9f), rr + 4.dp.toPx(), c, style = Stroke(1.dp.toPx()))
        }
        Column(Modifier.fillMaxSize().padding(top = 150.dp).navigationBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)) {
            Kicker(stringResource(R.string.pulse_lens_kicker), color = ink.accent)
            Text(stringResource(R.string.pulse_lens_title), Modifier.padding(top = 8.dp),
                style = InkText.title.copy(fontSize = 30.sp, lineHeight = 34.sp), color = on)
            Text(stringResource(R.string.pulse_lens_body), Modifier.padding(top = 8.dp, bottom = 12.dp), style = InkText.small.copy(fontSize = 14.sp),
                color = on.copy(alpha = .7f))
            if (x >= 0f) {
                BeadControl(stringResource(R.string.pulse_lens_x), x, 0f, screenWidth.coerceAtMost(LauncherSettings.MAX_LENS_X)) { x = it }
                BeadControl(stringResource(R.string.pulse_lens_y), y, 0f, 120f) { y = it }
                BeadControl(stringResource(R.string.pulse_lens_size), r, LauncherSettings.MIN_LENS_R, 40f) { r = it }
            }
            Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 24.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Quiet(stringResource(R.string.pulse_lens_back), onBack, color = on.copy(alpha = .7f))
                Spacer(Modifier.weight(1f))
                Quiet(stringResource(R.string.pulse_lens_auto), {
                    auto?.let { x = it.x.snap(); y = it.y.snap(); r = it.r.snap() }
                    onSave(null)
                }, color = on)
                Capsule(stringResource(R.string.flow_save), { onSave(PulseLens(x, y, r)) })
            }
        }
    }
}

/** Label and value, then ‹ thread-with-bead › : one row per coordinate. */
@Composable
private fun BeadControl(label: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
    val ink = LocalInk.current
    val on = ink.onNight
    fun set(v: Float) = onChange(v.coerceIn(min, max).snap())
    Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
        val shown = String.format(Locale.ROOT, "%.1f dp", value)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = InkText.body, color = on)
            Text(shown, Modifier.padding(start = 10.dp),
                style = InkText.label.copy(letterSpacing = 0.sp), color = on.copy(alpha = .55f))
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            StepRing("‹", stringResource(R.string.pulse_lens_less, label)) { set(value - STEP) }
            BeadSlider(value, min, max, STEP, label, shown, Modifier.weight(1f).padding(horizontal = 10.dp), ::set)
            StepRing("›", stringResource(R.string.pulse_lens_more, label)) { set(value + STEP) }
        }
    }
}

@Composable
private fun StepRing(mark: String, description: String, onClick: () -> Unit) {
    val on = LocalInk.current.onNight
    Box(Modifier.size(48.dp).clip(CircleShape).border(1.dp, on.copy(alpha = .3f), CircleShape)
        .clickable(role = Role.Button, onClickLabel = description, onClick = onClick)
        .semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        Text(mark, style = InkText.item.copy(fontSize = 24.sp, lineHeight = 24.sp), color = on)
    }
}

/**
 * A hairline track; the chosen part is accent thread, the value is a bead. Tap or drag anywhere on it.
 * TalkBack reads [valueText] (not a percentage of the range) and its swipes move by one [step].
 */
@Composable
internal fun BeadSlider(value: Float, min: Float, max: Float, step: Float, label: String, valueText: String, modifier: Modifier,
    onChange: (Float) -> Unit, track: Color = LocalInk.current.onNight) {
    val ink = LocalInk.current
    val current by rememberUpdatedState(onChange)
    val span = (max - min).coerceAtLeast(1f)
    val fraction = ((value - min) / span).coerceIn(0f, 1f)
    val steps = if (step > 0f) ((max - min) / step).roundToInt() - 1 else 0
    Canvas(modifier.height(44.dp)
        .semantics {
            contentDescription = label
            stateDescription = valueText
            progressBarRangeInfo = ProgressBarRangeInfo(value, min..max, steps.coerceAtLeast(0))
            setProgress { v -> current(v); true }
        }
        .focusable()
        .pointerInput(min, max) {
            fun at(px: Float) = current(min + (px / size.width).coerceIn(0f, 1f) * span)
            detectTapGestures { at(it.x) }
        }
        .pointerInput(min, max) {
            fun at(px: Float) = current(min + (px / size.width).coerceIn(0f, 1f) * span)
            detectHorizontalDragGestures(onDragStart = { at(it.x) }) { change, _ -> change.consume(); at(change.position.x) }
        }) {
        val cy = size.height / 2f
        val bx = size.width * fraction
        drawLine(track.copy(alpha = .25f), Offset(0f, cy), Offset(size.width, cy), 1.dp.toPx(), StrokeCap.Round)
        drawLine(ink.accent, Offset(0f, cy), Offset(bx, cy), 2.dp.toPx(), StrokeCap.Round)
        drawCircle(ink.accent, 8.dp.toPx(), Offset(bx, cy))
        drawCircle(Color.Black, 3.dp.toPx(), Offset(bx, cy))
    }
}
