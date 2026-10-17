package dev.intelia.launcher.action

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ControlsTest {
    @Test fun migrationKeepsLegacyContentAndOrder() {
        val c = Controls.migrate(listOf("b/B", "a/A"), listOf("f2", "f1"), listOf("com.x/open", "bad", "com.y/a/b"))
        assertEquals(listOf(Action.App("b/B"), Action.App("a/A"), Action.Flow("f2"), Action.Flow("f1"),
            Action.Shortcut("com.x", "open"), Action.Shortcut("com.y", "a/b")), c.arcSet(Controls.MAIN_SET)!!.slots)
        assertEquals(Action.Flow("f2"), c.bound(Trigger.ArcSlot(Controls.MAIN_SET, 2)))
        assertNull(c.bound(Trigger.ArcSlot(Controls.MAIN_SET, 9)))
    }

    @Test fun sanitizeDropsDeletedFlowsAndDuplicates() {
        val raw = Controls(
            arcSets = listOf(ArcSet("main", slots = listOf(Action.Flow("gone"), Action.App(" "), Action.Torch)), ArcSet("main"), ArcSet("")),
            bindings = listOf(
                Binding(Trigger.Pulse(Gesture.Tap), Action.Flow("f1")),
                Binding(Trigger.Pulse(Gesture.Tap), Action.Torch),
                Binding(Trigger.Key(0, Gesture.Tap), Action.Torch),
                Binding(Trigger.ArcSlot("main", 0), Action.Torch),
            ),
        )
        val c = raw.sanitized(flowIds = setOf("f1"))
        assertEquals(listOf(ArcSet("main", slots = listOf(null, null, Action.Torch))), c.arcSets)
        assertEquals(listOf(Binding(Trigger.Pulse(Gesture.Tap), Action.Flow("f1"))), c.bindings)
    }

    @Test fun serialisesTriggersWithStableNames() {
        val c = Controls(listOf(ArcSet("main", "Práce", listOf(Action.Flow("f1"), null))),
            listOf(Binding(Trigger.Key(287, Gesture.DoubleTap), Action.Lock), Binding(Trigger.Home(Gesture.SwipeDown), Action.SystemPanel(PanelKind.Notifications))))
        val text = Json.encodeToString(Controls.serializer(), c)
        assertEquals(c, Json.decodeFromString(Controls.serializer(), text))
        assertEquals(true, text.contains(""""trigger":{"type":"key","code":287,"gesture":"DoubleTap"}"""))
    }
}
