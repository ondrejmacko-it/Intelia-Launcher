package dev.intelia.launcher.action

import dev.intelia.launcher.flow.Flow as FlowRules
import dev.intelia.launcher.flow.FlowAlias
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One thing a gesture can do (D10): LENS-ARC slots, Global ARC, PULSE gestures and the Search exact
 * match all bind to an [Action]. Pure Kotlin; the Android side is behind [ActionHost].
 * Actions come from restored settings, so every field is untrusted until [unavailableReason] says otherwise.
 */
@Serializable
sealed interface Action {
    /** Launcher component key, as stored in settings ("package/class"). */
    @Serializable @SerialName("app") data class App(val component: String) : Action

    /** A FLOW by its stable id: renaming the FLOW keeps the binding; deleting it makes it [Reason.FlowMissing]. */
    @Serializable @SerialName("flow") data class Flow(val id: String) : Action

    @Serializable @SerialName("search") data object Search : Action

    @Serializable @SerialName("shortcut") data class Shortcut(val pkg: String, val id: String) : Action

    @Serializable @SerialName("media") data class Media(val key: MediaKey) : Action

    @Serializable @SerialName("torch") data object Torch : Action

    @Serializable @SerialName("screenshot") data object Screenshot : Action

    @Serializable @SerialName("sound") data object SoundMode : Action

    @Serializable @SerialName("panel") data class SystemPanel(val kind: PanelKind) : Action

    @Serializable @SerialName("card") data class Card(val id: String) : Action

    @Serializable @SerialName("lock") data object Lock : Action

    /** Null when the action can run now; otherwise why not. Never has side effects. */
    fun unavailableReason(host: ActionHost): Reason? = when (this) {
        is App -> when {
            !wellFormed(component) -> Reason.Invalid
            !host.hasApp(component) -> Reason.AppMissing
            else -> null
        }
        is Flow -> when (val flow = host.findFlow(id)) {
            null -> Reason.FlowMissing
            else -> if (flow.targets.all(host::hasApp)) null else Reason.AppMissing
        }
        Search -> null
        is Shortcut -> when {
            !wellFormed(pkg) || !wellFormed(id) -> Reason.Invalid
            !host.isDefaultHome() -> Reason.NotDefaultHome
            !host.hasShortcut(pkg, id) -> Reason.ShortcutMissing
            else -> null
        }
        is Media -> null
        Torch -> if (host.hasTorch()) null else Reason.Unsupported
        Screenshot -> if (host.hasPulseService()) null else Reason.NeedsPulseService
        SoundMode -> null
        is SystemPanel -> null
        is Card -> if (wellFormed(id) && host.hasCard(id)) null else Reason.CardMissing
        Lock -> if (host.hasPulseService()) null else Reason.NeedsPulseService
    }

    /** What the user has to grant before this can run; shown next to the action in pickers. */
    val requirement: Requirement get() = when (this) {
        is Shortcut -> Requirement.DefaultHome
        Screenshot, Lock -> Requirement.PulseService
        else -> Requirement.None
    }

    /**
     * Runs from PULSE outside Home (M3): system actions only, which need nothing from Intelia's own screens.
     * Apps, FLOWs, Search, shortcuts and cards stay on Home, where FLOW keeps its preview and Search its sheet.
     */
    val outsideHome: Boolean get() = when (this) {
        is Media, Torch, Screenshot, SoundMode, is SystemPanel, Lock -> true
        is App, is Flow, Search, is Shortcut, is Card -> false
    }

    /** Structural check for restored data; availability on this device is [unavailableReason]. */
    fun wellFormed(): Boolean = when (this) {
        is App -> wellFormed(component)
        is Flow -> wellFormed(id)
        is Shortcut -> wellFormed(pkg) && wellFormed(id)
        is Card -> wellFormed(id)
        else -> true
    }

    fun isAvailable(host: ActionHost): Boolean = unavailableReason(host) == null

    /**
     * Runs only after [isAvailable]; reports [ActionResult.Done] only when the system call said it succeeded,
     * [ActionResult.Opened] when something else (an app, a panel, a sheet) now has to finish the job.
     */
    fun run(host: ActionHost): ActionResult {
        unavailableReason(host)?.let { return ActionResult.Unavailable(it) }
        val ok = when (this) {
            is App -> return opened(host.launchApp(component))
            is Flow -> return opened(host.runFlow(checkNotNull(host.findFlow(id))))
            Search -> return opened(host.openSearch())
            is Shortcut -> return opened(host.startShortcut(pkg, id))
            is Media -> host.sendMedia(key)
            Torch -> host.toggleTorch()
            Screenshot -> host.takeScreenshot()
            SoundMode -> host.toggleSoundMode()
            is SystemPanel -> return opened(host.openPanel(kind))
            is Card -> return opened(host.openCard(id))
            Lock -> host.lockScreen()
        }
        return if (ok) ActionResult.Done else ActionResult.Unavailable(Reason.Failed)
    }

    companion object {
        private const val MAX_KEY = 512

        fun wellFormed(value: String) = value.isNotBlank() && value.length <= MAX_KEY && value.none(Char::isISOControl)

        private fun opened(ok: Boolean): ActionResult = if (ok) ActionResult.Opened else ActionResult.Unavailable(Reason.Failed)
    }
}

@Serializable enum class MediaKey { PlayPause, Next, Previous }

/** System panels; Wi-Fi, Bluetooth and mobile data cannot be toggled by an app, only opened (D10, honesty). */
@Serializable enum class PanelKind { Internet, Wifi, Bluetooth, Sound, Display, Notifications, QuickSettings }

enum class Requirement { None, DefaultHome, PulseService }

enum class Reason { Invalid, AppMissing, FlowMissing, ShortcutMissing, NotDefaultHome, CardMissing, NeedsPulseService, Unsupported, Failed }

sealed interface ActionResult {
    /** The system confirmed the change (torch on, media key delivered, ...). */
    data object Done : ActionResult

    /** Another surface took over (an app, a system panel, Search). */
    data object Opened : ActionResult

    data class Unavailable(val reason: Reason) : ActionResult
}

/**
 * Everything an [Action] needs from the device. Queries must be cheap and side-effect free; performers
 * return true only when the platform call succeeded.
 */
interface ActionHost {
    fun hasApp(component: String): Boolean
    /** By stable id; Search's exact name match uses [exactFlow] instead. */
    fun findFlow(id: String): FlowAlias?
    fun isDefaultHome(): Boolean
    fun hasShortcut(pkg: String, id: String): Boolean
    fun hasTorch(): Boolean
    fun hasPulseService(): Boolean
    fun hasCard(id: String): Boolean

    fun launchApp(component: String): Boolean
    fun runFlow(flow: FlowAlias): Boolean
    fun openSearch(): Boolean
    fun startShortcut(pkg: String, id: String): Boolean
    fun sendMedia(key: MediaKey): Boolean
    fun toggleTorch(): Boolean
    fun takeScreenshot(): Boolean
    fun toggleSoundMode(): Boolean
    fun openPanel(kind: PanelKind): Boolean
    fun openCard(id: String): Boolean
    fun lockScreen(): Boolean
}

/** Exact FLOW lookup shared by hosts: same normalisation as Search, never a prefix match. */
fun List<FlowAlias>.exactFlow(name: String): FlowAlias? {
    val wanted = FlowRules.normalize(name)
    return if (wanted.isEmpty()) null else firstOrNull { FlowRules.normalize(it.name) == wanted }
}
