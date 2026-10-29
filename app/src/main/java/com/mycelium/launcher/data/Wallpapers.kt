package com.mycelium.launcher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.mycelium.launcher.R

data class Wallpaper(val id: String, val label: String, val res: Int)

val WALLPAPERS = listOf(
    Wallpaper("wp_01", "Misty spruce", R.drawable.wp_01_misty_spruce),
    Wallpaper("wp_02", "Glowcaps", R.drawable.wp_02_glowcaps),
    Wallpaper("wp_03", "Mycelium", R.drawable.wp_03_mycelium),
    Wallpaper("wp_04", "Fern & dew", R.drawable.wp_04_fern_dew),
    Wallpaper("wp_05", "Aurora lake", R.drawable.wp_05_aurora_lake),
    Wallpaper("wp_06", "Autumn floor", R.drawable.wp_06_autumn_floor),
    Wallpaper("wp_07", "Roots", R.drawable.wp_07_roots),
    Wallpaper("wp_08", "Rain on glass", R.drawable.wp_08_rain_glass),
    Wallpaper("wp_09", "Topo map", R.drawable.wp_09_topo_map),
    Wallpaper("wp_10", "Fireflies", R.drawable.wp_10_fireflies),
)

fun wallpaperById(id: String): Wallpaper? = WALLPAPERS.firstOrNull { it.id == id }

/** Decodes a bundled wallpaper, downsampled so its width is at least [targetWidth] px. */
fun decodeWallpaper(context: Context, res: Int, targetWidth: Int): Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeResource(context.resources, res, bounds)
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= targetWidth) sample *= 2
    BitmapFactory.decodeResource(context.resources, res, BitmapFactory.Options().apply { inSampleSize = sample })
}.getOrNull()
