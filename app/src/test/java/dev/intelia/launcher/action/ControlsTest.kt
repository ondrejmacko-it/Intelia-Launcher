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

    @Test fun globalArcBindsOnlyToHeldPulseGestures() {
        val arc = Action.Arc("set2")
        val c = Controls().bind(Trigger.Pulse(Gesture.Hold), arc).bind(Trigger.Pulse(Gesture.DoubleTapHold), Action.Torch)
            .bind(Trigger.Pulse(Gesture.Tap), arc).bind(Trigger.Home(Gesture.DoubleTap), arc)
        assertEquals(arc, c.bound(Trigger.Pulse(Gesture.Hold)))
        assertEquals(Action.Torch, c.bound(Trigger.Pulse(Gesture.DoubleTapHold)))
        assertNull(c.bound(Trigger.Pulse(Gesture.Tap)))
        assertNull(c.bound(Trigger.Home(Gesture.DoubleTap)))
    }

    @Test fun sanitizedDropsGlobalArcOutsideHeldPulseAndInsideSlots() {
        val arc = Action.Arc()
        val c = Controls(arcSets = listOf(ArcSet("main", slots = listOf(arc, Action.Torch))),
            bindings = listOf(Binding(Trigger.Pulse(Gesture.DoubleTapHold), arc), Binding(Trigger.Lens(Gesture.SwipeLeft), arc),
                Binding(Trigger.Pulse(Gesture.Hold), Action.Arc(""))))
            .sanitized(emptySet())
        assertEquals(listOf(null, Action.Torch), c.arcSet("main")!!.slots)
        assertEquals(listOf(Binding(Trigger.Pulse(Gesture.DoubleTapHold), arc)), c.bindings)
    }

    @Test fun globalArcFallsBackToTheFirstSetWhenItsSetIsGone() {
        val c = Controls(arcSets = listOf(ArcSet("main"), ArcSet("set2")))
        assertEquals("set2", c.arcSetFor(Action.Arc("set2")).id)
        assertEquals("main", c.arcSetFor(Action.Arc("gone")).id)
        assertEquals(Controls.MAIN_SET, Controls().arcSetFor(Action.Arc("x")).id)
    }

    @Test fun globalArcSurvivesTheJsonRoundTrip() {
        val c = Controls().bind(Trigger.Pulse(Gesture.Hold), Action.Arc("set2"))
        assertEquals(c, Json.decodeFromString<Controls>(Json.encodeToString(c)))
    }

    @Test fun globalArcLookIsSeparateFromLensArc() {
        val c = Controls().withLook(ArcLook(slots = 7, radius = ArcLook.RADIUS_S, span = ArcLook.SPAN_S))
            .withGlobalLook(GlobalArcLook(radius = ArcLook.RADIUS_L, span = ArcLook.SPAN_L, haptics = false, dim = GlobalArcLook.DIM_L))
        val g = c.globalArc.over(c.arc)
        assertEquals(7, g.slots)
        assertEquals(ArcLook.RADIUS_L, g.radius)
        assertEquals(ArcLook.SPAN_L, g.span)
        assertEquals(false, g.haptics)
        assertEquals(ArcLook.RADIUS_S, c.arc.radius)
    }

    @Test fun globalArcLookIsSanitizedAndOldConfigsGetDefaults() {
        val wild = Controls(globalArc = GlobalArcLook(radius = 999, span = 1, dim = 100)).sanitized(emptySet()).globalArc
        assertEquals(GlobalArcLook(ArcLook.RADIUS_L, ArcLook.SPAN_S, true, GlobalArcLook.DIM_L), wild)
        val old = Json { ignoreUnknownKeys = true }.decodeFromString<Controls>("""{"arc":{"slots":6,"radius":104}}""")
        assertEquals(GlobalArcLook(), old.globalArc)
        assertEquals(6, old.arc.slots)
    }

    @Test fun homeSwipeUpAndHoldHaveDefaultsThatCanBeRebound() {
        val up = Trigger.Home(Gesture.SwipeUp)
        val hold = Trigger.Home(Gesture.Hold)
        val c = Controls()
        assertEquals(Action.Drawer, c.effective(up))
        assertEquals(Action.EditHome, c.effective(hold))
        assertNull(c.effective(Trigger.Home(Gesture.DoubleTap)))
        val torch = c.bind(up, Action.Torch)
        assertEquals(Action.Torch, torch.effective(up))
        // Unbinding brings the default back; binding the default itself stores nothing.
        assertEquals(Action.Drawer, torch.bind(up, null).effective(up))
        assertTrue(c.bind(hold, Action.EditHome).bindings.isEmpty())
        // Global ARC still only on held PULSE gestures.
        assertEquals(c, c.bind(hold, Action.Arc()))
        val json = Json.encodeToString(Controls.serializer(), torch.bind(hold, Action.Drawer))
        assertEquals(Action.Drawer, Json.decodeFromString(Controls.serializer(), json).sanitized(emptySet()).effective(hold))
    }
}
