package com.mycelium.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.mycelium.launcher.data.AppRepository
import com.mycelium.launcher.data.LauncherState
import com.mycelium.launcher.util.launchApp
import com.mycelium.launcher.util.toast

enum class Screen { HOME, DRAWER, SETTINGS }

@Composable
fun LauncherRoot(state: LauncherState, repo: AppRepository, homeSignal: Int) {
    val context = LocalContext.current
    var screen by remember { mutableStateOf(Screen.HOME) }

    // Pressing the system Home button while in the archive/settings returns to home.
    LaunchedEffect(homeSignal) { screen = Screen.HOME }

    val appMap by remember { derivedStateOf { repo.apps.associateBy { it.key } } }

    // Back does nothing on the home screen itself.
    BackHandler(enabled = screen == Screen.HOME) { }

    Box(Modifier.fillMaxSize()) {
        if (!state.useWallpaper) EarthBackdrop(state.accent)

        HomeScreen(
            state = state,
            apps = appMap,
            onOpenDrawer = { screen = Screen.DRAWER },
            onOpenSettings = { screen = Screen.SETTINGS },
        )

        AnimatedVisibility(
            visible = screen == Screen.DRAWER,
            enter = fadeIn() + slideInVertically { it / 8 },
            exit = fadeOut() + slideOutVertically { it / 8 },
        ) {
            AppDrawer(
                apps = repo.apps.toList(),
                accent = state.accent,
                onLaunch = {
                    launchApp(context, it.pkg, it.cls)
                    screen = Screen.HOME
                },
                onAddToHome = { app ->
                    val ok = state.addApp(state.currentPage, app.pkg, app.cls)
                    context.toast(if (ok) "Planted on page ${state.currentPage + 1}" else "No room on this page")
                },
                onClose = { screen = Screen.HOME },
            )
        }

        AnimatedVisibility(
            visible = screen == Screen.SETTINGS,
            enter = fadeIn() + slideInVertically { it / 8 },
            exit = fadeOut() + slideOutVertically { it / 8 },
        ) {
            SettingsScreen(state = state, onClose = { screen = Screen.HOME })
        }
    }
}
