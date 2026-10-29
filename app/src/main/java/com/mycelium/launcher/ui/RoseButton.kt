package com.mycelium.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mycelium.launcher.data.LauncherState
import com.mycelium.launcher.ui.theme.Fern
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

// ---- Rose geometry (all radii are fractions of the rose radius) ----

/** rOut, petal count, rotation in degrees. Outer ring first. */
private val LAYERS = listOf(
    Triple(1.00f, 5, -90f),
    Triple(0.80f, 5, -54f),
    Triple(0.62f, 5, -78f),
    Triple(0.46f, 4, -40f),
    Triple(0.32f, 4, -85f),
    Triple(0.20f, 3, -20f),
)
private val LEAF_ANGLES = listOf(-54f, 90f, 234f)
private const val ROSE_SCALE = 0.86f // rose radius relative to the button radius (leaves reach the edge)
private const val PI_F = Math.PI.toFloat()

private fun rad(deg: Float) = deg * PI_F / 180f

private fun Path.addPetal(cx: Float, cy: Float, r: Float, rIn: Float, rOut: Float, ang: Float, hw: Float) {
    fun px(rr: Float, a: Float) = cx + cos(a) * rr * r
    fun py(rr: Float, a: Float) = cy + sin(a) * rr * r
    val k = (8 * rOut - 2 * rIn) / 6
    moveTo(px(rIn, ang - hw), py(rIn, ang - hw))
    cubicTo(
        px(k, ang - hw * 1.05f), py(k, ang - hw * 1.05f),
        px(k, ang + hw * 1.05f), py(k, ang + hw * 1.05f),
        px(rIn, ang + hw), py(rIn, ang + hw),
    )
    close()
}

private fun Path.addLeaf(cx: Float, cy: Float, r: Float, angDeg: Float) {
    val a = rad(angDeg)
    val bx = cx + cos(a) * 0.45f * r
    val by = cy + sin(a) * 0.45f * r
    val tx = cx + cos(a) * r
    val ty = cy + sin(a) * r
    val nx = -sin(a)
    val ny = cos(a)
    val mx = (bx + tx) / 2
    val my = (by + ty) / 2
    val w = 0.26f * r
    moveTo(bx, by)
    quadraticBezierTo(mx + nx * w, my + ny * w, tx, ty)
    quadraticBezierTo(mx - nx * w, my - ny * w, bx, by)
    close()
}

/** The button's silhouette: outer petals + leaves. Used for clipping and the shadow. */
val RoseShape = GenericShape { size, _ ->
    val cx = size.width / 2
    val cy = size.height / 2
    val r = size.minDimension / 2
    val rr = r * ROSE_SCALE
    val (rOut, n, rot) = LAYERS.first()
    val hw = PI_F / n * 1.25f
    for (k in 0 until n) addPetal(cx, cy, rr, rOut * 0.18f, rOut, rad(rot) + k * 2 * PI_F / n, hw)
    LEAF_ANGLES.forEach { addLeaf(cx, cy, r, it) }
    addOval(androidx.compose.ui.geometry.Rect(Offset(cx, cy), rr * 0.7f))
}

