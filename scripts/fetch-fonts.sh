#!/usr/bin/env bash
# Downloads the six bundled fonts (all SIL Open Font License) from the Google Fonts repo
# into app/src/main/assets/fonts. Runs during the GitHub build. A font that fails to
# download is skipped — the app just shows it as "not included in this build".
set -u
DEST="app/src/main/assets/fonts"
BASE="https://github.com/google/fonts/raw/main/ofl"
mkdir -p "$DEST"

fetch() {
  local out="$1"; shift
  for path in "$@"; do
    if curl -fsSL --retry 2 -o "$DEST/$out" "$BASE/$path"; then
      echo "✓ $out  ($path)"
      return 0
    fi
  done
  rm -f "$DEST/$out"
  echo "✗ $out — not found, skipping"
}

fetch lora.ttf       "lora/Lora%5Bwght%5D.ttf" "lora/Lora-Regular.ttf"
fetch cormorant.ttf  "cormorantgaramond/CormorantGaramond%5Bwght%5D.ttf" "cormorantgaramond/CormorantGaramond-Regular.ttf" "cormorant/Cormorant%5Bwght%5D.ttf"
fetch caveat.ttf     "caveat/Caveat%5Bwght%5D.ttf" "caveat/Caveat-Regular.ttf"
fetch quicksand.ttf  "quicksand/Quicksand%5Bwght%5D.ttf" "quicksand/Quicksand-Regular.ttf"
fetch cinzel.ttf     "cinzel/Cinzel%5Bwght%5D.ttf" "cinzel/Cinzel-Regular.ttf"
fetch josefin.ttf    "josefinsans/JosefinSans%5Bwght%5D.ttf" "josefinsans/JosefinSans-Regular.ttf"
exit 0
