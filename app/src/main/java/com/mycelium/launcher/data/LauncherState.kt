package com.mycelium.launcher.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.min
import kotlin.math.roundToInt

private val DEFAULT_ACCENT = 0xFF6B8E4E.toInt() // moss

/** All launcher settings + home page contents, persisted to SharedPreferences. */
class LauncherState(context: Context) {
    private val prefs = context.getSharedPreferences("mycelium_prefs", Context.MODE_PRIVATE)

    var accent by mutableStateOf(Color(prefs.getInt("accent", DEFAULT_ACCENT)))
        private set

    var gridMode by mutableStateOf(
        runCatching { GridMode.valueOf(prefs.getString("grid", null) ?: "") }
            .getOrDefault(GridMode.GRID_4x9)
    )
        private set

    var useWallpaper by mutableStateOf(prefs.getBoolean("wallpaper", false))
        private set

    /** Leaf button centre, as fractions of the screen. */
    var buttonX by mutableFloatStateOf(prefs.getFloat("btnX", 0.5f))
        private set
    var buttonY by mutableFloatStateOf(prefs.getFloat("btnY", 0.88f))
        private set

    var currentPage by mutableIntStateOf(0)

    val pages = mutableStateListOf<List<HomeItem>>()

    private var nextId = prefs.getLong("nextId", 1L)

    init {
        loadPages()
        if (pages.isEmpty()) {
            pages.add(emptyList())
            pages.add(emptyList())
            savePages()
        }
    }

    // ---------- settings ----------

    fun updateAccent(c: Color) {
        accent = c
        prefs.edit().putInt("accent", c.toArgb()).apply()
    }

    fun updateWallpaper(on: Boolean) {
        useWallpaper = on
        prefs.edit().putBoolean("wallpaper", on).apply()
    }

    fun setButton(x: Float, y: Float) {
        buttonX = x.coerceIn(0f, 1f)
        buttonY = y.coerceIn(0f, 1f)
        prefs.edit().putFloat("btnX", buttonX).putFloat("btnY", buttonY).apply()
    }

    fun resetButton() = setButton(0.5f, 0.88f)

    fun updateGrid(mode: GridMode) {
        if (mode == gridMode) return
        val old = gridMode
        for (p in pages.indices) {
            pages[p] = pages[p].map {
                when {
                    // grid -> free: keep the visual arrangement
                    mode == GridMode.FREE -> it.copy(
                        fx = it.col.toFloat() / old.cols,
                        fy = it.row.toFloat() / old.rows,
                    )
                    // free -> grid: snap to nearest cell
                    old == GridMode.FREE -> it.copy(
                        col = (it.fx * mode.cols).roundToInt(),
                        row = (it.fy * mode.rows).roundToInt(),
                    )
                    else -> it
                }
            }
        }
        gridMode = mode
        if (mode != GridMode.FREE) reflow(mode)
        prefs.edit().putString("grid", mode.name).apply()
        savePages()
    }

    // ---------- pages ----------

    fun addPage() {
        if (pages.size >= 9) return
        pages.add(emptyList())
        savePages()
    }

    /** Only removes the last page if it's empty. */
    fun removeLastPage(): Boolean {
        if (pages.size <= 1 || pages.last().isNotEmpty()) return false
        pages.removeAt(pages.lastIndex)
        currentPage = currentPage.coerceAtMost(pages.lastIndex)
        savePages()
        return true
    }

    // ---------- items ----------

    fun addApp(page: Int, pkg: String, cls: String): Boolean {
        if (page !in pages.indices) return false
        return place(page, HomeItem(id = newId(), kind = KIND_APP, pkg = pkg, cls = cls))
    }

    fun addWidget(page: Int, type: WidgetType): Boolean {
        if (page !in pages.indices) return false
        return place(page, HomeItem(id = newId(), kind = KIND_WIDGET, widget = type.id))
    }

    fun removeItem(page: Int, id: Long) {
        val list = pages.getOrNull(page) ?: return
        pages[page] = list.filterNot { it.id == id }
        prefs.edit().remove("note_$id").apply()
        savePages()
    }

    fun moveGrid(page: Int, id: Long, col: Int, row: Int): Boolean {
        val mode = gridMode
        val list = pages.getOrNull(page) ?: return false
        val item = list.find { it.id == id } ?: return false
        val (w, h) = item.span(mode)
        val c = col.coerceIn(0, mode.cols - w)
        val r = row.coerceIn(0, mode.rows - h)
        val occ = occupancy(list.filter { it.id != id }, mode)
        if (!fitsAt(occ, c, r, w, h, mode)) return false
        pages[page] = list.map { if (it.id == id) it.copy(col = c, row = r) else it }
        savePages()
        return true
    }

    fun moveFree(page: Int, id: Long, fx: Float, fy: Float) {
        val list = pages.getOrNull(page) ?: return
        pages[page] = list.map { if (it.id == id) it.copy(fx = fx, fy = fy) else it }
        savePages()
    }

