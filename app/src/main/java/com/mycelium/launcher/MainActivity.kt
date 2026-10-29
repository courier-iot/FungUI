package com.mycelium.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.mycelium.launcher.data.AppRepository
import com.mycelium.launcher.data.FontRepository
import com.mycelium.launcher.data.LauncherState
import com.mycelium.launcher.ui.LauncherRoot
import com.mycelium.launcher.ui.theme.MyceliumTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var state: LauncherState
    private lateinit var repo: AppRepository
    private lateinit var fonts: FontRepository
    private var homeSignal by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        state = LauncherState(applicationContext)
        repo = AppRepository(applicationContext)
        fonts = FontRepository(applicationContext)
        setContent {
            val font = remember(state.fontId) { fonts.family(state.fontId) }
            MyceliumTheme(accent = state.accent, font = font) {
                LauncherRoot(state = state, repo = repo, fonts = fonts, homeSignal = homeSignal)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-scan apps so installs/uninstalls show up.
        lifecycleScope.launch { repo.refresh() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        homeSignal++
    }
}
