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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mycelium.launcher.data.AppEntry
import com.mycelium.launcher.ui.theme.Bark
import com.mycelium.launcher.ui.theme.Cream
import com.mycelium.launcher.util.openAppInfo
import kotlinx.coroutines.launch

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
    val headerIndex = remember(groups) {
        val m = mutableMapOf<Char, Int>()
        var i = 0
        groups.forEach { (k, v) -> m[k] = i; i += 1 + v.size }
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
                groups.forEach { (letter, list) ->
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
                    items(list, key = { it.key }) { app ->
                        DrawerRow(app, onLaunch, onAddToHome)
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
private fun DrawerRow(app: AppEntry, onLaunch: (AppEntry) -> Unit, onAddToHome: (AppEntry) -> Unit) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = { onLaunch(app) }, onLongClick = { menu = true })
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(14.dp))
            Text(app.label, color = Cream, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
