# 🍄 Mycelium Launcher

An earthy Android home-screen launcher — vines, fungi, and a rose button.

## Getting the APK (no Android Studio needed)

1. Create a new GitHub repo and upload **everything in this folder** (including the hidden `.github` folder — it holds the build recipe).
2. Open the repo's **Actions** tab. The **Build APK** workflow runs on every push (or press *Run workflow*).
3. When it finishes (~5 min), open the run and download **mycelium-launcher-debug** at the bottom. It downloads as a `.zip`.
4. Unzip it on your phone → tap `app-debug.apk` → allow "install unknown apps" for your file manager.
5. Press your phone's Home button and pick **Mycelium** (or use Settings → *Set Mycelium as default home app*).

The debug APK is signed automatically with a debug key, so no signing setup is needed.

## What works now

| Feature | How |
|---|---|
| Rose button | Tap → app archive · Double-tap → settings · Hold & drag → move it anywhere. Tinted by your accent colour |
| App archive | Icon grid (4 across, names underneath), A–Z sections, scrubbable letter rail, search |
| Add to home | Long-press an app in the archive → *Add to home page* |
| Multiple home pages | Swipe to flip between pages (card-flip). Add/remove pages in settings |
| Layouts | 4×9, 5×10, or Free place |
| Move / remove icons | Hold an icon and drag. Hold without moving → *Remove / App info* |
| Widgets | Notes works. Hold the header to move, drag the ◢ corner to resize |
| Accent colour | Earthy presets + hue / saturation / brightness sliders |
| Fonts | 6 bundled (Lora, Cormorant Garamond, Caveat, Quicksand, Cinzel, Josefin Sans) + upload your own .ttf/.otf |
| Backdrops | 10 bundled wallpapers, the drawn forest floor, or your phone wallpaper |

The six fonts are downloaded by the GitHub build (`scripts/fetch-fonts.sh`) from the Google Fonts repository. They're all under the SIL Open Font License.

## Placeholders / roadmap

- **Custom clock** — waiting on design.
- **YouTube Music playlists** — YT Music has no public playback API. Likely approach: a `NotificationListenerService` to control the active YT Music media session + deep links to open specific playlists.
- **Proton Calendar** — Proton has no public API, but calendars can be shared as an ICS link. Plan: paste the ICS link in settings, the widget fetches and parses it periodically.
- **Critters** — sprite layer sitting between the home pages and the rose button (see the comment in `HomeScreen.kt`).

## Project layout

```
app/src/main/java/com/mycelium/launcher/
  MainActivity.kt          – entry point, app list refresh
  data/LauncherState.kt    – settings, pages, placement, persistence
  data/AppRepository.kt    – installed-app scanning
  data/HomeModel.kt        – grid modes, widget types, home items
  data/Fonts.kt            – bundled + uploaded fonts
  data/Wallpapers.kt       – the 10 bundled wallpapers
  ui/LauncherRoot.kt       – switches between home / archive / settings
  ui/HomeScreen.kt         – pages, grid & free layout, drag to move
  ui/RoseButton.kt         – the rose button + its shape
  ui/Backdrop.kt           – wallpaper / forest floor / phone wallpaper
  ui/AppDrawer.kt          – the app archive
  ui/SettingsScreen.kt     – settings + colour picker
  ui/EarthBackdrop.kt      – vines & mushrooms backdrop
  ui/widgets/Widgets.kt    – widget frame, notes, placeholders
```

Built with Kotlin + Jetpack Compose. minSdk 26 (Android 8.0).
