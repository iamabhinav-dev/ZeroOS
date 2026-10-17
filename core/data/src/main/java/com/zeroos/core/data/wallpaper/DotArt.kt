package com.zeroos.core.data.wallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A wallpaper stored as circles (F6.8): the dots found in a generated image by `scripts/add-wallpapers.py
 * --format dots`. Drawn fresh at the phone's exact resolution, so the dots stay sharp at any size.
 *
 * File: "ZDOT", version (1 byte), reserved (1), width and height of the source image (2 + 2), dot count (4),
 * then per dot x, y and radius (2 bytes each) in 1/8 pixel, all little-endian.
 */
class DotArt(val width: Int, val height: Int, private val dots: ShortArray) {
    val count: Int get() = dots.size / FIELDS

    /** Draws the dots centre-cropped to [targetWidth]×[targetHeight], never stretched (same fit as images). */
    fun render(targetWidth: Int, targetHeight: Int, foreground: Int, background: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(background)
        val crop = WallpaperFit.centerCrop(width, height, targetWidth, targetHeight)
        val scale = targetWidth.toFloat() / crop.width
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = foreground }
        for (i in 0 until count) {
            val x = ((dots[i * FIELDS].toInt() and MASK) / UNIT - crop.left) * scale
            val y = ((dots[i * FIELDS + 1].toInt() and MASK) / UNIT - crop.top) * scale
            val r = (dots[i * FIELDS + 2].toInt() and MASK) / UNIT * scale
            if (visible(x, y, r, targetWidth, targetHeight)) canvas.drawCircle(x, y, r, paint)
        }
        return bitmap
    }

    companion object {
        private val MAGIC = byteArrayOf('Z'.code.toByte(), 'D'.code.toByte(), 'O'.code.toByte(), 'T'.code.toByte())
        private const val VERSION = 1
        private const val HEADER = 14
        private const val FIELDS = 3
        private const val UNIT = 8f
        private const val MASK = 0xFFFF

        /** Reads a `.dots` file; throws [IOException] if it isn't one. */
        fun parse(bytes: ByteArray): DotArt {
            ensure(bytes.size >= HEADER && bytes.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) { "not a dots file" }
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            buffer.position(MAGIC.size)
            val version = buffer.get().toInt()
            ensure(version == VERSION) { "unsupported dots version $version" }
            buffer.get()
            val width = buffer.short.toInt() and MASK
            val height = buffer.short.toInt() and MASK
            val count = buffer.int
            val sizeOk = width > 0 && height > 0
            ensure(sizeOk && count >= 0 && bytes.size >= HEADER + count * FIELDS * 2) { "damaged dots file" }
            val dots = ShortArray(count * FIELDS) { buffer.short }
            return DotArt(width, height, dots)
        }

        private inline fun ensure(ok: Boolean, message: () -> String) {
            if (!ok) throw IOException(message())
        }

        /** Whether a circle at ([x], [y]) with radius [r] touches the [width]×[height] bitmap. */
        private fun visible(x: Float, y: Float, r: Float, width: Int, height: Int): Boolean {
            val horizontally = x + r >= 0 && x - r <= width
            return horizontally && y + r >= 0 && y - r <= height
        }
    }
}
