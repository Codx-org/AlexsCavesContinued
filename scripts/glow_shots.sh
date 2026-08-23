#!/bin/bash
# Capture N consecutive dev-client frames of one entity against pure sky, so an ANIMATED shader
# averages out. See docs/notes/gotchas-runtime.md, "Driving the dev client without a mouse" #12:
# a single frame of the irradiated glow varies 1.8x on its own sine, so a one-frame A/B is noise.
#
#   scripts/glow_shots.sh <node> <time> <entity-id> <outprefix> [n]
#
# Needs: ydotoold running, the node's client already in a world (--quickPlaySingleplayer),
# run/options.txt with pauseOnLostFocus:false, and the world's level.dat with allowCommands=1.
# mcdrive.py (X focus via _NET_ACTIVE_WINDOW) is expected beside this script.
set -u
NODE="$1"; T="$2"; ENT="$3"; PRE="$4"; N="${5:-8}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RUN="$ROOT/versions/$NODE/run"
k(){ ydotool key "$1:1" "$1:0"; sleep "${2:-0.5}"; }
chat(){ k 20 0.6; ydotool type -- "$1"; sleep 0.5; k 28 0.7; }   # 20 = t, 28 = Enter
python3 "$(dirname "$0")/mcdrive.py" focus >/dev/null 2>&1; sleep 0.8
chat "/gamerule doDaylightCycle false"
chat "/gamerule doWeatherCycle false"
chat "/weather clear"
chat "/time set $T"
chat "/gamemode spectator"
chat "/kill @e[type=$ENT]"
chat "/tp @s ~ 220 ~ 0 -25"            # above the terrain, looking slightly up: pure sky behind
sleep 1
chat "/summon $ENT ^ ^-0.2 ^3.5 {NoGravity:1b,NoAI:1b,Silent:1b,PersistenceRequired:1b}"
sleep 2
k 59 1.0                                # F1: hide GUI
for i in $(seq 1 "$N"); do k 60 0.55; done   # F2: screenshot
k 59 0.5
sleep 1
i=0
ls -t "$RUN/screenshots"/*.png | head -"$N" | tac | while IFS= read -r f; do
  i=$((i+1)); cp "$f" "${PRE}_$i.png"; echo "  ${PRE}_$i.png <- $(basename "$f")"
done
echo "captured $N frames -> ${PRE}_*.png"
