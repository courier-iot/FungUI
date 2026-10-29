package com.mycelium.launcher.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mycelium.launcher.data.HomeItem
import com.mycelium.launcher.data.LauncherState
import com.mycelium.launcher.data.WidgetType
import com.mycelium.launcher.ui.theme.Cream
import com.mycelium.launcher.ui.theme.Soil

/** Common frame for home widgets. The header strip is the drag handle (hold to move / remove). */
@Composable
fun WidgetCard(
    state: LauncherState,
    item: HomeItem,
    accent: Color,
    handleModifier: Modifier,
    resizeModifier: Modifier,
) {
    val type = WidgetType.from(item.widget) ?: return
    val shape = RoundedCornerShape(18.dp)
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(4.dp)
                .clip(shape)
                .background(Soil.copy(alpha = 0.82f))
                .border(1.dp, accent.copy(alpha = 0.45f), shape)
        ) {
            Row(
                Modifier.fillMaxWidth().then(handleModifier).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "❦  ${type.label}",
                    color = accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Text("⋮⋮", color = Cream.copy(alpha = 0.45f), fontSize = 12.sp)
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (type) {
                    WidgetType.NOTES -> NotesBody(state, item.id, accent)
                    WidgetType.MUSIC -> Placeholder("Playlist controls will grow here.")
                    WidgetType.CALENDAR -> Placeholder("Proton Calendar events will sprout here.")
                }
            }
        }
        // Resize grip (bottom-right corner)
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .size(30.dp)
                .then(resizeModifier),
            contentAlignment = Alignment.Center,
        ) {
            Text("◢", color = accent.copy(alpha = 0.8f), fontSize = 14.sp)
        }
    }
}

@Composable
private fun NotesBody(state: LauncherState, id: Long, accent: Color) {
    var text by remember(id) { mutableStateOf(state.getNote(id)) }
    BasicTextField(
        value = text,
        onValueChange = {
            text = it
            state.setNote(id, it)
        },
        textStyle = LocalTextStyle.current.copy(color = Cream, fontSize = 14.sp),
        cursorBrush = SolidColor(accent),
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp),
        decorationBox = { inner ->
            Box {
                if (text.isEmpty()) {
                    Text("Jot something down…", color = Cream.copy(alpha = 0.4f), fontSize = 14.sp)
                }
                inner()
            }
        },
    )
}

@Composable
private fun Placeholder(message: String) {
    Box(Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center) {
        Text(
            message,
            color = Cream.copy(alpha = 0.55f),
            fontSize = 12.sp,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
        )
    }
}
