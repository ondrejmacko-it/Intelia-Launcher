package dev.intelia.launcher.data

import dev.intelia.launcher.flow.Flow
import dev.intelia.launcher.flow.FlowAlias
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Serializable enum class ThemeMode { System, Dark, Light }
@Serializable enum class Accent { Dynamic, Mono, Sage, Slate }
@Serializable enum class Reach { Left, Center, Right }

@Serializable
data class LauncherSettings(
    val theme: ThemeMode = ThemeMode.System,
    val accent: Accent = Accent.Mono,
    val showClock: Boolean = true,
    val showDate: Boolean = true,
    val reduceMotion: Boolean = false,
    val reach: Reach = Reach.Center,
    val arc: List<String> = emptyList(),
    val hidden: Set<String> = emptySet(),
    val aliases: List<FlowAlias> = emptyList(),
) {
    companion object {
        const val ARC_SLOTS = 5
        private const val MAX_KEY = 512
        private const val MAX_HIDDEN = 1000
        private const val MAX_ALIASES = 200
        private fun validKey(key: String) = key.isNotBlank() && key.length <= MAX_KEY && key.none(Char::isISOControl)
    }

    /** Bounded and self-consistent; applied on every read and write, including restores. */
    fun sanitized(): LauncherSettings {
        val accepted = mutableListOf<FlowAlias>()
        aliases.take(MAX_ALIASES).forEach { if (Flow.validate(it, accepted) == null) accepted += it }
        return copy(
            arc = arc.filter(::validKey).distinct().take(ARC_SLOTS),
            hidden = hidden.filter(::validKey).take(MAX_HIDDEN).toSet(),
            aliases = accepted,
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
