package com.mycelium.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.mycelium.launcher.ui.theme.Amanita
import com.mycelium.launcher.ui.theme.Bark
import com.mycelium.launcher.ui.theme.Chanterelle
import com.mycelium.launcher.ui.theme.Cream
import com.mycelium.launcher.ui.theme.Spore

/** Hand-drawn forest floor: dusk gradient, drifting spores, vines (accent tinted) and mushrooms. */
@Composable
fun EarthBackdrop(accent: Color) {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val u = density // 1dp in px

        drawRect(Brush.verticalGradient(listOf(Color(0xFF1F2A1B), Bark, Color(0xFF171009))))

        val rnd = java.util.Random(42)
        repeat(45) {
            drawCircle(
                color = Spore.copy(alpha = 0.05f + rnd.nextFloat() * 0.12f),
                radius = u * (1f + rnd.nextFloat() * 2f),
                center = Offset(rnd.nextFloat() * w, rnd.nextFloat() * h),
            )
        }

        vine(Offset(-10 * u, h * 0.05f), Offset(w * 0.35f, h * 0.02f), Offset(w * 0.15f, h * 0.30f), Offset(w * 0.42f, h * 0.38f), accent, u)
        vine(Offset(w + 10 * u, h * 0.12f), Offset(w * 0.6f, h * 0.10f), Offset(w * 0.9f, h * 0.42f), Offset(w * 0.62f, h * 0.55f), accent, u)
        vine(Offset(-10 * u, h * 0.62f), Offset(w * 0.2f, h * 0.55f), Offset(w * 0.05f, h * 0.80f), Offset(w * 0.28f, h * 0.86f), accent, u)

        drawOval(Color(0xFF243318), topLeft = Offset(-w * 0.2f, h * 0.93f), size = Size(w * 1.4f, h * 0.2f))
        mushroom(w * 0.12f, h * 0.95f, 1.1f, Chanterelle, u)
        mushroom(w * 0.21f, h * 0.955f, 0.7f, accent, u)
        mushroom(w * 0.84f, h * 0.952f, 0.9f, Amanita, u)
        mushroom(w * 0.92f, h * 0.958f, 0.55f, Chanterelle, u)
    }
}

private fun bez(p0: Offset, p1: Offset, p2: Offset, p3: Offset, t: Float): Offset {
    val m = 1f - t
    return p0 * (m * m * m) + p1 * (3f * m * m * t) + p2 * (3f * m * t * t) + p3 * (t * t * t)
}

private fun DrawScope.vine(p0: Offset, p1: Offset, p2: Offset, p3: Offset, color: Color, u: Float) {
    val path = Path().apply {
        moveTo(p0.x, p0.y)
        cubicTo(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)
    }
    drawPath(path, color.copy(alpha = 0.45f), style = Stroke(width = 3f * u, cap = StrokeCap.Round))
    var i = 0
    var t = 0.12f
    while (t < 0.97f) {
        val pt = bez(p0, p1, p2, p3, t)
        val angle = (if (i % 2 == 0) -35f else 145f) + i * 9f
        rotate(angle, pivot = pt) {
            drawOval(color.copy(alpha = 0.38f), topLeft = Offset(pt.x, pt.y - 4f * u), size = Size(16f * u, 8f * u))
        }
        i++
        t += 0.09f
    }
}

private fun DrawScope.mushroom(x: Float, baseY: Float, scale: Float, cap: Color, u: Float) {
    val k = scale * u
    drawRoundRect(
        Cream.copy(alpha = 0.85f),
        topLeft = Offset(x - 5f * k, baseY - 30f * k),
        size = Size(10f * k, 30f * k),
        cornerRadius = CornerRadius(4f * k),
    )
    drawArc(cap, 180f, 180f, true, topLeft = Offset(x - 22f * k, baseY - 44f * k), size = Size(44f * k, 30f * k))
    drawCircle(Cream.copy(alpha = 0.8f), 2.5f * k, Offset(x - 8f * k, baseY - 36f * k))
    drawCircle(Cream.copy(alpha = 0.8f), 3f * k, Offset(x + 5f * k, baseY - 39f * k))
    drawCircle(Cream.copy(alpha = 0.8f), 2f * k, Offset(x + 13f * k, baseY - 33f * k))
}
