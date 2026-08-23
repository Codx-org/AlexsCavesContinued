#!/bin/bash
# block_shots.sh <node> <block-id> <outprefix> [n]
#
# Matched-rig capture of ONE block against open sky, for cross-node A/B of a block or
# block-entity renderer. Companion to glow_shots.sh (entities) and block_occlusion_test.sh.
#
# Fixes everything that makes two screenshots comparable: daylight cycle off, a pinned time,
# spectator, an exact position AND rotation (so the block lands dead centre), and the 5x5x5
# volume around it cleared first. Takes n consecutive frames because anything animated
# (the ambersol shine, any GameTime shader) cannot be judged from a single frame -- see
# docs/notes/gotchas-runtime.md, rig rule 12.
#
# The client must already be running with the rig keybinds and pauseOnLostFocus:false; see
# the rig section of gotchas-runtime.md. Needs mcdrive.py beside it and ydotool.
#
#   ./scripts/block_shots.sh 26.2-fabric alexscaves:ambersol /tmp/as262 4
#   ./scripts/block_shots.sh 26.2-fabric air               /tmp/bg262 2   # background frame,
#                                                                        # to diff out and
#                                                                        # recover the added light
NODE="$1"; BLK="$2"; OUT="$3"; N="${4:-4}"
RUN="/home/niels/Documents/Projects/Minecraft Mods/AlexsCavesContinued/versions/$NODE/run"
k(){ ydotool key "$1:1" "$1:0"; sleep "${2:-0.5}"; }
chat(){ k 20 0.6; ydotool type -- "$1"; sleep 0.5; k 28 0.8; }
python3 "$(dirname "$0")/mcdrive.py" focus >/dev/null 2>&1; sleep 0.6
chat "/gamerule doDaylightCycle false"
chat "/time set 18000"
chat "/gamemode spectator"
chat "/tp @s 800 220 1160 0 0"
sleep 1.2
chat "/fill 798 218 1163 802 222 1167 air"
chat "/setblock 800 220 1165 $BLK"
sleep 2.0
k 59 1.0
for i in $(seq 1 "$N"); do k 60 0.55; done
k 59 0.6; sleep 1.0
c=0
ls -t "$RUN/screenshots"/*.png | head -"$N" | tac | while IFS= read -r f; do
  c=$(( c + 1 ))
  cp "$f" "${OUT}_$c.png"
done
ls -t "$RUN/screenshots"/*.png | head -"$N" | tac | nl
echo "$BLK -> ${OUT}_*.png"
