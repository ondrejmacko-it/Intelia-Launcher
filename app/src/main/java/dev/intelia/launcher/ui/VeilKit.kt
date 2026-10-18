package dev.intelia.launcher.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/*
 * Intelia Veil 3.0 settings kit: one group container, one row, one switch, one choice, used by every Studio page.
 * Matte surfaces washed faintly with the owner's accent, a crystal edge lit from the top, accent only where
 * something is active or chosen. Rows are at least 56 dp (choices 48 dp), text follows the text-size setting.
 */

/** The row a Studio search asked for: brought into view and flashed once, then forgotten. */
class SettingFocus(key: String?) { var key by mutableStateOf(key) }

val LocalSettingFocus = staticCompositionLocalOf<SettingFocus?> { null }

/** Marks a row as the search target [key]; when it is the asked one, it scrolls into view and glows briefly. */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.settingAnchor(key: String?): Modifier = composed {
    val focus = LocalSettingFocus.current
    if (key == null || focus == null || focus.key != key) return@composed this
    val ink = LocalInk.current
    val request = remember { BringIntoViewRequester() }
    val glow = remember { Animatable(0f) }
    val wait = motionMs(VeilMotion.PAGE_MS)
    val fade = motionMs(1400)
    LaunchedEffect(Unit) {
        delay(wait + 80L)
        request.bringIntoView()
        glow.snapTo(1f)
        // Reduced motion: the glow simply stays a moment and goes, no fade.
        if (fade == 0) { delay(1200); glow.snapTo(0f) } else glow.animateTo(0f, tween(fade))
        focus.key = null
    }
    bringIntoViewRequester(request).drawBehind { if (glow.value > 0f) drawRect(ink.accent.copy(alpha = .26f * glow.value)) }
}

/** A caption above a group, in the accent's quiet voice. */
@Composable
fun VeilCaption(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), modifier.padding(start = 28.dp, end = 24.dp, top = 16.dp, bottom = 6.dp),
        style = InkText.label.copy(fontSize = 11.sp, letterSpacing = 1.4.sp), color = LocalInk.current.accent.copy(alpha = .9f), maxLines = 1)
}

/**
 * Related settings on one shared plate: accent-washed matte fill, a crystal edge brighter at the top and hairlines
 * between the rows (never around each row).
 */
@Composable
fun VeilGroup(title: String? = null, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val ink = LocalInk.current
    if (title != null) VeilCaption(title)
    val shape = RoundedCornerShape(22.dp)
    val cuts = remember { ArrayList<Int>() }
    val edge = Brush.verticalGradient(listOf(ink.accent.copy(alpha = if (ink.dark) .42f else .34f), ink.accent.copy(alpha = .08f)))
    Layout(content, modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp).clip(shape)
        .background(ink.ink.copy(alpha = if (ink.dark) .035f else .025f))
        .background(ink.accent.copy(alpha = if (ink.dark) .10f else .07f))
        .border(1.dp, edge, shape)
        .drawBehind {
            val inset = 16.dp.toPx()
            cuts.forEach { y -> drawLine(ink.line, Offset(inset, y.toFloat()), Offset(size.width - inset, y.toFloat()), 1.dp.toPx() / 2f) }
        }) { measurables, c ->
        val placed = measurables.map { it.measure(c.copy(minHeight = 0)) }
        layout(c.maxWidth, placed.sumOf { it.height }) {
            cuts.clear()
            var y = 0
            placed.forEach { p ->
                if (y > 0 && p.height > 0) cuts.add(y)
                p.place(0, y)
                y += p.height
            }
        }
    }
}

/** The accent-washed square an area or a row carries its glyph on. */
@Composable
fun VeilTile(glyph: Glyph, modifier: Modifier = Modifier) {
    val ink = LocalInk.current
    Box(modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(ink.accent.copy(alpha = if (ink.dark) .22f else .16f)),
        contentAlignment = Alignment.Center) { HomeGlyph(glyph, null, tint = ink.accent, size = 20.dp) }
}

/** One setting or one place: optional glyph tile, name, current state below, then [trailing] or a chevron. */
@Composable
fun SettingRow(title: String, summary: String?, onClick: (() -> Unit)?, modifier: Modifier = Modifier, glyph: Glyph? = null,
    anchor: String? = null, trailing: (@Composable () -> Unit)? = null) {
    val ink = LocalInk.current
    Row(modifier.fillMaxWidth().settingAnchor(anchor).heightIn(min = 56.dp)
        .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
        .padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (glyph != null) VeilTile(glyph, Modifier.padding(end = 12.dp))
        Column(Modifier.weight(1f).padding(start = if (glyph == null) 4.dp else 0.dp)) {
            Text(title, style = InkText.body.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium), maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!summary.isNullOrEmpty()) Text(summary, style = InkText.small, color = ink.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        when {
            trailing != null -> Box(Modifier.padding(start = 10.dp)) { trailing() }
            onClick != null -> Text("›", Modifier.padding(start = 10.dp).clearAndSetSemantics { }, style = InkText.body.copy(fontSize = 22.sp),
                color = ink.muted)
        }
    }
}

/** The one switch: an accent track with a light bead when on, a quiet track when off. */
@Composable
fun VeilSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val ink = LocalInk.current
    val at by animateFloatAsState(if (checked) 1f else 0f, tween(motionMs(160)), label = "veil-switch")
    Box(modifier.size(44.dp, 26.dp).clip(CircleShape)
        .background(if (checked) ink.accent else ink.ink.copy(alpha = .14f)), contentAlignment = Alignment.CenterStart) {
        Box(Modifier.offset(x = (3 + 18 * at).dp).size(20.dp).clip(CircleShape)
            .background(if (checked) ink.onAccent else ink.ink.copy(alpha = .55f)))
    }
}

