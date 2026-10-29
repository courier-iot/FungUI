package com.mycelium.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.mycelium.launcher.data.BACKDROP_FOREST
import com.mycelium.launcher.data.BACKDROP_SYSTEM
import com.mycelium.launcher.data.decodeWallpaper
import com.mycelium.launcher.data.wallpaperById
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Draws whatever sits behind the home pages. "system" draws nothing so the phone wallpaper shows. */
@Composable
fun Backdrop(id: String, accent: Color) {
    when {
        id == BACKDROP_SYSTEM -> Unit
        id == BACKDROP_FOREST -> EarthBackdrop(accent)
        else -> {
            val wp = wallpaperById(id)
            if (wp == null) {
                EarthBackdrop(accent)
                return
            }
            val context = LocalContext.current
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val width = constraints.maxWidth
                var bitmap by remember(id) { mutableStateOf<ImageBitmap?>(null) }
                LaunchedEffect(id, width) {
                    bitmap = withContext(Dispatchers.IO) {
                        decodeWallpaper(context, wp.res, width)?.asImageBitmap()
                    }
                }
                val b = bitmap
                if (b == null) EarthBackdrop(accent)
                else Image(b, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
