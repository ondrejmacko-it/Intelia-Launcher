package dev.intelia.launcher.action

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ControlsTest {
    @Test fun migrationKeepsLegacyContentAndOrder() {
        val c = Controls.migrate(listOf("b/B", "a/A"), listOf("f2", "f1"), listOf("com.x/open", "bad", "com.y/a/b"))
        assertEquals(listOf(Action.App("b/B"), Action.App("a/A")), c.arcSet(Controls.MAIN_SET)!!.slots)
        assertEquals(listOf(Action.Flow("f2"), Action.Flow("f1")), c.arcSet(Controls.FLOW_SET)!!.slots)
        assertEquals(listOf(Action.Shortcut("com.x", "open"), Action.Shortcut("com.y", "a/b")), c.arcSet(Controls.SHORTCUT_SET)!!.slots)
        assertEquals(Action.Flow("f1"), c.bound(Trigger.ArcSlot(Controls.FLOW_SET, 1)))
        assertNull(c.bound(Trigger.ArcSlot(Controls.MAIN_SET, 9)))
        assertEquals(5, c.arc.slots)
    }

    @Test fun migrationFollowsTheRingCountAndSkipsEmptyRings() {
        val c = Controls.migrate(emptyList(), List(7) { "f$it" }, emptyList(), rings = 2)
        assertEquals(listOf(Controls.MAIN_SET, Controls.FLOW_SET), c.arcSets.map { it.id })
        assertEquals(7, c.arc.slots)
        assertEquals(listOf(Controls.MAIN_SET), Controls.migrate(listOf("a/A"), listOf("f"), listOf("p/s"), rings = 1).arcSets.map { it.id })
    }

    @Test fun editsKeepOtherSlotsInPlace() {
        var c = Controls.migrate(listOf("a/A", "b/B", "c/C"), emptyList(), emptyList())
        c = c.setSlot(Controls.MAIN_SET, 1, null)
        assertEquals(listOf(Action.App("a/A"), null, Action.App("c/C")), c.arcSet(Controls.MAIN_SET)!!.slots)
        c = c.toggleInArc(Action.Torch, on = true)
        assertEquals(Action.Torch, c.arcSet(Controls.MAIN_SET)!!.slots[1])
        c = c.moveSlot(Controls.MAIN_SET, 2, -1)
        assertEquals(listOf(Action.App("a/A"), Action.App("c/C"), Action.Torch), c.arcSet(Controls.MAIN_SET)!!.slots)
        c = c.toggleInArc(Action.App("c/C"), on = false)
        assertEquals(listOf(Action.App("a/A"), null, Action.Torch), c.arcSet(Controls.MAIN_SET)!!.slots)
        assertEquals(c, c.setSlot(Controls.MAIN_SET, 5, Action.Torch)) // beyond the visible slots
        assertEquals(c, c.moveSlot(Controls.MAIN_SET, 0, -1))
    }

    @Test fun setsAddAndRemoveButTheFirstStays() {
        val base = Controls()
        val two = checkNotNull(base.addSet())
        assertEquals(listOf(Controls.MAIN_SET, "set2"), two.arcSets.map { it.id })
        assertEquals(two, two.removeSet(Controls.MAIN_SET))
        assertEquals(listOf(Controls.MAIN_SET), two.removeSet("set2").arcSets.map { it.id })
        var many: Controls = base
        repeat(Controls.MAX_SETS - 1) { many = checkNotNull(many.addSet()) }
        assertNull(many.addSet())
        val fullArc = (0 until 5).fold(Controls()) { acc, i -> acc.toggleInArc(Action.App("p/C$i"), true) }
        assertTrue(fullArc.arcFull())
        assertEquals(fullArc, fullArc.toggleInArc(Action.Torch, true))
    }

    @Test fun lookIsClamped() {
        assertEquals(ArcLook(8, ArcLook.RADIUS_S, ArcLook.SPAN_L, false), ArcLook(99, 1, 999, false).sanitized())
        assertEquals(ArcLook(3), Controls(arc = ArcLook(0)).sanitized(emptySet()).arc)
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

    @Test fun pulseBindingsReplaceAndUnbind() {
        val tap = Trigger.Pulse(Gesture.Tap)
        val hold = Trigger.Pulse(Gesture.Hold)
        val c = Controls().bind(tap, Action.Torch).bind(hold, Action.Lock).bind(tap, Action.Media(MediaKey.PlayPause))
        assertEquals(Action.Media(MediaKey.PlayPause), c.bound(tap))
        assertEquals(Action.Lock, c.bound(hold))
        assertEquals(2, c.bindings.size)
        assertNull(c.bind(tap, null).bound(tap))
        assertNull(c.bound(Trigger.Pulse(Gesture.DoubleTap)))
        assertEquals(c, c.bind(Trigger.ArcSlot(Controls.MAIN_SET, 0), Action.Torch))
    }

    @Test fun onlySystemActionsRunOutsideHome() {
        assertEquals(true, Action.Torch.outsideHome && Action.Lock.outsideHome && Action.SystemPanel(PanelKind.Wifi).outsideHome)
        assertEquals(false, Action.App("a/A").outsideHome || Action.Flow("f").outsideHome || Action.Search.outsideHome ||
            Action.Shortcut("p", "s").outsideHome || Action.Card("c").outsideHome)
    }

    @Test fun keyBindingsNeedAVerifiedKey() {
        val torch = Binding(Trigger.Key(24, Gesture.DoubleTap), Action.Torch)
        val unverified = Controls(bindings = listOf(torch)).sanitized(emptySet())
        assertEquals(emptyList<Binding>(), unverified.bindings)
        val verified = Controls(bindings = listOf(torch, Binding(Trigger.Key(24, Gesture.SwipeUp), Action.Torch)), keys = listOf(24, 24, 4))
            .sanitized(emptySet())
        assertEquals(listOf(torch), verified.bindings)
        assertEquals(listOf(24), verified.keys)
        assertEquals(Controls(), Controls().withKey(4).withKey(26))
        val forgotten = verified.withoutKey(24)
        assertEquals(emptyList<Binding>(), forgotten.bindings)
        assertEquals(false, forgotten.keyBound(24))
    }

    @Test fun triggersOfFindsEveryPlace() {
        val c = Controls(arcSets = listOf(ArcSet("main", slots = listOf(null, Action.Torch))))
            .bind(Trigger.Home(Gesture.DoubleTap), Action.Torch)
        assertEquals(listOf(Trigger.Home(Gesture.DoubleTap), Trigger.ArcSlot("main", 1)), c.triggersOf(Action.Torch))
    }
}
