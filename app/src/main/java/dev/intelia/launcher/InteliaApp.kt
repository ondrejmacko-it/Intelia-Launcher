package dev.intelia.launcher

import android.app.Application
import android.content.Context
import dev.intelia.launcher.data.AppRepository
import dev.intelia.launcher.data.IconCache
import dev.intelia.launcher.data.SettingsStore

class InteliaApp : Application() {
    val apps by lazy { AppRepository(this) }
    val icons by lazy { IconCache(this, apps) }
    val settings by lazy { SettingsStore(this) }
}

val Context.intelia: InteliaApp get() = applicationContext as InteliaApp
