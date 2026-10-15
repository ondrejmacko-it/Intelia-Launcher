package dev.intelia.launcher.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet

/** One small JSON document in private SharedPreferences; local only, no sync. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("intelia", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(SettingsCodec.decode(prefs.getString(KEY, null)))
    val state: StateFlow<LauncherSettings> = _state.asStateFlow()

    fun update(transform: (LauncherSettings) -> LauncherSettings) {
        val next = _state.updateAndGet { transform(it).sanitized() }
        prefs.edit().putString(KEY, SettingsCodec.encode(next)).apply()
    }

    private companion object {
        const val KEY = "settings.v1"
    }
}
