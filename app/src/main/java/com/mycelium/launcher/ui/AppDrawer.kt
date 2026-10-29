package com.mycelium.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mycelium.launcher.data.AppEntry
import com.mycelium.launcher.ui.theme.Bark
import com.mycelium.launcher.ui.theme.Cream
import com.mycelium.launcher.util.openAppInfo
import kotlinx.coroutines.launch

private const val COLUMNS = 4
private val RAIL: List<Char> = listOf('#') + ('A'..'Z').toList()

private fun letterOf(label: String): Char {
    val c = label.trim().firstOrNull()?.uppercaseChar()
    return if (c != null && c in 'A'..'Z') c else '#'
}

/** Alphabetical app archive with sticky letter headers and a scrubbable letter rail. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawer(
    apps: List<AppEntry>,
    accent: Color,
    onLaunch: (AppEntry) -> Unit,
    onAddToHome: (AppEntry) -> Unit,
    onClose: () -> Unit,
) {
    BackHandler { onClose() }

    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) {
        if (query.isBlank()) apps else apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
    }
    val groups = remember(filtered) { filtered.groupBy { letterOf(it.label) }.toSortedMap() }
    // Each letter = one sticky header + rows of COLUMNS icons.
    val rows = remember(groups) { groups.mapValues { (_, v) -> v.chunked(COLUMNS) } }
    val headerIndex = remember(rows) {
        val m = mutableMapOf<Char, Int>()
        var i = 0
        rows.forEach { (k, v) -> m[k] = i; i += 1 + v.size }
        m
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var railLetter by remember { mutableStateOf<Char?>(null) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Bark.copy(alpha = 0.95f))
            .systemBarsPadding()
    ) {
        Column(Modifier.fillMaxSize().padding(end = 34.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, top = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "App archive",
                    color = Cream,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onClose) { Text("Close", color = accent) }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search the undergrowth…") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    unfocusedBorderColor = Cream.copy(alpha = 0.3f),
                    focusedTextColor = Cream,
                    unfocusedTextColor = Cream,
                    cursorColor = accent,
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                rows.forEach { (letter, letterRows) ->
                    stickyHeader(key = "h_$letter") {
                        Text(
                            letter.toString(),
                            color = accent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Bark)
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                        )
                    }
                    items(letterRows, key = { row -> "r_" + row.first().key }) { row ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                            row.forEach { app ->
                                DrawerCell(app, accent, onLaunch, onAddToHome, Modifier.weight(1f))
                            }
                            repeat(COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }

        LetterRail(
            available = groups.keys,
            accent = accent,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(0.8f)
                .width(30.dp),
            onPick = { letter ->
                railLetter = letter
                val target = groups.keys.firstOrNull { it >= letter } ?: groups.keys.lastOrNull()
                val idx = target?.let { headerIndex[it] }
                if (idx != null) scope.launch { listState.scrollToItem(idx) }
            },
            onRelease = { railLetter = null },
        )

        railLetter?.let { letter ->
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center,
            ) {
                Text(letter.toString(), color = Bark, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerCell(
    app: AppEntry,
    accent: Color,
    onLaunch: (AppEntry) -> Unit,
    onAddToHome: (AppEntry) -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    Box(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .combinedClickable(onClick = { onLaunch(app) }, onLongClick = { menu = true })
                .padding(vertical = 10.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(50.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                app.label,
                color = Cream,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("Add to home page") }, onClick = { menu = false; onAddToHome(app) })
            DropdownMenuItem(text = { Text("App info") }, onClick = { menu = false; openAppInfo(context, app.pkg) })
        }
    }
}

@Composable
private fun LetterRail(
    available: Set<Char>,
    accent: Color,
    modifier: Modifier,
    onPick: (Char) -> Unit,
    onRelease: () -> Unit,
) {
    val pick by rememberUpdatedState(onPick)
    val release by rememberUpdatedState(onRelease)
    Column(
        modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown()
                fun letterAt(y: Float): Char {
                    val i = ((y / size.height) * RAIL.size).toInt().coerceIn(0, RAIL.lastIndex)
                    return RAIL[i]
                }
                down.consume()
                pick(letterAt(down.position.y))
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    change.consume()
                    pick(letterAt(change.position.y))
                }
                release()
            }
        },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RAIL.forEach { c ->
            Text(
                c.toString(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (c in available) accent else Cream.copy(alpha = 0.25f),
            )
        }
    }
}
