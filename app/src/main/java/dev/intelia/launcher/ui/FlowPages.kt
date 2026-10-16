package dev.intelia.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.intelia.launcher.R
import dev.intelia.launcher.data.AppEntry
import dev.intelia.launcher.flow.Flow
import dev.intelia.launcher.flow.FlowAlias
import dev.intelia.launcher.flow.FlowError
import dev.intelia.launcher.search.AppSearch
import java.util.UUID

/** The library of FLOW commands: name in italic, its steps as beads, where it can be found. */
@Composable
fun FlowList(
    aliases: List<FlowAlias>,
    arcFlows: List<String>,
    flowSteps: (FlowAlias) -> List<String?>,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (FlowAlias) -> Unit,
    onRun: (FlowAlias) -> Unit,
) {
    val ink = LocalInk.current
    val missing = stringResource(R.string.flow_target_missing)
    InkPage(stringResource(R.string.home), onBack, kicker = stringResource(R.string.flow), title = stringResource(R.string.flow_title)) {
        Text(stringResource(R.string.flow_hint), Modifier.padding(start = 24.dp, end = 24.dp, bottom = 18.dp), style = InkText.small.copy(fontSize = 14.sp),
            color = ink.muted)
        LazyColumn(Modifier.weight(1f)) {
            if (aliases.isEmpty()) item { Text(stringResource(R.string.flow_empty), Modifier.padding(24.dp), style = InkText.body, color = ink.muted) }
            items(aliases, key = { it.id }) { alias ->
                Hairline()
                Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).clickable(role = Role.Button, onClickLabel = stringResource(R.string.flow_edit)) { onEdit(alias) }
                    .padding(start = 24.dp, end = 12.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(alias.name, style = InkText.item.copy(fontSize = 26.sp, lineHeight = 30.sp, fontStyle = FontStyle.Italic),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        BeadThread(flowSteps(alias), missing, Modifier.padding(top = 4.dp))
                    }
                    Kicker(if (alias.id in arcFlows) "ARC · LENS" else "LENS", Modifier.padding(horizontal = 8.dp), color = ink.faint)
                    Quiet(stringResource(R.string.flow_run), { onRun(alias) })
                }
            }
            if (aliases.isNotEmpty()) item { Hairline() }
        }
        Capsule("+ " + stringResource(R.string.flow_create), onCreate, Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            filled = false)
    }
}

private fun FlowError.message(): Int = when (this) {
    FlowError.Empty -> R.string.flow_error_empty
    FlowError.ControlCharacter -> R.string.flow_error_control
    FlowError.TooLong -> R.string.flow_error_long
    FlowError.Duplicate -> R.string.flow_error_duplicate
    FlowError.NoTargets, FlowError.TooManyTargets, FlowError.InvalidTarget -> R.string.flow_error_targets
}

