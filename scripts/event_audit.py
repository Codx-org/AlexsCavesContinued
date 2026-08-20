#!/usr/bin/env python3
"""Check that every Forge game-bus event this mod *consumes* has a Fabric-side *producer*.

Why this exists
---------------
The Fabric port supplies Forge's API *shapes* — stand-in event classes under
``fabric/forge/**`` and a ``MinecraftForge.EVENT_BUS`` — and registers the mod's
handlers on that bus. For a whole milestone the bus was **never fired**: the
shapes all existed, the handlers were all registered, and roughly twenty of them
had simply never run on any of the 22 Fabric nodes. Nothing caught it, because a
compile-green, boot-green loader port proves the shapes exist, not that anything
calls them.

This is the one-line check that would have caught it: the set difference between
every event type named by an ``@SubscribeEvent`` handler and every event type
*constructed* under ``fabric/`` or ``mixin/fabric/``.

Run it after any wave that adds a handler.  Exit code is 1 if anything is
unaccounted for.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "src/main/java/com/github/alexmodguy/alexscaves"

# Classes whose @SubscribeEvent methods are *consumers* — the handlers that have
# to be driven on Fabric.  The stand-in/dispatch classes under fabric/ are the
# other side of the ledger and are excluded.
CONSUMERS = [
    "server/event/CommonEvents.java",
    "server/event/ACVillagerTradeEvents.java",
    "client/event/ClientEvents.java",
    "citadel/CitadelEvents.java",
    "citadel/CitadelClientEvents.java",
    "client/render/entity/layer/ClientLayerRegistry.java",
    "server/entity/ACEntityRegistry.java",
    "AlexsCaves.java",
]

PRODUCER_DIRS = ["fabric", "mixin/fabric"]
STANDIN_DIR = "fabric/forge"

# Events deliberately answered WITHOUT a producer.  Each entry is a promise that
# the handler still runs on Fabric by some other route; keep the reason current.
EXEMPT = {
    "RenderLevelStageEvent":
        "superseded tree-wide by client/ACLevelRenderStage, fed on every loader by "
        "mixin/client/LevelRenderStageMixin — there is no Forge event left to post",
    "RenderGuiOverlayEvent.Pre":
        "mixin/fabric/client/GuiHudMixin drives ClientEvents#hidePossessedPlayerOverlay / "
        "#hideExperienceBar directly at the vanilla draw sites, rather than posting a stand-in",
    "RenderGuiOverlayEvent.Post":
        "see RenderGuiOverlayEvent.Pre",
}

# Forge/NeoForge *mod-bus* lifecycle and registration events.  Fabric has its own
# init path (AlexsCavesFabric / ACFabricEntityRegistration), so these are not
# game-bus traffic and are not expected to be posted.
# Forge/NeoForge *mod-bus* lifecycle and registration events.  Fabric has its own
# init path (AlexsCavesFabric / ACFabricEntityRegistration), so these are not
# game-bus traffic and are not expected to be posted.
MOD_BUS = re.compile(
    r"^(FML\w+Event|Register\w*Event|.*RegistryEvent|EntityRenderersEvent.*|"
    r"ModConfigEvent.*|AddPackFindersEvent|BuildCreativeModeTabContentsEvent|"
    r"GatherComponentsEvent.*|ModifyDefaultComponentsEvent|AddGuiOverlayLayersEvent|"
    r"EntityAttributeCreationEvent|SpawnPlacementRegisterEvent|TextureAtlasStitchedEvent|"
    r"RegisterShadersEvent|RegisterClientReloadListenersEvent|.*ClientSetupEvent)$"
)

SUBSCRIBE = re.compile(
    r"@SubscribeEvent[^\n]*\n(?:\s*@[\w.]+[^\n]*\n)*"
    r"\s*(?:public|private|protected)\s+\w+\s+\w+\s*\(\s*(?:final\s+)?([\w.$]+)\s+\w+"
)
CONSTRUCT = re.compile(r"\bnew\s+([A-Z][\w.$]*Event[\w.$]*)\s*\(")
# Nested event classes are spelled ``Pre`` / ``Added`` / ``RightClickItem`` — the
# name need not contain "Event", only the file (and so the outer class) does.
DECLARE = re.compile(r"^\s*(?:public\s+)?(?:static\s+)?(?:final\s+)?(?:abstract\s+)?"
                     r"(?:class|interface)\s+(\w+)\b", re.M)

def simple(name: str) -> str:
    """Drop any package prefix, keep the nested spelling (``A.B``)."""
    parts = name.replace("$", ".").split(".")
    while parts and not parts[0][:1].isupper():
        parts.pop(0)
    return ".".join(parts)


def java_files(rel: str):
    p = SRC / rel
    return sorted(p.rglob("*.java")) if p.is_dir() else [p]


def standins() -> set[str]:
    """Every Forge event type this tree supplies a stand-in class for.

    This is what scopes the audit.  The mod's handlers are written once and
    Stonecutter-gated per loader, so a raw scan sees every band's spelling at
    once — ``TickEvent.ServerTickEvent.Post`` (Forge below eventbus 7),
    ``ServerTickEvent.Pre`` (Forge from 1.21.6), ``EntityTickEvent.Pre`` and
    ``RenderGuiLayerEvent`` (NeoForge).  None of those arms compiles on Fabric,
    and the proof is that no stand-in declares the type: a handler cannot
    consume an event whose class does not exist on the loader.  So an event with
    no stand-in is out of scope, and one whose *outer* class is a stand-in is in
    scope under that outer name — Fabric's ``TickEvent.ServerTickEvent`` carries
    a ``phase`` field rather than ``Pre``/``Post`` subclasses.
    """
    found = set()
    for f in java_files(STANDIN_DIR):
        outer = f.stem
        if "Event" not in outer:
            continue
        found.add(outer)
        found.update(f"{outer}.{m.group(1)}" for m in DECLARE.finditer(f.read_text())
                     if m.group(1) != outer)
    return found


def resolve(ev: str, known: set[str]) -> str | None:
    """Map a consumed spelling onto the stand-in that must be posted, or None."""
    parts = ev.split(".")
    for n in range(len(parts), 0, -1):
        cand = ".".join(parts[:n])
        if cand in known:
            return cand
    return None


def main() -> int:
    consumed: dict[str, set[str]] = {}
    for rel in CONSUMERS:
        f = SRC / rel
        if not f.exists():
            print(f"!! consumer not found: {rel}")
            return 1
        for m in SUBSCRIBE.finditer(f.read_text()):
            ev = simple(m.group(1))
            if ev.endswith("Event") or "Event." in ev:
                consumed.setdefault(ev, set()).add(rel)

    produced: set[str] = set()
    for d in PRODUCER_DIRS:
        for f in java_files(d):
            produced.update(simple(m.group(1)) for m in CONSTRUCT.finditer(f.read_text()))

    known = standins()
    missing: dict[str, set[str]] = {}
    offloader: dict[str, set[str]] = {}
    for ev, srcs in consumed.items():
        if ev in EXEMPT or MOD_BUS.match(ev):
            continue
        target = resolve(ev, known)
        if target is None:
            offloader.setdefault(ev, set()).update(srcs)
        elif target not in produced:
            missing.setdefault(ev, set()).update(srcs)

    print(f"consumers   {len(CONSUMERS)} classes, {len(consumed)} distinct event types")
    print(f"producers   {len(produced)} distinct event types constructed under "
          f"{' + '.join(PRODUCER_DIRS)}/")
    for e, why in sorted(EXEMPT.items()):
        if e in consumed:
            print(f"exempt      {e}\n              {why}")
    stale = sorted(set(EXEMPT) - set(consumed))
    for e in stale:
        print(f"note        {e} is exempted but no handler consumes it any more — drop the entry")
    if offloader:
        print(f"\nnot on Fabric ({len(offloader)}) — no stand-in class declares these, so the arm "
              f"that consumes them is gated out there:")
        for e in sorted(offloader):
            print(f"  {e}")
    if missing:
        print(f"\nMISSING PRODUCERS ({len(missing)}) — these handlers can never run on Fabric:")
        for e, srcs in sorted(missing.items()):
            print(f"  {e:<44} consumed by {', '.join(sorted(srcs))}")
        return 1
    print("\nOK — every consumed game-bus event has a Fabric-side producer.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
