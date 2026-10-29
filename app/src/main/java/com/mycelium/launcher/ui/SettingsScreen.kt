package com.mycelium.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mycelium.launcher.data.GridMode
import com.mycelium.launcher.data.LauncherState
import com.mycelium.launcher.data.WidgetType
import com.mycelium.launcher.ui.theme.Amanita
import com.mycelium.launcher.ui.theme.Bark
import com.mycelium.launcher.ui.theme.Chanterelle
import com.mycelium.launcher.ui.theme.Cream
import com.mycelium.launcher.ui.theme.Fern
import com.mycelium.launcher.ui.theme.Glowcap
import com.mycelium.launcher.ui.theme.Lichen
import com.mycelium.launcher.ui.theme.Moss
import com.mycelium.launcher.ui.theme.Rust
import com.mycelium.launcher.ui.theme.Soil
import com.mycelium.launcher.ui.theme.Spore
import com.mycelium.launcher.util.openHomeSettings
import com.mycelium.launcher.util.toast

@Composable
fun SettingsScreen(state: LauncherState, onClose: () -> Unit) {
    BackHandler { onClose() }
    val context = LocalContext.current
    val accent = state.accent

    Column(
        Modifier
            .fillMaxSize()
            .background(Bark.copy(alpha = 0.97f))
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Mycelium settings",
                color = Cream,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClose) { Text("Done", color = accent) }
        }

        Section("Accent colour", accent) {
            AccentPicker(initial = accent, onChange = state::updateAccent)
        }

        Section("Home layout", accent) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GridMode.entries.forEach { mode ->
                    FilterChip(
                        selected = state.gridMode == mode,
                        onClick = { state.updateGrid(mode) },
                        label = { Text(mode.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent,
                            selectedLabelColor = Bark,
                            labelColor = Cream,
                        ),
                    )
                }
            }
            Hint("Hold an icon to move it. Hold without moving for remove / app info.")
        }

        Section("Home pages", accent) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = {
                    if (!state.removeLastPage()) context.toast("Clear the last page first")
                }) { Text("−", color = Cream, fontSize = 18.sp) }
                Text(
                    "${state.pages.size} page${if (state.pages.size == 1) "" else "s"}",
                    color = Cream,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                OutlinedButton(onClick = { state.addPage() }) { Text("+", color = Cream, fontSize = 18.sp) }
            }
        }

        Section("Widgets", accent) {
            Hint("Added to the page you were on (page ${state.currentPage + 1}).")
            Spacer(Modifier.height(6.dp))
            WidgetType.entries.forEach { type ->
                OutlinedButton(
                    onClick = {
                        val ok = state.addWidget(state.currentPage, type)
                        context.toast(if (ok) "${type.label} added" else "No room on that page")
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                ) { Text("Add ${type.label}", color = Cream) }
            }
            Hint("YouTube Music and Proton Calendar are placeholders for now. Custom clock is waiting on your design.")
        }

        Section("Backdrop", accent) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Use system wallpaper instead of forest floor", color = Cream, modifier = Modifier.weight(1f))
                Switch(
                    checked = state.useWallpaper,
                    onCheckedChange = state::updateWallpaper,
                    colors = SwitchDefaults.colors(checkedTrackColor = accent, checkedThumbColor = Cream),
                )
            }
        }

        Section("Leaf button", accent) {
            Hint("Tap: app archive · Double-tap: settings · Hold & drag: move")
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = { state.resetButton() }) { Text("Reset position", color = Cream) }
        }

        Section("System", accent) {
            Button(
                onClick = { openHomeSettings(context) },
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Bark),
            ) { Text("Set Mycelium as default home app") }
        }

        Section("Critters", accent) {
            Hint("Little sprites that wander your home screen and interact with it — coming soon.")
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun Section(title: String, accent: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Soil.copy(alpha = 0.9f))
            .padding(16.dp)
    ) {
        Text(title, color = accent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, color = Cream.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
}

private val SWATCHES = listOf(
    "Moss" to Moss,
    "Fern" to Fern,
    "Lichen" to Lichen,
    "Chanterelle" to Chanterelle,
    "Rust" to Rust,
    "Amanita" to Amanita,
    "Spore" to Spore,
    "Glowcap" to Glowcap,
)

private fun hsvOf(c: Color): FloatArray =
    FloatArray(3).also { android.graphics.Color.colorToHSV(c.toArgb(), it) }

/** Earthy presets + hue / saturation / brightness sliders. */
@Composable
fun AccentPicker(initial: Color, onChange: (Color) -> Unit) {
    val start = remember { hsvOf(initial) }
    var hue by remember { mutableFloatStateOf(start[0].coerceIn(0f, 359.9f)) }
    var sat by remember { mutableFloatStateOf(start[1]) }
    var value by remember { mutableFloatStateOf(start[2].coerceIn(0.15f, 1f)) }
    val current = Color.hsv(hue, sat, value)

    fun push() = onChange(Color.hsv(hue, sat, value))

    Row(Modifier.horizontalScroll(rememberScrollState())) {
        SWATCHES.forEach { (name, c) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(end = 10.dp)
                    .clickable {
                        val f = hsvOf(c)
                        hue = f[0].coerceIn(0f, 359.9f)
                        sat = f[1]
                        value = f[2].coerceIn(0.15f, 1f)
                        push()
                    },
            ) {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(c)
                        .border(2.dp, if (c.toArgb() == current.toArgb()) Cream else Color.Transparent, CircleShape)
                )
                Text(name, fontSize = 10.sp, color = Cream.copy(alpha = 0.7f))
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(current))
        Spacer(Modifier.width(12.dp))
        Text("#%06X".format(current.toArgb() and 0xFFFFFF), color = Cream)
    }

    Spacer(Modifier.height(8.dp))
    Box(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                Brush.horizontalGradient(
                    (0..6).map { Color.hsv(minOf(it * 60f, 359.9f), sat.coerceAtLeast(0.3f), value.coerceAtLeast(0.5f)) }
                )
            )
    )

    val sliderColors = SliderDefaults.colors(
        thumbColor = current,
        activeTrackColor = current,
        inactiveTrackColor = Cream.copy(alpha = 0.2f),
    )
    Text("Hue", color = Cream.copy(alpha = 0.7f), fontSize = 12.sp)
    Slider(value = hue, onValueChange = { hue = it; push() }, valueRange = 0f..359.9f, colors = sliderColors)
    Text("Saturation", color = Cream.copy(alpha = 0.7f), fontSize = 12.sp)
    Slider(value = sat, onValueChange = { sat = it; push() }, valueRange = 0f..1f, colors = sliderColors)
    Text("Brightness", color = Cream.copy(alpha = 0.7f), fontSize = 12.sp)
    Slider(value = value, onValueChange = { value = it; push() }, valueRange = 0.15f..1f, colors = sliderColors)
}