/** A setting that is on or off; the whole row toggles. */
@Composable
fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit, detail: String? = null, glyph: Glyph? = null,
    anchor: String? = null) {
    val ink = LocalInk.current
    Row(Modifier.fillMaxWidth().settingAnchor(anchor).heightIn(min = 56.dp).toggleable(checked, role = Role.Switch, onValueChange = onChange)
        .padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (glyph != null) VeilTile(glyph, Modifier.padding(end = 12.dp))
        Column(Modifier.weight(1f).padding(start = if (glyph == null) 4.dp else 0.dp, end = 12.dp)) {
            Text(title, style = InkText.body.copy(fontSize = 16.sp))
            if (detail != null) Text(detail, style = InkText.small, color = ink.muted)
        }
        VeilSwitch(checked)
    }
}

/** The one choice: options as soft tabs that wrap, the chosen one washed and edged in the accent. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> VeilChoice(options: List<Pair<T, String>>, selected: T?, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val ink = LocalInk.current
    val shape = RoundedCornerShape(14.dp)
    FlowRow(modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { (value, label) ->
            val on = value == selected
            Box(Modifier.heightIn(min = 44.dp).clip(shape)
                .background(if (on) ink.accent.copy(alpha = if (ink.dark) .30f else .22f) else ink.ink.copy(alpha = .06f))
                .then(if (on) Modifier.border(1.dp, ink.accent.copy(alpha = .65f), shape) else Modifier)
                .selectable(on, role = Role.RadioButton) { onSelect(value) }.padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center) {
                Text(label, style = InkText.body.copy(fontSize = 14.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal),
                    color = if (on) ink.ink else ink.muted, maxLines = 1)
            }
        }
    }
}

/** A labelled choice: name (and its current meaning) above, [VeilChoice] below, so long words never squeeze. */
@Composable
fun <T> ChoiceSetting(title: String, options: List<Pair<T, String>>, selected: T?, onSelect: (T) -> Unit, detail: String? = null,
    anchor: String? = null) {
    Column(Modifier.fillMaxWidth().settingAnchor(anchor).padding(horizontal = 18.dp, vertical = 10.dp)) {
        Text(title, style = InkText.body.copy(fontSize = 16.sp))
        if (detail != null) Text(detail, style = InkText.small, color = LocalInk.current.muted)
        VeilChoice(options, selected, onSelect, Modifier.padding(top = 8.dp))
    }
}

/** Free content (sliders, swatches, editors) inside a group, at the rows' margins. */
@Composable
fun SettingBlock(anchor: String? = null, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().settingAnchor(anchor).padding(horizontal = 14.dp, vertical = 6.dp)) { content() }
}

/**
 * "Obnovit výchozí" at the bottom of a page: says exactly what goes back to the defaults ([what]) and asks first.
 */
@Composable
fun ResetDefaults(what: String, onReset: () -> Unit) {
    var asking by remember { mutableStateOf(false) }
    val ink = LocalInk.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp)) {
        Quiet(androidx.compose.ui.res.stringResource(dev.intelia.launcher.R.string.reset_defaults), { asking = true })
        Text(what, style = InkText.small, color = ink.muted)
    }
    if (asking) ConfirmDialog(what, androidx.compose.ui.res.stringResource(dev.intelia.launcher.R.string.reset_defaults_do),
        onConfirm = { asking = false; onReset() }, onDismiss = { asking = false })
}

/**
 * One gesture → one action (Veil 3.0, after Essential Key Remapper): the action's icon, the gesture's name, what it
 * does now and an optional note (default, also elsewhere), then [trailing] (remove / reset, or a lock).
 */
@Composable
fun GestureRow(name: String, action: String, icon: @Composable () -> Unit, onClick: (() -> Unit)?, clickLabel: String? = null,
    note: String? = null, quiet: Boolean = false, anchor: String? = null, trailing: (@Composable () -> Unit)? = null) {
    val ink = LocalInk.current
    Row(Modifier.fillMaxWidth().settingAnchor(anchor).heightIn(min = 56.dp)
        .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClickLabel = clickLabel, onClick = onClick) else Modifier)
        .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) { icon() }
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 6.dp)) {
            Text(name, style = InkText.body.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium), color = if (quiet) ink.muted else ink.ink,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(action, style = InkText.small, color = if (quiet) ink.faint else ink.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (note != null) Text(note, style = InkText.small, color = ink.faint, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing?.invoke()
    }
}

/** A quiet note inside a group, at the rows' margins. */
@Composable
fun GroupNote(text: String) {
    Text(text, Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), style = InkText.small, color = LocalInk.current.muted)
}
