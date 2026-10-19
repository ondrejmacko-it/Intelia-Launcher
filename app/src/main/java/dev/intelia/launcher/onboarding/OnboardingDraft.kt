package dev.intelia.launcher.onboarding

import dev.intelia.launcher.data.Anchor
import dev.intelia.launcher.data.HomeLayout
import dev.intelia.launcher.data.HomeLayouts
import dev.intelia.launcher.data.HomeTemplates
import dev.intelia.launcher.data.ItemKind
import dev.intelia.launcher.data.LauncherSettings
import dev.intelia.launcher.data.LayoutSpec
import dev.intelia.launcher.data.TypeFace
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The guide's screens in order (ONBOARDING_V2 §4, owner revision): every one can be skipped. */
@Serializable enum class GuideStep { Welcome, Home, Look, Controls, Access, Done }

/** "Jak má vypadat tvoje plocha?": clock and LENS only, apps around LENS, or the owner's own arrangement. */
@Serializable enum class HomeChoice { Empty, Basic, Custom }

/** The four parts of the Controls step; each is tried on the real Home. */
@Serializable enum class ControlTab { Lens, Arc, Pulse, Flow }

/** What the user tried on the sandboxed Home; only used to tick the coach's chips. */
@Serializable enum class Tried { Tap, Swipe, Hold, ArcPick, PulseTap, FlowShown, PulseArc }

/** Where PULSE works: the three choices of the Controls step. */
@Serializable enum class PulsePick { Everywhere, HomeOnly, Off }

/**
 * The guide's work in progress, apart from the stored [LauncherSettings] until Done (ONBOARDING_V2 §7, §11.1).
 * [start] is what "Výchozí" shows: a new user's default Home, or on a rerun the settings the guide started from.
 * [settings] is "Moje úpravy"; it goes through [LauncherSettings.sanitized] on every change, so the stage never shows
 * a state the store would not accept. Pure Kotlin, saved as JSON after every change (process death keeps it).
 */
