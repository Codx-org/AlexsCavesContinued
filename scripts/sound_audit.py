#!/usr/bin/env python3
"""Audit the sound tree both ways: registry <-> sounds.json <-> .ogg <-> lang.

FOUR CHECKS.  Each one caught a real shipped bug, and the first two caught the
*same* bug from opposite ends -- which is the whole point of sweeping both
directions rather than only asking "is every registered event defined?".

== 1/2. Registry <-> sounds.json ==

`ACSoundRegistry.createSoundEvent("x")` registers `alexscaves:x`.  A registered
event with no `sounds.json` key logs

    Missing sound for event: alexscaves:luxtructosaurus_breath

and is then **silent for the rest of the session**.  A `sounds.json` key that
no event registers is simply dead weight and is never reported at all.

A key-name typo produces BOTH at once, and the pairing is the diagnosis: the
counts stayed equal (481/481) while `luxtructosaurus_breath` was missing and
`luxtructosaurus_breathe` was dead, i.e. the audio shipped fine and only the
spelling was wrong.  Two of this mod's three sound bugs looked like "missing
content" in the log and were one renamed key each.  Check the two lists
against each other before believing anything is absent.

== 3. sounds.json -> .ogg on disk ==

An entry naming a file that does not exist logs one error per file at startup
and drops that variation.  `purple_soda_swim` listed five files where three
ever shipped -- a copy-paste from `acid_swim_*`, which really does have five.

== 4. subtitle -> lang ==

A `subtitle` key with no `en_us` translation renders the raw key on screen for
anyone playing with subtitles on.  Nothing logs it.  Note the *mood* entries
carry no subtitle at all by design, so absence is only a finding when the key
is actually referenced.

Exits 1 on any finding.  Run before every release.
"""
import collections
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src/main/resources/assets/alexscaves")
REGISTRY = os.path.join(
    ROOT, "src/main/java/com/github/alexmodguy/alexscaves/server/misc",
    "ACSoundRegistry.java")


def registered():
    src = open(REGISTRY, encoding="utf-8").read()
    return set(re.findall(r'createSoundEvent\("([^"]+)"\)', src))


def defined():
    return json.loads(open(os.path.join(ASSETS, "sounds.json"),
                           encoding="utf-8").read(),
                      object_pairs_hook=collections.OrderedDict)


def report(title, items):
    items = sorted(items)
    print("\n-- %s: %d" % (title, len(items)))
    for i in items:
        print("   %s" % i)
    return len(items)


def audit():
    reg, snd = registered(), defined()
    print("registered events: %d   sounds.json keys: %d" % (len(reg), len(snd)))
    bad = 0
    bad += report("registered but NOT in sounds.json (silent, warns)",
                  reg - set(snd))
    bad += report("in sounds.json but NOT registered (dead entry)",
                  set(snd) - reg)

    missing, subtitles = [], set()
    for key, body in snd.items():
        if "subtitle" in body:
            subtitles.add(body["subtitle"])
        for entry in body.get("sounds", []):
            name = entry["name"] if isinstance(entry, dict) else entry
            # an `event` entry redirects to another sounds.json key, not a file
            if isinstance(entry, dict) and entry.get("type") == "event":
                if name.split(":", 1)[-1] not in snd:
                    missing.append("%s -> event %s" % (key, name))
                continue
            path = os.path.join(ASSETS, "sounds",
                                name.split(":", 1)[-1] + ".ogg")
            if not os.path.isfile(path):
                missing.append("%s -> %s" % (key, name))
    bad += report("sounds.json refs with no .ogg on disk", missing)

    lang = json.loads(open(os.path.join(ASSETS, "lang/en_us.json"),
                           encoding="utf-8").read())
    bad += report("subtitle keys with no en_us translation",
                  [s for s in subtitles if s not in lang])

    print("\nsound problems: %d" % bad)
    return 0 if bad == 0 else 1


if __name__ == "__main__":
    sys.exit(audit())
