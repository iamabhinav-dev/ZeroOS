package com.zeroos.core.data.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zeroos.core.model.settings.WallpaperScreen
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class AndroidWallpaperSystemTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    /** A 300×1000 test image: white top half, black bottom half. */
    private val png: ByteArray = ByteArrayOutputStream().also { out ->
        Bitmap.createBitmap(300, 1000, Bitmap.Config.ARGB_8888).apply {
            for (y in 0 until 1000) for (x in 0 until 300) setPixel(x, y, if (y < 500) Color.WHITE else Color.BLACK)
        }.compress(Bitmap.CompressFormat.PNG, 100, out)
    }.toByteArray()

    private val system = AndroidWallpaperSystem(context, list = { listOf("test_dark.webp") }, open = { ByteArrayInputStream(png) })
    private val image = WallpaperCatalog.find(listOf("test_dark.webp"), "test_dark")!!

    @Test
    fun f6_8_listsTheWallpaperAssets() {
        assertEquals(listOf("test_dark.webp"), system.files())
    }

    @Test
    fun f6_8_thumbnailHasTheAskedSizeWithoutStretching() {
        val thumb = system.thumbnail(image, 90, 200)
        assertEquals(90, thumb.width)
        assertEquals(200, thumb.height)
        // Centre-cropped, not squashed: the white/black edge stays in the middle.
        assertEquals(Color.WHITE, thumb.getPixel(45, 60))
        assertEquals(Color.BLACK, thumb.getPixel(45, 140))
    }

    @Test
    fun f6_8_setsOnlyTheChosenScreens() {
        val accepted = system.set(image, setOf(WallpaperScreen.LOCK))
        assertEquals(setOf(WallpaperScreen.LOCK), accepted)
        val shadow = shadowOf(WallpaperManager.getInstance(context))
        assertTrue(shadow.getBitmap(WallpaperManager.FLAG_LOCK) != null)
        assertEquals(null, shadow.getBitmap(WallpaperManager.FLAG_SYSTEM))
    }

    @Test
    fun f6_8_solidWallpaperFillsTheScreen() {
        val black = WallpaperCatalog.find(emptyList(), WallpaperCatalog.PURE_BLACK)!!
        assertEquals(setOf(WallpaperScreen.HOME), system.set(black, setOf(WallpaperScreen.HOME)))
        val set = shadowOf(WallpaperManager.getInstance(context)).getBitmap(WallpaperManager.FLAG_SYSTEM)!!
        assertEquals(Color.BLACK, set.getPixel(set.width / 2, set.height / 2))
    }
}
