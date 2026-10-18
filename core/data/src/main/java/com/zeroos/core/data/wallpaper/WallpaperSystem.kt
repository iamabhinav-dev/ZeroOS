package com.zeroos.core.data.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.Build
import android.view.WindowManager
import com.zeroos.core.model.settings.WallpaperScreen
import java.io.IOException
import java.io.InputStream

/** What [PhoneWallpapers] needs from Android; [AndroidWallpaperSystem] in the app, a fake in tests. */
interface WallpaperSystem {
    /** File names in `assets/wallpapers/`. */
    fun files(): List<String>

    /** A small preview of [item], [width]×[height] pixels, centre-cropped. */
    fun thumbnail(item: WallpaperItem, width: Int, height: Int): Bitmap

    /**
     * Sets [item] on each of [screens], one at a time, and returns the screens Android accepted (F6.8).
     * Throws [IOException] if the image can't be read, [SecurityException] if setting isn't allowed.
     */
    fun set(item: WallpaperItem, screens: Set<WallpaperScreen>): Set<WallpaperScreen>
}

/**
 * [WallpaperSystem] backed by `WallpaperManager` (public API only, D-038). Images are decoded with a sample size
 * and centre-cropped to the real screen size, so they're never stretched and large files don't run out of memory.
 */
class AndroidWallpaperSystem(
    private val context: Context,
    private val list: (String) -> List<String> = { context.assets.list(it)?.toList().orEmpty() },
    private val open: (String) -> InputStream = context.assets::open
) : WallpaperSystem {

    override fun files(): List<String> = list(WallpaperCatalog.ASSET_DIR)

    override fun thumbnail(item: WallpaperItem, width: Int, height: Int): Bitmap = bitmap(item, width, height)

    override fun set(item: WallpaperItem, screens: Set<WallpaperScreen>): Set<WallpaperScreen> {
        val manager = WallpaperManager.getInstance(context)
        if (!manager.isWallpaperSupported || !manager.isSetWallpaperAllowed) return emptySet()
        val size = screenSize()
        val image = bitmap(item, size.x, size.y)
        // One screen at a time, home first, so a rejected lock screen is known (some phones, D-038).
        return WallpaperScreen.entries.filter { it in screens }.filter { screen ->
            manager.setBitmap(image, null, true, screen.flag()) != 0
        }.toSet()
    }

    private fun WallpaperScreen.flag() = when (this) {
        WallpaperScreen.HOME -> WallpaperManager.FLAG_SYSTEM
        WallpaperScreen.LOCK -> WallpaperManager.FLAG_LOCK
    }

    /** The phone's full screen size in pixels, portrait. */
    private fun screenSize(): Point {
        val windows = context.getSystemService(WindowManager::class.java)
        val point = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windows.maximumWindowMetrics.bounds.let { Point(it.width(), it.height()) }
        } else {
            @Suppress("DEPRECATION")
            Point().also { windows.defaultDisplay.getRealSize(it) }
        }
        return Point(minOf(point.x, point.y), maxOf(point.x, point.y))
    }

    private fun bitmap(item: WallpaperItem, width: Int, height: Int): Bitmap = when (val source = item.source) {
        is WallpaperSource.Solid -> Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565).apply { eraseColor(source.argb) }

        is WallpaperSource.Asset -> decode(source.path, width, height)

        is WallpaperSource.Dots -> DotArt.parse(open(source.path).use { it.readBytes() }).render(
            width,
            height,
            foreground = if (source.dark) WHITE else BLACK,
            background = if (source.dark) BLACK else WHITE
        )
    }

    private fun decode(path: String, width: Int, height: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open(path).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("not an image: $path")
        val options = BitmapFactory.Options().apply {
            inSampleSize = WallpaperFit.sampleSize(bounds.outWidth, bounds.outHeight, width, height)
        }
        val decoded = open(path).use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IOException("could not decode $path")
        val crop = WallpaperFit.centerCrop(decoded.width, decoded.height, width, height)
        val cropped = Bitmap.createBitmap(decoded, crop.left, crop.top, crop.width, crop.height)
        val scaled = Bitmap.createScaledBitmap(cropped, width, height, true)
        if (cropped !== decoded) decoded.recycle()
        if (scaled !== cropped) cropped.recycle()
        return scaled
    }
}

private const val BLACK = 0xFF000000.toInt()
private const val WHITE = 0xFFFFFFFF.toInt()
