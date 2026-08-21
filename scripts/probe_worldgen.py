#!/usr/bin/env python3
"""Prove, over RCON against a headless server, that every Alex's Caves biome generates
and every Alex's Caves mob can be created and ticked.

    python3 scripts/probe_worldgen.py <rconport> [password]

Three passes, none of which needs a client:

  1. BIOMES   `locate biome` for each of the six cave biomes. A hit proves the biome is
              in the overworld's biome source and that worldgen places it; a miss inside
              6400 blocks is a real failure, not slowness.
  2. MOBS     summon every non-MISC entity type, hold it for a second of real ticking,
              then confirm it is still there. Construction, attribute suppliers and the
              first second of goal selection are all on this path -- which is exactly
              where the `Can't find attribute minecraft:tempt_range` crash lived.
  3. TABLES   cross-check every id in the six biomes' `spawners` blocks against the
              registry, so a renamed mob cannot silently drop out of a spawn table.

What this CANNOT prove is NATURAL spawning: `NaturalSpawner` only considers chunks the
DistanceManager counts as natural-spawn chunks, and those come from player tickets --
`forceload` does not create them. With nobody connected, nothing spawns by itself no
matter how long the server runs. Seeing a live cave populate needs a player in it.

The mob list is read out of ACEntityRegistry.java rather than hardcoded, so a new mob is
covered the day it is added. MISC-category types (projectiles, falling blocks, the two
bombs) are skipped on purpose.
"""
import os, re, subprocess, sys, time, json, zipfile, glob

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
RCON = os.path.join(HERE, 'rcon.py')
REGISTRY = os.path.join(ROOT, 'src/main/java/com/github/alexmodguy/alexscaves/'
                              'server/entity/ACEntityRegistry.java')

PORT = sys.argv[1] if len(sys.argv) > 1 else '25595'
PASSWORD = sys.argv[2] if len(sys.argv) > 2 else 'accdev'

BIOMES = ['magnetic_caves', 'primordial_caves', 'toxic_caves',
          'abyssal_chasm', 'forlorn_hollows', 'candy_cavity']

# The console has no dimension of its own: a bare @e selector finds nothing and says so
# in a way that reads exactly like a mob that failed to spawn.
IN = 'execute in minecraft:overworld run '
X, Y, Z = 0, 220, 0          # open air, so nothing suffocates and every goal still ticks
TAG = 'acprobe'


def rcon(*cmds):
    out = subprocess.run(['python3', RCON, '--port', PORT, '--password', PASSWORD, *cmds],
                         capture_output=True, text=True)
    if out.returncode != 0:
        raise SystemExit(f'rcon failed: {out.stderr.strip()}')
    return out.stdout


def mob_ids():
    """Every registered type whose MobCategory is not MISC, in registry order."""
    src = open(REGISTRY).read()
    ids = []
    for m in re.finditer(r'DEF_REG\.register\("([a-z_]+)".*?EntityType\.Builder\.'
                         r'(?:of|createNothing)\([^,]+,\s*([A-Za-z_.]+)', src, re.S):
        name, category = m.group(1), m.group(2)
        if category.endswith('MISC'):
            continue
        ids.append(name)
    return ids


def spawn_table_ids():
    """ids named in the shipped biome JSONs, from whichever built jar we can find."""
    jars = sorted(glob.glob(os.path.join(ROOT, 'versions/*/build/libs/alexscaves-*.jar')))
    jars = [j for j in jars if 'sources' not in j]
    if not jars:
        return None
    table = {}
    with zipfile.ZipFile(jars[-1]) as z:
        for b in BIOMES:
            try:
                d = json.loads(z.read(f'data/alexscaves/worldgen/biome/{b}.json'))
            except KeyError:
                continue
            ids = []
            for entries in d.get('spawners', {}).values():
                for e in entries or []:
                    if e['type'].startswith('alexscaves:'):
                        ids.append(e['type'].split(':', 1)[1])
            table[b] = ids
    return table


def main():
    print(f'== rig: rcon 127.0.0.1:{PORT} ==')
    rcon('difficulty normal', 'time set midnight', 'gamerule doDaylightCycle false',
         'gamerule spawn_mobs false', 'gamerule doMobSpawning false',
         f'forceload add {X-16} {Z-16} {X+16} {Z+16}')

    fails = []

    print('\n== 1. biomes ==')
    for b in BIOMES:
        reply = rcon(IN + f'locate biome alexscaves:{b}')
        ok = 'is at' in reply or 'blocks away' in reply
        coords = ''
        m = re.search(r'\[(-?\d+, -?\d+, -?\d+)\]', reply)
        if m:
            coords = f'  [{m.group(1)}]'
        print(f'  {"OK  " if ok else "FAIL"} {b}{coords}')
        if not ok:
            fails.append(f'biome {b}: {reply.strip().splitlines()[-1][:120]}')

    print('\n== 2. mobs (summon, tick 1s, still alive) ==')
    ids = mob_ids()
    print(f'  {len(ids)} non-MISC entity types from ACEntityRegistry.java')
    BATCH = 8
    for i in range(0, len(ids), BATCH):
        chunk = ids[i:i + BATCH]
        rcon(*[IN + f'summon alexscaves:{e} {X} {Y} {Z} {{Tags:["{TAG}"],'
                    f'PersistenceRequired:1b,NoGravity:1b}}' for e in chunk])
        time.sleep(1.2)
        for e in chunk:
            reply = rcon(IN + f'data get entity @e[type=alexscaves:{e},tag={TAG},'
                              f'limit=1] Health')
            # `data get` echoes only the VALUE, so a live mob replies with a float and a
            # dead/never-created one replies "No entity was found".
            alive = 'No entity was found' not in reply and 'Unable' not in reply
            print(f'  {"OK  " if alive else "FAIL"} {e}')
            if not alive:
                fails.append(f'mob {e}: {reply.strip().splitlines()[-1][:120]}')
        rcon(IN + f'kill @e[tag={TAG}]')

    print('\n== 3. spawn tables vs registry ==')
    table = spawn_table_ids()
    if table is None:
        print('  SKIP  no built jar under versions/*/build/libs to read biome JSON from')
    else:
        known = set(ids) | {'hullbreaker'}
        for b, entries in table.items():
            missing = [e for e in entries if e not in known]
            print(f'  {"OK  " if not missing else "FAIL"} {b}: {len(entries)} '
                  f'alexscaves entries' + (f' -- unknown: {missing}' if missing else ''))
            if missing:
                fails.append(f'spawn table {b} names unregistered {missing}')

    rcon(f'forceload remove {X-16} {Z-16} {X+16} {Z+16}',
         'gamerule spawn_mobs true', 'gamerule doMobSpawning true')

    print('\n== result ==')
    if fails:
        print(f'{len(fails)} FAILURES')
        for f in fails:
            print('  -', f)
        sys.exit(1)
    print(f'all green: {len(BIOMES)} biomes, {len(ids)} mobs')
    print('NOT covered here: natural spawning (needs a player in the chunk) and rendering.')


if __name__ == '__main__':
    main()
