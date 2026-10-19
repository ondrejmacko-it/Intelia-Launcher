package dev.intelia.launcher

import dev.intelia.launcher.action.Action
import dev.intelia.launcher.action.Gesture
import dev.intelia.launcher.action.Trigger
import dev.intelia.launcher.data.LauncherSettings
import dev.intelia.launcher.flow.FlowAlias
import dev.intelia.launcher.onboarding.Discovery
import dev.intelia.launcher.onboarding.Feature
import dev.intelia.launcher.onboarding.FeatureState
import dev.intelia.launcher.onboarding.GuideStep
import dev.intelia.launcher.onboarding.OnboardingDraft
import org.junit.Assert.assertEquals
import org.junit.Test

class DiscoveryTest {
    private val fresh = LauncherSettings.fresh()

    @Test fun labsAreNeverActionableAndLensCanAlwaysBeTried() {
        assertEquals(FeatureState.Soon, Discovery.state(Feature.Labs, fresh, pulseService = true))
        assertEquals(FeatureState.Try, Discovery.state(Feature.Lens, fresh, pulseService = false))
        assertEquals(Feature.entries, Discovery.states(fresh, false).map { it.first })
    }

    @Test fun pulseEverywhereIsDoneOnlyWhileItsServiceRuns() {
        val everywhere = fresh.copy(pulse = true, pulseEverywhere = true)
        assertEquals(FeatureState.SetUp, Discovery.state(Feature.Pulse, everywhere, pulseService = false))
        assertEquals(FeatureState.Done, Discovery.state(Feature.Pulse, everywhere, pulseService = true))
        assertEquals(FeatureState.Done, Discovery.state(Feature.Pulse, fresh.copy(pulse = true, pulseEverywhere = false), pulseService = false))
        assertEquals(FeatureState.SetUp, Discovery.state(Feature.Pulse, fresh.copy(pulse = false), pulseService = true))
    }

    @Test fun arcFlowAndKeysFollowWhatIsSetUp() {
        val empty = fresh.copy(controlConfig = fresh.controls.copy(arcSets = fresh.controls.arcSets.map { s -> s.copy(slots = s.slots.map { null }) }))
        assertEquals(FeatureState.SetUp, Discovery.state(Feature.Arc, empty, false))
        val arc = empty.copy(controlConfig = empty.controls.toggleInArc(Action.App("a/A"), true))
        assertEquals(FeatureState.Done, Discovery.state(Feature.Arc, arc, false))
        assertEquals(FeatureState.SetUp, Discovery.state(Feature.Flow, fresh.copy(aliases = emptyList()), false))
        assertEquals(FeatureState.Done, Discovery.state(Feature.Flow, fresh.copy(aliases = listOf(FlowAlias("f", "Ráno", listOf("a/A")))), false))
        val keys = fresh.copy(controlConfig = fresh.controls.copy(bindings = emptyList()))
        assertEquals(FeatureState.SetUp, Discovery.state(Feature.Keys, keys, false))
        val bound = keys.copy(controlConfig = keys.controls.bind(Trigger.Key(24, Gesture.Hold), Action.Torch))
        assertEquals(FeatureState.Done, Discovery.state(Feature.Keys, bound, false))
    }

    @Test fun skipAllLandsOnDoneWithTheEditsKept() {
        val d = OnboardingDraft.firstRun(fresh, listOf("a/A")).next().edit { it.copy(pulse = false) }
        val skipped = d.skipAll()
        assertEquals(GuideStep.Done, skipped.step)
        assertEquals(d.settings, skipped.settings)
        assertEquals(false, skipped.commit(musicAllowed = false).pulse)
    }
}
