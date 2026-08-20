#!/usr/bin/env python3
"""Check that every vanilla AI goal a mob uses reads only attributes the mob declares.

Why this exists
---------------
Up to 1.21.1 ``TemptGoal`` used a hard-coded ``TargetingConditions.range(10.0)``.
From 1.21.2 ``canUse`` opens with ``mob.getAttributeValue(Attributes.TEMPT_RANGE)``,
unconditionally — and vanilla adds that attribute in ``Animal.createAnimalAttributes()``
**alone**.  ``AttributeSupplier#getAttributeInstance`` throws for anything the supplier
never declared, so eleven ``Monster``-based mobs here crashed the server one tick after
spawning, on all three loaders, on 40 of the 58 nodes.  Neither loader patches the
lookup to be lenient.

Nothing in the toolchain has an opinion about this: it compiles, it boots, and it only
fires when the mob actually ticks.  So the check has to be made explicitly, and it
generalises past the one attribute — any vanilla goal may start reading a new one on
any MC bump.

What it does, per node:

* reads ``ACEntityRegistry#initializeAttributes`` for the mob → supplier mapping;
* resolves each mob's declared attributes: the vanilla base supplier
  (``createMonsterAttributes`` and friends, followed transitively through the
  bytecode) plus every ``Attributes.X`` the mod names in its own ``createAttributes``,
  including through the ``ACCompat.temptable`` helper;
* walks each mob's mod-side superclass chain, collects every vanilla
  ``ai.goal`` / ``ai.behavior`` class it constructs, and javaps each for the
  ``Attributes.X`` it reads;
* reports any attribute a goal reads that the mob does not declare.

Run it on every MC bump.  Exit code is 1 if anything is unaccounted for.
"""
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import verify_mixins as vm  # noqa: E402  (jar resolution, javap, import resolution)

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "src/main/java/com/github/alexmodguy/alexscaves"
REGISTRY = SRC / "server/entity/ACEntityRegistry.java"
COMPAT = SRC / "server/misc/ACCompat.java"

ATTR_READ = re.compile(r"Field net/minecraft/world/entity/ai/attributes/Attributes\.(\w+):")
BASE_CALL = re.compile(r"Method [\w/$]+\.(create\w*Attributes):")
PUT = re.compile(r"event\.put\(\s*[\w.]+\.get\(\)\s*,\s*(\w+)\.createAttributes\(\)")
NEW = re.compile(r"\bnew\s+([A-Z]\w*)\s*\(")
EXTENDS = re.compile(r"\bclass\s+\w+(?:<[^>]*>)?\s+extends\s+([\w.]+)")
BASE_SUPPLIER = re.compile(r"\b(\w+)\.(create\w*Attributes)\(\)")
ATTR_NAMED = re.compile(r"\bAttributes\.([A-Z_]+)")
SUPER = re.compile(r"^(?:\w+ )*(?:class|interface) [\w.$]+(?:<.*?>)? extends ([\w.$]+)", re.M)

# A goal reached through these packages is vanilla AI.  A goal of the mod's own
# carries no contract itself, but it inherits one from whatever it extends, so the
# resolution below follows a mod goal up to its first vanilla ancestor.
AI_PACKAGES = ("net.minecraft.world.entity.ai.goal", "net.minecraft.world.entity.ai.behavior")

_JAVAP: dict[tuple[str, str], str] = {}


def disasm(node: str, fqn: str) -> str:
    key = (node, fqn)
    if key not in _JAVAP:
        _JAVAP[key] = vm.javap(node, fqn) or ""
    return _JAVAP[key]


def method_body(text: str, name: str) -> str:
    """The disassembly of one method, from its `Code:` block to the next member."""
    m = re.search(rf"^\s+(?:\w+ )*[\w.$<>\[\], ]*\b{re.escape(name)}\(.*?\);\n\s+descriptor.*?\n"
                  rf"(.*?)(?=\n\s{{2}}[\w.$<>\[\], ]+ [\w.$<>]+\(|\n\}})", text, re.S | re.M)
    return m.group(1) if m else ""


def base_attributes(node: str, owner_simple: str, method: str,
                    seen: set[str] | None = None) -> set[str]:
    """Attributes a vanilla `createXAttributes()` declares, followed transitively."""
    seen = seen if seen is not None else set()
    if method in seen:
        return set()
    seen.add(method)
    # The four suppliers live on Animal / Monster / Mob / LivingEntity; find whichever
    # class in this mob's own hierarchy declares the one named at the call site.
    for fqn in ("net.minecraft.world.entity.animal.Animal",
                "net.minecraft.world.entity.monster.Monster",
                "net.minecraft.world.entity.Mob",
                "net.minecraft.world.entity.LivingEntity",
                f"net.minecraft.world.entity.{owner_simple}"):
        body = method_body(disasm(node, fqn), method)
        if not body:
            continue
        found = set(ATTR_READ.findall(body))
        for nxt in BASE_CALL.findall(body):
            found |= base_attributes(node, owner_simple, nxt, seen)
        return found
    return set()


