#!/usr/bin/env bash
# Re-bake the launch-card player skin: 24 rotation frames from NMSR for a given player,
# knee-cropped and scaled, written to launcher/src/main/resources/skin/rot/NN.png.
#
# The launcher currently ships a fixed reference skin; this is how those frames are produced.
# When real Microsoft auth lands, the same NMSR fetch happens at runtime keyed by the signed-in
# UUID instead of being pre-baked (see RotatableSkin.kt).
#
# Usage:  tools/skin-bake/bake-skin.sh <minecraft-username-or-uuid>
# Example: tools/skin-bake/bake-skin.sh Bihzeh
set -euo pipefail

PLAYER="${1:?usage: bake-skin.sh <username-or-uuid>}"
FRAMES=24            # yaw steps (360 / 24 = 15 deg each)
SRC_WIDTH=900        # NMSR render width before cropping (higher = crisper)
OUT_W=720; OUT_H=928 # final per-frame size (matches the card's 0.776 aspect)
KNEE=0.84            # fraction of model height kept (head-top -> knees)

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
DEST="$ROOT/launcher/src/main/resources/skin/rot"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
mkdir -p "$TMP/raw"

echo "Fetching $FRAMES frames for '$PLAYER' from NMSR..."
for i in $(seq 0 $((FRAMES - 1))); do
  yaw=$((i * 360 / FRAMES))
  printf -v n "%02d" "$i"
  curl -fsS --max-time 30 -o "$TMP/raw/$n.png" \
    "https://nmsr.nickac.dev/fullbody/${PLAYER}?width=${SRC_WIDTH}&yaw=${yaw}"
done

echo "Cropping + scaling -> ${OUT_W}x${OUT_H} (knee=$KNEE)..."
javac -d "$TMP" "$(dirname "$0")/Bake.java"
java -Djava.awt.headless=true -cp "$TMP" Bake "$TMP/raw" "$DEST" "$OUT_W" "$OUT_H" "$KNEE"

echo "Done -> $DEST"
