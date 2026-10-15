package dev.intelia.launcher.ui

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.intelia.launcher.R
import dev.intelia.launcher.data.Accent
import dev.intelia.launcher.data.AppEntry
import dev.intelia.launcher.data.Reach
import dev.intelia.launcher.data.ThemeMode
import dev.intelia.launcher.flow.Flow
import dev.intelia.launcher.flow.FlowAlias
import dev.intelia.launcher.flow.FlowError
import dev.intelia.launcher.search.AppSearch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Sheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        content()
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp),
        style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> Choice(options: List<Pair<T, Int>>, selected: T, onSelect: (T) -> Unit) {
    FlowRow(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(stringResource(label)) })
        }
    }
}

@Composable
private fun Toggle(label: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(headlineContent = { Text(stringResource(label)) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { onChange(!checked) }.padding(horizontal = 8.dp))
}

private fun isDefaultHome(context: android.content.Context): Boolean =
    if (Build.VERSION.SDK_INT >= 29) context.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME)
    else context.packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
        ?.activityInfo?.packageName == context.packageName

@Composable
fun SettingsSheet(model: HomeViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val settings by model.settings.collectAsStateWithLifecycle()
    val hidden by model.hiddenApps.collectAsStateWithLifecycle()
    var isHome by remember { mutableStateOf(isDefaultHome(context)) }
    val roleRequest = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { isHome = isDefaultHome(context) }
    var about by rememberSaveable { mutableStateOf(false) }
    Sheet(onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                InteliaMark(Modifier.size(28.dp))
                Text(stringResource(R.string.settings), Modifier.padding(start = 12.dp), style = MaterialTheme.typography.headlineSmall)
            }
            if (!isHome) FilledTonalButton(onClick = {
                val intent = if (Build.VERSION.SDK_INT >= 29) context.getSystemService(RoleManager::class.java).createRequestRoleIntent(RoleManager.ROLE_HOME)
                else Intent(Settings.ACTION_HOME_SETTINGS)
                roleRequest.launch(intent)
            }, Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) { Text(stringResource(R.string.default_home)) }
            else Text(stringResource(R.string.default_home_active), Modifier.padding(24.dp, 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)

            SectionTitle(stringResource(R.string.appearance))
            Choice(listOf(ThemeMode.System to R.string.theme_system, ThemeMode.Dark to R.string.theme_dark, ThemeMode.Light to R.string.theme_light),
                settings.theme) { value -> model.updateSettings { it.copy(theme = value) } }
            Spacer(Modifier.height(4.dp))
            Choice(listOf(Accent.Mono to R.string.accent_mono, Accent.Dynamic to R.string.accent_dynamic,
                Accent.Sage to R.string.accent_sage, Accent.Slate to R.string.accent_slate),
                settings.accent) { value -> model.updateSettings { it.copy(accent = value) } }
            Toggle(R.string.show_clock, settings.showClock) { value -> model.updateSettings { it.copy(showClock = value) } }
            Toggle(R.string.show_date, settings.showDate) { value -> model.updateSettings { it.copy(showDate = value) } }
            Toggle(R.string.reduce_motion, settings.reduceMotion) { value -> model.updateSettings { it.copy(reduceMotion = value) } }

            SectionTitle(stringResource(R.string.reach))
            Choice(listOf(Reach.Left to R.string.reach_left, Reach.Center to R.string.reach_center, Reach.Right to R.string.reach_right),
                settings.reach) { value -> model.updateSettings { it.copy(reach = value) } }

            SectionTitle(stringResource(R.string.hidden_apps))
            if (hidden.isEmpty()) Text(stringResource(R.string.no_hidden), Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            hidden.forEach { app ->
                ListItem(headlineContent = { Text(app.label) }, leadingContent = { AppIcon(app, Modifier.size(32.dp)) },
                    trailingContent = { TextButton(onClick = { model.unhide(app) }) { Text(stringResource(R.string.unhide)) } },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent), modifier = Modifier.padding(horizontal = 8.dp))
            }

            HorizontalDivider(Modifier.padding(top = 20.dp, start = 24.dp, end = 24.dp))
            Text(stringResource(R.string.local_only), Modifier.padding(24.dp, 16.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { about = !about }, Modifier.padding(horizontal = 12.dp)) { Text(stringResource(R.string.about_title)) }
            if (about) Text(stringResource(R.string.about_text), Modifier.padding(horizontal = 24.dp), fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AppOptionsSheet(app: AppEntry, inArc: Boolean, arcFull: Boolean, onArc: () -> Unit, onHide: () -> Unit,
    onDetails: () -> Unit, onDismiss: () -> Unit) {
    Sheet(onDismiss) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Row(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                AppIcon(app, Modifier.size(40.dp))
                Text(app.label, Modifier.padding(start = 16.dp), style = MaterialTheme.typography.headlineSmall)
            }
            TextButton(onClick = onArc, enabled = inArc || !arcFull, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text(stringResource(if (inArc) R.string.arc_remove else if (arcFull) R.string.arc_full else R.string.arc_add))
            }
            TextButton(onClick = onHide, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) { Text(stringResource(R.string.hide)) }
            TextButton(onClick = onDetails, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) { Text(stringResource(R.string.app_info)) }
        }
    }
}

private fun FlowError.message(): Int = when (this) {
    FlowError.Empty -> R.string.flow_error_empty
    FlowError.ControlCharacter -> R.string.flow_error_control
    FlowError.TooLong -> R.string.flow_error_long
    FlowError.Duplicate -> R.string.flow_error_duplicate
    FlowError.NoTargets, FlowError.TooManyTargets, FlowError.InvalidTarget -> R.string.flow_error_targets
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowEditorSheet(initial: FlowAlias?, apps: List<AppEntry>, onSave: (FlowAlias) -> FlowError?, onDelete: (String) -> Unit,
    onDismiss: () -> Unit) {
    val id = remember(initial) { initial?.id ?: UUID.randomUUID().toString() }
    var name by rememberSaveable(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var targets by rememberSaveable(initial) { mutableStateOf(initial?.targets.orEmpty()) }
    var filter by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<FlowError?>(null) }
    val byKey = remember(apps) { apps.associateBy { it.key } }
    val candidates = remember(apps, filter, targets) {
        (if (filter.isBlank()) apps else AppSearch.rank(apps, filter) { it.searchKey }).filter { it.key !in targets }
    }
    Sheet(onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            // Actions live in the header so the keyboard and the app list can never cover them.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(if (initial == null) R.string.flow_create else R.string.flow_edit),
                    Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                FilledTonalButton(onClick = {
                    val result = onSave(FlowAlias(id, name.trim(), targets))
                    if (result == null) onDismiss() else error = result
                }) { Text(stringResource(R.string.flow_save)) }
            }
            OutlinedTextField(name, { name = it; error = null }, Modifier.fillMaxWidth().padding(top = 16.dp), singleLine = true,
                label = { Text(stringResource(R.string.flow_name)) }, isError = error in setOf(FlowError.Empty, FlowError.ControlCharacter, FlowError.TooLong, FlowError.Duplicate))
            Text(stringResource(R.string.flow_targets, Flow.MAX_TARGETS), Modifier.padding(top = 16.dp, bottom = 8.dp),
                style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                targets.forEachIndexed { index, key ->
                    val entry = byKey[key]
                    InputChip(selected = false, onClick = { targets = targets - key; error = null },
                        label = { Text("${index + 1}. ${entry?.label ?: stringResource(R.string.flow_target_missing)}") },
                        avatar = entry?.let { { AppIcon(it, Modifier.size(20.dp)) } })
                }
            }
            error?.let { Text(stringResource(it.message()), Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            if (targets.size < Flow.MAX_TARGETS) {
                OutlinedTextField(filter, { filter = it }, Modifier.fillMaxWidth().padding(top = 12.dp), singleLine = true,
                    placeholder = { Text(stringResource(R.string.flow_find_app)) }, leadingIcon = { HomeGlyph(Glyph.Search, null) })
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 260.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(candidates, key = { it.key }) { app ->
                        Row(Modifier.fillMaxWidth().clickable { targets = targets + app.key; filter = ""; error = null }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            AppIcon(app, Modifier.size(28.dp))
                            Text(app.label, Modifier.padding(start = 14.dp))
                        }
                    }
                }
            }
            if (initial != null) TextButton(onClick = { onDelete(initial.id) }, Modifier.padding(top = 8.dp)) {
                Text(stringResource(R.string.flow_delete), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
