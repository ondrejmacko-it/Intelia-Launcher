package dev.intelia.launcher.onboarding

import dev.intelia.launcher.action.Trigger
import dev.intelia.launcher.data.LauncherSettings

/** What the Discovery Hub (Studio › Prozkoumat Intelii, ONBOARDING_V2 §6) lists, in its order. */
enum class Feature { Lens, Arc, Pulse, Flow, Keys, HomeEditor, Labs }

/**
 * A feature's state on the hub. [Soon] is never actionable, so something unavailable never looks functional;
 * [Try] means it works already and can be tried; [SetUp] needs a choice or a system permission first.
 */
enum class FeatureState { Done, SetUp, Try, Soon }

object Discovery {
    /** [pulseService]: the PULSE accessibility service really runs now (read from the system, never stored). */
    fun state(feature: Feature, settings: LauncherSettings, pulseService: Boolean): FeatureState {
        val controls = settings.controls
        return when (feature) {
            Feature.Lens, Feature.HomeEditor -> FeatureState.Try
            Feature.Arc -> if (controls.arcSets.any { set -> set.slots.any { it != null } }) FeatureState.Done else FeatureState.SetUp
            Feature.Pulse -> when {
                !settings.pulse -> FeatureState.SetUp
                settings.pulseEverywhere && !pulseService -> FeatureState.SetUp
                else -> FeatureState.Done
            }
            Feature.Flow -> if (settings.aliases.isNotEmpty()) FeatureState.Done else FeatureState.SetUp
            Feature.Keys -> if (controls.bindings.any { it.trigger is Trigger.Key }) FeatureState.Done else FeatureState.SetUp
            Feature.Labs -> FeatureState.Soon
        }
    }

    fun states(settings: LauncherSettings, pulseService: Boolean): List<Pair<Feature, FeatureState>> =
        Feature.entries.map { it to state(it, settings, pulseService) }
}
