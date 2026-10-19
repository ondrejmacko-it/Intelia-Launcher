package dev.intelia.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.intelia.launcher.R
import dev.intelia.launcher.data.LauncherSettings
import dev.intelia.launcher.onboarding.Discovery
import dev.intelia.launcher.onboarding.Feature
import dev.intelia.launcher.onboarding.FeatureState

/**
 * Studio › Prozkoumat Intelii (ONBOARDING_V2 §6): every pillar with its real state, long after the first run. A row
 * opens where it is set up or tried; "Připravujeme" rows have no action, so nothing unavailable looks functional.
 */
@Composable
internal fun DiscoveryHub(settings: LauncherSettings, onOpen: (Feature) -> Unit) {
    val service by PulseAccessibilityService.running.collectAsStateWithLifecycle()
    VeilGroup {
        Discovery.states(settings, service).forEach { (feature, state) ->
            val (title, detail, glyph) = when (feature) {
                Feature.Lens -> Triple("LENS", R.string.hub_lens, Glyph.Search)
                Feature.Arc -> Triple("ARC", R.string.hub_arc, Glyph.Arc)
                Feature.Pulse -> Triple("PULSE", R.string.hub_pulse, Glyph.Pulse)
                Feature.Flow -> Triple("FLOW", R.string.hub_flow, Glyph.Flow)
                Feature.Keys -> Triple(stringResource(R.string.keys_title), R.string.hub_keys, Glyph.Key)
                Feature.HomeEditor -> Triple(stringResource(R.string.edit_home), R.string.hub_editor, Glyph.Grid)
                Feature.Labs -> Triple(stringResource(R.string.hub_labs_title), R.string.hub_labs, Glyph.Layers)
            }
            SettingRow(title, stringResource(detail), if (state == FeatureState.Soon) null else { { onOpen(feature) } }, glyph = glyph,
                trailing = { StateTag(state) })
        }
    }
}

@Composable
private fun StateTag(state: FeatureState) {
    val ink = LocalInk.current
    val on = state == FeatureState.Done
    Text(stringResource(when (state) {
        FeatureState.Done -> R.string.hub_done
        FeatureState.SetUp -> R.string.hub_setup
        FeatureState.Try -> R.string.hub_try
        FeatureState.Soon -> R.string.hub_soon
    }), Modifier.clip(CircleShape).background(if (on) ink.accent.copy(alpha = .2f) else ink.ink.copy(alpha = .07f))
        .padding(horizontal = 12.dp, vertical = 6.dp),
        style = InkText.small.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
        color = when (state) { FeatureState.Done -> ink.accent; FeatureState.Soon -> ink.muted; else -> ink.ink })
}
