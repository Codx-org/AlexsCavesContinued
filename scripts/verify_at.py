#!/usr/bin/env python3
"""Check a Mojang-named access transformer against a Minecraft jar.

An AT entry that resolves to nothing is a *hard error* on NeoForge and a silent no-op on Forge,
so every MC bump has to re-check the whole file. This does that by reading the merged Minecraft
jar loom already downloaded for a node -- the bytecode, not a decompile.

    python3 scripts/verify_at.py <mc-jar> [at-file]

Reports, per entry: MISSING CLASS / MISSING FIELD / MISSING METHOD, or nothing when it resolves.
Exit status is 1 if anything is unresolved.
"""
import re
import sys
import zipfile
from pathlib import Path

DEFAULT_AT = Path(__file__).resolve().parent.parent / "src/main/resources/META-INF/accesstransformer_mojmap.cfg"


def read_classes(jar_path):
    """Every class in the jar, as {internal name: (fields, methods)}."""
    import struct

    classes = {}
    with zipfile.ZipFile(jar_path) as jar:
        for name in jar.namelist():
            if name.endswith(".class"):
                classes[name[:-6]] = jar.read(name)
    return classes


def parse_class(data):
    """(field names, {method name: {descriptors}}) straight out of the constant pool + tables."""
    import struct

    pos = 10  # magic, minor, major
    count = struct.unpack_from(">H", data, 8)[0]
    pool = {}
    i = 1
    while i < count:
        tag = data[pos]
        pos += 1
        if tag == 1:  # Utf8
            length = struct.unpack_from(">H", data, pos)[0]
            pool[i] = data[pos + 2:pos + 2 + length].decode("utf-8", "replace")
            pos += 2 + length
        elif tag in (7, 8, 16, 19, 20):
            pos += 2
        elif tag == 15:
            pos += 3
        elif tag in (5, 6):  # long/double take two slots
            pos += 8
            i += 1
        else:
            pos += 4
        i += 1

    pos += 6  # access, this, super
    interfaces = struct.unpack_from(">H", data, pos)[0]
    pos += 2 + interfaces * 2

    def skip_attributes(pos):
        n = struct.unpack_from(">H", data, pos)[0]
        pos += 2
        for _ in range(n):
            length = struct.unpack_from(">I", data, pos + 2)[0]
            pos += 6 + length
        return pos

    fields = set()
    methods = {}
    for table, sink in ((0, fields), (1, methods)):
        n = struct.unpack_from(">H", data, pos)[0]
        pos += 2
        for _ in range(n):
            name = pool[struct.unpack_from(">H", data, pos + 2)[0]]
            desc = pool[struct.unpack_from(">H", data, pos + 4)[0]]
            pos = skip_attributes(pos + 6)
            if table == 0:
                fields.add(name)
            else:
                methods.setdefault(name, set()).add(desc)
    return fields, methods


def main():
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    jar = sys.argv[1]
    at_file = Path(sys.argv[2]) if len(sys.argv) > 2 else DEFAULT_AT

    raw = read_classes(jar)
    parsed = {}

    def members(internal):
        if internal not in parsed:
            parsed[internal] = parse_class(raw[internal])
        return parsed[internal]

    bad = 0
    for number, line in enumerate(at_file.read_text().splitlines(), 1):
        line = line.split("#", 1)[0].strip()
        if not line:
            continue
        parts = line.split()
        if len(parts) < 2:
            continue
        owner = parts[1]
        internal = owner.replace(".", "/")
        target = parts[2] if len(parts) > 2 else None

        if internal not in raw:
            print(f"{at_file.name}:{number}: MISSING CLASS  {owner}")
            bad += 1
            continue
        if target is None or target == "*":
            continue

        fields, methods = members(internal)
        if "(" in target:
            name, desc = target.split("(", 1)
            desc = "(" + desc
            name = {"<init>": "<init>"}.get(name, name)
            if desc not in methods.get(name, ()):
                where = sorted(methods.get(name, ())) or "no such name"
                print(f"{at_file.name}:{number}: MISSING METHOD {owner}#{target}  (has: {where})")
                bad += 1
        elif target not in fields:
            print(f"{at_file.name}:{number}: MISSING FIELD  {owner}#{target}")
            bad += 1

    print(f"\n{at_file.name} vs {Path(jar).name}: {bad} unresolved")
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
