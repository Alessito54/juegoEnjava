#!/usr/bin/env bash
set -euo pipefail

SRC=src
OUT=bin
DIST=dist
MAIN=org.drgnst.game.Main

rm -rf "$OUT" "$DIST"
mkdir -p "$OUT" "$DIST"

echo "Compiling Java sources..."
find "$SRC" -name '*.java' > sources.txt
javac -d "$OUT" @sources.txt
rm sources.txt

echo "Creating jar..."
jar cfe "$DIST/game.jar" "$MAIN" -C "$OUT" .

for d in image sonidos res levels textures; do
  if [ -d "$d" ]; then
    jar uf "$DIST/game.jar" -C "$d" . || true
  fi
done

# Include any top-level wav files (like DoomEternalOST.wav) into the jar
for f in *.wav; do
  if [ -f "$f" ]; then
    jar uf "$DIST/game.jar" "$f" || true
  fi
done

echo "Build complete: $DIST/game.jar"
