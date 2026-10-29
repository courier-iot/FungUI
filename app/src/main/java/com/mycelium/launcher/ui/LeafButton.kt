package com.mycelium.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mycelium.launcher.data.LauncherState
import com.mycelium.launcher.ui.theme.Cream
import kotlin.math.roundToInt

/**
 * The floating leaf button.
 *  - single tap  → app archive
 *  - double tap  → UI settings
 *  - press & hold, then drag → move it anywhere (position is saved)
 */
@Composable
fun LeafButton(
    state: LauncherState,
    areaW: Float,
    areaH: Float,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
) {
    val sizeDp = 60.dp
    val sizePx = with(LocalDensity.current) { sizeDp.toPx() }
    val haptic = LocalHapticFeedback.current
    val tap by rememberUpdatedState(onTap)
    val doubleTap by rememberUpdatedState(onDoubleTap)

    var pos by remember(areaW, areaH, state.buttonX, state.buttonY) {
        mutableStateOf(
            Offset(
                (state.buttonX * areaW - sizePx / 2).coerceIn(0f, (areaW - sizePx).coerceAtLeast(0f)),
                (state.buttonY * areaH - sizePx / 2).coerceIn(0f, (areaH - sizePx).coerceAtLeast(0f)),
            )
        )
    }
    var dragging by remember { mutableStateOf(false) }
    val accent = state.accent

    Box(
        Modifier
            .zIndex(2f)
            .offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }
            .size(sizeDp)
            .graphicsLayer {
                val s = if (dragging) 1.15f else 1f
                scaleX = s
                scaleY = s
            }
            .shadow(10.dp, CircleShape)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(accent.copy(alpha = 0.85f), accent)))
            // Outer: taps. Inner: long-press drag (consumes the release, so a hold never counts as a tap).
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { tap() },
                    onDoubleTap = { doubleTap() },
                )
            }
            .pointerInput(areaW, areaH) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        dragging = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        pos = Offset(
                            (pos.x + amount.x).coerceIn(0f, areaW - sizePx),
                            (pos.y + amount.y).coerceIn(0f, areaH - sizePx),
                        )
                    },
                    onDragEnd = {
                        dragging = false
                        state.setButton((pos.x + sizePx / 2) / areaW, (pos.y + sizePx / 2) / areaH)
                    },
                    onDragCancel = { dragging = false },
                )
            }
    ) {
        Canvas(Modifier.fillMaxSize().padding(15.dp)) {
            val w = size.width
            val h = size.height
            val leaf = Path().apply {
                moveTo(w * 0.5f, h * 0.02f)
                cubicTo(w * 1.05f, h * 0.30f, w * 0.85f, h * 0.80f, w * 0.5f, h * 0.98f)
                cubicTo(w * 0.15f, h * 0.80f, w * -0.05f, h * 0.30f, w * 0.5f, h * 0.02f)
                close()
            }
            drawPath(leaf, Cream)
            drawLine(accent, Offset(w * 0.5f, h * 0.18f), Offset(w * 0.5f, h * 0.95f), strokeWidth = w * 0.07f)
            drawLine(accent, Offset(w * 0.5f, h * 0.45f), Offset(w * 0.72f, h * 0.30f), strokeWidth = w * 0.05f)
            drawLine(accent, Offset(w * 0.5f, h * 0.62f), Offset(w * 0.28f, h * 0.47f), strokeWidth = w * 0.05f)
        }
    }
}
