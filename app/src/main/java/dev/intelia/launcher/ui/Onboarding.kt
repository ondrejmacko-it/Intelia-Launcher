package dev.intelia.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.intelia.launcher.R
import dev.intelia.launcher.data.HomeLayout
import dev.intelia.launcher.data.LauncherSettings
import kotlin.math.cos
import kotlin.math.sin

/** Three steps: what Intelia is, how much Home shows, and the four gestures plus "set as home". */
@Composable
fun Onboarding(model: HomeViewModel, settings: LauncherSettings) {
    val ink = LocalInk.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    var layout by rememberSaveable { mutableStateOf(HomeLayout.Essentials) }
    BackHandler { if (step > 0) step-- }
    SystemBarIcons(darkIcons = !ink.dark)

    Column(Modifier.fillMaxSize().background(ink.paper).statusBarsPadding().navigationBarsPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 28.dp)) {
            Spacer(Modifier.height(40.dp))
            when (step) {
                0 -> Intro()
                1 -> LayoutChoice(settings, layout) { layout = it }
                else -> Gestures()
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (step == 2) {
                val role = rememberHomeRole()
                if (!role.held) Capsule(stringResource(R.string.default_home), role.request, Modifier.fillMaxWidth(), filled = false)
                else Text(stringResource(R.string.default_home_active), Modifier.align(Alignment.CenterHorizontally), style = InkText.small, color = ink.muted)
            }
            Capsule(stringResource(when (step) { 0 -> R.string.onboarding_start; 1 -> R.string.onboarding_next; else -> R.string.onboarding_done }),
                { if (step < 2) step++ else model.finishOnboarding(layout) }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Header(kicker: String, title: String, body: String) {
    val ink = LocalInk.current
    Kicker(kicker, color = ink.accent)
    Text(title, Modifier.padding(top = 10.dp), style = InkText.title.copy(fontSize = 34.sp, lineHeight = 38.sp))
    Text(body, Modifier.padding(top = 10.dp), style = InkText.body.copy(fontSize = 15.sp), color = ink.muted)
}

@Composable
private fun ColumnScope.Intro() {
    val ink = LocalInk.current
    Header("1 / 3 · INTELIA", stringResource(R.string.onboarding_intro_title), stringResource(R.string.onboarding_intro_body))
    Orbits(Modifier.fillMaxWidth().height(220.dp).padding(top = 20.dp).semantics { hideFromAccessibility() })
    listOf(
        "LENS" to R.string.onboarding_lens,
        "ARC" to R.string.onboarding_arc,
        "FLOW" to R.string.onboarding_flow,
        "PULSE" to R.string.onboarding_pulse,
    ).let { pillars ->
        Column(Modifier.fillMaxWidth().veilPane(RoundedCornerShape(22.dp), ink)) {
            pillars.forEachIndexed { i, (name, text) ->
                if (i > 0) Hairline(Modifier.padding(horizontal = 16.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.Top) {
                    Kicker(name, Modifier.width(64.dp).padding(top = 3.dp), color = ink.accent)
                    Text(stringResource(text), style = InkText.small.copy(fontSize = 14.sp), color = ink.ink)
                }
            }
        }
    }
}

/** Three orbits over LENS, an accent thread to a FLOW bead: ARC in one picture. */
@Composable
private fun Orbits(modifier: Modifier) {
    val ink = LocalInk.current
    Canvas(modifier) {
        val c = Offset(size.width / 2f, size.height - 18.dp.toPx())
        val radii = listOf(62f, 112f, 160f).map { it.dp.toPx() }
        radii.forEachIndexed { i, r ->
            drawArc(ink.ink.copy(alpha = if (i == 1) .5f else .22f), 200f, 140f, false, Offset(c.x - r, c.y - r), Size(2 * r, 2 * r),
                style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 6.dp.toPx()))))
            val n = 5 + i * 2
            for (k in 0 until n) {
                val a = Math.toRadians((205.0 + k * 130.0 / (n - 1)))
                val p = Offset(c.x + r * cos(a).toFloat(), c.y + r * sin(a).toFloat())
                drawCircle(ink.ink.copy(alpha = .55f), (if (i == 0) 4.5f else 3f).dp.toPx(), p, style = Stroke(1.2.dp.toPx()))
            }
        }
        val a = Math.toRadians(205.0 + 4 * 130.0 / 6)
        val end = Offset(c.x + radii[1] * cos(a).toFloat(), c.y + radii[1] * sin(a).toFloat())
        drawLine(ink.accent, Offset(c.x + (end.x - c.x) * .16f, c.y + (end.y - c.y) * .16f), end, 1.5.dp.toPx(), StrokeCap.Round)
        drawCircle(ink.accent, 5.dp.toPx(), end)
        drawCircle(ink.ink, 12.dp.toPx(), c, style = Stroke(1.6.dp.toPx()))
        drawLine(ink.accent, Offset(c.x - 7.dp.toPx(), c.y + 19.dp.toPx()), Offset(c.x + 7.dp.toPx(), c.y + 19.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
    }
}

@Composable
private fun LayoutChoice(settings: LauncherSettings, layout: HomeLayout, onSelect: (HomeLayout) -> Unit) {
    val ink = LocalInk.current
    val now by rememberNow()
    Header("2 / 3 · " + stringResource(R.string.home_section).uppercase(), stringResource(R.string.onboarding_layout_title),
        stringResource(R.string.onboarding_layout_body))
    Box(Modifier.fillMaxWidth().padding(top = 22.dp), contentAlignment = Alignment.Center) {
        HomePreview(settings.copy(layout = layout), emptyList(), now, ink.ink, ink.accent,
            Modifier.size(176.dp, 330.dp).veilPane(RoundedCornerShape(28.dp), ink), scale = .46f)
    }
    Column(Modifier.padding(top = 18.dp).fillMaxWidth().veilPane(RoundedCornerShape(22.dp), ink).selectableGroup()) {
        listOf(
            HomeLayout.Pure to (R.string.layout_pure to R.string.layout_pure_detail),
            HomeLayout.Essentials to (R.string.layout_essentials to R.string.layout_essentials_detail),
            HomeLayout.Custom to (R.string.layout_custom to R.string.layout_custom_detail),
        ).forEachIndexed { i, (value, text) ->
            val on = value == layout
            if (i > 0) Hairline(Modifier.padding(horizontal = 16.dp))
            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).selectable(on, role = Role.RadioButton) { onSelect(value) }
                .padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                RingToggle(on)
                Column(Modifier.padding(start = 14.dp)) {
                    Text(stringResource(text.first), style = InkText.item.copy(fontSize = 22.sp))
                    Text(stringResource(text.second), style = InkText.small, color = ink.muted)
                }
            }
        }
    }
}

@Composable
private fun Gestures() {
    val ink = LocalInk.current
    Header("3 / 3 · " + stringResource(R.string.onboarding_gestures_kicker), stringResource(R.string.onboarding_gestures_title),
        stringResource(R.string.onboarding_gestures_body))
    Spacer(Modifier.height(18.dp))
    listOf(
        R.string.gesture_tap_lens to R.string.gesture_tap_lens_detail,
        R.string.gesture_swipe_up to R.string.gesture_swipe_up_detail,
        R.string.gesture_hold_lens to R.string.gesture_hold_lens_detail,
        R.string.gesture_hold_home to R.string.gesture_hold_home_detail,
    ).let { gestures ->
        Column(Modifier.fillMaxWidth().veilPane(RoundedCornerShape(22.dp), ink)) {
            gestures.forEachIndexed { index, (title, detail) ->
                if (index > 0) Hairline(Modifier.padding(horizontal = 16.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.Top) {
                    Text((index + 1).toString(), Modifier.width(28.dp), style = InkText.item.copy(fontSize = 22.sp), color = ink.accent)
                    Column {
                        Text(stringResource(title), style = InkText.body)
                        Text(stringResource(detail), style = InkText.small, color = ink.muted)
                    }
                }
            }
        }
    }
}
