package com.zeroos.core.data.wallpaper

import android.content.Context
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zeroos.core.model.settings.WallpaperScreen
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class DotArtTest {
    /** A .dots file for a [width]×[height] source with the given dots (x, y, radius in pixels). */
    private fun file(width: Int, height: Int, vararg dots: Triple<Float, Float, Float>): ByteArray =
        ByteBuffer.allocate(14 + dots.size * 6).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("ZDOT".toByteArray())
            put(1)
            put(0)
            putShort(width.toShort())
            putShort(height.toShort())
            putInt(dots.size)
            dots.forEach { (x, y, r) ->
                putShort((x * 8).toInt().toShort())
                putShort((y * 8).toInt().toShort())
                putShort((r * 8).toInt().toShort())
            }
        }.array()

    @Test
    fun f6_8_readsCircles() {
        val art = DotArt.parse(file(90, 200, Triple(45f, 100f, 10f), Triple(10f, 10f, 2.5f)))
        assertEquals(90, art.width)
        assertEquals(200, art.height)
        assertEquals(2, art.count)
    }

    @Test
    fun f6_8_rejectsFilesThatArentDots() {
        assertThrows(IOException::class.java) { DotArt.parse("PNG....".toByteArray()) }
        assertThrows(IOException::class.java) { DotArt.parse(file(90, 200, Triple(1f, 1f, 1f)).copyOf(16)) }
    }

    @Test
    fun f6_8_drawsSharpAtAnySizeInTheRightPlace() {
        // One dot in the middle of a 90×200 source, drawn at three times the size.
        val art = DotArt.parse(file(90, 200, Triple(45f, 100f, 10f)))
        val bitmap = art.render(270, 600, Color.WHITE, Color.BLACK)
        assertEquals(Color.WHITE, bitmap.getPixel(135, 300))
        assertEquals(Color.WHITE, bitmap.getPixel(135 + 25, 300)) // radius 10 → 30 px
        assertEquals(Color.BLACK, bitmap.getPixel(135 + 35, 300))
        assertEquals(Color.BLACK, bitmap.getPixel(5, 5))
    }

    @Test
    fun f6_8_lightVersionsAreBlackOnWhite() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val bytes = file(90, 200, Triple(45f, 100f, 10f))
        val system = AndroidWallpaperSystem(context, list = { listOf("dot_light.dots") }, open = { ByteArrayInputStream(bytes) })
        val item = WallpaperCatalog.find(system.files(), "dot_light")!!
        assertEquals(WallpaperSource.Dots("wallpapers/dot_light.dots", dark = false), item.source)
        val thumb = system.thumbnail(item, 90, 200)
        assertEquals(Color.BLACK, thumb.getPixel(45, 100))
        assertEquals(Color.WHITE, thumb.getPixel(5, 5))
        assertEquals(setOf(WallpaperScreen.HOME), system.set(item, setOf(WallpaperScreen.HOME)))
    }
}
