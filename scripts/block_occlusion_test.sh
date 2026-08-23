#!/bin/bash
# block_occlusion_test.sh <node> <outprefix>
#
# Depth-test regression check: seals an ambersol inside a solid 5x5x5 stone box and photographs
# the box. A correct node shows PLAIN STONE. Any glow, ray or silhouette bleeding through means
# that node's render types are failing their depth test -- which is exactly what 26.2's
# reversed-Z flip caused before ACPipelineState.NEARER_OR_EQUAL was banded (see the class doc).
#
# The ambersol is used because its shine is the mod's loudest additive, non-depth-writing draw,
# so it fails visibly; the check generalises to any block whose renderer you suspect.
#
#   ./scripts/block_occlusion_test.sh 26.2-fabric /tmp/box262
NODE="$1"; OUT="$2"
RUN="/home/niels/Documents/Projects/Minecraft Mods/AlexsCavesContinued/versions/$NODE/run"
k(){ ydotool key "$1:1" "$1:0"; sleep "${2:-0.5}"; }
chat(){ k 20 0.6; ydotool type -- "$1"; sleep 0.5; k 28 0.8; }
python3 "$(dirname "$0")/mcdrive.py" focus >/dev/null 2>&1; sleep 0.6
chat "/gamerule doDaylightCycle false"
chat "/time set 18000"
chat "/gamemode spectator"
chat "/tp @s 800 220 1160 0 0"
sleep 1.2
chat "/fill 794 214 1161 806 226 1172 air"
chat "/fill 798 218 1163 802 222 1167 stone"
chat "/setblock 800 220 1165 alexscaves:ambersol"
sleep 2.5
k 59 1.0; k 60 0.6; k 60 0.6; k 59 0.6; sleep 1.0
c=0
ls -t "$RUN/screenshots"/*.png | head -2 | tac | while IFS= read -r f; do
  c=$(( c + 1 )); cp "$f" "${OUT}_$c.png"
done
echo "box -> ${OUT}_*.png"