def helper_attributes() -> dict[str, set[str]]:
    """Attributes the mod's own supplier helpers add, keyed by helper name."""
    text = COMPAT.read_text()
    out = {}
    for m in re.finditer(r"\bBuilder\s+(\w+)\(.*?Builder\s+\w+\)\s*\{(.*?)\n    \}", text, re.S):
        out[m.group(1)] = set(ATTR_NAMED.findall(m.group(2)))
    return out


def source_of(simple: str) -> Path | None:
    hits = [p for p in SRC.rglob(f"{simple}.java")]
    return hits[0] if len(hits) == 1 else (hits[0] if hits else None)


def mob_chain(simple: str) -> list[Path]:
    """The mob's own class and every mod superclass above it."""
    chain, seen = [], set()
    while simple and simple not in seen:
        seen.add(simple)
        f = source_of(simple)
        if f is None:
            break
        chain.append(f)
        m = EXTENDS.search(vm.strip_comments(f.read_text()))
        simple = m.group(1).split(".")[-1] if m else ""
    return chain


def ai_reads(node: str, fqn: str) -> set[str]:
    """Every ``Attributes.X`` a vanilla goal reads, INCLUDING through its supers.

    ``NearestAttackableTargetGoal`` reads nothing itself — ``FOLLOW_RANGE`` is read by
    ``TargetGoal`` above it — so a class-local scan silently under-reports.
    """
    found, seen = set(), set()
    while fqn and fqn.startswith(AI_PACKAGES) and fqn not in seen:
        seen.add(fqn)
        text = disasm(node, fqn)
        found |= set(ATTR_READ.findall(text))
        m = SUPER.search(text)
        fqn = m.group(1) if m else None
    return found


def resolve_ai(node: str, simple: str, text: str) -> str | None:
    """Resolve a constructed goal's simple name to a vanilla AI class, or None.

    ⚠ Ten of the eleven mobs that hit the TEMPT_RANGE bug reach ``TemptGoal`` through a
    **wildcard** import, so an explicit-import map alone silently resolves nothing and the
    check passes vacuously — the exact failure mode ``verify_mixins.py`` had with
    ``@Mixin`` targets.  Wildcards are probed against the node's own jars.
    """
    imports = vm.resolve_imports(text)
    fqn = imports.get(simple)
    if fqn is None:
        for pkg in vm.wildcard_packages(text):
            if pkg.startswith(AI_PACKAGES) and disasm(node, f"{pkg}.{simple}"):
                fqn = f"{pkg}.{simple}"
                break
    if fqn and fqn.startswith(AI_PACKAGES):
        return fqn
    # A mod goal inherits its superclass's attribute reads.
    f = source_of(simple)
    if f is None or fqn and not fqn.startswith("com.github.alexmodguy"):
        return None
    own = vm.strip_comments(f.read_text())
    m = EXTENDS.search(own)
    if not m:
        return None
    parent = m.group(1).split(".")[-1]
    return None if parent == simple else resolve_ai(node, parent, own)


def audit_node(node: str, helpers: dict[str, set[str]]) -> tuple[list[str], tuple[int, int, int]]:
    reg = vm.strip_comments(REGISTRY.read_text())
    mobs = sorted(set(PUT.findall(reg)))
    problems: list[str] = []
    goals: set[str] = set()
    reads: set[str] = set()
    for mob in mobs:
        chain = mob_chain(mob)
        if not chain:
            problems.append(f"  {mob}: no source file")
            continue
        own = vm.strip_comments(chain[0].read_text())
        m = re.search(r"createAttributes\(\)\s*\{(.*?)\n    \}", own, re.S)
        body = m.group(1) if m else ""
        declared = set(ATTR_NAMED.findall(body))
        for helper, attrs in helpers.items():
            if re.search(rf"\b{helper}\s*\(", body):
                declared |= attrs
        bm = BASE_SUPPLIER.search(body)
        if bm:
            declared |= base_attributes(node, bm.group(1), bm.group(2))

        for f in chain:
            text = vm.strip_comments(f.read_text())
            for simple in set(NEW.findall(text)):
                fqn = resolve_ai(node, simple, text)
                if fqn is None:
                    continue
                goals.add(fqn)
                needed = ai_reads(node, fqn)
                reads |= needed
                for attr in sorted(needed - declared):
                    problems.append(
                        f"  {mob} builds {simple}, which reads Attributes.{attr}, "
                        f"but its supplier never declares it")
    return problems, (len(mobs), len(goals), len(reads))


def main(argv: list[str]) -> int:
    nodes = argv or sorted(p.name for p in (ROOT / "versions").iterdir() if p.is_dir())
    helpers = helper_attributes()
    bad = 0
    for node in nodes:
        raw, (nmobs, ngoals, nreads) = audit_node(node, helpers)
        problems = sorted(set(raw))
        # Print the coverage figures, not just the verdict: a resolution bug turns this
        # check green by finding nothing at all, and only the counts show that.
        print(f"{node:<20} {nmobs:>3} mobs {ngoals:>3} vanilla AI classes "
              f"{nreads:>2} attributes read   "
              f"{'OK' if not problems else f'{len(problems)} PROBLEM(S)'}")
        for p in problems:
            print(p)
        bad += len(problems)
    print(f"\n{'OK — every vanilla goal reads only declared attributes.' if not bad else f'{bad} problem(s)'}")
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
