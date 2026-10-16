package dev.intelia.launcher.data

import dev.intelia.launcher.flow.Flow
import dev.intelia.launcher.flow.FlowAlias
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Serializable enum class ThemeMode { System, Dark, Light }
@Serializable enum class Accent { Dynamic, Mono, Sage, Slate, Terracotta }
@Serializable enum class Reach { Left, Center, Right }
@Serializable enum class HomeLayout { Pure, Essentials, Custom }
@Serializable enum class ClockStyle { Serif, Grotesk, Mono }
@Serializable enum class Motion { Calm, Brisk, Off }
@Serializable enum class Haptics { Soft, Strong, Off }
@Serializable enum class IconStyle { System, Ink }

@Serializable
data class LauncherSettings(
    val theme: ThemeMode = ThemeMode.System,
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
    val clockStyle: ClockStyle = ClockStyle.Serif,
    val motion: Motion = Motion.Calm,
    val haptics: Haptics = Haptics.Soft,
    val iconStyle: IconStyle = IconStyle.System,
    val pulse: Boolean = true,
) {
    companion object {
        const val ARC_SLOTS = 5
        const val ARC_FLOW_SLOTS = 7
        const val ARC_SHORTCUT_SLOTS = 7
        const val MAX_FAVORITES = 7
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
            reduceMotion = false,
        )
    }
}

object SettingsCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    fun decode(text: String?): LauncherSettings {
        if (text.isNullOrBlank()) return LauncherSettings()
        return try {
            json.decodeFromString(LauncherSettings.serializer(), text).sanitized()
        } catch (_: SerializationException) {
            LauncherSettings()
        } catch (_: IllegalArgumentException) {
            LauncherSettings()
        }
    }

    fun encode(settings: LauncherSettings): String = json.encodeToString(LauncherSettings.serializer(), settings)
}
