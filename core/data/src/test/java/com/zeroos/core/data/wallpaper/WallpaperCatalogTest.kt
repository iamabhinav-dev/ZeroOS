package com.zeroos.core.data.wallpaper

import com.zeroos.core.model.settings.PhoneWallpaperHistory
import com.zeroos.core.model.settings.WallpaperScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WallpaperCatalogTest {
    private val files = listOf("rose_dark.webp", "rose_light.webp", "koi_dark.webp", "Bad Name.webp", "leaf_dark.png", "notes.txt")

    @Test
    fun f6_8_galleryShowsOnlyTheCurrentThemeSolidFirst() {
        assertEquals(listOf("pure_black", "koi_dark", "rose_dark"), WallpaperCatalog.forTone(files, WallpaperTone.DARK).map { it.id })
        assertEquals(listOf("pure_white", "rose_light"), WallpaperCatalog.forTone(files, WallpaperTone.LIGHT).map { it.id })
    }

    @Test
    fun f6_8_withoutImagesOnlyTheSolidsRemain() {
        assertEquals(listOf("pure_black"), WallpaperCatalog.forTone(emptyList(), WallpaperTone.DARK).map { it.id })
        assertEquals(listOf("pure_white"), WallpaperCatalog.forTone(emptyList(), WallpaperTone.LIGHT).map { it.id })
    }

    @Test
    fun f6_8_imagesAreNamedByTheirFileAndPointAtTheirAsset() {
        val rose = WallpaperCatalog.find(files, "rose_light")!!
        assertEquals("rose", rose.name)
        assertEquals(WallpaperSource.Asset("wallpapers/rose_light.webp"), rose.source)
        assertNull(WallpaperCatalog.find(files, "leaf_dark"))
    }

    @Test
    fun f6_8_historyKeepsCurrentAndPreviousPerScreen() {
        val history = PhoneWallpaperHistory()
            .withSet(WallpaperScreen.HOME, "rose_dark")
            .withSet(WallpaperScreen.HOME, "koi_dark")
            .withSet(WallpaperScreen.HOME, "leaf_dark")
            .withSet(WallpaperScreen.LOCK, "pure_black")
        assertEquals("leaf_dark", history.current(WallpaperScreen.HOME))
        assertEquals("koi_dark", history.previous(WallpaperScreen.HOME))
        assertEquals(2, history.home.size)
        assertEquals("pure_black", history.current(WallpaperScreen.LOCK))
        assertNull(history.previous(WallpaperScreen.LOCK))
        // Restoring brings the previous one back to current and keeps the other as previous.
        val restored = history.withSet(WallpaperScreen.HOME, "koi_dark")
        assertEquals(listOf("koi_dark", "leaf_dark"), restored.home)
    }
}
