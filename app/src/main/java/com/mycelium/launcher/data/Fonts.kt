package com.mycelium.launcher.data

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.text.font.FontFamily
import java.io.File

/** A font bundled in assets/fonts (downloaded by the GitHub build — see scripts/fetch-fonts.sh). */
data class BundledFont(val id: String, val label: String, val asset: String)

val BUNDLED_FONTS = listOf(
    BundledFont("lora", "Lora", "fonts/lora.ttf"),
    BundledFont("cormorant", "Cormorant Garamond", "fonts/cormorant.ttf"),
    BundledFont("caveat", "Caveat", "fonts/caveat.ttf"),
    BundledFont("quicksand", "Quicksand", "fonts/quicksand.ttf"),
    BundledFont("cinzel", "Cinzel", "fonts/cinzel.ttf"),
    BundledFont("josefin", "Josefin Sans", "fonts/josefin.ttf"),
)

/** Loads bundled + user-uploaded fonts and caches them as Compose font families. */
class FontRepository(private val context: Context) {
    private val dir = File(context.filesDir, "fonts").apply { mkdirs() }
    private val cache = HashMap<String, FontFamily?>()

    /** File names of uploaded fonts. */
    val customFonts = mutableStateListOf<String>().apply {
        addAll(dir.listFiles()?.map { it.name }?.sorted() ?: emptyList())
    }

    fun family(id: String): FontFamily? {
        if (id == FONT_SYSTEM) return null
        return cache.getOrPut(id) { typeface(id)?.let { FontFamily(androidx.compose.ui.text.font.Typeface(it)) } }
    }

    fun isAvailable(id: String): Boolean = id == FONT_SYSTEM || family(id) != null

    private fun typeface(id: String): Typeface? = runCatching {
        if (id.startsWith(CUSTOM_FONT_PREFIX)) {
            val f = File(dir, id.removePrefix(CUSTOM_FONT_PREFIX))
            if (f.exists()) Typeface.Builder(f).build() else null
        } else {
            val font = BUNDLED_FONTS.firstOrNull { it.id == id } ?: return@runCatching null
            context.assets.open(font.asset).close() // throws if the build didn't include it
            Typeface.Builder(context.assets, font.asset).build()
        }
    }.getOrNull()

    /** Copies a .ttf/.otf the user picked into app storage. Returns the new font id, or null if it isn't a valid font. */
    fun import(uri: Uri): String? {
        val resolver = context.contentResolver
        var name = "font_${System.currentTimeMillis()}.ttf"
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0)?.let { name = it }
        }
        name = name.replace(Regex("[^A-Za-z0-9._ -]"), "_")
        val target = File(dir, name)
        runCatching {
            resolver.openInputStream(uri)?.use { input -> target.outputStream().use { input.copyTo(it) } }
        }.onFailure { return null }
        val ok = runCatching { Typeface.Builder(target).build() }.getOrNull() != null
        if (!ok) {
            target.delete()
            return null
        }
        val id = CUSTOM_FONT_PREFIX + name
        cache.remove(id)
        if (name !in customFonts) customFonts.add(name)
        return id
    }

    fun delete(id: String) {
        val name = id.removePrefix(CUSTOM_FONT_PREFIX)
        File(dir, name).delete()
        cache.remove(id)
        customFonts.remove(name)
    }
}
