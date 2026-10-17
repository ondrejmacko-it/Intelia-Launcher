package dev.intelia.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.intelia.launcher.ui.HomeRoot
import dev.intelia.launcher.ui.HomeViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    /** Incremented when Home is pressed while Intelia is already in front. */
    private val homeSignal = MutableStateFlow(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            val model: HomeViewModel = viewModel()
            val signal = homeSignal.collectAsStateWithLifecycle()
            HomeRoot(model, signal.value)
        }
    }

    /** The repository loads once when it is created; only later returns to Home re-read the list. */
    private var started = false

    override fun onStart() {
        super.onStart()
        val apps = intelia.apps
        if (started) apps.refresh()
        started = true
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) homeSignal.value++
    }
}
