#!/usr/bin/env python3
"""Mean green-excess (g - (r+b)/2) over an annulus centred on the brightest green blob.

The A/B metric for this mod's additive post-effect glows. Feed it the frames
scripts/glow_shots.sh captured and compare the *mean over all frames*, never one frame --
see docs/notes/gotchas-runtime.md #12 and docs/notes/1.0.1-triage.md, report #5.

    python3 scripts/glow_ring.py frames_*.png

Frames must share a resolution: the annulus is in pixels, and a fresh run directory does not
give every node the same window size.
"""
import sys
from PIL import Image
import math
def ring(path, r0=45, r1=75):
    im = Image.open(path).convert('RGB')
    w,h = im.size
    px = im.load()
    # centroid of green-excess weighted
    best=None; sx=sy=sw=0.0
    for y in range(h):
        for x in range(w):
            r,g,b = px[x,y]
            e = g - (r+b)/2
            if e > 60:
                sx += x*e; sy += y*e; sw += e
    if sw==0: return None
    cx, cy = sx/sw, sy/sw
    tot=0.0; n=0
    for y in range(h):
        for x in range(w):
            d = math.hypot(x-cx, y-cy)
            if r0 <= d <= r1:
                r,g,b = px[x,y]
                tot += g - (r+b)/2; n+=1
    return cx, cy, tot/n, n
vals = []
for p in sys.argv[1:]:
    res = ring(p)
    if res is None:
        print(f"{p}: no green blob found"); continue
    vals.append(res[2])
    print(f"{p}: centroid=({res[0]:.1f},{res[1]:.1f}) ring green-excess={res[2]:.2f} n={res[3]}")
if len(vals) > 1:
    m = sum(vals) / len(vals)
    print(f"-- {len(vals)} frames: mean={m:.2f} min={min(vals):.2f} max={max(vals):.2f} "
          f"spread={max(vals)/max(min(vals), 1e-6):.2f}x")
    print("-- compare MEANS across nodes, never single frames (gotchas-runtime #12)")
