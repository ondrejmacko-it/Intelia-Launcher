package dev.intelia.launcher

import dev.intelia.launcher.data.HomeLayout
import dev.intelia.launcher.data.HomeLayouts
import dev.intelia.launcher.data.HomeTemplates
import dev.intelia.launcher.data.ItemKind
import dev.intelia.launcher.data.LauncherSettings
import dev.intelia.launcher.data.ThemeMode
import dev.intelia.launcher.data.TypeFace
import dev.intelia.launcher.onboarding.DraftCodec
import dev.intelia.launcher.onboarding.GuideStep
import dev.intelia.launcher.onboarding.HomeChoice
import dev.intelia.launcher.onboarding.OnboardingDraft
import dev.intelia.launcher.onboarding.PulsePick
import dev.intelia.launcher.onboarding.Tried
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingDraftTest {
    private val apps = listOf("a/A", "b/B", "c/C", "d/D", "e/E")
    private val fresh = LauncherSettings.fresh()

    private fun apps(s: LauncherSettings) = s.home.activeLayout().items.filter { it.kind == ItemKind.App }.map { it.target }

    @Test fun firstRunStartsOnTheSignatureHome() {
        val d = OnboardingDraft.firstRun(fresh, apps)
        assertEquals(d.start, d.settings)
        assertEquals(HomeLayouts.USER_ID, d.settings.home.active)
        assertEquals(apps.take(4), apps(d.settings))
        assertFalse(d.settings.onboarded)
        assertFalse(d.rerun)
    }

    @Test fun stepsMoveBothWaysAndStopAtTheEnds() {
        var d = OnboardingDraft.firstRun(fresh, apps)
        assertTrue(d.first)
        assertEquals(d, d.back().copy(step = d.step))
        GuideStep.entries.drop(1).forEach { d = d.next(); assertEquals(it, d.step) }
        assertTrue(d.last)
        assertEquals(GuideStep.Done, d.next().step)
        assertEquals(GuideStep.Access, d.back().step)
        assertEquals(GuideStep.Look, d.goTo(GuideStep.Look).step)
    }

    @Test fun backKeepsTheChoicesMade() {
        val d = OnboardingDraft.firstRun(fresh, apps).next().withHome(HomeChoice.Empty, apps).next()
            .edit { it.copy(theme = ThemeMode.Light) }
        val back = d.back().back()
        assertEquals(GuideStep.Welcome, back.step)
        assertEquals(ThemeMode.Light, back.settings.theme)
        assertEquals(HomeChoice.Empty, back.home)
    }

    @Test fun homeChoicesRebuildTheStage() {
        val d = OnboardingDraft.firstRun(fresh, apps)
        val empty = d.withHome(HomeChoice.Empty, apps)
        assertTrue(apps(empty.settings).isEmpty())
        assertEquals(HomeLayout.Pure, empty.settings.layout)
        assertTrue(empty.settings.home.activeLayout().items.any { it.kind == ItemKind.Clock })
        val basic = empty.withHome(HomeChoice.Basic, apps)
        assertEquals(apps.take(4), apps(basic.settings))
        assertEquals(HomeLayout.Essentials, basic.settings.layout)
    }

    @Test fun rerunNeverReplacesTheOwnHome() {
        val own = HomeTemplates.signature(listOf("x/X")).copy(name = "Moje")
        val stored = fresh.copy(onboarded = true, home = HomeLayouts(HomeLayouts.USER_ID, fresh.home.layouts + own)).sanitized()
        val d = OnboardingDraft.rerun(stored)
        assertEquals(stored, d.settings)
        val basic = d.withHome(HomeChoice.Basic, apps)
        assertEquals(OnboardingDraft.GUIDE_ID, basic.settings.home.active)
        assertEquals("Moje", basic.settings.home.layouts.first { it.id == HomeLayouts.USER_ID }.name)
        val custom = basic.withHome(HomeChoice.Custom, apps)
        assertEquals(HomeLayouts.USER_ID, custom.settings.home.active)
        assertEquals(listOf("x/X"), apps(custom.settings))
    }

    @Test fun clockFaceSurvivesAHomeSwitch() {
        val d = OnboardingDraft.firstRun(fresh, apps).withClockFace(TypeFace.BodoniModa)
        assertEquals(TypeFace.BodoniModa, d.clockFace())
        assertEquals(TypeFace.BodoniModa, d.withHome(HomeChoice.Empty, apps).clockFace())
    }

    @Test fun lensAppsRebuildBasic() {
        val d = OnboardingDraft.firstRun(fresh, apps).withHome(HomeChoice.Basic, apps).withLensApps(listOf("e/E", "a/A"), apps)
        assertEquals(listOf("e/E", "a/A"), apps(d.settings).take(2))
    }

    @Test fun pulseChoices() {
        val d = OnboardingDraft.firstRun(fresh, apps)
        assertEquals(PulsePick.Everywhere, d.pulse)
        assertEquals(PulsePick.HomeOnly, d.withPulse(PulsePick.HomeOnly).pulse)
        val off = d.withPulse(PulsePick.Off)
        assertFalse(off.settings.pulse)
        assertEquals(PulsePick.Everywhere, off.withPulse(PulsePick.Everywhere).pulse)
    }

    @Test fun commitIsSanitizedAndOnboarded() {
        val d = OnboardingDraft.firstRun(fresh, apps).edit { it.copy(theme = ThemeMode.Dark) }
        val c = d.commit(musicAllowed = false)
        assertTrue(c.onboarded)
        assertEquals(ThemeMode.Dark, c.theme)
        assertEquals(c, c.sanitized())
        assertFalse(c.pulseMusic)
        assertTrue(d.copy(wantsMusic = true).commit(musicAllowed = true).pulseMusic)
        assertFalse(d.copy(wantsMusic = true).commit(musicAllowed = false).pulseMusic)
    }

    @Test fun draftLeavesTheStartAlone() {
        val d = OnboardingDraft.firstRun(fresh, apps)
        val edited = d.edit { it.copy(theme = ThemeMode.Light) }.withHome(HomeChoice.Empty, apps)
        assertEquals(d.start, edited.start)
        assertNotEquals(edited.start, edited.settings)
    }

    @Test fun roundTripSurvivesProcessDeath() {
        val d = OnboardingDraft.firstRun(fresh, apps).next().withHome(HomeChoice.Empty, apps).tried(Tried.Tap)
            .copy(wantsMusic = true)
        assertEquals(d, DraftCodec.decode(DraftCodec.encode(d)))
        assertNull(DraftCodec.decode("{broken"))
        assertNull(DraftCodec.decode(""))
    }
}
