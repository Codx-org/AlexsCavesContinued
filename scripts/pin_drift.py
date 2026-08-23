#!/usr/bin/env python3
"""Report loader pins in stonecutter.properties.toml that are behind the latest published build.

Why this exists: NeoForge reintroduced FluidType into EntityFluidInteraction between builds
26.1.2.87 and 26.1.2.97 -- the same MC version, both in players' hands -- and the newer shape
hard-throws for a modded fluid tag. Compiling against .87 gave no warning at all; the first
symptom was a crash report against shipped 1.0.0. A point release's build number keeps moving by
dozens after the MC version freezes, so "the build we started the wave on" is not the build
anyone runs, and the gap is invisible from inside the tree.

Run it before any release build. Drift is not automatically a problem -- Forge patch builds
almost never move API -- so this only reports; bumping is a judgement call, and every bump needs
that node recompiled.

    python3 scripts/pin_drift.py            # all loaders
    python3 scripts/pin_drift.py --neoforge # one loader
"""
import argparse
import pathlib
import re
import sys
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parent.parent
PINS = ROOT / "stonecutter.properties.toml"

NEOFORGE_META = "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml"
FORGE_META = "https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml"


def fetch(url):
    # maven.minecraftforge.net answers 403 to urllib's default User-Agent; NeoForge does not care.
    req = urllib.request.Request(url, headers={"User-Agent": "curl/8"})
    with urllib.request.urlopen(req, timeout=60) as fh:
        return re.findall(r"<version>([^<]+)</version>", fh.read().decode("utf-8", "replace"))


def key(build):
    """Sort a dotted build number numerically, ignoring a -beta suffix."""
    return [int(p) if p.isdigit() else -1 for p in build.replace("-beta", "").split(".")]


def check(label, pinned, candidates, prefix_of):
    """Compare each pinned build against the newest candidate sharing its release line."""
    drift = []
    for pin in pinned:
        line = prefix_of(pin)
        same = [c for c in candidates if prefix_of(c) == line]
        if not same:
            drift.append((pin, "NO PUBLISHED BUILDS -- is the line spelled right?"))
            continue
        latest = max(same, key=key)
        if key(latest) > key(pin):
            drift.append((pin, latest))
    for pin, latest in drift:
        print(f"  {label:9} {pin:<18} -> {latest}")
    return len(drift)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--neoforge", action="store_true")
    ap.add_argument("--forge", action="store_true")
    args = ap.parse_args()
    both = not (args.neoforge or args.forge)

    text = PINS.read_text(encoding="utf-8")
    total = 0

    if both or args.neoforge:
        # NeoForge builds are <mc-major>.<mc-minor>[.<mc-patch>].<build>; the release line is
        # everything but the last component, so 26.1.2.87 and 26.1.2.97 compare and 26.1.1.15 does
        # not join them.
        pinned = re.findall(r'deps\.neoforge = "([^"]+)"', text)
        latest = fetch(NEOFORGE_META)
        total += check("neoforge", pinned, latest,
                       lambda v: ".".join(v.replace("-beta", "").split(".")[:-1]))

    if both or args.forge:
        # Forge's metadata is "<mc>-<build>"; deps.forge holds only the build half, whose first
        # component is the line (47.x is 1.20.1, 65.x is 26.2).
        pinned = re.findall(r'deps\.forge = "([^"]+)"', text)
        latest = [v.split("-", 1)[1] for v in fetch(FORGE_META) if "-" in v]
        latest = [v for v in latest if re.fullmatch(r"[0-9.]+", v)]
        total += check("forge", pinned, latest, lambda v: v.split(".")[0])

    if total:
        print(f"\n{total} pin(s) behind the latest published build.")
        print("Bumping is a judgement call -- recompile every node you bump.")
    else:
        print("every pin is at the latest published build")
    return 0


if __name__ == "__main__":
    sys.exit(main())
