package dev.intelia.launcher.data

import dev.intelia.launcher.flow.Flow
import dev.intelia.launcher.flow.FlowAlias
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Serializable enum class ThemeMode { System, Dark, Light }
/** [Frost] and [Silver] arrive with the Crystal design (D10); [Dynamic] is "from the wallpaper". */
@Serializable enum class Accent { Dynamic, Mono, Sage, Slate, Terracotta, Frost, Silver }
@Serializable enum class Reach { Left, Center, Right }
@Serializable enum class HomeLayout { Pure, Essentials, Custom }
/** [Crystal] is Roboto Flex (D10, D11); [Serif] is the original "Ink" clock and stays an option. */
@Serializable enum class ClockStyle { Serif, Grotesk, Mono, Crystal }
@Serializable enum class Motion { Calm, Brisk, Off }
@Serializable enum class Haptics { Soft, Strong, Off }
@Serializable enum class IconStyle { System, Ink }

/** Manual camera position for PULSE, in dp from the top-left of the portrait screen; [r] is the lens radius. */
@Serializable data class PulseLens(val x: Float, val y: Float, val r: Float)

@Serializable
data class LauncherSettings(
    val theme: ThemeMode = ThemeMode.System,
    /**
     * Stored documents written before [SCHEMA] 1 never contained this key while it was the default,
     * so the declared default must stay [Accent.Terracotta]; new installs start from [fresh] (Frost).
     */
    val accent: Accent = Accent.Terracotta,
    val showClock: Boolean = true,
    val showDate: Boolean = true,
    /** Legacy switch from the foundation build; folded into [motion] by [sanitized]. */
    val reduceMotion: Boolean = false,
    val reach: Reach = Reach.Center,
    /** ARC ring 1: app keys in fixed slots. */
    val arc: List<String> = emptyList(),
    val hidden: Set<String> = emptySet(),
    val aliases: List<FlowAlias> = emptyList(),
    val onboarded: Boolean = false,
    val layout: HomeLayout = HomeLayout.Essentials,
    /** Home favourites: the first four sit around LENS (Essentials), all of them form the list (Custom) and "Mine". */
    val favorites: List<String> = emptyList(),
    /** ARC ring 2: FLOW ids in fixed slots. */
    val arcFlows: List<String> = emptyList(),
    /** ARC ring 3: app shortcut keys ("package/id") in fixed slots. */
    val arcShortcuts: List<String> = emptyList(),
    val arcRings: Int = 2,
    /** Same rule as [accent]: absent in older documents means [ClockStyle.Serif]; new installs get Crystal. */
    val clockStyle: ClockStyle = ClockStyle.Serif,
    val motion: Motion = Motion.Calm,
    val haptics: Haptics = Haptics.Soft,
    val iconStyle: IconStyle = IconStyle.System,
    val pulse: Boolean = true,
    /** Null: use the display cutout reported by the system. */
    val pulseLens: PulseLens? = null,
    val pulseEcho: Boolean = true,
    val pulseAlarm: Boolean = true,
    val pulseCharging: Boolean = true,
    /** How close an alarm must be, in hours, before the capsule shows it. */
    val pulseAlarmHours: Int = 12,
    /** Capsule background opacity in percent; text stays opaque. */
    val pulseOpacity: Int = 100,
    /** Document version; 0 = written before the Crystal design (phase B1). Migrated by [sanitized]. */
    val schema: Int = 0,
) {
    companion object {
        const val SCHEMA = 1

        /** Settings of a new install: current schema and the Crystal defaults (accent, clock). */
        fun fresh() = LauncherSettings(accent = Accent.Frost, clockStyle = ClockStyle.Crystal, schema = SCHEMA)

        const val ARC_SLOTS = 5
        const val ARC_FLOW_SLOTS = 7
        const val ARC_SHORTCUT_SLOTS = 7
        const val MAX_FAVORITES = 7
        const val MAX_LENS_X = 1000f
        const val MAX_LENS_Y = 300f
        const val MIN_LENS_R = 3f
        const val MAX_LENS_R = 60f
        const val MIN_PULSE_OPACITY = 40
        private const val MAX_KEY = 512
        private const val MAX_HIDDEN = 1000
        private const val MAX_ALIASES = 200
        private fun validKey(key: String) = key.isNotBlank() && key.length <= MAX_KEY && key.none(Char::isISOControl)
    }

    /** Bounded and self-consistent; applied on every read and write, including restores. */
    fun sanitized(): LauncherSettings {
        val accepted = mutableListOf<FlowAlias>()
        aliases.take(MAX_ALIASES).forEach { if (Flow.validate(it, accepted) == null) accepted += it }
        val ids = accepted.mapTo(HashSet()) { it.id }
        return copy(
            arc = arc.filter(::validKey).distinct().take(ARC_SLOTS),
            hidden = hidden.filter(::validKey).take(MAX_HIDDEN).toSet(),
            aliases = accepted,
            favorites = favorites.filter(::validKey).distinct().take(MAX_FAVORITES),
            arcFlows = arcFlows.filter { it in ids }.distinct().take(ARC_FLOW_SLOTS),
            arcShortcuts = arcShortcuts.filter(::validKey).distinct().take(ARC_SHORTCUT_SLOTS),
            arcRings = arcRings.coerceIn(1, 3),
            motion = if (reduceMotion) Motion.Off else motion,
            pulseLens = pulseLens?.takeIf { it.x.isFinite() && it.y.isFinite() && it.r.isFinite() }
                ?.let { PulseLens(it.x.coerceIn(0f, MAX_LENS_X), it.y.coerceIn(0f, MAX_LENS_Y), it.r.coerceIn(MIN_LENS_R, MAX_LENS_R)) },
            pulseAlarmHours = pulseAlarmHours.coerceIn(1, 24),
            pulseOpacity = pulseOpacity.coerceIn(MIN_PULSE_OPACITY, 100),
            reduceMotion = false,
            // 0 → 1: nothing to rewrite; an existing user's accent (Terracotta when absent) is kept as is.
            schema = SCHEMA,
        )
    }
}

object SettingsCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    fun decode(text: String?): LauncherSettings {
        if (text.isNullOrBlank()) return LauncherSettings.fresh()
        return try {
            json.decodeFromString(LauncherSettings.serializer(), text).sanitized()
        } catch (_: SerializationException) {
            LauncherSettings.fresh()
        } catch (_: IllegalArgumentException) {
            LauncherSettings.fresh()
        }
    }

    fun encode(settings: LauncherSettings): String = json.encodeToString(LauncherSettings.serializer(), settings)
}
