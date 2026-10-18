package dev.intelia.launcher.ui

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.intelia.launcher.R
import dev.intelia.launcher.data.AppEntry

fun isDefaultHome(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= 29) context.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME)
    else context.packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
        ?.activityInfo?.packageName == context.packageName

/** Whether Intelia is the default home, re-read on every start, and a way to ask the system for it. */
class HomeRole(val held: Boolean, val request: () -> Unit)

@Composable
fun rememberHomeRole(): HomeRole {
    val context = LocalContext.current
    var held by remember { mutableStateOf(isDefaultHome(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { held = isDefaultHome(context) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { held = isDefaultHome(context) }
    return HomeRole(held) {
        val intent = if (Build.VERSION.SDK_INT >= 29) context.getSystemService(RoleManager::class.java).createRequestRoleIntent(RoleManager.ROLE_HOME)
        else Intent(Settings.ACTION_HOME_SETTINGS)
        try {
            launcher.launch(intent)
        } catch (_: android.content.ActivityNotFoundException) {
            launcher.launch(Intent(Settings.ACTION_HOME_SETTINGS))
        }
    }
}

/** Long-press options for an app: plain rows on paper, no chips or cards. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppOptionsSheet(
    app: AppEntry,
    inArc: Boolean,
    arcFull: Boolean,
    favorite: Boolean,
    favoritesFull: Boolean,
    onArc: () -> Unit,
    onFavorite: () -> Unit,
    onHide: () -> Unit,
    onDetails: () -> Unit,
    onDismiss: () -> Unit,
    level: VeilLevel = VeilLevel.Dense,
) {
    val ink = LocalInk.current
    val state = rememberBottomSheetState(SheetValue.Hidden, setOf(SheetValue.Hidden, SheetValue.Expanded))
    // Over Home the sheet softens it through the shared Dense depth; over Search or the drawer, Home is already in Focus.
    if (level == VeilLevel.Dense) {
        val open by animateFloatAsState(if (state.targetValue == SheetValue.Expanded) 1f else 0f, tween(motionMs(300)), label = "depth")
        VeilDepthSource(VeilLevel.Dense) { open }
    }
    val scrim = veilScrim()
    // Over Search or the drawer the rows below are text, which showed through a 74 % sheet on the Phone (3):
    // there the sheet takes the Matte preset of the same material. Legibility first.
    val c = LocalCrystal.current
    val tokens = if (level == VeilLevel.Focus) veilTokens(level, c.dark, c.acc, LocalVeil.current.copy(solid = true)) else veil(level)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = Color.Transparent,
        contentColor = ink.ink,
        scrimColor = if (level == VeilLevel.Focus) scrim else scrim.copy(alpha = scrim.alpha * DENSE_SCRIM),
        tonalElevation = 0.dp,
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0) },
    ) {
        // Veil Dense (§5.2; Focus over a layer): the sheet rises from the icon row at the bottom, so its edge is lit there.
        Column(Modifier.fillMaxWidth().veilSurface(tokens, VeilOrigin(.5f, 1.15f),
            RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).navigationBarsPadding().padding(bottom = 12.dp)) {
            Box(Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp, bottom = 6.dp).size(28.dp, 3.dp)
                .clip(RoundedCornerShape(2.dp)).background(ink.line))
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                AppIcon(app, Modifier.size(40.dp))
                Text(app.label, Modifier.padding(start = 16.dp), style = InkText.title.copy(fontSize = 24.sp, lineHeight = 28.sp),
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Hairline(Modifier.padding(start = 24.dp, end = 24.dp, top = 6.dp))
            OptionRow(stringResource(if (inArc) R.string.arc_remove else if (arcFull) R.string.arc_full else R.string.arc_add),
                enabled = inArc || !arcFull, mark = inArc, onClick = onArc)
            OptionRow(stringResource(if (favorite) R.string.favorite_remove else if (favoritesFull) R.string.favorite_full else R.string.favorite_add),
                enabled = favorite || !favoritesFull, mark = favorite, onClick = onFavorite)
            OptionRow(stringResource(R.string.hide), onClick = onHide)
            OptionRow(stringResource(R.string.app_info), onClick = onDetails)
        }
    }
}

@Composable
private fun OptionRow(label: String, enabled: Boolean = true, mark: Boolean = false, onClick: () -> Unit) {
    val ink = LocalInk.current
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = InkText.body, color = if (enabled) ink.ink else ink.faint)
        if (mark) HomeGlyph(Glyph.Check, null, tint = ink.accent, size = 20.dp)
    }
}
