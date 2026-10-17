package dev.intelia.launcher.ui

import android.accessibilityservice.AccessibilityService
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import dev.intelia.launcher.action.ActionHost
import dev.intelia.launcher.action.MediaKey
import dev.intelia.launcher.action.PanelKind
import dev.intelia.launcher.flow.FlowAlias

/**
 * The Action Engine's Android side (Control System V1): public platform APIs only. Queries are cheap reads of
 * state the launcher already holds; performers report true only when the platform accepted the call.
 */
class AndroidActionHost(
    private val context: Context,
    /** Null in the PULSE service outside Home: apps, FLOWs and shortcuts then report missing (M3). */
    private val model: HomeViewModel?,
    private val onSearch: () -> Boolean,
) : ActionHost {
    private val camera = context.getSystemService(CameraManager::class.java)
    private val torchId: String? by lazy { camera?.let(::torchCamera) }
    internal var torchOn = false

    internal val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) { if (cameraId == torchId) torchOn = enabled }
    }

    override fun hasApp(component: String) = model?.entry(component)?.suspended == false
    override fun findFlow(id: String): FlowAlias? = model?.settings?.value?.aliases?.firstOrNull { it.id == id }
    override fun isDefaultHome(): Boolean = if (Build.VERSION.SDK_INT >= 29)
        context.getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_HOME) == true
    else context.packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
        ?.activityInfo?.packageName == context.packageName
    override fun hasShortcut(pkg: String, id: String) = model?.shortcuts?.value.orEmpty().any { it.packageName == pkg && it.id == id }
    override fun hasTorch() = torchId != null
    override fun hasPulseService() = PulseAccessibilityService.running.value
    override fun hasCard(id: String) = false

    override fun launchApp(component: String): Boolean = model?.let { m -> m.entry(component)?.let(m::launch) } ?: false
    override fun runFlow(flow: FlowAlias) = model?.runFlow(flow) == FlowRun.Launched
    override fun openSearch() = onSearch()
    override fun startShortcut(pkg: String, id: String): Boolean =
        model?.let { m -> m.shortcuts.value.firstOrNull { it.packageName == pkg && it.id == id }?.let(m::launch) } ?: false

    override fun sendMedia(key: MediaKey): Boolean {
        val audio = context.getSystemService(AudioManager::class.java) ?: return false
        val code = when (key) {
            MediaKey.PlayPause -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            MediaKey.Next -> KeyEvent.KEYCODE_MEDIA_NEXT
            MediaKey.Previous -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
        }
        val now = SystemClock.uptimeMillis()
        audio.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, code, 0))
        audio.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, code, 0))
        return true
    }

    override fun toggleTorch(): Boolean {
        val id = torchId ?: return false
        return try {
            camera?.setTorchMode(id, !torchOn); true
        } catch (_: CameraAccessException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    override fun takeScreenshot() = Build.VERSION.SDK_INT >= 28 &&
        PulseAccessibilityService.performGlobal(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)

    override fun lockScreen() = Build.VERSION.SDK_INT >= 28 &&
        PulseAccessibilityService.performGlobal(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)

    /** Normal ↔ vibrate; silent needs Do Not Disturb access, which Intelia does not ask for. */
    override fun toggleSoundMode(): Boolean {
        val audio = context.getSystemService(AudioManager::class.java) ?: return false
        return try {
            audio.ringerMode = if (audio.ringerMode == AudioManager.RINGER_MODE_NORMAL) AudioManager.RINGER_MODE_VIBRATE
            else AudioManager.RINGER_MODE_NORMAL
            true
        } catch (_: SecurityException) {
            false
        }
    }

    override fun openPanel(kind: PanelKind): Boolean {
        val action = when (kind) {
            PanelKind.Notifications -> return PulseAccessibilityService.performGlobal(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
            PanelKind.QuickSettings -> return PulseAccessibilityService.performGlobal(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
            PanelKind.Internet -> if (Build.VERSION.SDK_INT >= 29) Settings.Panel.ACTION_INTERNET_CONNECTIVITY else Settings.ACTION_WIRELESS_SETTINGS
            PanelKind.Wifi -> if (Build.VERSION.SDK_INT >= 29) Settings.Panel.ACTION_WIFI else Settings.ACTION_WIFI_SETTINGS
            PanelKind.Bluetooth -> Settings.ACTION_BLUETOOTH_SETTINGS
            PanelKind.Sound -> if (Build.VERSION.SDK_INT >= 29) Settings.Panel.ACTION_VOLUME else Settings.ACTION_SOUND_SETTINGS
            PanelKind.Display -> Settings.ACTION_DISPLAY_SETTINGS
        }
        return try {
            context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    override fun openCard(id: String) = false
}

/** One host per Home; the torch listener lives exactly as long as it (no polling). */
@Composable
fun rememberActionHost(model: HomeViewModel, onSearch: () -> Unit): AndroidActionHost {
    val context = LocalContext.current
    val search by rememberUpdatedState(onSearch)
    val host = remember(model) { AndroidActionHost(context.applicationContext, model) { search(); true } }
    DisposableEffect(host) {
        val camera = context.getSystemService(CameraManager::class.java)
        camera?.registerTorchCallback(host.torchCallback, null)
        onDispose { camera?.unregisterTorchCallback(host.torchCallback) }
    }
    return host
}
