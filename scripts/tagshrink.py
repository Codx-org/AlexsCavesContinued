#!/usr/bin/env python3
"""Diff the MEMBERSHIP of every vanilla tag between two MC versions.

Why this exists
---------------
A vanilla tag that SHRINKS is invisible to every check this repo has.  It parses
clean, loads clean, logs nothing, and quietly makes whatever referenced it match
less than it used to.  None of the boot-log markers fire, because nothing failed.

That is exactly what MC 26.1 did to ``#minecraft:dirt``: 10 members on 1.21.11,
**3** from 26.1 (``dirt``, ``coarse_dirt``, ``rooted_dirt``).  Alex's Caves named
that tag in 11 places, so on the four 26.x nodes the dinosaurs had nothing to
spawn on and the trees had nothing to grow in -- with no log line anywhere.

Usage
-----
    python3 scripts/tagshrink.py <old-mc> <new-mc> [--all]

Prints every tag that lost members (``--all`` also prints ones that gained).
Exit code is 1 if anything shrank, so it can gate a release.

Reads the tags straight out of loom's cached ``minecraft-extracted_server.jar``
for each version -- note that is the EXTRACTED server, not ``minecraft-server.jar``,
which is only the bundler and contains no ``data/`` at all.
"""
import json
import os
import re
import sys
import zipfile

LOOM = os.path.expanduser("~/.gradle/caches/fabric-loom")


def load(version):
    jar = f"{LOOM}/{version}/minecraft-extracted_server.jar"
    if not os.path.exists(jar):
        sys.exit(f"no cached jar for {version}: {jar}\n"
                 f"(build any node of that MC version once, then re-run)")
    out = {}
    with zipfile.ZipFile(jar) as z:
        for name in z.namelist():
            m = re.match(r"data/minecraft/tags/([a-z_/]+)/(.+)\.json$", name)
            if not m:
                continue
            try:
                data = json.loads(z.read(name))
            except Exception:
                continue
            members = set()
            for v in data.get("values", []):
                members.add(v if isinstance(v, str) else v.get("id"))
            out[(m.group(1), m.group(2))] = members
    return out


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    show_all = "--all" in sys.argv
    if len(args) != 2:
        sys.exit(__doc__)
    old_v, new_v = args
    old, new = load(old_v), load(new_v)

    shrank = 0
    for key in sorted(set(old) | set(new)):
        a, b = old.get(key, set()), new.get(key, set())
        lost, gained = a - b, b - a
        label = f"{key[0]}/{key[1]}"
        if key not in new:
            print(f"GONE    {label}  (had {len(a)} members)")
            shrank += 1
        elif lost:
            print(f"SHRANK  {label}  -{sorted(lost)}"
                  + (f"  +{sorted(gained)}" if gained else ""))
            shrank += 1
        elif gained and show_all:
            print(f"grew    {label}  +{sorted(gained)}")
    print(f"\n{old_v} -> {new_v}: {shrank} tag(s) lost members or vanished")
    return 1 if shrank else 0


if __name__ == "__main__":
    sys.exit(main())
