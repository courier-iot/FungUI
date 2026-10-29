package com.mycelium.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mycelium.launcher.data.LauncherState
import com.mycelium.launcher.ui.theme.Fern
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

/** Plain sRGB mix — avoids colour-space conversions entirely. */
private fun mix(a: Color, b: Color, t: Float): Color {
    val k = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * k,
        green = a.green + (b.green - a.green) * k,
        blue = a.blue + (b.blue - a.blue) * k,
        alpha = 1f,
    )
}

private val WHITE = Color(1f, 1f, 1f)
private val BLACK = Color(0f, 0f, 0f)

/** Draws a top-down rose tinted by [accent]. Solid fills only, so it renders the same everywhere. */
fun DrawScope.drawRose(accent: Color) {
    val cx = size.width / 2
    val cy = size.height / 2
    val c = Offset(cx, cy)
    val r = size.minDimension / 2
    val rr = r * ROSE_SCALE
    val acc = Color(accent.red, accent.green, accent.blue)
    val deep = mix(acc, BLACK, 0.55f)
    val light = mix(acc, WHITE, 0.45f)

    // soft drop shadow under the whole flower
    for (i in 6 downTo 1) {
        drawCircle(BLACK.copy(alpha = 0.06f), radius = rr * (0.9f + i * 0.04f), center = Offset(cx, cy + r * 0.05f))
    }

    // leaves
    val leafDark = mix(Fern, BLACK, 0.35f)
    LEAF_ANGLES.forEach { ang ->
        val leaf = Path().apply { addLeaf(cx, cy, r, ang) }
        drawPath(leaf, Fern)
        drawPath(leaf, leafDark, style = Stroke(width = r * 0.02f))
        val a = rad(ang)
        drawLine(
            leafDark,
            Offset(cx + cos(a) * 0.5f * r, cy + sin(a) * 0.5f * r),
            Offset(cx + cos(a) * 0.95f * r, cy + sin(a) * 0.95f * r),
            strokeWidth = r * 0.025f,
            cap = StrokeCap.Round,
        )
    }

    LAYERS.forEachIndexed { li, (rOut, n, rot) ->
        val rIn = rOut * 0.18f
        val depth = li / (LAYERS.size - 1f)
        // shadow this ring casts on the ring below it: a few translucent discs
        if (li > 0) {
            for (i in 3 downTo 1) {
                drawCircle(BLACK.copy(alpha = 0.10f), radius = rr * (rOut + i * 0.03f), center = c)
            }
        }
        // outer rings lighter, centre deeper — like looking into the cup of a rose
        val body = mix(light, deep, 0.15f + 0.6f * depth)
        val rim = mix(body, WHITE, 0.35f)
        val hw = PI_F / n * 1.25f
        for (k in 0 until n) {
            val ang = rad(rot) + k * 2 * PI_F / n
            val petal = Path().apply { addPetal(cx, cy, rr, rIn, rOut, ang, hw) }
            // overlap shadow from the neighbouring petal
            rotate(-6f, c) { drawPath(petal, BLACK.copy(alpha = 0.22f)) }
            drawPath(petal, body)
            // inner shading: a smaller, darker copy of the petal toward its base
            val inner = Path().apply { addPetal(cx, cy, rr, rIn, rIn + (rOut - rIn) * 0.55f, ang, hw * 0.85f) }
            drawPath(inner, mix(body, deep, 0.45f).copy(alpha = 0.55f))
            drawPath(petal, rim.copy(alpha = 0.7f), style = Stroke(width = r * 0.02f))
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
    drawPath(swirl, mix(deep, BLACK, 0.4f), style = Stroke(width = rr * 0.03f, cap = StrokeCap.Round))
}

/**
 * The floating rose button.
 *  - single tap  → app archive
 *  - double tap  → UI settings
 *  - press & hold, then drag → move it anywhere (position is saved)
 *
 * All three are handled by one gesture loop so they can't fight each other.
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

    var pos by remember { mutableStateOf(Offset.Unspecified) }
    // (Re)place from saved fractions when first shown, when the screen size changes, or after "Reset position".
    LaunchedEffect(areaW, areaH, state.buttonX, state.buttonY) {
        if (areaW > 0 && areaH > 0) {
            pos = Offset(
                (state.buttonX * areaW - sizePx / 2).coerceIn(0f, (areaW - sizePx).coerceAtLeast(0f)),
                (state.buttonY * areaH - sizePx / 2).coerceIn(0f, (areaH - sizePx).coerceAtLeast(0f)),
            )
        }
    }
    var dragging by remember { mutableStateOf(false) }
    val accent = state.accent
    if (pos == Offset.Unspecified) return

    Box(
        Modifier
            .zIndex(2f)
            .offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }
            .size(sizeDp)
            .pointerInput(areaW, areaH, sizePx) {
                val longPressMs = viewConfiguration.longPressTimeoutMillis
                val doubleTapMs = viewConfiguration.doubleTapTimeoutMillis
                val slop = viewConfiguration.touchSlop
                var pendingTap: Job? = null
                var lastTapUp = 0L
                val half = sizePx / 2

                coroutineScope {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        // only the flower counts, not the empty corners of its square
                        if (hypot(down.position.x - half, down.position.y - half) > half) return@awaitEachGesture
                        down.consume()

                        var released = false
                        var movedEarly = false
                        val longPressed = withTimeoutOrNull(longPressMs) {
                            while (true) {
                                val ev = awaitPointerEvent()
                                val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                                if (!ch.pressed) { ch.consume(); released = true; break }
                                if ((ch.position - down.position).getDistance() > slop) { movedEarly = true; break }
                            }
                            false
                        } ?: true

                        when {
                            released -> {
                                val now = System.currentTimeMillis()
                                if (pendingTap?.isActive == true && now - lastTapUp <= doubleTapMs) {
                                    pendingTap?.cancel()
                                    pendingTap = null
                                    doubleTap()
                                } else {
                                    lastTapUp = now
                                    pendingTap = launch {
                                        delay(doubleTapMs)
                                        tap()
                                    }
                                }
                            }
                            longPressed && !movedEarly -> {
                                pendingTap?.cancel()
                                dragging = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                while (true) {
                                    val ev = awaitPointerEvent()
                                    val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!ch.pressed) { ch.consume(); break }
                                    val delta = ch.position - ch.previousPosition
                                    ch.consume()
                                    pos = Offset(
                                        (pos.x + delta.x).coerceIn(0f, (areaW - sizePx).coerceAtLeast(0f)),
                                        (pos.y + delta.y).coerceIn(0f, (areaH - sizePx).coerceAtLeast(0f)),
                                    )
                                }
                                dragging = false
                                state.setButton((pos.x + half) / areaW, (pos.y + half) / areaH)
                            }
                            else -> Unit // moved before the long-press: not ours, ignore
                        }
                    }
                }
            }
    ) {
        // Visual effects live on the child so they never distort touch coordinates.
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val s = if (dragging) 1.15f else 1f
                    scaleX = s
                    scaleY = s
                    rotationZ = if (dragging) 12f else 0f
                }
        ) { drawRose(accent) }
    }
}

/** Static preview used in settings. */
@Composable
fun RosePreview(accent: Color, modifier: Modifier) {
    Canvas(modifier) { drawRose(accent) }
}
