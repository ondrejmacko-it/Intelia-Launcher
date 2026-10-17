package dev.intelia.launcher.ui

import android.app.WallpaperManager
import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperExportTest {
    @Test fun exportIsPortraitAtScreenSize() {
        assertEquals(1080 to 2392, WallpaperExport.size(1080, 2392))
        assertEquals(1080 to 2392, WallpaperExport.size(2392, 1080))   // saved while rotated
    }

    @Test fun hugeScreensAreCappedKeepingTheShape() {
        val (w, h) = WallpaperExport.size(3000, 8192)
        assertEquals(WallpaperExport.MAX_SIDE, h)
        assertEquals(1500, w)
        assertEquals(1 to 1, WallpaperExport.size(0, -5))
    }

    @Test fun lockScreenOnlyWhenAsked() {
        assertEquals(WallpaperManager.FLAG_SYSTEM, WallpaperExport.flags(WallpaperExport.Target.Home))
        assertEquals(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK, WallpaperExport.flags(WallpaperExport.Target.HomeAndLock))
    }
}
