package com.mycelium.launcher.data

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppEntry(
    val label: String,
    val pkg: String,
    val cls: String,
    val icon: ImageBitmap,
) {
    val key: String get() = "$pkg/$cls"
}

/** Loads every launchable app, alphabetically. Icons are cached between refreshes. */
class AppRepository(private val context: Context) {
    val apps = mutableStateListOf<AppEntry>()
    private val iconCache = HashMap<String, ImageBitmap>()

    suspend fun refresh() {
        val list = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            pm.queryIntentActivities(intent, 0)
                .filter { it.activityInfo.packageName != context.packageName }
                .mapNotNull { ri ->
                    runCatching {
                        val pkg = ri.activityInfo.packageName
                        val cls = ri.activityInfo.name
                        val key = "$pkg/$cls"
                        val icon = iconCache.getOrPut(key) {
                            ri.loadIcon(pm).toBitmap(144, 144).asImageBitmap()
                        }
                        AppEntry(ri.loadLabel(pm).toString(), pkg, cls, icon)
                    }.getOrNull()
                }
                .sortedBy { it.label.lowercase() }
        }
        if (list != apps.toList()) {
            apps.clear()
            apps.addAll(list)
        }
    }
}
