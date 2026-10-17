package com.zeroos.core.data.wallpaper

import kotlin.math.roundToInt

/** A region of the source image, in source pixels. */
data class CropRegion(val left: Int, val top: Int, val width: Int, val height: Int)

/**
 * Image fitting for wallpapers (F6.8): images are scaled uniformly and centre-cropped to the screen, never
 * stretched, and decoded at a reduced size first so large files can't run out of memory.
 */
object WallpaperFit {
    /** Largest power-of-two `inSampleSize` that still leaves the image at least as big as the screen. */
    fun sampleSize(srcWidth: Int, srcHeight: Int, screenWidth: Int, screenHeight: Int): Int {
        var size = 1
        while (srcWidth / (size * 2) >= screenWidth && srcHeight / (size * 2) >= screenHeight) size *= 2
        return size
    }

    /** The centred part of a [srcWidth]×[srcHeight] image that has the screen's shape. */
    fun centerCrop(srcWidth: Int, srcHeight: Int, screenWidth: Int, screenHeight: Int): CropRegion {
        val screenRatio = screenWidth.toDouble() / screenHeight
        return if (srcWidth.toDouble() / srcHeight > screenRatio) {
            // Wider than the screen: cut the sides.
            val width = (srcHeight * screenRatio).roundToInt().coerceAtMost(srcWidth)
            CropRegion((srcWidth - width) / 2, 0, width, srcHeight)
        } else {
            // Taller (or equal): cut top and bottom.
            val height = (srcWidth / screenRatio).roundToInt().coerceAtMost(srcHeight)
            CropRegion(0, (srcHeight - height) / 2, srcWidth, height)
        }
    }
}