/** Draws a top-down rose tinted by [accent]. */
fun DrawScope.drawRose(accent: Color) {
    val cx = size.width / 2
    val cy = size.height / 2
    val r = size.minDimension / 2
    val rr = r * ROSE_SCALE
    val deep = Color(accent.red * 0.5f, accent.green * 0.5f, accent.blue * 0.5f)
    val light = lerp(accent, Color.White, 0.5f)

    // leaves
    LEAF_ANGLES.forEach { ang ->
        val leaf = Path().apply { addLeaf(cx, cy, r, ang) }
        drawPath(leaf, Brush.radialGradient(listOf(lerp(Fern, Color.White, 0.12f), Fern, lerp(Fern, Color.Black, 0.3f)), Offset(cx, cy), r))
        val a = rad(ang)
        drawLine(
            lerp(Fern, Color.Black, 0.4f),
            Offset(cx + cos(a) * 0.5f * r, cy + sin(a) * 0.5f * r),
            Offset(cx + cos(a) * 0.95f * r, cy + sin(a) * 0.95f * r),
            strokeWidth = r * 0.025f,
            cap = StrokeCap.Round,
        )
    }

    LAYERS.forEachIndexed { li, (rOut, n, rot) ->
        val rIn = rOut * 0.18f
        val depth = li / (LAYERS.size - 1f)
        // shadow this ring casts onto the ring below
        if (li > 0) {
            val outer = (rOut + 0.16f) * rr
            drawCircle(
                Brush.radialGradient(
                    0f to Color.Transparent,
                    (rOut * 0.85f * rr / outer) to Color.Transparent,
                    (rOut * rr / outer) to Color.Black.copy(alpha = 0.4f),
                    1f to Color.Transparent,
                    center = Offset(cx, cy),
                    radius = outer,
                ),
                radius = outer,
                center = Offset(cx, cy),
            )
        }
        val base = lerp(deep, Color.Black, 0.2f * depth)
        val tip = lerp(light, deep, 0.15f * depth)
        val petalBrush = Brush.radialGradient(
            0f to base,
            (rIn / rOut) to base,
            ((rIn / rOut) + (1 - rIn / rOut) * 0.45f) to lerp(base, tip, 0.35f),
            1f to tip,
            center = Offset(cx, cy),
            radius = rOut * rr,
        )
        val hw = PI_F / n * 1.25f
        for (k in 0 until n) {
            val ang = rad(rot) + k * 2 * PI_F / n
            val petal = Path().apply { addPetal(cx, cy, rr, rIn, rOut, ang, hw) }
            // soft overlap shadow from the neighbouring petal
            rotate(-5f, Offset(cx, cy)) { drawPath(petal, Color.Black.copy(alpha = 0.18f)) }
            drawPath(petal, petalBrush)
            drawPath(petal, lerp(light, Color.White, 0.4f).copy(alpha = 0.5f), style = Stroke(width = r * 0.018f))
        }
    }

    // centre swirl
    val swirl = Path()
    var first = true
    var t = 0.5f
    while (t <= 11f) {
        val x = cx + cos(t) * t * rr * 0.011f
        val y = cy + sin(t) * t * rr * 0.011f
        if (first) swirl.moveTo(x, y) else swirl.lineTo(x, y)
        first = false
        t += 0.05f
    }
    drawPath(swirl, Color(deep.red * 0.6f, deep.green * 0.6f, deep.blue * 0.6f), style = Stroke(width = rr * 0.03f, cap = StrokeCap.Round))
}

/**
 * The floating rose button.
 *  - single tap  → app archive
 *  - double tap  → UI settings
 *  - press & hold, then drag → move it anywhere (position is saved)
 */
@Composable
fun RoseButton(
    state: LauncherState,
    areaW: Float,
    areaH: Float,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
) {
    val sizeDp = 72.dp
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

    // Only react to touches on the flower itself, not the empty corners of its square.
    fun onRose(p: Offset) = hypot(p.x - sizePx / 2, p.y - sizePx / 2) <= sizePx / 2 * 0.95f

    Box(
        Modifier
            .zIndex(2f)
            .offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }
            .size(sizeDp)
            .graphicsLayer {
                val s = if (dragging) 1.15f else 1f
                scaleX = s
                scaleY = s
                rotationZ = if (dragging) 12f else 0f
            }
            .shadow(10.dp, RoseShape)
            // Outer: taps. Inner: long-press drag (consumes the release, so a hold never counts as a tap).
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { if (onRose(it)) tap() },
                    onDoubleTap = { if (onRose(it)) doubleTap() },
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
        Canvas(Modifier.fillMaxSize()) { drawRose(accent) }
    }
}

/** Static preview used in settings. */
@Composable
fun RosePreview(accent: Color, modifier: Modifier) {
    Canvas(modifier) { drawRose(accent) }
}