/** Full-screen editor; Save sits at the top so the keyboard can never cover it. */
@Composable
fun FlowEditor(
    initial: FlowAlias?,
    inArc: Boolean,
    arcFull: Boolean,
    apps: List<AppEntry>,
    resolve: (String) -> AppEntry?,
    onSave: (FlowAlias, Boolean) -> FlowError?,
    onDelete: (String) -> Unit,
    onBack: () -> Unit,
) {
    val ink = LocalInk.current
    val id = rememberSaveable(initial?.id) { initial?.id ?: UUID.randomUUID().toString() }
    var name by rememberSaveable(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var targets by rememberSaveable(initial?.id) { mutableStateOf(ArrayList(initial?.targets.orEmpty())) }
    var arc by rememberSaveable(initial?.id) { mutableStateOf(if (initial == null) !arcFull else inArc) }
    var picking by rememberSaveable { mutableStateOf(initial == null) }
    var filter by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<FlowError?>(null) }
    val candidates = remember(apps, filter, targets) {
        (if (filter.isBlank()) apps else AppSearch.rank(apps, filter) { it.searchKey }).filter { it.key !in targets }
    }
    fun change(next: List<String>) { targets = ArrayList(next); error = null }

    InkPage(stringResource(R.string.flow_back_unsaved), onBack, action = {
        Capsule(stringResource(R.string.flow_save), {
            val result = onSave(FlowAlias(id, name.trim(), targets.toList()), arc)
            error = result
        })
    }) {
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp)) {
                    Kicker(stringResource(R.string.flow_name), color = ink.muted)
                    val label = stringResource(R.string.flow_name)
                    BasicTextField(name, { name = it; error = null }, Modifier.fillMaxWidth().padding(top = 4.dp)
                        .semantics { contentDescription = label }
                        .drawBehind { drawLine(ink.faint, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) }
                        .padding(bottom = 10.dp),
                        singleLine = true, cursorBrush = SolidColor(ink.accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        textStyle = InkText.title.copy(fontSize = 40.sp, lineHeight = 46.sp, fontStyle = FontStyle.Italic, color = ink.ink),
                        decorationBox = { inner ->
                            if (name.isEmpty()) Text(stringResource(R.string.flow_name_hint), style = InkText.title.copy(fontSize = 40.sp,
                                lineHeight = 46.sp, fontStyle = FontStyle.Italic), color = ink.line)
                            inner()
                        })
                    error?.let { Text(stringResource(it.message()), Modifier.padding(top = 8.dp), style = InkText.small, color = ink.danger) }
                }
            }
            item { Kicker(stringResource(R.string.flow_steps), Modifier.padding(start = 24.dp, end = 24.dp, top = 26.dp, bottom = 8.dp), color = ink.muted) }
            itemsIndexed(targets, key = { _, key -> "step:$key" }) { index, key ->
                val entry = resolve(key)
                Hairline()
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 24.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text((index + 1).toString(), Modifier.width(22.dp), style = InkText.label.copy(letterSpacing = 0.sp), color = ink.muted)
                    if (entry != null) AppIcon(entry, Modifier.size(32.dp)) else Spacer(Modifier.size(32.dp))
                    Text(entry?.label ?: stringResource(R.string.flow_target_missing), Modifier.padding(start = 14.dp).weight(1f),
                        style = InkText.body, color = if (entry == null) ink.danger else ink.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (index > 0) IconAction(Glyph.Up, stringResource(R.string.flow_step_earlier), {
                        change(targets.toMutableList().apply { add(index - 1, removeAt(index)) })
                    }, tint = ink.muted)
                    IconAction(Glyph.Close, stringResource(R.string.flow_step_remove), { change(targets - key) }, tint = ink.muted)
                }
            }
            item { Hairline() }
            if (targets.size < Flow.MAX_TARGETS) {
                if (!picking) item {
                    Quiet("+ " + stringResource(R.string.flow_add_step), { picking = true }, Modifier.padding(start = 56.dp))
                } else {
                    item { StepFilter(filter) { filter = it } }
                    items(candidates, key = { "pick:" + it.key }) { app ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button) {
                            change(targets + app.key); filter = ""; if (targets.size >= Flow.MAX_TARGETS) picking = false
                        }.padding(start = 56.dp, end = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                            AppIcon(app, Modifier.size(28.dp))
                            Text(app.label, Modifier.padding(start = 14.dp), style = InkText.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    item { Hairline() }
                }
            }
            item {
                Kicker(stringResource(R.string.flow_where), Modifier.padding(start = 24.dp, end = 24.dp, top = 26.dp, bottom = 8.dp), color = ink.muted)
                Hairline()
                ChoiceRow(stringResource(R.string.flow_in_lens)) { Text(stringResource(R.string.always), style = InkText.body, color = ink.accent) }
                Hairline()
                ToggleRow(stringResource(R.string.flow_in_arc), arc, onChange = { on -> if (!on || !arcFull || inArc) arc = on },
                    detail = stringResource(if (arcFull && !inArc) R.string.arc_flows_full else R.string.flow_in_arc_detail))
                Hairline()
                Text(stringResource(R.string.flow_rule), Modifier.padding(start = 24.dp, end = 24.dp, top = 14.dp), style = InkText.small, color = ink.muted)
                if (initial != null) Quiet(stringResource(R.string.flow_delete), { onDelete(initial.id) },
                    Modifier.padding(start = 20.dp, top = 18.dp), color = ink.danger)
            }
        }
    }
}

@Composable
private fun StepFilter(value: String, onChange: (String) -> Unit) {
    val ink = LocalInk.current
    val label = stringResource(R.string.flow_find_app)
    Row(Modifier.fillMaxWidth().padding(start = 56.dp, end = 24.dp, top = 6.dp, bottom = 6.dp).heightIn(min = 48.dp)
        .drawBehind { drawLine(ink.line, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) },
        verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(14.dp)) { drawCircle(ink.muted, size.minDimension / 2f - .75.dp.toPx(), style = Stroke(1.5.dp.toPx())) }
        BasicTextField(value, onChange, Modifier.weight(1f).padding(start = 12.dp).semantics { contentDescription = label }, singleLine = true,
            cursorBrush = SolidColor(ink.accent), textStyle = InkText.body.copy(color = ink.ink),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(label, style = InkText.body, color = ink.faint)
                inner()
            })
    }
}