    // ---------- notes widget storage ----------

    fun getNote(id: Long): String = prefs.getString("note_$id", "") ?: ""
    fun setNote(id: Long, text: String) = prefs.edit().putString("note_$id", text).apply()

    // ---------- placement helpers ----------

    private fun newId(): Long {
        val id = nextId++
        prefs.edit().putLong("nextId", nextId).apply()
        return id
    }

    private fun place(page: Int, item: HomeItem): Boolean {
        val mode = gridMode
        val existing = if (mode == GridMode.FREE) {
            pages[page].map {
                it.copy(col = (it.fx * mode.cols).roundToInt(), row = (it.fy * mode.rows).roundToInt())
            }
        } else pages[page]
        val (w, h) = item.span(mode)
        val spot = findFree(existing, w, h, mode)
        val placed = when {
            spot != null -> item.copy(
                col = spot.first, row = spot.second,
                fx = spot.first.toFloat() / mode.cols, fy = spot.second.toFloat() / mode.rows,
            )
            mode == GridMode.FREE -> item.copy(fx = 0.35f, fy = 0.4f)
            else -> return false
        }
        pages[page] = pages[page] + placed
        savePages()
        return true
    }

    /** After a grid change, keep items that still fit and move the rest to free cells (or later pages). */
    private fun reflow(mode: GridMode) {
        var carry = listOf<HomeItem>()
        var p = 0
        while (p < pages.size || carry.isNotEmpty()) {
            if (p >= pages.size) pages.add(emptyList())
            val placed = mutableListOf<HomeItem>()
            val leftover = mutableListOf<HomeItem>()
            for (item in pages[p]) {
                val (w, h) = item.span(mode)
                if (fitsAt(occupancy(placed, mode), item.col, item.row, w, h, mode)) placed += item
                else leftover += item
            }
            leftover += carry
            val next = mutableListOf<HomeItem>()
            for (item in leftover) {
                val (w, h) = item.span(mode)
                val spot = findFree(placed, w, h, mode)
                if (spot != null) placed += item.copy(col = spot.first, row = spot.second)
                else next += item
            }
            pages[p] = placed
            carry = next
            p++
        }
    }

    private fun occupancy(items: List<HomeItem>, mode: GridMode): Array<BooleanArray> {
        val occ = Array(mode.rows) { BooleanArray(mode.cols) }
        for (it in items) {
            val (w, h) = it.span(mode)
            for (r in it.row until min(it.row + h, mode.rows)) {
                for (c in it.col until min(it.col + w, mode.cols)) {
                    if (r >= 0 && c >= 0) occ[r][c] = true
                }
            }
        }
        return occ
    }

    private fun fitsAt(occ: Array<BooleanArray>, c: Int, r: Int, w: Int, h: Int, mode: GridMode): Boolean {
        if (c < 0 || r < 0 || c + w > mode.cols || r + h > mode.rows) return false
        for (rr in r until r + h) for (cc in c until c + w) if (occ[rr][cc]) return false
        return true
    }

    private fun findFree(items: List<HomeItem>, w: Int, h: Int, mode: GridMode): Pair<Int, Int>? {
        val occ = occupancy(items, mode)
        for (r in 0..mode.rows - h) {
            for (c in 0..mode.cols - w) {
                if (fitsAt(occ, c, r, w, h, mode)) return c to r
            }
        }
        return null
    }

    // ---------- persistence ----------

    private fun savePages() {
        val arr = JSONArray()
        for (page in pages) {
            val pa = JSONArray()
            for (item in page) {
                pa.put(
                    JSONObject()
                        .put("id", item.id)
                        .put("kind", item.kind)
                        .put("pkg", item.pkg)
                        .put("cls", item.cls)
                        .put("widget", item.widget)
                        .put("col", item.col)
                        .put("row", item.row)
                        .put("fx", item.fx.toDouble())
                        .put("fy", item.fy.toDouble())
                )
            }
            arr.put(pa)
        }
        prefs.edit().putString("pages", arr.toString()).apply()
    }

    private fun loadPages() {
        val raw = prefs.getString("pages", null) ?: return
        runCatching {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val pa = arr.getJSONArray(i)
                val items = (0 until pa.length()).map { j ->
                    val o = pa.getJSONObject(j)
                    HomeItem(
                        id = o.getLong("id"),
                        kind = o.getString("kind"),
                        pkg = o.optString("pkg"),
                        cls = o.optString("cls"),
                        widget = o.optString("widget"),
                        col = o.optInt("col"),
                        row = o.optInt("row"),
                        fx = o.optDouble("fx", 0.0).toFloat(),
                        fy = o.optDouble("fy", 0.0).toFloat(),
                    )
                }
                pages.add(items)
            }
        }
    }
}
