package dev.intelia.launcher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import dev.intelia.launcher.action.Action
import dev.intelia.launcher.action.Controls
import dev.intelia.launcher.action.PanelKind
import dev.intelia.launcher.action.QuickAction
import dev.intelia.launcher.data.PulseHit

class PulseMenuTest {
    @Test fun quickActionsDefaultToThreeEverywhereItems() {
        val shown = quickEntries(Controls(), emptyList(), emptyList(), service = true).map { it.slot.action }
        assertEquals(listOf(Action.Torch, Action.Alarms, Action.SystemPanel(PanelKind.Notifications)), shown)
        // Without the PULSE service the shade cannot open, so it is left out instead of drawn as a dead button.
        assertEquals(listOf(Action.Torch, Action.Alarms), quickEntries(Controls(), emptyList(), emptyList(), service = false).map { it.slot.action })
    }

    @Test fun quickActionsKeepOrderSkipHiddenAndMissing() {
        val c = Controls().withQuick(listOf(QuickAction(Action.Alarms, glyph = "Torch"), QuickAction(Action.Torch, hidden = true),
            QuickAction(Action.App("gone.pkg/.Main")), QuickAction(Action.Screenshot), QuickAction(Action.Lock), QuickAction(Action.SoundMode),
            QuickAction(Action.Search)))
        val shown = quickEntries(c, emptyList(), emptyList(), service = true)
        assertEquals(listOf(Action.Alarms, Action.Screenshot, Action.Lock, Action.SoundMode), shown.map { it.slot.action })
        assertEquals(Glyph.Torch, shown.first().glyph)
        assertTrue(quickEntries(c, emptyList(), emptyList(), service = false).none { it.slot.action == Action.Screenshot })
    }

    @Test fun hitboxIs48dpCentredOnPhone3Lens() {
        // Phone (3): lens 205.5 / 26 dp, screen 411 dp wide.
        val t = pulseHitbox(205.5f, 26f, PulseHit(), 411f)
        assertEquals(48f, t.width, 0f)
        assertEquals(48f, t.height, 0f)
        assertEquals(181.5f, t.left, 0.001f)
        assertEquals(2f, t.top, 0.001f)
        assertTrue(t.contains(205.5f, 26f))
    }

    @Test fun hitboxIgnoresTheCapsuleAndFollowsOnlyItsOwnOffset() {
        val moved = pulseHitbox(205.5f, 26f, PulseHit(56f, dx = 10f, dy = 12f), 411f)
        assertEquals(56f, moved.width, 0f)
        assertEquals(187.5f, moved.left, 0.001f)
        assertEquals(10f, moved.top, 0.001f)
    }

    @Test fun hitboxStaysOnScreen() {
        val t = pulseHitbox(10f, 4f, PulseHit(), 411f)
        assertEquals(0f, t.left, 0f)
        assertEquals(0f, t.top, 0f)
        assertEquals(363f, pulseHitbox(405f, 26f, PulseHit(), 411f).left, 0.001f)
    }

    @Test fun homeHitboxReachesBelowTheStatusBar() {
        // On Home the status bar (40 dp) takes touches, so the target keeps 24 dp under it.
        val t = pulseHitbox(205.5f, 26f, PulseHit(), 411f, blockedTop = 40f)
        assertEquals(64f, t.bottom, 0.001f)
        assertEquals(48f, t.width, 0f)
    }

    @Test fun hitSettingsAreBounded() {
        val s = PulseHit(Float.NaN, 500f, -500f).sanitized()
        assertEquals(PulseHit.DEFAULT_SIZE, s.size, 0f)
        assertEquals(PulseHit.MAX_DX, s.dx, 0f)
        assertEquals(PulseHit.MIN_DY, s.dy, 0f)
    }

    @Test fun menuIsCentredAndClamped() {
        assertEquals(66f to 948f, menuLeftAndWidth(1080f, 540f, 32f, 948f))
        val (left, width) = menuLeftAndWidth(1080f, 540f, 32f, 600f)
        assertEquals(240f, left, 0.01f)
        assertEquals(600f, width, 0.01f)
        // Lens near the left edge: the menu stays on screen.
        assertEquals(32f, menuLeftAndWidth(1080f, 80f, 32f, 600f).first, 0.01f)
        assertEquals(448f, menuLeftAndWidth(1080f, 1060f, 32f, 600f).first, 0.01f)
    }

    @Test fun menuStartsBelowStatusBarAndLens() {
        // Lens ring (68 + 33 + 11) is lower than a 110 px status bar here.
        assertEquals(136f, menuTop(110f, 68f, 33f, 11f, 24f), 0.01f)
        // A lens hanging lower than the status bar pushes the sheet down.
        assertEquals(154f, menuTop(80f, 80f, 40f, 10f, 24f), 0.01f)
    }
}

class MusicCapsuleTest {
    @org.junit.Test fun barsLeftArtRightLensKept() {
        val rest = restCapsule(200f, 14f, 9f, 32f, mark = false)
        val m = musicCapsule(200f, 14f, 9f, 32f)
        org.junit.Assert.assertEquals(rest.height, m.height, 0f)
        org.junit.Assert.assertEquals(rest.top, m.top, 0f)
        org.junit.Assert.assertEquals(200f, m.left + m.lensX, 0.001f)
        org.junit.Assert.assertEquals(m.left + m.width - m.height / 2f, m.left + m.artX, 0.001f)
        org.junit.Assert.assertTrue(m.top >= 0f && m.top + m.height <= 32f)
    }
}
