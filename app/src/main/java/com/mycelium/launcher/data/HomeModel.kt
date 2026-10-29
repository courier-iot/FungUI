package com.mycelium.launcher.data

import kotlin.math.min

const val KIND_APP = "app"
const val KIND_WIDGET = "widget"

/** Home screen layouts. FREE uses a 4×9 grid only as a sizing reference. */
enum class GridMode(val cols: Int, val rows: Int, val label: String) {
    GRID_4x9(4, 9, "4 × 9"),
    GRID_5x10(5, 10, "5 × 10"),
    FREE(4, 9, "Free place"),
}

/**
 * Widget types. Default size is full grid width × [rows] tall; users can resize
 * down to [minW] × [minH] cells.
 */
enum class WidgetType(val id: String, val label: String, val rows: Int, val minW: Int, val minH: Int) {
    NOTES("notes", "Notes", 2, 2, 1),
    MUSIC("music", "YouTube Music", 1, 2, 1),
    CALENDAR("calendar", "Proton Calendar", 3, 2, 2);
    // CLOCK will slot in here once its design is described.

    companion object {
        fun from(id: String): WidgetType? = entries.firstOrNull { it.id == id }
    }
}

/**
 * One thing living on a home page: an app shortcut or a widget.
 * [col]/[row] are used by the grid layouts, [fx]/[fy] (0..1 fractions of the page) by free mode.
 * [sw]/[sh] are a widget's custom size in cells (0 = default).
 */
data class HomeItem(
    val id: Long,
    val kind: String,
    val pkg: String = "",
    val cls: String = "",
    val widget: String = "",
    val col: Int = 0,
    val row: Int = 0,
    val fx: Float = 0f,
    val fy: Float = 0f,
    val sw: Int = 0,
    val sh: Int = 0,
) {
    val appKey: String get() = "$pkg/$cls"

    fun span(mode: GridMode): Pair<Int, Int> {
        if (kind != KIND_WIDGET) return 1 to 1
        val type = WidgetType.from(widget) ?: return 1 to 1
        val w = if (sw > 0) sw else mode.cols
        val h = if (sh > 0) sh else type.rows
        return min(w, mode.cols) to min(h, mode.rows)
    }
}
