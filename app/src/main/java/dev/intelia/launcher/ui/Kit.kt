package dev.intelia.launcher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.intelia.launcher.data.ClockStyle
import java.time.LocalTime
import java.util.Locale

/** Monospaced capitals for metadata and section names. */
@Composable
fun Kicker(text: String, modifier: Modifier = Modifier, color: Color = LocalInk.current.muted) {
    Text(text.uppercase(Locale.getDefault()), modifier, style = InkText.label, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
fun Hairline(modifier: Modifier = Modifier, color: Color = LocalInk.current.line) {
    Box(modifier.fillMaxWidth().height(1.dp).background(color))
}

/** The tide dash: the "this is selected" mark of the Ink screens (no longer under LENS, 4.3). */
@Composable
fun TideDash(visible: Boolean, modifier: Modifier = Modifier, width: Dp = 14.dp, color: Color = LocalInk.current.accent) {
    Box(modifier.size(width, 2.dp).clip(RoundedCornerShape(1.dp)).background(if (visible) color else Color.Transparent))
}

/**
 * LENS mark (4.3): a 44 dp ring with a 1.5 dp stroke and nothing beneath it. While ARC is open it
 * settles to 86 % with a faint fill; with Reduced Motion that change is instant.
 */
@Composable
fun LensMark(ink: Color, modifier: Modifier = Modifier, active: Boolean = false, ring: Dp = CrystalDims.lensRing, filled: Boolean = false) {
    val press by animateFloatAsState(if (active) 1f else 0f, tween(motionMs(110)), label = "lens-press")
    Canvas(modifier.size(ring)) {
        val stroke = CrystalDims.lensStroke.toPx() * (ring / CrystalDims.lensRing)
        val r = (size.minDimension / 2f - stroke / 2f) * (1f - .14f * press)
        if (press > 0f) drawCircle(ink.copy(alpha = .12f * press), r)
        if (filled) drawCircle(ink.copy(alpha = .22f), r)
        drawCircle(ink.copy(alpha = .88f), r, style = Stroke(stroke))
    }
}

/**
 * Home clock, hours and minutes only; no seconds and no per-second work. [ClockStyle.Crystal] is one
 * voice; the Ink styles keep two voices: upright hours, italic minutes, an accent ring for the colon.
 */
@Composable
fun SplitClock(now: LocalTime, style: ClockStyle, ink: Color, accent: Color, size: TextUnit, modifier: Modifier = Modifier,
    shadow: Boolean = true, weight: Int = 0) {
    val time = "%02d".format(now.hour) to "%02d".format(now.minute)
    val shade = if (shadow) homeShadow(ink) else null
    val family = when (style) {
        ClockStyle.Crystal -> {
            // 4.2: Roboto Flex 220 · 118, tabular figures so the width never jumps.
            Text("${time.first}:${time.second}", modifier, color = ink, maxLines = 1,
                style = CrystalType.clock.copy(fontFamily = if (weight == 0) CrystalType.clock.fontFamily else flex(weight, 118f),
                    fontSize = size, lineHeight = size, letterSpacing = (-1).sp, shadow = shade))
            return
        }
        ClockStyle.Serif -> InkFonts.serif
        ClockStyle.Grotesk -> InkFonts.sans
        ClockStyle.Mono -> InkFonts.mono
    }
    val base = TextStyle(fontFamily = family, fontWeight = if (weight == 0) FontWeight.Light else FontWeight(weight), fontSize = size, lineHeight = size,
        letterSpacing = if (style == ClockStyle.Mono) (-0.06f).em(size) else (-0.03f).em(size), shadow = shade)
    Row(modifier.semantics(mergeDescendants = true) { contentDescription = "${time.first}:${time.second}" },
        verticalAlignment = Alignment.CenterVertically) {
        Text(time.first, style = base, color = ink, modifier = Modifier.clearAndSetSemantics { })
        val ringSize = (size.value * .11f).dp
        Canvas(Modifier.padding(horizontal = (size.value * .07f).dp).size(ringSize)) {
            drawCircle(accent, this.size.minDimension / 2f - 1.dp.toPx(), style = Stroke(1.6.dp.toPx()))
        }
        Text(time.second, style = base.copy(fontStyle = if (style == ClockStyle.Serif) FontStyle.Italic else FontStyle.Normal),
            color = ink, modifier = Modifier.clearAndSetSemantics { })
    }
}

private fun Float.em(size: TextUnit): TextUnit = (this * size.value).sp

/** Primary action: a filled capsule. Outline variant for secondary actions. */
@Composable
fun Capsule(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, filled: Boolean = true, enabled: Boolean = true) {
    val ink = LocalInk.current
    val shape = CircleShape
    Box(
        modifier.heightIn(min = 48.dp).clip(shape)
            .then(if (filled) Modifier.background(if (enabled) ink.accent else ink.line) else Modifier.border(1.5.dp, ink.ink, shape))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = InkText.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
            color = if (filled) ink.onAccent else ink.ink, maxLines = 1)
    }
}

/** Text action without chrome. */
@Composable
fun Quiet(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = LocalInk.current.accent) {
    Box(modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 4.dp),
        contentAlignment = Alignment.CenterStart) {
        Text(text, style = InkText.body.copy(fontWeight = FontWeight.Medium, fontSize = 15.sp), color = color)
    }
}