@Serializable
data class OnboardingDraft(
    val start: LauncherSettings,
    val settings: LauncherSettings,
    val step: GuideStep = GuideStep.Welcome,
    /** Null until the user picks one: the Home stays as it started. */
    val home: HomeChoice? = null,
    val tab: ControlTab = ControlTab.Lens,
    val tried: Set<Tried> = emptySet(),
    /** The user tapped "Povolit" on the music row; music turns on at Done only if the system access is granted. */
    val wantsMusic: Boolean = false,
    /** Started again from Studio: Skip leaves the stored settings exactly as they are. */
    val rerun: Boolean = false,
) {
    val steps: List<GuideStep> get() = GuideStep.entries

    /** 0-based place of [step] in [steps], for the progress bar. */
    val index: Int get() = steps.indexOf(step)

    val first: Boolean get() = index == 0
    val last: Boolean get() = step == GuideStep.Done

    fun next(): OnboardingDraft = copy(step = steps.getOrElse(index + 1) { step })
    fun back(): OnboardingDraft = copy(step = steps.getOrElse(index - 1) { step })
    fun goTo(target: GuideStep): OnboardingDraft = copy(step = target)

    /** Skipping a step keeps what was chosen so far and moves on; nothing is lost. */
    fun skip(): OnboardingDraft = next()

    /** "Přeskočit průvodce": straight to Done, which shows what will be kept; nothing is saved before its button. */
    fun skipAll(): OnboardingDraft = goTo(GuideStep.Done)

    fun edit(transform: (LauncherSettings) -> LauncherSettings): OnboardingDraft = copy(settings = transform(settings).sanitized())

    fun tried(what: Tried): OnboardingDraft = if (what in tried) this else copy(tried = tried + what)

    /**
     * "Porovnat" has something to show: the stage of [settings] looks different from the stage of [start]. A layout
     * rebuilt to the same content, or a change the Home does not draw (PULSE scope, ARC, haptics), is not a difference.
     */
    val looksChanged: Boolean get() = start.stage() != settings.stage()

    val pulse: PulsePick get() = when {
        !settings.pulse -> PulsePick.Off
        settings.pulseEverywhere -> PulsePick.Everywhere
        else -> PulsePick.HomeOnly
    }

    fun withPulse(pick: PulsePick): OnboardingDraft = edit {
        when (pick) {
            PulsePick.Everywhere -> it.copy(pulse = true, pulseEverywhere = true)
            PulsePick.HomeOnly -> it.copy(pulse = true, pulseEverywhere = false)
            PulsePick.Off -> it.copy(pulse = false)
        }
    }

    /**
     * The Home for [choice], built on the real layout model. The guide writes its own layout: on a first run the
     * user's ([HomeLayouts.USER_ID]); on a rerun Empty and Basic go to [GUIDE_ID], so an own Home made in the editor is
     * never replaced, while Custom continues the user's own layout. [apps] fill the slots around LENS when the user
     * has no favourites yet. The clock face picked in Look is kept across a switch.
     */
    fun withHome(choice: HomeChoice, apps: List<String>): OnboardingDraft {
        val s = settings
        val favorites = s.favorites.ifEmpty { apps.distinct().take(LauncherSettings.MAX_FAVORITES) }
        val own = start.home.layouts.firstOrNull { it.id == HomeLayouts.USER_ID && !it.linked }
        val id = if (own == null || choice == HomeChoice.Custom) HomeLayouts.USER_ID else GUIDE_ID
        val face = clockFace()
        val layout = when (choice) {
            HomeChoice.Custom -> own ?: HomeTemplates.signature(favorites)
            HomeChoice.Basic -> HomeTemplates.signature(favorites)
            HomeChoice.Empty -> HomeTemplates.signature(emptyList())
        }.copy(id = id, linked = false).let { l -> if (face != null && choice != HomeChoice.Custom) l.withClockFace(face) else l }
        val layouts = s.home.layouts.filter { it.id != id } + layout
        return copy(home = choice, settings = s.copy(
            layout = if (choice == HomeChoice.Empty) HomeLayout.Pure else HomeLayout.Essentials,
            favorites = favorites,
            home = HomeLayouts(id, layouts),
        ).sanitized())
    }

    /** The four apps beside LENS on Basic (in slot order); changing them rebuilds that Home. */
    fun withLensApps(keys: List<String>, apps: List<String>): OnboardingDraft {
        val picked = keys.distinct().take(LENS_APPS)
        val rest = settings.favorites.filter { it !in picked }
        return edit { it.copy(favorites = (picked + rest).take(LauncherSettings.MAX_FAVORITES)) }
            .let { d -> if (d.home == HomeChoice.Basic) d.withHome(HomeChoice.Basic, apps) else d }
    }

    /** The face of the active layout's clock; null when the clock follows the global style. */
    fun clockFace(): TypeFace? = settings.home.activeLayout().items.firstOrNull { it.kind == ItemKind.Clock }?.look?.face
        ?.takeIf { it != TypeFace.Style }

    /** Look › Hodiny: the active layout's clock in [face]. A linked preset is first copied, so presets never change. */
    fun withClockFace(face: TypeFace): OnboardingDraft = edit { s ->
        val active = s.home.activeLayout()
        s.copy(home = if (active.linked) s.home.save(active.withClockFace(face))
        else s.home.copy(layouts = s.home.layouts.map { if (it.id == active.id) it.withClockFace(face) else it }))
    }

    /** The Home Editor's ✓ inside the guide: the edited layout lands in the draft only. */
    fun withEditedLayout(edited: LayoutSpec): OnboardingDraft = copy(home = HomeChoice.Custom).edit { it.copy(home = it.home.save(edited)) }

    /**
     * Done (§11.1): the validated draft as the new stored settings, in one step. Music needs both the user's tap and
     * the system's notification access; everything else is what the stage showed.
     */
    fun commit(musicAllowed: Boolean): LauncherSettings =
        settings.copy(onboarded = true, pulseMusic = if (wantsMusic && musicAllowed) true else settings.pulseMusic && musicAllowed).sanitized()

    companion object {
        /** The layout a rerun writes for Empty and Basic, beside the user's own. */
        const val GUIDE_ID = "guide"
        const val LENS_APPS = 4

        /**
         * A first run: "Výchozí" is the signature Home with the phone's everyday apps ([apps]), which is also what
         * "Použít výchozí nastavení" keeps. [stored] is what the store holds (a fresh install's defaults).
         */
        fun firstRun(stored: LauncherSettings, apps: List<String>): OnboardingDraft {
            val base = OnboardingDraft(stored, stored).withHome(HomeChoice.Basic, apps)
            return OnboardingDraft(start = base.settings, settings = base.settings)
        }

        /** Studio › "Spustit průvodce znovu": starts from the current settings and resets nothing. */
        fun rerun(stored: LauncherSettings): OnboardingDraft = OnboardingDraft(stored, stored, rerun = true)
    }
}

