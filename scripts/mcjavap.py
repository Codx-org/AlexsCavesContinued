#!/usr/bin/env python3
"""javap a Minecraft class out of the loom jar cache for a given MC version.

The version walk constantly needs the *real* signature of a vanilla member on the version
being ported to, because a name-level grep is not evidence (a method can keep its name and
change its parameters — see the mixin-descriptor rule in DEVELOPMENT.md).

    python3 scripts/mcjavap.py 1.20.4 net.minecraft.world.level.block.ButtonBlock
    python3 scripts/mcjavap.py 1.20.4 ButtonBlock SaplingBlock          # bare names are searched
    python3 scripts/mcjavap.py 1.20.4 ButtonBlock --grep '<init>'
"""
import argparse
import glob
import os
import re
import subprocess
import sys
import zipfile

CACHE = os.path.expanduser("~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft")


def jar_for(mc):
    """The Mojmap-named merged client+server jar for this MC version.

    26.x ships unobfuscated, so loom keeps it under `-deobf`; everything older is remapped
    to Mojmap via a layered mappings tree under the plain `-minecraft-merged` directory.
    Sources and backup jars sort into the same glob and must be filtered out — picking one
    silently reports every class as missing.
    """
    pat = "-minecraft-merged-deobf" if mc.startswith("26.") else "-minecraft-merged"
    dirs = [d for d in glob.glob(f"{CACHE}/*{pat}") if f"-{mc}-" in d]
    jars = sorted(
        j
        for d in dirs
        for j in glob.glob(f"{d}/*/*.jar")
        if "sources" not in j and "backup" not in j
    )
    if not jars:
        sys.exit(f"no cached jar for MC {mc} (looked in {CACHE}/*{pat})")
    return jars[-1]


def resolve(jar, name):
    """Map a bare class name to its fully-qualified form, or pass an FQN straight through."""
    if "." in name:
        return name
    with zipfile.ZipFile(jar) as z:
        hits = [
            e[:-6].replace("/", ".")
            for e in z.namelist()
            if e.endswith(".class") and e[:-6].rsplit("/", 1)[-1] == name
        ]
    if not hits:
        sys.exit(f"no class named {name} in {os.path.basename(jar)}")
    if len(hits) > 1:
        print(f"# {name} is ambiguous: {', '.join(hits)}", file=sys.stderr)
    return hits[0]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("mc")
    ap.add_argument("classes", nargs="+")
    ap.add_argument("--grep", help="only print lines matching this regex")
    ap.add_argument("-p", "--private", action="store_true", help="include private members")
    args = ap.parse_args()

    jar = jar_for(args.mc)
    for name in args.classes:
        fqn = resolve(jar, name)
        cmd = ["javap", "-cp", jar]
        if args.private:
            cmd.append("-p")
        out = subprocess.run(cmd + [fqn], capture_output=True, text=True).stdout
        if args.grep:
            rx = re.compile(args.grep)
            out = "".join(l for l in out.splitlines(True) if rx.search(l))
        print(f"=== {fqn} ===")
        print(out.rstrip())


if __name__ == "__main__":
    main()
