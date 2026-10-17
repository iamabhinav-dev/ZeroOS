package com.zeroos.core.data.wallpaper

/** Wallpapers come in dark and light versions; the gallery shows those of the current theme (F6.8). */
enum class WallpaperTone { DARK, LIGHT }

/** Where a wallpaper's pixels come from. */
sealed interface WallpaperSource {
    /** One flat colour, generated on the device (Pure black, Pure white). */
    data class Solid(val argb: Int) : WallpaperSource

    /** An image in the app's assets, added by scripts/add-wallpapers.py. */
    data class Asset(val path: String) : WallpaperSource
}

/**
 * One gallery entry. [id] is stored in settings; [name] picks the label string `wallpaper_<name>`
 * (no code change per image, D-038).
 */
data class WallpaperItem(val id: String, val name: String, val tone: WallpaperTone, val source: WallpaperSource)

/** The built-in gallery (F6.8): the generated solids plus whatever images are in `assets/wallpapers/`. */
object WallpaperCatalog {
    const val ASSET_DIR = "wallpapers"
    const val PURE_BLACK = "pure_black"
    const val PURE_WHITE = "pure_white"

    private const val BLACK = 0xFF000000.toInt()
    private const val WHITE = 0xFFFFFFFF.toInt()
    private val FILE = Regex("""^([a-z0-9_]+)_(dark|light)\.webp$""")

    private val solids = listOf(
        WallpaperItem(PURE_BLACK, PURE_BLACK, WallpaperTone.DARK, WallpaperSource.Solid(BLACK)),
        WallpaperItem(PURE_WHITE, PURE_WHITE, WallpaperTone.LIGHT, WallpaperSource.Solid(WHITE))
    )

    /** Everything available, given the file names in `assets/wallpapers/`; badly named files are skipped. */
    fun all(assetFiles: List<String>): List<WallpaperItem> = solids + assetFiles.sorted().mapNotNull(::fromFile)

    /** What the gallery shows for [tone]: its solid first, then the images, alphabetically. */
    fun forTone(assetFiles: List<String>, tone: WallpaperTone): List<WallpaperItem> = all(assetFiles).filter { it.tone == tone }

    fun find(assetFiles: List<String>, id: String): WallpaperItem? = all(assetFiles).firstOrNull { it.id == id }

    private fun fromFile(file: String): WallpaperItem? {
        val match = FILE.matchEntire(file) ?: return null
        val (name, tone) = match.destructured
        return WallpaperItem(
            id = "${name}_$tone",
            name = name,
            tone = if (tone == "dark") WallpaperTone.DARK else WallpaperTone.LIGHT,
            source = WallpaperSource.Asset("$ASSET_DIR/$file")
        )
    }
}