/** Words in a row; the chosen one is ink with the tide dash beneath. */
@Composable
fun <T> Segments(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val ink = LocalInk.current
    Row(modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        options.forEach { (value, label) ->
            val on = value == selected
            Column(Modifier.heightIn(min = 48.dp).selectable(on, role = Role.RadioButton, onClick = { onSelect(value) }),
                verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, style = InkText.body.copy(fontSize = 14.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal),
                    color = if (on) ink.ink else ink.muted, maxLines = 1)
                Spacer(Modifier.height(5.dp))
                TideDash(on)
            }
        }
    }
}

/** A ring that fills with accent when on: the toggle in Intelia's ring language. */
@Composable
fun RingToggle(checked: Boolean, modifier: Modifier = Modifier) {
    val ink = LocalInk.current
    val fill by animateFloatAsState(if (checked) 1f else 0f, tween(motionMs(120)), label = "ring-toggle")
    Canvas(modifier.size(24.dp)) {
        val stroke = 1.5.dp.toPx()
        val r = size.minDimension / 2f - stroke
        drawCircle(if (checked) ink.accent else ink.faint, r, style = Stroke(stroke))
        if (fill > 0f) drawCircle(ink.accent, r * .55f * fill)
    }
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier, detail: String? = null) {
    Row(modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(checked, role = Role.Switch, onValueChange = onChange)
        .padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = InkText.body)
            if (detail != null) Text(detail, style = InkText.small, color = LocalInk.current.muted)
        }
        RingToggle(checked)
    }
}

@Composable
fun ChoiceRow(label: String, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 24.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f).padding(end = 12.dp), style = InkText.body, maxLines = 2)
        content()
    }
}

@Composable
fun NavRow(label: String, value: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val ink = LocalInk.current
    Row(modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = InkText.body)
        if (value != null) Text(value, style = InkText.small.copy(fontSize = 14.sp), color = ink.muted, maxLines = 1)
        Text("  →", style = InkText.body, color = ink.muted)
    }
}

@Composable
fun BackButton(label: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onBack), contentAlignment = Alignment.Center) {
        HomeGlyph(Glyph.Back, label)
    }
}

/** A full-screen paper page: back at the thumb-free top, optional action on the right. */
@Composable
fun InkPage(
    backLabel: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    kicker: String? = null,
    title: String? = null,
    action: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val ink = LocalInk.current
    Column(modifier.fillMaxSize().background(ink.paper).statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 16.dp, top = 4.dp).heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically) {
            BackButton(backLabel, onBack)
            Spacer(Modifier.weight(1f))
            action()
        }
        if (kicker != null) Kicker(kicker, Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp), color = ink.accent)
        if (title != null) Text(title, Modifier.padding(start = 24.dp, end = 24.dp, top = 6.dp, bottom = 6.dp), style = InkText.title)
        content()
    }
}

/** FLOW steps as beads on a thread: Maps ─○─ Trains ─○─ Music. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BeadThread(labels: List<String?>, missing: String, modifier: Modifier = Modifier, color: Color = LocalInk.current.muted,
    bead: Color = LocalInk.current.accent, style: TextStyle = InkText.small) {
    val ink = LocalInk.current
    FlowRow(modifier, verticalArrangement = Arrangement.Center, itemVerticalAlignment = Alignment.CenterVertically) {
        labels.forEachIndexed { index, label ->
            if (index > 0) Canvas(Modifier.width(30.dp).height(16.dp)) {
                val y = size.height / 2f
                val stroke = 1.dp.toPx()
                drawLine(color.copy(alpha = .5f), Offset(2.dp.toPx(), y), Offset(size.width - 2.dp.toPx(), y), stroke)
                drawCircle(bead, 3.2.dp.toPx(), Offset(size.width / 2f, y))
            }
            Text(label ?: missing, style = style, color = if (label == null) ink.danger else color, maxLines = 1)
        }
    }
}
