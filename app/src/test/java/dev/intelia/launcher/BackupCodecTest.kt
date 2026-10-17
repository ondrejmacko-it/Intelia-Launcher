package dev.intelia.launcher

import dev.intelia.launcher.data.BackupCodec
import dev.intelia.launcher.data.BackupError
import dev.intelia.launcher.data.BackupRead
import dev.intelia.launcher.data.CardFrame
import dev.intelia.launcher.data.LauncherSettings
import dev.intelia.launcher.data.PulseLens
import dev.intelia.launcher.flow.FlowAlias
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class BackupCodecTest {
    private val sample = LauncherSettings.fresh().copy(
        favorites = listOf("com.whatsapp/.Main", "com.google.android.keep/.Main"),
        hidden = setOf("com.example/.A"),
        aliases = listOf(FlowAlias("f1", "ráno", listOf("com.whatsapp/.Main"))),
        wallpaper = "aurora",
        iconSize = 48,
    ).sanitized()

    private fun failed(text: String?) = (BackupCodec.decode(text) as BackupRead.Failed).error

    @Test fun roundTripKeepsTheDocument() {
        val read = BackupCodec.decode(BackupCodec.encode(sample))
        assertEquals(BackupRead.Ok(sample), read)
    }

    @Test fun anythingElseIsNotABackup() {
        assertEquals(BackupError.NotABackup, failed("""{"theme":"Dark"}"""))
        assertEquals(BackupError.NotABackup, failed("""{"format":"other","version":1,"settings":{}}"""))
        assertEquals(BackupError.NotABackup, failed("[1,2]"))
        assertEquals(BackupError.Damaged, failed("not json at all"))
        assertEquals(BackupError.Damaged, failed("""{"format":"intelia-settings","version":1,"settings":"x"}"""))
    }

    @Test fun aNewerFileIsRefused() {
        assertEquals(BackupError.Newer, failed("""{"format":"intelia-settings","version":2,"settings":{}}"""))
        val schema = LauncherSettings.SCHEMA + 1
        assertEquals(BackupError.Newer, failed("""{"format":"intelia-settings","version":1,"settings":{"schema":$schema}}"""))
    }

    @Test fun oversizeIsRefused() {
        val big = ByteArray(BackupCodec.MAX_BYTES + 1) { ' '.code.toByte() }
        assertNull(BackupCodec.readBounded(ByteArrayInputStream(big)))
        assertEquals(BackupError.TooLarge, failed(null))
        assertEquals("{}", BackupCodec.readBounded(ByteArrayInputStream("{}".toByteArray())))
    }

    @Test fun valuesGoThroughSanitized() {
        val text = """{"format":"intelia-settings","version":1,"settings":{"schema":2,"iconSize":999,"arcRings":9,
            |"wallpaper":"nope","favorites":["","a/.B","a/.B"]}}""".trimMargin()
        val s = (BackupCodec.decode(text) as BackupRead.Ok).settings
        assertEquals(LauncherSettings.MAX_ICON, s.iconSize)
        assertEquals(3, s.arcRings)
        assertEquals("", s.wallpaper)
        assertEquals(listOf("a/.B"), s.favorites)
    }

    @Test fun deviceGeometryStaysFromThisPhone() {
        val here = LauncherSettings.fresh().copy(pulseLens = PulseLens(540f, 40f, 20f), editorCard = CardFrame(10f, 20f, 300f, 400f))
        val other = sample.copy(pulseLens = PulseLens(100f, 10f, 10f), editorCard = null, onboarded = false)
        val applied = BackupCodec.applyTo(here, other)
        assertEquals(here.pulseLens, applied.pulseLens)
        assertEquals(here.editorCard, applied.editorCard)
        assertTrue(applied.onboarded)
        assertEquals(sample.favorites, applied.favorites)
    }
}
