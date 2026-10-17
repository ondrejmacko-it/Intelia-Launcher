package dev.intelia.launcher.action

import dev.intelia.launcher.flow.FlowAlias
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeHost(
    val apps: Set<String> = setOf("a/A", "b/B"),
    val flows: List<FlowAlias> = listOf(FlowAlias("1", "Vlak", listOf("a/A", "b/B"))),
    var home: Boolean = true,
    val shortcuts: Set<Pair<String, String>> = setOf("a" to "s1"),
    var torch: Boolean = true,
    var pulse: Boolean = false,
    var succeed: Boolean = true,
) : ActionHost {
    val calls = mutableListOf<String>()
    override fun hasApp(component: String) = component in apps
    override fun findFlow(id: String) = flows.firstOrNull { it.id == id }
    override fun isDefaultHome() = home
    override fun hasShortcut(pkg: String, id: String) = (pkg to id) in shortcuts
    override fun hasTorch() = torch
    override fun hasPulseService() = pulse
    override fun hasCard(id: String) = id == "media"
    private fun call(name: String): Boolean { calls += name; return succeed }
    override fun launchApp(component: String) = call("launch $component")
    override fun runFlow(flow: FlowAlias) = call("flow ${flow.id}")
    override fun openSearch() = call("search")
    override fun startShortcut(pkg: String, id: String) = call("shortcut $pkg/$id")
    override fun sendMedia(key: MediaKey) = call("media $key")
    override fun toggleTorch() = call("torch")
    override fun takeScreenshot() = call("screenshot")
    override fun toggleSoundMode() = call("sound")
    override fun openPanel(kind: PanelKind) = call("panel $kind")
    override fun openCard(id: String) = call("card $id")
    override fun lockScreen() = call("lock")
}

class ActionTest {
    @Test fun appOpensOrExplainsWhyNot() {
        val host = FakeHost()
        assertEquals(ActionResult.Opened, Action.App("a/A").run(host))
        assertEquals(ActionResult.Unavailable(Reason.AppMissing), Action.App("gone/G").run(host))
        assertEquals(ActionResult.Unavailable(Reason.Invalid), Action.App(" ").run(host))
        assertEquals(ActionResult.Unavailable(Reason.Invalid), Action.App("a/\u0000").run(host))
        assertEquals(listOf("launch a/A"), host.calls)
    }

    @Test fun flowMatchesExactNameOnlyAndNeedsAllTargets() {
        val host = FakeHost()
        assertEquals(ActionResult.Opened, Action.Flow("1").run(host))
        assertEquals(ActionResult.Unavailable(Reason.FlowMissing), Action.Flow("2").run(host))
        assertEquals(ActionResult.Unavailable(Reason.FlowMissing), Action.Flow("").run(host))
        val broken = FakeHost(apps = setOf("a/A"))
        assertEquals(Reason.AppMissing, Action.Flow("1").unavailableReason(broken))
        assertEquals(listOf("flow 1"), host.calls)
    }

    @Test fun shortcutsNeedDefaultHome() {
        val host = FakeHost(home = false)
        assertEquals(Reason.NotDefaultHome, Action.Shortcut("a", "s1").unavailableReason(host))
        host.home = true
        assertTrue(Action.Shortcut("a", "s1").isAvailable(host))
        assertEquals(Reason.ShortcutMissing, Action.Shortcut("a", "nope").unavailableReason(host))
    }

    @Test fun systemActionsReportDoneOnlyOnSuccess() {
        val host = FakeHost()
        assertEquals(ActionResult.Done, Action.Torch.run(host))
        assertEquals(ActionResult.Done, Action.Media(MediaKey.Next).run(host))
        assertEquals(ActionResult.Opened, Action.SystemPanel(PanelKind.Wifi).run(host))
        host.succeed = false
        assertEquals(ActionResult.Unavailable(Reason.Failed), Action.SoundMode.run(host))
        assertEquals(ActionResult.Unavailable(Reason.Failed), Action.Search.run(host))
    }

    @Test fun unavailableActionsNeverReachTheHost() {
        val host = FakeHost(torch = false)
        assertEquals(ActionResult.Unavailable(Reason.Unsupported), Action.Torch.run(host))
        assertEquals(ActionResult.Unavailable(Reason.NeedsPulseService), Action.Screenshot.run(host))
        assertEquals(ActionResult.Unavailable(Reason.CardMissing), Action.Card("x").run(host))
        assertFalse(Action.Screenshot.isAvailable(host))
        assertTrue(host.calls.isEmpty())
        host.pulse = true
        assertNull(Action.Screenshot.unavailableReason(host))
    }

    @Test fun serialisesWithStableNames() {
        val json = Json
        val actions = listOf(Action.App("a/A"), Action.Flow("1"), Action.Lock, Action.Search, Action.Media(MediaKey.PlayPause),
            Action.SystemPanel(PanelKind.Bluetooth), Action.Card("media"))
        actions.forEach { assertEquals(it, json.decodeFromString(Action.serializer(), json.encodeToString(Action.serializer(), it))) }
        assertEquals("""{"type":"app","component":"a/A"}""", json.encodeToString(Action.serializer(), Action.App("a/A")))
    }
}