/**
 * The clock in [face]. The signature date sits beside the slanted Unbounded hours; with any other face it would run into
 * them, so it follows the clock (bound under it, as in the presets) and returns beside it with Unbounded.
 */
private fun LayoutSpec.withClockFace(face: TypeFace): LayoutSpec {
    val signatureDate = HomeTemplates.signature(emptyList()).items.first { it.kind == ItemKind.Date }
    return copy(items = items.map {
        when (it.kind) {
            ItemKind.Clock -> it.copy(look = it.look.copy(face = face))
            ItemKind.Date -> if (face == TypeFace.Unbounded) it.copy(anchor = signatureDate.anchor) else it.copy(anchor = Anchor.Bound)
            else -> it
        }
    })
}

/**
 * What the guide's stage draws from these settings: the active layout by its content (not its id, name, place in the
 * list or link to a preset) and the look fields. Behaviour the Home does not draw is reset, so it never counts; so are
 * the legacy Home fields, which [LauncherSettings.sanitized] already builds into the linked layout.
 */
private fun LauncherSettings.stage(): LauncherSettings {
    val active = home.layouts.firstOrNull { it.id == home.active } ?: home.layouts.firstOrNull()
    val plain = LauncherSettings()
    return copy(
        home = HomeLayouts("", listOfNotNull(active?.copy(id = "", name = "", linked = false))),
        layout = plain.layout, favorites = plain.favorites, showClock = plain.showClock, showDate = plain.showDate, reach = plain.reach,
        hidden = plain.hidden, aliases = plain.aliases, onboarded = plain.onboarded,
        arc = plain.arc, arcFlows = plain.arcFlows, arcShortcuts = plain.arcShortcuts, arcRings = plain.arcRings, controlConfig = plain.controlConfig,
        motion = plain.motion, reduceMotion = plain.reduceMotion, haptics = plain.haptics, haptic = plain.haptic, editorCard = plain.editorCard,
        pulseEverywhere = plain.pulseEverywhere, pulseHit = plain.pulseHit, pulseTapPlayer = plain.pulseTapPlayer,
        pulseDoubleTapMs = plain.pulseDoubleTapMs, pulseTouchHaptic = plain.pulseTouchHaptic, pulseAlarmHours = plain.pulseAlarmHours,
    )
}

/** The draft as a small JSON document; anything unreadable is dropped, never half-applied. */
object DraftCodec {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    fun encode(draft: OnboardingDraft): String = json.encodeToString(OnboardingDraft.serializer(), draft)

    fun decode(text: String?): OnboardingDraft? {
        if (text.isNullOrBlank()) return null
        return try {
            json.decodeFromString(OnboardingDraft.serializer(), text).let { it.copy(start = it.start.sanitized(), settings = it.settings.sanitized()) }
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
