package com.zeroos.core.data.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperFitTest {
    @Test
    fun f6_8_tallerImageLosesTopAndBottom() {
        val crop = WallpaperFit.centerCrop(1000, 3000, 1260, 2800)
        assertEquals(CropRegion(0, 389, 1000, 2222), crop)
    }

    @Test
    fun f6_8_widerImageLosesTheSides() {
        // A 9:16 image on a 9:20 screen: the sides are cut, centred.
        val crop = WallpaperFit.centerCrop(1080, 1920, 1260, 2800)
        assertEquals(CropRegion(108, 0, 864, 1920), crop)
    }

    @Test
    fun f6_8_sameShapeIsKeptWhole() {
        assertEquals(CropRegion(0, 0, 841, 1869), WallpaperFit.centerCrop(841, 1870, 1260, 2800))
    }

    @Test
    fun f6_8_neverStretched() {
        // The crop always has the screen's shape, so scaling it to the screen is uniform.
        listOf(500 to 500, 4000 to 3000, 841 to 1870, 2000 to 9000).forEach { (w, h) ->
            val crop = WallpaperFit.centerCrop(w, h, 1260, 2800)
            assertEquals(1260.0 / 2800, crop.width.toDouble() / crop.height, 0.01)
        }
    }

    @Test
    fun f6_8_largeImagesAreDecodedSmallerButNeverBelowScreenSize() {
        assertEquals(1, WallpaperFit.sampleSize(841, 1870, 1260, 2800))
        assertEquals(2, WallpaperFit.sampleSize(2600, 5800, 1260, 2800))
        assertEquals(4, WallpaperFit.sampleSize(6000, 12000, 1260, 2800))
    }
}
