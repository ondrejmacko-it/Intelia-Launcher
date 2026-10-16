package dev.intelia.launcher.data

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.provider.MediaStore
import dev.intelia.launcher.search.AppSearch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.concurrent.ConcurrentHashMap

data class AppEntry(
    val key: String,
    val label: String,
    val component: ComponentName,
    val user: UserHandle,
    val mainProfile: Boolean,
    val suspended: Boolean,
) {
    val searchKey: String = AppSearch.fold(label)
}

data class ShortcutEntry(val key: String, val label: String, val packageName: String, val id: String, val rank: Int)

/** Launchable activities from public LauncherApps, refreshed on package and profile events. */
class AppRepository(private val context: Context) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val infos = ConcurrentHashMap<String, LauncherActivityInfo>()
    private val _apps = MutableStateFlow<List<AppEntry>?>(null)
    val apps: StateFlow<List<AppEntry>?> = _apps.asStateFlow()
    private val _changed = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val changedPackages: SharedFlow<String> = _changed
    private var loading: Job? = null

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = changed(packageName)
        override fun onPackageAdded(packageName: String, user: UserHandle) = changed(packageName)
        override fun onPackageChanged(packageName: String, user: UserHandle) = changed(packageName)
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
            packageNames.forEach(::changed)
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
            packageNames.forEach(::changed)
        override fun onPackagesSuspended(packageNames: Array<out String>, user: UserHandle) = packageNames.forEach(::changed)
        override fun onPackagesUnsuspended(packageNames: Array<out String>, user: UserHandle) = packageNames.forEach(::changed)
    }

    init {
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        refresh()
    }

    private fun changed(packageName: String) {
        _changed.tryEmit(packageName)
        refresh()
    }

    /** Also called when Home starts, which covers profile lock/unlock without extra receivers. */
    fun refresh() {
        loading?.cancel()
        loading = scope.launch { _apps.value = load() }
    }

    private fun load(): List<AppEntry> {
        val main = Process.myUserHandle()
        val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }
        val next = HashMap<String, LauncherActivityInfo>()
        val entries = userManager.userProfiles.flatMap { user ->
            if (user != main && userManager.isQuietModeEnabled(user)) return@flatMap emptyList()
            val serial = userManager.getSerialNumberForUser(user)
            launcherApps.getActivityList(null, user).mapNotNull { info ->
                val component = info.componentName
                if (component.packageName == context.packageName) return@mapNotNull null
                val base = "${component.packageName}/${component.className}"
                val key = if (user == main) base else "$base@$serial"
                next[key] = info
                AppEntry(
                    key = key,
                    label = info.label?.toString()?.trim().orEmpty().ifEmpty { component.packageName },
                    component = component,
                    user = user,
                    mainProfile = user == main,
                    suspended = info.applicationInfo.flags and ApplicationInfo.FLAG_SUSPENDED != 0,
                )
            }
        }.sortedWith(compareBy(collator) { it.label })
        infos.keys.retainAll(next.keys)
        infos.putAll(next)
        return entries
    }

    /** Default dialer, messages, camera and browser packages, for the Essentials layout. Local lookup only. */
    fun essentialPackages(): List<String> = listOf(
        Intent(Intent.ACTION_DIAL),
        Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING),
        Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
        Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER),
    ).mapNotNull { intent ->
        context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
            ?.takeIf { it != "android" }
    }.distinct()

    fun icon(entry: AppEntry, densityDpi: Int): Drawable? {
        val icon = infos[entry.key]?.getIcon(densityDpi) ?: return null
        return if (entry.mainProfile) icon else context.packageManager.getUserBadgedIcon(icon, entry.user)
    }

    fun launch(entry: AppEntry, bounds: Rect? = null, options: Bundle? = null): Boolean = try {
        launcherApps.startMainActivity(entry.component, entry.user, bounds, options)
        true
    } catch (_: SecurityException) {
        false
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: IllegalStateException) {
        false
    }

    /** App shortcuts are only visible to the default home app; false otherwise. */
    fun canUseShortcuts(): Boolean = try {
        launcherApps.hasShortcutHostPermission()
    } catch (_: SecurityException) {
        false
    } catch (_: IllegalStateException) {
        false
    }

    private val shortcutInfos = ConcurrentHashMap<String, ShortcutInfo>()

    /** Static, dynamic and pinned shortcuts of main-profile apps, sorted by app then rank. */
    fun shortcuts(): List<ShortcutEntry> {
        if (!canUseShortcuts()) return emptyList()
        val query = LauncherApps.ShortcutQuery().setQueryFlags(
            LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
        )
        val infos = try {
            launcherApps.getShortcuts(query, Process.myUserHandle()).orEmpty()
        } catch (_: SecurityException) {
            emptyList()
        } catch (_: IllegalStateException) {
            emptyList()
        }
        return infos.filter { it.isEnabled && it.`package` != context.packageName }.map { info ->
            val key = "${info.`package`}/${info.id}"
            shortcutInfos[key] = info
            ShortcutEntry(key, (info.shortLabel ?: info.longLabel ?: info.id).toString().trim(), info.`package`, info.id, info.rank)
        }.sortedWith(compareBy({ it.packageName }, { it.rank }))
    }

    fun shortcutIcon(key: String, densityDpi: Int): Drawable? = shortcutInfos[key]?.let {
        try {
            launcherApps.getShortcutIconDrawable(it, densityDpi)
        } catch (_: SecurityException) {
            null
        } catch (_: IllegalStateException) {
            null
        }
    }

    fun startShortcut(entry: ShortcutEntry): Boolean = try {
        launcherApps.startShortcut(entry.packageName, entry.id, null, null, Process.myUserHandle())
        true
    } catch (_: SecurityException) {
        false
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: IllegalStateException) {
        false
    }

    fun openDetails(entry: AppEntry): Boolean = try {
        launcherApps.startAppDetailsActivity(entry.component, entry.user, null, null)
        true
    } catch (_: SecurityException) {
        false
    } catch (_: ActivityNotFoundException) {
        false
    }
}
