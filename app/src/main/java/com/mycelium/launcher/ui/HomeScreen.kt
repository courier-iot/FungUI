package com.mycelium.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.mycelium.launcher.data.AppEntry
import com.mycelium.launcher.data.GridMode
import com.mycelium.launcher.data.HomeItem
import com.mycelium.launcher.data.KIND_APP
import com.mycelium.launcher.data.LauncherState
import com.mycelium.launcher.ui.theme.Cream
import com.mycelium.launcher.ui.widgets.WidgetCard
import com.mycelium.launcher.util.launchApp
import com.mycelium.launcher.util.openAppInfo
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    state: LauncherState,
    apps: Map<String, AppEntry>,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = state.currentPage.coerceIn(0, (state.pages.size - 1).coerceAtLeast(0))
    ) { state.pages.size }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { state.currentPage = it }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val areaW = constraints.maxWidth.toFloat()
        val areaH = constraints.maxHeight.toFloat()

        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) { pageIndex ->
                HomePage(state, pageIndex, apps)
            }
            PageDots(state.pages.size, pagerState.currentPage, state.accent)
        }

        // Sprite / critter layer will sit here, above the pages and below the leaf button.

        LeafButton(
            state = state,
            areaW = areaW,
            areaH = areaH,
            onTap = onOpenDrawer,
            onDoubleTap = onOpenSettings,
        )
    }
}

@Composable
private fun HomePage(state: LauncherState, pageIndex: Int, apps: Map<String, AppEntry>) {
    val items = state.pages.getOrNull(pageIndex) ?: emptyList()
    val mode = state.gridMode
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 4.dp)) {
        val areaW = constraints.maxWidth.toFloat()
        val areaH = constraints.maxHeight.toFloat()
        for (item in items) {
            key(item.id) {
                HomeItemView(state, pageIndex, item, mode, areaW, areaH, apps[item.appKey])
            }
        }
    }
}

@Composable
private fun HomeItemView(
    state: LauncherState,
    pageIndex: Int,
    item: HomeItem,
    mode: GridMode,
    areaW: Float,
    areaH: Float,
    app: AppEntry?,
) {
    if (item.kind == KIND_APP && app == null) return // app uninstalled or not loaded yet

    val context = LocalContext.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    val cellW = areaW / mode.cols
    val cellH = areaH / mode.rows
    val (spanW, spanH) = item.span(mode)
    val itemW = cellW * spanW
    val itemH = cellH * spanH
    val baseX = if (mode == GridMode.FREE) item.fx * areaW else item.col * cellW
    val baseY = if (mode == GridMode.FREE) item.fy * areaH else item.row * cellH

    var drag by remember { mutableStateOf(Offset.Zero) }
    var dragging by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    // Hold → drag to move. Hold without moving → item menu.
    val dragModifier = Modifier.pointerInput(item, mode, areaW, areaH) {
        detectDragGesturesAfterLongPress(
            onDragStart = {
                dragging = true
                drag = Offset.Zero
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            onDrag = { change, amount ->
                change.consume()
                drag += amount
            },
            onDragEnd = {
                dragging = false
                if (drag.getDistance() < 16f) {
                    menuOpen = true
                } else {
                    val nx = baseX + drag.x
                    val ny = baseY + drag.y
                    if (mode == GridMode.FREE) {
                        state.moveFree(
                            pageIndex, item.id,
                            (nx / areaW).coerceIn(0f, (1f - itemW / areaW).coerceAtLeast(0f)),
                            (ny / areaH).coerceIn(0f, (1f - itemH / areaH).coerceAtLeast(0f)),
                        )
                    } else {
                        state.moveGrid(pageIndex, item.id, (nx / cellW).roundToInt(), (ny / cellH).roundToInt())
                    }
                }
                drag = Offset.Zero
            },
            onDragCancel = {
                dragging = false
                drag = Offset.Zero
            },
        )
    }

    Box(
        Modifier
            .offset { IntOffset((baseX + drag.x).roundToInt(), (baseY + drag.y).roundToInt()) }
            .size(with(density) { itemW.toDp() }, with(density) { itemH.toDp() })
            .zIndex(if (dragging) 1f else 0f)
            .graphicsLayer {
                val s = if (dragging) 1.08f else 1f
                scaleX = s
                scaleY = s
                alpha = if (dragging) 0.85f else 1f
            }
    ) {
        if (item.kind == KIND_APP && app != null) {
            AppIcon(
                app = app,
                cellW = cellW,
                cellH = cellH,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(app.key) {
                        detectTapGestures(onTap = { launchApp(context, app.pkg, app.cls) })
                    }
                    .then(dragModifier),
            )
        } else {
            WidgetCard(state, item, state.accent, dragModifier)
        }

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            if (app != null) {
                DropdownMenuItem(
                    text = { Text("App info") },
                    onClick = { menuOpen = false; openAppInfo(context, app.pkg) },
                )
            }
            DropdownMenuItem(
                text = { Text("Remove from home") },
                onClick = { menuOpen = false; state.removeItem(pageIndex, item.id) },
            )
        }
    }
}

@Composable
private fun AppIcon(app: AppEntry, cellW: Float, cellH: Float, modifier: Modifier) {
    val density = LocalDensity.current
    val iconDp = with(density) { (minOf(cellW, cellH * 0.7f) * 0.72f).toDp() }.coerceAtMost(56.dp)
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(bitmap = app.icon, contentDescription = app.label, modifier = Modifier.size(iconDp))
        Spacer(Modifier.height(3.dp))
        Text(
            text = app.label,
            color = Cream,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.7f), Offset(1f, 1f), 4f)),
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

@Composable
private fun PageDots(count: Int, current: Int, accent: Color) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { i ->
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (i == current) 9.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (i == current) accent else Cream.copy(alpha = 0.35f))
            )
        }
    }
}
