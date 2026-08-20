#!/usr/bin/env python3
"""Generate the Alex's Caves Continued store icon.

The centre of the icon *is* upstream's logo, pixel for pixel -- this mod is a
continuation, and the icon should say so at a glance. Around it this script
draws the part that is ours: a cave-rock frame with six gems set into it, one
per cave biome (magnetic, primordial, toxic, abyssal, forlorn, candy), lit by
whatever the logo's own edge happens to be doing at that point so the two read
as one image rather than as a sticker on a plaque.

Upstream art: ``assets/alexscaves/textures/misc/mod_logo.png``, from
AlexModGuy/AlexsCaves, LGPL-3.0 -- the same licence this continuation carries,
so incorporating it is same-licence rather than merely compatible-licence.

⚠ That file is 144x144 but is natively **48x48** pixel art upscaled 3x. It is
downsampled back to its native grid here rather than resized, so not one
upstream pixel is resampled or lost.

Outputs (paths relative to the repo root):

    icon-source.png   64x64    the master; edit this script, not the PNG
    icon.png          512x512  store icon (Modrinth / CurseForge), an exact 8x
    src/main/resources/assets/alexscaves/icon.png
                      256x256  the in-jar logo every manifest's `logoFile`
                               points at, an exact 4x -- so the mod list and
                               the store pages show the same art

Deterministic: the rock noise is seeded, so re-running never shifts a pixel.
"""
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
UPSTREAM = ROOT / "src/main/resources/assets/alexscaves/textures/misc/mod_logo.png"

SIZE = 64          # logical grid
BORDER = 8         # rock frame, all four sides
SCALE = 8          # 64 * 8 == 512
SEED = 20250819

ROCK_HI   = (104, 94, 88)
ROCK_LO   = (40, 34, 36)
ROCK_DEEP = (18, 15, 18)
EMBER     = (255, 150, 58)

# the six cave biomes, in the order the mod registers them; positions are in
# the rock frame, so none of them covers any upstream pixel
BIOME_GEMS = [
    ("magnetic",   (232, 104, 44), (2, 12)),
    ("primordial", (124, 190, 78), (2, 34)),
    ("toxic",      (206, 220, 62), (3, 55)),
    ("abyssal",    (64, 164, 216), (60, 11)),
    ("forlorn",    (162, 106, 208),(60, 33)),
    ("candy",      (244, 122, 178),(59, 55)),
]

rng = random.Random(SEED)


def lerp(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(round(x + (y - x) * t) for x, y in zip(a, b))


def clamp(v, lo=0.0, hi=1.0):
    return max(lo, min(hi, v))


def luma(c):
    return (0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]) / 255.0


# -- upstream logo, back on its native grid --------------------------------
src = Image.open(UPSTREAM).convert("RGBA")
block = src.width // 48
assert src.width == src.height == 48 * block, f"unexpected logo size {src.size}"
logo = Image.new("RGBA", (48, 48))
for y in range(48):
    for x in range(48):
        logo.putpixel((x, y), src.getpixel((x * block, y * block)))
assert logo.size == (SIZE - 2 * BORDER, SIZE - 2 * BORDER)

L0, L1 = BORDER, SIZE - BORDER - 1     # inclusive bounds of the logo rect


def logo_edge_pixel(x, y):
    """The logo pixel nearest to a frame position -- what lights that spot."""
    return logo.getpixel((min(max(x, L0), L1) - L0, min(max(y, L0), L1) - L0))


px = [[None] * SIZE for _ in range(SIZE)]

# -- the rock frame --------------------------------------------------------
for x in range(SIZE):
    for y in range(SIZE):
        if L0 <= x <= L1 and L0 <= y <= L1:
            px[x][y] = logo.getpixel((x - L0, y - L0))[:3]
            continue

        # base rock, lighter toward the middle of each edge, noisy throughout
        d_out = min(x, y, SIZE - 1 - x, SIZE - 1 - y) / float(BORDER)
        t = clamp(0.30 + 0.42 * d_out + rng.uniform(-0.10, 0.10))
        c = lerp(ROCK_LO, ROCK_HI, t)

        # distance out from the logo, in whole pixels
        d = max(L0 - x, x - L1, L0 - y, y - L1)
        if d == 1:                       # a groove, so the frame reads as a frame
            c = lerp(c, ROCK_DEEP, 0.45)
        if d <= 5:                       # and the logo spills its own light onto it
            e = luma(logo_edge_pixel(x, y))
            c = lerp(c, EMBER, (1.0 - (d - 1) / 5.0) * 0.50 * e)

        px[x][y] = c

# -- the six biome gems ----------------------------------------------------
for _name, colour, (gx, gy) in BIOME_GEMS:
    for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1), (2, 1), (1, 2)):
        nx, ny = gx + dx, gy + dy
        if 0 <= nx < SIZE and 0 <= ny < SIZE and not (L0 <= nx <= L1 and L0 <= ny <= L1):
            px[nx][ny] = lerp(px[nx][ny], colour, 0.22)
    facets = ((0, 0, 0.30), (1, 0, -0.10), (0, 1, -0.10), (1, 1, -0.35))
    for dx, dy, shade in facets:
        nx, ny = gx + dx, gy + dy
        if 0 <= nx < SIZE and 0 <= ny < SIZE and not (L0 <= nx <= L1 and L0 <= ny <= L1):
            tint = (255, 255, 255) if shade > 0 else (0, 0, 0)
            px[nx][ny] = lerp(colour, tint, abs(shade))

# -- vignette, on the frame only -------------------------------------------
for x in range(SIZE):
    for y in range(SIZE):
        if L0 <= x <= L1 and L0 <= y <= L1:
            continue
        d = max(abs(x + 0.5 - SIZE / 2), abs(y + 0.5 - SIZE / 2)) / (SIZE / 2.0)
        if d > 0.88:
            px[x][y] = lerp(px[x][y], (0, 0, 0), (d - 0.88) / 0.12 * 0.40)

# -- write -----------------------------------------------------------------
icon = Image.new("RGBA", (SIZE, SIZE))
for x in range(SIZE):
    for y in range(SIZE):
        icon.putpixel((x, y), tuple(px[x][y]) + (255,))

icon.save(ROOT / "icon-source.png")
icon.resize((SIZE * SCALE, SIZE * SCALE), Image.NEAREST).save(ROOT / "icon.png")
# The in-jar logo. `mod.fabric.icon` in stonecutter.properties.toml names this
# path and feeds every generated manifest's logoFile, so it has to stay here.
in_jar = ROOT / "src/main/resources/assets/alexscaves/icon.png"
icon.resize((SIZE * 4, SIZE * 4), Image.NEAREST).save(in_jar)
print(f"wrote {ROOT / 'icon-source.png'} ({SIZE}x{SIZE})")
print(f"wrote {ROOT / 'icon.png'} ({SIZE * SCALE}x{SIZE * SCALE})")
print(f"wrote {in_jar} ({SIZE * 4}x{SIZE * 4})")
