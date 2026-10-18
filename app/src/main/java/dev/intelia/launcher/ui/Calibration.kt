package dev.intelia.launcher.ui

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.drawBehind
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
import dev.intelia.launcher.data.PulseHit
import dev.intelia.launcher.data.PulseLens
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import java.util.Locale
import kotlin.math.roundToInt

private const val STEP = .5f

private fun Float.snap(): Float = (this / STEP).roundToInt() * STEP

/**
 * Studio › PULSE › Kamera a dotyk (S3.1, VeilKit). On top a compact live strip on black, where the real camera hole
 * disappears: the accent dot is where Intelia believes the lens is, the dashed square the touch target. Below, the
 * Studio's own rows: the target size first, a safe touch test, and only on request the fine offsets and the camera
 * itself. Everything is a draft until Uložit; Zrušit and Back leave the saved values untouched.
 */
@Composable
fun LensCalibration(saved: PulseLens?, savedHit: PulseHit, onSave: (PulseLens?, PulseHit) -> Unit, onBack: () -> Unit) {
    val ink = LocalInk.current
    val density = LocalDensity.current
    val screenWidth = LocalConfiguration.current.screenWidthDp.toFloat()
    val detected = rememberDetectedHole()
    val auto = detected?.let { with(density) { PulseLens(it.x.toDp().value, it.y.toDp().value, it.radius.toDp().value) } }
    var x by rememberSaveable { mutableStateOf(saved?.x ?: -1f) }
    var y by rememberSaveable { mutableStateOf(saved?.y ?: -1f) }
    var r by rememberSaveable { mutableStateOf(saved?.r ?: -1f) }
    /** The draft follows the detected cutout (saved as null) until the owner moves the camera by hand. */
    var automatic by rememberSaveable { mutableStateOf(saved == null) }
    LaunchedEffect(auto, automatic) {
        val start = auto ?: if (x < 0f) PulseLens(screenWidth / 2f, 24f, 11f) else return@LaunchedEffect
        if (automatic || x < 0f) { x = start.x.snap(); y = start.y.snap(); r = start.r.snap() }
    }
    var hitSize by rememberSaveable { mutableStateOf(savedHit.size) }
    var hitDx by rememberSaveable { mutableStateOf(savedHit.dx) }
    var hitDy by rememberSaveable { mutableStateOf(savedHit.dy) }
    val hit = PulseHit(hitSize, hitDx, hitDy)
    var testing by rememberSaveable { mutableStateOf(false) }
    var advanced by rememberSaveable { mutableStateOf(saved != null || savedHit.dx != 0f || savedHit.dy != 0f) }
    var hits by remember { mutableStateOf(0) }
    var misses by remember { mutableStateOf(0) }
    var miss by remember { mutableStateOf<Offset?>(null) }
    val flash = remember { androidx.compose.animation.core.Animatable(0f) }
    // With PULSE everywhere the real lens overlay takes the draft and reports its touches here instead of acting.
    val serviceHits by PulseAccessibilityService.probeHits.collectAsState()
    LaunchedEffect(x, y, r, hitSize, hitDx, hitDy) { if (x >= 0f) PulseAccessibilityService.probe(PulseLens(x, y, r) to hit) }
    DisposableEffect(Unit) { onDispose { PulseAccessibilityService.probe(null) } }
    LaunchedEffect(serviceHits) { if (serviceHits > 0 && testing) { hits++; miss = null; flash.snapTo(1f); flash.animateTo(0f, androidx.compose.animation.core.tween(500)) } }
    SystemBarIcons(darkIcons = false)
    val draft = (if (automatic) null else PulseLens(x, y, r)) to hit
    val changed = draft != (saved to savedHit)
    val on = ink.onNight
    val stripH = (y + maxOf(r, hitSize / 2f + hitDy) + 34f).coerceAtLeast(96f)

    Column(Modifier.fillMaxSize().background(ink.paper)) {
        // The live strip: real coordinates from the top of the screen, so the camera sits inside it.
        Box(Modifier.fillMaxWidth().height(stripH.dp).background(Color.Black)
            .drawBehind { drawLine(ink.accent.copy(alpha = .35f), Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), 1.dp.toPx()) }) {
            if (x >= 0f) Canvas(Modifier.fillMaxSize().pointerInput(x, y, hitSize, hitDx, hitDy, testing) {
                if (testing) detectTapGestures { p ->
                    val box = pulseHitbox(x, y, hit, screenWidth)
                    if (box.contains(p.x.toDp().value, p.y.toDp().value)) { hits++; miss = null } else { misses++; miss = p }
                }
            }) {
                val c = Offset(x.dp.toPx(), y.dp.toPx())
                val rr = r.dp.toPx()
                val box = pulseHitbox(x, y, hit, screenWidth)
                val tl = Offset(box.left.dp.toPx(), box.top.dp.toPx())
                val bs = Size(box.width.dp.toPx(), box.height.dp.toPx())
                drawRoundRect(ink.accent.copy(alpha = .10f + .35f * flash.value), tl, bs, CornerRadius(12.dp.toPx()))
                drawRoundRect(ink.accent.copy(alpha = .8f), tl, bs, CornerRadius(12.dp.toPx()),
                    style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))))
                miss?.let { drawCircle(on.copy(alpha = .5f), 5.dp.toPx(), it, style = Stroke(1.5.dp.toPx())) }
                drawCircle(ink.accent, rr, c)
                drawCircle(on.copy(alpha = .9f), rr + 4.dp.toPx(), c, style = Stroke(1.dp.toPx()))
            }
            Text(stringResource(if (testing) R.string.pulse_hit_test else R.string.pulse_calib_strip, hits, misses),
                Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 8.dp), style = InkText.small,
                color = if (testing && hits > 0 && misses == 0) ink.accent else on.copy(alpha = .7f))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 8.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.pulse_lens), Modifier.weight(1f), style = InkText.title.copy(fontSize = 24.sp, lineHeight = 28.sp))
                Text(stringResource(if (changed) R.string.pulse_calib_draft else R.string.pulse_calib_saved),
                    Modifier.clip(RoundedCornerShape(10.dp)).background(if (changed) ink.accent.copy(alpha = .22f) else ink.ink.copy(alpha = .07f))
                        .padding(horizontal = 10.dp, vertical = 3.dp), style = InkText.label, color = if (changed) ink.ink else ink.muted)
            }
            // S4.4: the explanation belongs to the touch area it explains, inside its group like every Studio note.
            if (x < 0f) Text(stringResource(R.string.pulse_calib_body), Modifier.padding(horizontal = 20.dp), style = InkText.small, color = ink.muted)
            if (x >= 0f) {
                VeilGroup(stringResource(R.string.pulse_hit_group)) {
                    GroupNote(stringResource(R.string.pulse_calib_body))
                    val sizes = listOf(40f, 48f, 56f, 64f)
                    ChoiceSetting(stringResource(R.string.pulse_hit_size), (sizes + listOfNotNull(hitSize.takeIf { it !in sizes })).sorted()
                        .map { it to "${it.toInt()} dp" }, hitSize, { hitSize = it })
                    if (!testing) SettingRow(stringResource(R.string.pulse_test), stringResource(R.string.pulse_test_detail),
                        { hits = 0; misses = 0; miss = null; testing = true }, glyph = Glyph.Touch)
                    else SettingRow(stringResource(R.string.pulse_hit_test, hits, misses), stringResource(R.string.pulse_test_detail), null,
                        glyph = Glyph.Touch, trailing = { Quiet(stringResource(R.string.pulse_test_done), { testing = false; miss = null }) })
                }
                if (!advanced) VeilGroup { SettingRow(stringResource(R.string.pulse_calib_advanced), null, { advanced = true }) }
                else {
                    VeilGroup(stringResource(R.string.pulse_calib_offsets)) {
                        SettingBlock {
                            SliderRow(stringResource(R.string.pulse_hit_dx), hitDx, -PulseHit.MAX_DX, PulseHit.MAX_DX, STEP, dp(hitDx)) { hitDx = it.snap() }
                            SliderRow(stringResource(R.string.pulse_hit_dy), hitDy, PulseHit.MIN_DY, PulseHit.MAX_DY, STEP, dp(hitDy)) { hitDy = it.snap() }
                        }
                    }
                    VeilGroup(stringResource(R.string.pulse_calib_camera)) {
                        ChoiceSetting(stringResource(R.string.pulse_calib_camera_mode), listOf(true to stringResource(R.string.pulse_lens_auto_short),
                            false to stringResource(R.string.pulse_lens_custom)), automatic, { automatic = it })
                        if (!automatic) SettingBlock {
                            SliderRow(stringResource(R.string.pulse_lens_x), x, 0f, screenWidth.coerceAtMost(LauncherSettings.MAX_LENS_X), STEP, dp(x)) { x = it.snap() }
                            SliderRow(stringResource(R.string.pulse_lens_y), y, 0f, 120f, STEP, dp(y)) { y = it.snap() }
                            SliderRow(stringResource(R.string.pulse_lens_size), r, LauncherSettings.MIN_LENS_R, 40f, STEP, dp(r)) { r = it.snap() }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Quiet(stringResource(R.string.preset_cancel), onBack, color = ink.muted)
            Spacer(Modifier.weight(1f))
            Capsule(stringResource(R.string.flow_save), { onSave(draft.first, draft.second) }, enabled = changed)
        }
    }
}

private fun dp(v: Float) = String.format(Locale.ROOT, "%.1f dp", v)

/**
 * A rounded track; the chosen part is accent, the value is a bead that grows a halo while held. Tap or drag anywhere on it.
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
    var held by remember { mutableStateOf(false) }
    val grow by animateFloatAsState(if (held) 1f else 0f, label = "bead")
    // The track runs between the bead's own margins, so the bead never leaves the slider's bounds at either end.
    Canvas(modifier.height(44.dp)
        .semantics {
            contentDescription = label
            stateDescription = valueText
            progressBarRangeInfo = ProgressBarRangeInfo(value, min..max, steps.coerceAtLeast(0))
            setProgress { v -> current(v); true }
        }
        .focusable()
        .pointerInput(min, max) {
            val pad = BEAD_PAD.dp.toPx()
            fun at(px: Float) = current(min + ((px - pad) / (size.width - 2 * pad)).coerceIn(0f, 1f) * span)
            detectTapGestures(onPress = { held = true; tryAwaitRelease(); held = false }) { at(it.x) }
        }
        .pointerInput(min, max) {
            val pad = BEAD_PAD.dp.toPx()
            fun at(px: Float) = current(min + ((px - pad) / (size.width - 2 * pad)).coerceIn(0f, 1f) * span)
            detectHorizontalDragGestures(onDragStart = { held = true; at(it.x) }, onDragEnd = { held = false }, onDragCancel = { held = false }) { change, _ ->
                change.consume(); at(change.position.x)
            }
        }) {
        val pad = BEAD_PAD.dp.toPx()
        val cy = size.height / 2f
        val bx = pad + (size.width - 2 * pad) * fraction
        val h = 4.dp.toPx()
        drawLine(track.copy(alpha = .16f), Offset(pad, cy), Offset(size.width - pad, cy), h, StrokeCap.Round)
        drawLine(ink.accent, Offset(pad, cy), Offset(bx, cy), h, StrokeCap.Round)
        // A soft halo while held, then the bead: accent with a light core.
        if (grow > 0f) drawCircle(ink.accent.copy(alpha = .18f * grow), (10f + 6f * grow).dp.toPx(), Offset(bx, cy))
        drawCircle(ink.accent, 9.dp.toPx(), Offset(bx, cy))
        drawCircle(Color.White.copy(alpha = .92f), 3.5.dp.toPx(), Offset(bx, cy))
    }
}

/** Room left of the track's start and right of its end for the bead (its halo included), dp. */
private const val BEAD_PAD = 16f
