#!/usr/bin/env python3
"""Verify every Alex's Caves mixin injection point against the Minecraft version it targets.

Mixins are not compile-checked: a target that changed name *or descriptor* only shows up at runtime,
and with `injectors.defaultRequire = 1` a single miss is a hard crash on launch. Worse, you learn
about exactly one miss per launch. This mod carries 66 mixins (53 of its own plus the 13 vendored
from Citadel) across a 49-node matrix, so finding them one boot at a time is unaffordable — this
script does the whole matrix statically.

For every node it reads the *generated* mixin config (so the per-node source excludes are already
applied) and the *generated* mixin sources under `build/generated/stonecutter/main/java` (so the
`//?` branches are resolved and the `replacements.string` renames applied), then javap-diffs each
injection point against the vanilla Mojmap jar in arch-loom's cache.

Checked per injection:

  * `method = "name"`               -> the target class declares a method with that name.
  * `method = "name(desc)ret"`      -> it declares one with exactly that descriptor.
  * `method = "Lowner;name(desc)R"` -> likewise, against the owner the spec names (Alex's Caves and
    Citadel both write targets in this fully-qualified form).
  * `@At(target = "Lowner;name(desc)R")` -> that owner declares it. When a parameter type changes,
    the invoke owner moves with it, so a name-only grep lies.
  * `@At(target = "Lowner;field:Ldesc;")` -> that owner declares the field with that type.
  * `@ModifyVariable(argsOnly = true, ordinal = N)` -> the target really has more than N arguments of
    the captured type, so the ordinal still points at the argument we think it does.
  * `@ModifyConstant(constant = @Constant(...))` and `@At(value = "CONSTANT", args = "...")` -> that
    constant is actually loaded in the target *method's* bytecode, and — when the annotation states
    an `expect`/`require` — the right number of times. This is the check that catches 1.20.3 having
    rewritten `MinecraftServer.runServer` from four 50L millisecond literals to nanosecond timing.
  * handler `static`ness matches the target's.
  * an `@Inject` handler that declares the target's arguments still declares the *right* ones —
    count and type, compared by simple name. Mixin allows a handler to take only the `CallbackInfo`,
    which is how one mixin file can span several eras of a churning method, but a partial match is
    an error. Skipped when the annotation captures locals, since those append to the list.

Vanilla jars are enough for all three loaders: Forge and NeoForge patch Minecraft additively. A
target that lives in a loader class rather than `net.minecraft` is reported as a skip, not a pass.

Usage:
    scripts/verify_mixins.py [node ...]      # default: every node under versions/

Exit status is non-zero if anything is missing. Build (or at least `processResources`) the nodes you
want covered first, so the generated config and sources exist.
"""

from __future__ import annotations

import json
import re
import subprocess
import sys
import tempfile
import zipfile
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
GRADLE = Path.home() / ".gradle"
LOOM = GRADLE / "caches/fabric-loom/minecraftMaven/net/minecraft"
MIXIN_CONFIG = "alexscaves.mixins.json"

PRIMITIVES = {
    "boolean": "Z", "byte": "B", "char": "C", "short": "S",
    "int": "I", "long": "J", "float": "F", "double": "D", "void": "V",
}


# ─────────────────────────────────────────────────────────────── vanilla class members ──

@dataclass
class ClassMembers:
    """What a Minecraft class declares, as javap sees it."""

    methods: dict[str, dict[str, bool]] = field(default_factory=dict)   # name -> {desc: is_static}
    fields: dict[str, str] = field(default_factory=dict)                # name -> descriptor
    code: dict[tuple[str, str], str] = field(default_factory=dict)      # (name, desc) -> disassembly
    supers: list[str] = field(default_factory=list)                     # superclass then interfaces


def vanilla_jar(mc: str) -> Path | None:
    """The cached Mojmap merged jar for a Minecraft version.

    26.x lives under a different artifact name because that fork ships official names, so loom
    stores it "deobf" rather than remapped through a layered mapping set.
    """
    if mc.startswith("26."):
        candidates = sorted((LOOM / "minecraft-merged-deobf" / mc).glob("*.jar"))
    else:
        dirs = sorted((LOOM / "minecraft-merged").glob(f"{mc}-loom.mappings.*layered*"))
        candidates = sorted(dirs[-1].glob("*.jar")) if dirs else []
    jars = [p for p in candidates if "backup" not in p.name and not p.name.endswith("-sources.jar")]
    return jars[0] if jars else None


def module_cache_jar(group: str, name: str, version: str, classifier: str) -> Path | None:
    """One artifact out of the Gradle module cache, whose middle path element is a content hash."""
    base = GRADLE / "caches/modules-2/files-2.1" / group / name / version
    if not base.exists():
        return None
    found = sorted(base.glob(f"*/{name}-{version}-{classifier}.jar"))
    return found[0] if found else None


def loader_jars(node: str, mc: str, loader: str) -> list[Path]:
    """The loader-patched Minecraft for a node, plus the loader's own classes.

    This matters more than it looks. Forge and NeoForge patch vanilla classes *in place* — 1.20.1's
    `FoodData.eat` grows a `LivingEntity` argument, `MapDecoration` gains `render(int)` — and several
    of Alex's Caves' mixins target exactly those patched members. Checked against a pure vanilla jar
    they read as missing, so the patched jar has to come first and vanilla only as the fallback.
    """
    if loader == "neoforge":
        # MDG stages several artifacts next to each other and only one of them holds patched
        # classes. Globbing and taking the first sorted hit silently picked
        # `neoforge-<ver>-client-extra-aka-minecraft-resources.jar` ('-' sorts before '.'), which has
        # no classes at all — so every NeoForge node fell through to plain vanilla and the loader
        # patches went unchecked. That is precisely the blind spot the 1.20.6 FoodData bug lived in.
        #
        # On the unobfuscated 26.x line MDG renamed that artifact `minecraft-patched-<ver>.jar` AND
        # stopped bundling `net.neoforged.*` into it (a 26.1.2 patched jar holds zero of them, where
        # a 1.21.11 `neoforge-<ver>.jar` holds 1827), so those come from the universal jar in the
        # module cache instead. Naming only the old spelling made every 26.x NeoForge node read as
        # pure vanilla — the same silent-fallback bug in a new disguise.
        version = PINS.get(("neoforge", mc))
        if not version:
            return []
        artifacts = ROOT / "versions" / node / "build/moddev/artifacts"
        jars = [p for p in (artifacts / f"neoforge-{version}.jar",
                            artifacts / f"minecraft-patched-{version}.jar") if p.exists()]
        universal = module_cache_jar("net.neoforged", "neoforge", version, "universal")
        if universal is not None:
            jars.append(universal)
        return jars
    if loader == "forge":
        version = PINS.get(("forge", mc))
        if not version:
            return []
        jars: list[Path] = []
        # 26.x ships official names, so loom stores the merged jar "deobf" rather than remapped —
        # a different directory name. Its `-srg` sibling is deliberately not matched.
        for suffix in ("", "-deobf"):
            directory = LOOM / f"forge-{mc}-{version}-minecraft-merged{suffix}"
            if not directory.exists():
                continue
            found = [p for p in sorted(directory.rglob("*.jar"))
                     if not p.name.endswith("-sources.jar") and "backup" not in p.name]
            if found:
                jars.append(found[0])
                break
        universal = module_cache_jar("net.minecraftforge", "forge", f"{mc}-{version}", "universal")
        if universal is not None:
            jars.append(universal)
        return jars
    return []   # Fabric runs on plain Mojmap vanilla


def read_pins() -> dict[tuple[str, str], str]:
    """`deps.forge` / `deps.neoforge` per Minecraft version, out of the central pin table."""
    try:
        import tomllib
        data = tomllib.loads((ROOT / "stonecutter.properties.toml").read_text())
    except Exception:
        return {}
    pins: dict[tuple[str, str], str] = {}
    for loader in ("forge", "neoforge"):
        for mc, section in (data.get(loader) or {}).items():
            version = (section.get("deps") or {}).get(loader)
            if version:
                pins[(loader, mc)] = version
    return pins


PINS = read_pins()

_JARS_CACHE: dict[str, list[zipfile.ZipFile]] = {}
_MEMBER_CACHE: dict[tuple[str, str], ClassMembers | None] = {}


def node_jars(node: str) -> list[zipfile.ZipFile]:
    if node not in _JARS_CACHE:
        mc, loader = node.rsplit("-", 1)
        paths = loader_jars(node, mc, loader) + [p for p in (vanilla_jar(mc),) if p is not None]
        _JARS_CACHE[node] = [zipfile.ZipFile(p) for p in paths]
    return _JARS_CACHE[node]


def javap(node: str, fqn: str) -> str | None:
    """Disassemble one class, taking the first jar that has it."""
    entry = fqn.replace(".", "/") + ".class"
    for jar in node_jars(node):
        try:
            data = jar.read(entry)
        except KeyError:
            continue
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "T.class"
            path.write_bytes(data)
            out = subprocess.run(["javap", "-p", "-c", "-s", str(path)], capture_output=True, text=True)
            return out.stdout
    return None


def members(node: str, fqn: str) -> ClassMembers | None:
    key = (node, fqn)
    if key not in _MEMBER_CACHE:
        text = javap(node, fqn)
        _MEMBER_CACHE[key] = parse_javap(text, fqn) if text else None
    return _MEMBER_CACHE[key]


def lookup_method(node: str, fqn: str, name: str) -> tuple[ClassMembers, dict[str, bool]] | None:
    """Find the class in `fqn`'s hierarchy that declares `name`.

    A mixin's `@At(target = "Lnet/minecraft/client/multiplayer/ClientLevel;getTimeOfDay(F)F")` is
    valid even though `getTimeOfDay` is declared on `Level` — the invoke is emitted against the
    static type at the call site. So a member that isn't declared here may still be inherited.

    ⚠️ The walk merges the OVERLOADS it finds rather than stopping at the first class declaring the
    name, because a subclass overriding one overload does not hide the others: `ClientLevel` inherits
    `getBlockEntity(BlockPos)` from `Level` and `getBlockEntity(BlockPos, BlockEntityType)` from the
    `BlockGetter` interface two levels further up, and a stop-at-first-name search reports the second
    one missing. Nearest declaration wins on a descriptor collision — that is the one an invoke
    against this static type actually reaches — so the merge never overwrites an entry it already has.
    """
    seen: set[str] = set()
    queue = [fqn]
    merged: dict[str, bool] = {}
    code: dict[tuple[str, str], str] = {}
    nearest: ClassMembers | None = None
    while queue:
        current = queue.pop(0)
        if current in seen:
            continue
        seen.add(current)
        cls = members(node, current)
        if cls is None:
            continue
        if name in cls.methods:
            if nearest is None:
                nearest = cls
            for desc, is_static in cls.methods[name].items():
                merged.setdefault(desc, is_static)
                body = cls.code.get((name, desc))
                if body is not None:
                    code.setdefault((name, desc), body)
        queue += cls.supers
    if nearest is None:
        return None
    return ClassMembers(methods={name: merged}, fields=nearest.fields, code=code,
                        supers=nearest.supers), merged


def lookup_field(node: str, fqn: str, name: str) -> str | None:
    seen: set[str] = set()
    queue = [fqn]
    while queue:
        current = queue.pop(0)
        if current in seen:
            continue
        seen.add(current)
        cls = members(node, current)
        if cls is None:
            continue
        if name in cls.fields:
            return cls.fields[name]
        queue += cls.supers
    return None


def parse_javap(text: str, fqn: str) -> ClassMembers:
    """Pair each `descriptor:` line with the declaration above it and the code below it.

    javap prints a source-ish declaration and then the raw descriptor; only the descriptor is
    unambiguous, so the name comes from the declaration and the signature from the descriptor. The
    lines up to the next `descriptor:` are that member's disassembly, which is what the constant
    checks read — per method, not per class, so a constant surviving elsewhere can't mask a miss.
    """
    # javap prints a constructor under the class' *binary* name, so a nested class' constructor reads
    # `MapDecoration$Type(...)` while the source spells the class `Type` — accept either spelling.
    binary = fqn.rsplit(".", 1)[-1]
    ctor_names = {binary, binary.rsplit("$", 1)[-1]}
    result = ClassMembers()
    lines = text.splitlines()

    header = next((l for l in lines if re.search(r"\b(?:class|interface)\s+[\w.$]", l)), "")
    header = re.sub(r"<[^<>]*>", "", header)
    # An interface's `extends` is itself a comma-separated list, so both clauses are split the same.
    for clause in (r"\bextends\s+([\w.$,\s]+?)\s*(?:\bimplements\b|\{|$)",
                   r"\bimplements\s+([\w.$,\s]+?)\s*(?:\{|$)"):
        m = re.search(clause, header)
        if m:
            result.supers += [t.strip() for t in m.group(1).split(",") if t.strip()]

    marks = [i for i, line in enumerate(lines) if re.match(r"\s*descriptor: \S+", line)]
    for n, i in enumerate(marks):
        desc = re.match(r"\s*descriptor: (\S+)", lines[i]).group(1)
        decl = lines[i - 1] if i else ""
        body = "\n".join(lines[i:marks[n + 1] if n + 1 < len(marks) else len(lines)])
        if not desc.startswith("("):
            fm = re.findall(r"([\w$]+)\s*(?:=[^;]*)?;\s*$", decl)
            if fm:
                result.fields[fm[-1]] = desc
            continue
        if re.match(r"\s*static\s*\{\s*}", decl):
            name = "<clinit>"
        else:
            nm = re.findall(r"([\w$]+)\s*\(", decl)
            if not nm:
                continue
            name = nm[-1]
            if name in ctor_names:
                name = "<init>"
        result.methods.setdefault(name, {})[desc] = bool(re.search(r"\bstatic\b", decl))
        result.code[(name, desc)] = body
    return result


# ────────────────────────────────────────────────────────────────── mixin source parsing ──

def strip_comments(text: str) -> str:
    """Drop every comment, which is how Stonecutter disables a `//?` branch.

    Doing this first is the whole reason this checker can read generated sources: the inactive era's
    annotations are sitting right there in the file, disabled in place, and must not be verified.
    Both forms have to go — Stonecutter wraps a multi-line branch in `/* … */` but a single-line one
    just gets a `//` prefix, so leaving line comments in makes an inactive `//@Shadow` look live.
    String and char literals are tracked so a `//` inside one survives.
    """
    out: list[str] = []
    i, n = 0, len(text)
    while i < n:
        c = text[i]
        if c in "\"'":
            out.append(c)
            i += 1
            while i < n:
                ch = text[i]
                out.append(ch)
                i += 1
                if ch == "\\":
                    if i < n:
                        out.append(text[i])
                        i += 1
                elif ch == c:
                    break
            continue
        if text.startswith("/*", i):
            end = text.find("*/", i + 2)
            i = n if end < 0 else end + 2
            continue
        if text.startswith("//", i):
            end = text.find("\n", i)
            i = n if end < 0 else end
            continue
        out.append(c)
        i += 1
    return "".join(out)


def resolve_imports(text: str) -> dict[str, str]:
    return {fqn.rsplit(".", 1)[-1]: fqn
            for fqn in re.findall(r"^import\s+(?:static\s+)?([\w.$]+)\s*;", text, re.M)
            if not fqn.endswith(".*")}


def wildcard_packages(text: str) -> list[str]:
    """The packages a file pulls in with `import pkg.*;`.

    Worth its own function because a wildcard import is the *only* way to name a type that a
    `replacements.string` package-move rule can reach with one rule — `!mc2111-rendertype-import`
    rewrites `import net.minecraft.client.renderer.rendertype.*;` and thereby serves all six of that
    package's classes — so this tree has them deliberately, and a resolver that ignores them silently
    loses every member of the class they name. See `resolve_type`.
    """
    return [m[:-2] for m in re.findall(r"^import\s+([\w.$]+\.\*)\s*;", text, re.M)]


def resolve_type(spec: str, imports: dict[str, str], probe=None,
                 wildcards: list[str] | None = None) -> str:
    """`MapRenderer.MapInstance` -> `net.minecraft.client.gui.MapRenderer$MapInstance`.

    ⚠️ A name that arrives by **wildcard** import has no entry in `imports`, and returning the bare
    simple name for it is not a harmless miss: `check` treats an owner outside `net.minecraft.` as a
    loader class it cannot read and says nothing at all about it. `EntityMixin` imports
    `net.minecraft.world.entity.*`, so its 13 shadows and injections into `Entity` — the largest
    mixin in the tree — were verified on **no node** until this fallback existed. `probe` answers
    whether a candidate FQN exists in this node's jars, which is what picks the right package when a
    file carries several wildcards.
    """
    if "." not in spec:
        if spec in imports:
            return imports[spec]
        for pkg in wildcards or []:
            candidate = f"{pkg}.{spec}"
            if probe and probe(candidate) is not None:
                return candidate
        return spec
    head, *rest = spec.split(".")
    if head in imports:
        return "$".join([imports[head], *rest])
    for pkg in wildcards or []:
        candidate = "$".join([f"{pkg}.{head}", *rest])
        if probe and probe(candidate) is not None:
            return candidate
    return spec


def match_paren(text: str, open_idx: int) -> int:
    """Index just past the `)` matching the `(` at open_idx."""
    depth = 0
    in_str = False
    i = open_idx
    while i < len(text):
        c = text[i]
        if in_str:
            if c == "\\":
                i += 1
            elif c == '"':
                in_str = False
        elif c == '"':
            in_str = True
        elif c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
            if depth == 0:
                return i + 1
        i += 1
    return len(text)


HANDLER = re.compile(
    r"\b(?:private|public|protected)\s+((?:static\s+|final\s+|abstract\s+|synchronized\s+)*)"
    r"[\w.$<>,\[\]?\s]*?\s([\w$]+)\s*\(")

SUGAR = ("Local", "Share", "Cancellable", "CallbackInfo", "CallbackInfoReturnable", "Operation")


def split_params(raw: str) -> list[str]:
    """Top-level comma split, so `@Local(argsOnly = true) T x` stays one parameter."""
    out, depth, current = [], 0, ""
    for c in raw:
        if c in "(<[":
            depth += 1
        elif c in ")>]":
            depth -= 1
        if c == "," and depth == 0:
            out.append(current)
            current = ""
        else:
            current += c
    if current.strip():
        out.append(current)
    return [p.strip() for p in out if p.strip()]


def sugar_of(param: str) -> str | None:
    """The MixinExtras sugar annotation on one parameter, if it carries one.

    A sugared parameter is supplied by MixinExtras, not by the target's argument list, so it has to
    drop out of the mirror check. Matching is on the annotation's *last* dotted segment: a gated
    branch writes its annotations fully qualified (`@com.llamalad7.mixinextras.sugar.Local`) so the
    arm that is commented out needs no import, and a plain `"@Local" in raw` test misses those —
    which is how EntityMixin's builder capture read as a bogus mismatch until this was fixed.
    """
    for name in re.findall(r"@([\w.$]+)", param):
        if name.rsplit(".", 1)[-1] in SUGAR:
            return name.rsplit(".", 1)[-1]
    return None


def param_type(param: str) -> str:
    """The declared type of one parameter, as a simple name with annotations and generics gone."""
    param = re.sub(r"@[\w.$]+(\([^)]*\))?", " ", param)
    # Innermost-first, repeatedly: one pass leaves `Function<A, CompletableFuture<B>>` half-stripped.
    while True:
        stripped = re.sub(r"<[^<>]*>", "", param)
        if stripped == param:
            break
        param = stripped
    param = param.strip()
    tokens = param.split()
    if not tokens:
        return ""
    return tokens[0 if len(tokens) == 1 else -2].rsplit(".", 1)[-1]


@dataclass
class Handler:
    static: bool
    name: str
    params: list[str]     # declared types, simple names

    @property
    def target_params(self) -> list[str] | None:
        """The parameters that must mirror the target's, or None if the handler declares none.

        Mixin lets an `@Inject` handler take only the `CallbackInfo` — that is what lets one mixin
        file span several eras of a method whose parameters churned. Anything else has to line up.
        """
        kept = [p for p in self.params if p.split("<")[0] not in SUGAR]
        return kept or None


def parse_handler(text: str) -> Handler | None:
    m = HANDLER.search(text)
    if not m:
        return None
    open_idx = m.end() - 1
    close = match_paren(text, open_idx)
    params = [sugar_of(raw) or param_type(raw)
              for raw in split_params(text[open_idx + 1:close - 1])]
    return Handler("static" in m.group(1), m.group(2), params)


@dataclass
class Injection:
    node: str
    mc: str
    source: str
    kind: str
    owner: str          # FQN of the class the target member lives in
    name: str
    desc: str | None    # None = name-only target
    ordinal: int | None = None
    captured: str | None = None      # JVM descriptor of the @ModifyVariable captured type
    constant: str | None = None      # e.g. "intValue=-1" or "longValue=50L"
    expect: int | None = None        # declared expect=/require= on a constant injection
    handler: Handler | None = None
    own_target: bool = True          # False for an @At target in some other class
    field_desc: str | None = None    # set for an @At FIELD target
    mirror: bool = True              # False when the annotation captures locals
    name_only_field: bool = False    # a @Shadow/@Accessor names a FIELD on the target
    field_type: str | None = None    # that field's declared type, as a simple name
    member_params: list[str] | None = None   # a @Shadow/@Invoker method's declared parameter types
    at_value: str | None = None      # the @At's value= for an @At target ("INVOKE", "FIELD", …)
    sites: list[tuple[str, str, str | None]] | None = None
    """For an `@At` target, the enclosing `method =` targets it has to be found inside.

    Without this the checker only asserts that the named member *exists somewhere*, which is
    exactly how the 1.20.5 `Player#eat` → `FoodData#eat` divergence slipped through green: both
    overloads exist on `FoodData` on both loaders, only the call site differs.
    """


# Everything whose `method =` names a real target. @Accessor/@Invoker are deliberately absent: they
# are checked by the compiler's own signature on the generated tree only loosely, and their targets
# are derived from the method name rather than written out.
ANNOTATIONS = ("Inject|ModifyVariable|ModifyConstant|ModifyExpressionValue|ModifyReturnValue|"
               "ModifyArg|ModifyArgs|Redirect|WrapOperation|WrapWithCondition")

METHOD_SPEC = re.compile(r"^(?:L([\w/$]+);)?([\w$<>]+)(\(.*)?$")

# Mixin's *other* target selector. `TargetSelector.parse` hands anything **ending** in `/` to
# `MemberMatcher`, whose own pattern is `((owner|name|desc)\s*=\s*)?/(.*?)(?<!\\)/` and which matches
# with `find()` — a substring, not an anchored match — against the name when no prefix is given.
# It is how a synthetic lambda gets targeted without naming its index, which shifts with any loader
# patch that adds or removes a lambda earlier in the enclosing method.
REGEX_SPEC = re.compile(r"^(?:(owner|name|desc)\s*=\s*)?/(.*)/$", re.S)

# `@Shadow`, `@Accessor` and `@Invoker` name a target member just as surely as an injection does, and
# they fail just as hard: 1.20.2 renaming `MapDecoration.getType()` to `type()` breaks the @Shadow
# before any injection is even considered.
#
# The optional `(?:[\w.]+\.)?` is the same fix ANNOTATIONS' own matcher carries, and for the same
# reason: a Stonecutter arm cannot add an import, so every member annotation inside one is obliged to
# be written out in full — `@org.spongepowered.asm.mixin.Shadow`. Anchored at `@Shadow` this pattern
# saw none of them, which left every shadow, accessor and invoker in a gated arm silently unchecked.
MEMBER_ANNO = re.compile(r"@(?:[\w.]+\.)?(Shadow|Accessor|Invoker)\b\s*(\((?:[^()]|\([^()]*\))*\))?")


def implied_member(kind: str, method_name: str) -> str:
    """The member an unnamed `@Accessor`/`@Invoker` refers to, per Mixin's prefix convention."""
    for prefix in (("get", "set", "is") if kind == "Accessor" else ("invoke", "call")):
        if method_name.startswith(prefix) and len(method_name) > len(prefix):
            rest = method_name[len(prefix):]
            return rest[0].lower() + rest[1:]
    return method_name


def parse_members(node: str, mc: str, path: Path, text: str,
                  owner_at) -> tuple[list[Injection], list[str]]:
    """Every `@Shadow`/`@Accessor`/`@Invoker` target declared in one mixin file."""
    found: list[Injection] = []
    problems: list[str] = []
    for m in MEMBER_ANNO.finditer(text):
        kind = m.group(1)
        explicit = re.search(r'"([^"]+)"', m.group(2) or "")
        tail = text[m.end():m.end() + 600]
        stop = min((i for i in (tail.find(";"), tail.find("{")) if i >= 0), default=-1)
        if stop < 0:
            problems.append(f"{path.name}: @{kind} with no parseable declaration")
            continue
        decl = re.sub(r"@[\w.$]+\s*(\([^()]*\))?", " ", tail[:stop])
        is_method = "(" in decl
        if is_method:
            names = re.findall(r"([\w$]+)\s*\(", decl)
            if not names:
                problems.append(f"{path.name}: @{kind} with no parseable method name")
                continue
            declared = names[-1]
        else:
            names = re.findall(r"([\w$]+)\s*$", decl.strip())
            if not names:
                problems.append(f"{path.name}: @{kind} with no parseable field name")
                continue
            declared = names[-1]

        if kind == "Shadow":
            target = explicit.group(1) if explicit else declared
            is_field = not is_method
        else:
            # An @Accessor is always a method in the mixin but a *field* on the target; an @Invoker
            # is a method either way.
            target = explicit.group(1) if explicit else implied_member(kind, declared)
            is_field = kind == "Accessor"

        # A shadowed or invoked method is matched by name *and* descriptor, so the declaration's own
        # parameters are the signature being asserted — mirroring them catches a target that kept its
        # name and changed its arguments (MapDecoration.Type's constructor gaining a name + flag in
        # 1.20.2 is that case, and only the descriptor tells you).
        params = None
        if is_method and not is_field:
            open_idx = tail.index("(", tail.index(declared))
            close = match_paren(tail, open_idx)
            params = [param_type(raw) for raw in split_params(tail[open_idx + 1:close - 1])]

        # A shadowed or accessed FIELD is matched by name and descriptor too, and for a long time
        # this checker asserted only the name — so 1.21.11 retyping `SplashRenderer#splash` from
        # `String` to `Component` stayed green here and was caught by javac, which it will not
        # always be: a retyped field that still compiles fails at class-load exactly like a missing
        # one. The declared type is a simple name (`param_type` drops generics, which erasure drops
        # too), compared against `simple_name` of the real descriptor.
        field_type = None
        if is_field:
            if is_method:
                # @Accessor: the mixin member is a method but the target is a field, so the type is
                # the getter's return or the setter's single parameter.
                ret = re.search(r"([\w$.<>,\[\]]+)\s+" + re.escape(declared) + r"\s*\(", decl)
                open_idx = tail.index("(", tail.index(declared))
                close = match_paren(tail, open_idx)
                accessor_params = split_params(tail[open_idx + 1:close - 1])
                if ret and param_type(ret.group(1)) != "void":
                    field_type = param_type(ret.group(1))
                elif len(accessor_params) == 1:
                    field_type = param_type(accessor_params[0])
            else:
                field_type = param_type(decl)
            # An initialiser or an unparseable modifier list leaves a token that is not a type;
            # asserting on it would be a false red, so drop to the name-only check.
            if field_type and not re.fullmatch(r"[\w$.]+(?:\[\])*", field_type):
                field_type = None
            if field_type in ("void", "final", "static", "private", "public", "protected"):
                field_type = None

        found += [Injection(node, mc, path.name, kind, owner, target, None,
                            name_only_field=is_field, member_params=params, field_type=field_type)
                  for owner in owner_at(m.start())]
    return found, problems


def parse_mixin_source(node: str, mc: str, path: Path) -> tuple[list[Injection], list[str]]:
    """Every injection point declared by one generated mixin file, plus any parse complaints."""
    text = strip_comments(path.read_text())
    imports = resolve_imports(text)
    wildcards = wildcard_packages(text)
    probe = (lambda fqn: members(node, fqn)) if wildcards else None
    problems: list[str] = []

    # A file can hold several @Mixin classes (the sprite loader's nested accessor, for one), so each
    # injection belongs to the nearest @Mixin above it rather than to the first in the file.
    #
    # A @Mixin may also name *several* targets (`@Mixin({Pillager.class, Vindicator.class, …})`), and
    # every one of them has to satisfy every injection independently — checking only the first is how
    # IllagerMixin's `isAlliedTo` passed here and then crashed a 1.21 server, because 1.20.6 pulled
    # that method up to AbstractIllager and only Evoker kept an override.
    scopes: list[tuple[int, list[str]]] = []
    for m in re.finditer(r"@Mixin\s*\(", text):
        body = text[m.end() - 1:match_paren(text, m.end() - 1)]
        names = re.findall(r"([\w.$]+)\.class", body) or re.findall(r'"([^"]+)"', body)
        if names:
            scopes.append((m.start(), [resolve_type(n, imports, probe, wildcards) for n in names]))
    if not scopes:
        return [], [f"{path.name}: no resolvable @Mixin(...) target"]

    def owner_at(index: int) -> list[str]:
        above = [t for start, t in scopes if start < index]
        return above[-1] if above else scopes[0][1]

    injections, member_problems = parse_members(node, mc, path, text, owner_at)
    problems += member_problems

    # The optional package prefix matters: a gated-out Stonecutter arm has to spell its annotations
    # fully-qualified so the other era needs no import, so `@com.llamalad7.mixinextras.injector.
    # ModifyExpressionValue(...)` is a normal sight here and used to be skipped silently.
    for anno in re.finditer(rf"@(?:[\w.]+\.)?({ANNOTATIONS})\s*\(", text):
        kind = anno.group(1)
        start = anno.end() - 1
        end = match_paren(text, start)
        body = text[start:end]
        tail = text[end:end + 600]
        mixin_targets = owner_at(anno.start())

        spec_group = re.search(r"method\s*=\s*(\{[^}]*}|\"[^\"]*\")", body)
        methods = re.findall(r'"([^"]+)"', spec_group.group(1)) if spec_group else []
        if not methods:
            problems.append(f"{path.name}: @{kind} with no method= target")
            continue

        constant, expect = None, None
        cm = re.search(r"@Constant\s*\(", body)
        if cm:
            constant = body[cm.end():match_paren(body, cm.end() - 1) - 1].strip()
        elif re.search(r'value\s*=\s*"CONSTANT"', body):
            am = re.search(r'args\s*=\s*"([^"]+)"', body)
            constant = am.group(1) if am else None
        if constant:
            em = re.search(r"\b(?:expect|require)\s*=\s*(\d+)", body)
            expect = int(em.group(1)) if em else None

        handler = parse_handler(tail)
        if handler is None:
            problems.append(f"{path.name}: @{kind} on {methods} has no parseable handler")

        ordinal = captured = None
        if kind == "ModifyVariable" and re.search(r"argsOnly\s*=\s*true", body):
            om = re.search(r"ordinal\s*=\s*(\d+)", body)
            ordinal = int(om.group(1)) if om else 0
            if handler and handler.params:
                captured = PRIMITIVES.get(handler.params[0])
                if captured is None:
                    problems.append(f"{path.name}: ordinal check skipped, non-primitive capture "
                                    f"{handler.params[0]}")

        # LocalCapture appends the captured locals to the handler's parameter list, so the mirror
        # check cannot compare it against the target's arguments alone.
        mirror = "locals" not in body

        sites: list[tuple[str, str, str | None]] = []
        for spec in methods:
            rx = REGEX_SPEC.match(spec)
            if rx:
                if rx.group(1) not in (None, "name"):
                    problems.append(f"{path.name}: {rx.group(1)}= regex selector {spec!r} "
                                    f"is not checked")
                    continue
                # No Injection: there is no member name to assert the existence of. The selector is
                # still carried into `sites`, so the @At below is checked against every method it
                # picks — and a selector that picks nothing surfaces there as "never referenced".
                sites += [(owner, spec, None) for owner in mixin_targets]
                continue
            sm = METHOD_SPEC.match(spec)
            if not sm:
                problems.append(f"{path.name}: unparsed method target {spec!r}")
                continue
            owners = [sm.group(1).replace("/", ".")] if sm.group(1) else mixin_targets
            injections += [Injection(node, mc, path.name, kind, owner, sm.group(2), sm.group(3),
                                     ordinal, captured, constant, expect, handler, mirror=mirror)
                           for owner in owners]
            sites += [(owner, sm.group(2), sm.group(3)) for owner in owners]

        for at in re.finditer(r'target\s*=\s*"([^"]+)"', body):
            spec = at.group(1)
            # The `value =` that governs this target is the nearest one before it: an annotation can
            # hold several `@At`s (a @Slice's from/to, or @Redirect's at + slice), each with its own.
            values = re.findall(r'value\s*=\s*"(\w+)"', body[:at.start()])
            at_value = values[-1] if values else None
            # @At("NEW") names a constructor, and spells it as the *type being constructed* —
            # "(FFFF)Lorg/joml/Vector4f;" for the descriptor form, a bare "Lorg/joml/Vector4f;" when
            # the arguments are left open. Neither shape is a method reference, so both fall through
            # the two regexes below; Mixin resolves them to the owner's <init>, which is how the jar
            # declares them and therefore how they are checked.
            if at_value == "NEW":
                nm = re.match(r"(\(.*\))?L([\w/$]+);$", spec)
                if nm:
                    desc = nm.group(1) + "V" if nm.group(1) else None
                    injections.append(Injection(node, mc, path.name, f"{kind}@At",
                                                nm.group(2).replace("/", "."), "<init>", desc,
                                                own_target=False, at_value=at_value, sites=sites))
                    continue
            tm = re.match(r"L([\w/$]+);([\w$<>]+)(\(.*)$", spec)
            if tm:
                injections.append(Injection(node, mc, path.name, f"{kind}@At",
                                            tm.group(1).replace("/", "."), tm.group(2), tm.group(3),
                                            own_target=False, at_value=at_value, sites=sites))
                continue
            fm = re.match(r"L([\w/$]+);([\w$]+):(\[*(?:L[\w/$]+;|[ZBCSIJFDV]))$", spec)
            if fm:
                injections.append(Injection(node, mc, path.name, f"{kind}@At",
                                            fm.group(1).replace("/", "."), fm.group(2), None,
                                            own_target=False, field_desc=fm.group(3),
                                            at_value=at_value, sites=sites))
                continue
            if spec.startswith("L") or spec.startswith("("):
                problems.append(f"{path.name}: unparsed @At target {spec!r}")

    return injections, problems


# ──────────────────────────────────────────────────────────────────────────── the checks ──

def arg_types(desc: str) -> list[str]:
    """Split a method descriptor's argument list into JVM type strings."""
    args = desc[1:desc.rindex(")")]
    out: list[str] = []
    i = 0
    while i < len(args):
        start = i
        while args[i] == "[":
            i += 1
        if args[i] == "L":
            i = args.index(";", i) + 1
        else:
            i += 1
        out.append(args[start:i])
    return out


def simple_name(desc: str) -> str:
    """A JVM type descriptor as the simple name a handler would spell it with."""
    depth = 0
    while desc.startswith("["):
        depth += 1
        desc = desc[1:]
    base = desc[1:-1].rsplit("/", 1)[-1].rsplit("$", 1)[-1] if desc.startswith("L") else \
        next(k for k, v in PRIMITIVES.items() if v == desc)
    return base + "[]" * depth


def _literal(text: str) -> str:
    """A javap literal, as a regex that will not also match a longer one."""
    return re.escape(text) + r"(?![\w.])"


def constant_forms(spec: str) -> list[str] | None:
    """Regexes for how javap would render a `@Constant`, or None if we can't say confidently.

    Patterns rather than plain substrings because javap column-aligns an operand: the opcode forms
    have to tolerate a run of spaces, so `sipush        -256` counts as the one hit it is. Only the
    push opcodes carry an operand — an `ldc` is matched through its constant-pool comment instead.
    """
    m = re.match(r"(\w+)\s*=\s*([^,]+)", spec.strip())
    if not m:
        return None
    kind, raw = m.group(1), m.group(2).strip()
    if kind == "intValue":
        v = int(raw, 0)
        forms = ["iconst_m1"] if v == -1 else [f"iconst_{v}"] if 0 <= v <= 5 else []
        return [_literal(f) for f in forms] + \
            [rf"bipush\s+{_literal(str(v))}", rf"sipush\s+{_literal(str(v))}",
             _literal(f"// int {v}")]
    if kind == "longValue":
        v = int(raw.rstrip("lL"), 0)
        forms = [f"lconst_{v}"] if v in (0, 1) else []
        return [_literal(f) for f in forms] + [_literal(f"// long {v}l")]
    if kind == "floatValue":
        v = float(raw.rstrip("fF"))
        forms = [f"fconst_{int(v)}"] if v in (0.0, 1.0, 2.0) else []
        return [_literal(f) for f in forms] + [_literal(f"// float {v}f")]
    if kind == "doubleValue":
        v = float(raw.rstrip("dD"))
        forms = [f"dconst_{int(v)}"] if v in (0.0, 1.0) else []
        return [_literal(f) for f in forms] + [_literal(f"// double {v}d")]
    if kind == "stringValue":
        return [_literal(f"// String {raw.strip(chr(34))}")]
    return None


# A `@At` whose target names a member the enclosing method never touches is an injection failure at
# runtime ("failed injection check, (0/1) succeeded"), and it is invisible to the existence checks
# above — the member is there, it is just not called from *that* method. These are the `value =`s
# whose target is a call/access site inside the enclosing method, so presence can be asserted.
SITE_VALUES = {"INVOKE", "INVOKE_ASSIGN", "INVOKE_STRING", "FIELD"}


def site_bodies(node: str, sites: list[tuple[str, str, str | None]]) -> list[tuple[str, str]]:
    """(declaring class, disassembly) for every overload the enclosing `method =` selectors pick."""
    out: list[tuple[str, str]] = []
    for owner, name, desc in sites:
        cls = members(node, owner)
        if cls is None:
            continue
        rx = REGEX_SPEC.match(name)
        if rx:
            # The selector is read out of Java source, so a `\$` in the regex is spelled `\\$`
            # there; collapse the doubling before compiling.
            pattern = re.compile(rx.group(2).replace("\\\\", "\\"))
            hit = [(owner, cls.code.get((n, d), ""))
                   for n in sorted(cls.methods) if pattern.search(n)
                   for d in sorted(cls.methods[n])]
            # Matching nothing is a stale selector. One empty body says so: the caller's check
            # then reports the injection point as absent rather than passing on no evidence.
            out += hit or [(owner, "")]
            continue
        if name not in cls.methods:
            continue
        for d in ([desc] if desc else sorted(cls.methods[name])):
            if d in cls.methods[name]:
                out.append((owner, cls.code.get((name, d), "")))
    return out


def site_forms(inj: Injection, enclosing_owner: str) -> list[str]:
    """How javap renders the constant-pool comment for this injection point's insn.

    javap drops the owner when it is the class being disassembled and quotes `<init>`, so a call to
    a sibling method reads `// Method g:()I` and a constructor `// Method Foo."<init>":()V`.
    """
    slash = inj.owner.replace(".", "/")
    same = inj.owner == enclosing_owner
    if inj.field_desc is not None:
        forms = [f"Field {slash}.{inj.name}:{inj.field_desc}"]
        return forms + ([f"Field {inj.name}:{inj.field_desc}"] if same else [])
    member = f'"{inj.name}"' if inj.name.startswith("<") else inj.name
    tail = inj.desc or ""   # a name-only @At target matches any overload
    forms = [f"Method {slash}.{member}:{tail}", f"InterfaceMethod {slash}.{member}:{tail}"]
    return forms + ([f"Method {member}:{tail}"] if same else [])


def check(inj: Injection) -> list[str]:
    target = members(inj.node, inj.owner)
    if target is None:
        if inj.owner.startswith("net.minecraft."):
            return [f"class {inj.owner} not found in MC {inj.mc}"]
        # A name with no package is one the import resolver could not place, NOT a third-party
        # class — and staying quiet about it is how EntityMixin's 13 points went unchecked on every
        # node for the whole walk. Say so instead; the fix is usually a wildcard import the
        # resolver cannot see through (see `resolve_type`).
        if "." not in inj.owner:
            return [f"unresolved @Mixin target `{inj.owner}` — no import places it"]
        return []   # a loader or library class; not in the jars we read, nothing to say about it

    # An `@At` target is checked against the whole hierarchy — the bytecode names the static type at
    # the call site, so `ClientLevel.getTimeOfDay` is a legal owner for a method declared on Level.
    # Nothing else is: Mixin resolves a `method =` selector, a @Shadow and an @Accessor against the
    # target class *alone* and fails on an inherited one — "was not located in the target class" for
    # the members (the minecart damage accessors moving to VehicleEntity in 1.20.2), "could not find
    # any targets matching" for an injection (IllagerMixin's isAlliedTo on 1.20.6+). `own_target` is
    # exactly that distinction: it is False only for the classes an @At names.
    inherited = not inj.own_target
    find_field = (lambda: lookup_field(inj.node, inj.owner, inj.name)) if inherited \
        else (lambda: target.fields.get(inj.name))
    find_method = (lambda: lookup_method(inj.node, inj.owner, inj.name)) if inherited \
        else (lambda: (target, target.methods[inj.name]) if inj.name in target.methods else None)

    if inj.name_only_field:
        actual = find_field()
        if actual is None:
            return [f"{inj.owner}#{inj.name} field missing"]
        if inj.field_type and simple_name(actual) != inj.field_type:
            return [f"{inj.owner}#{inj.name} is {simple_name(actual)}, "
                    f"the @{inj.kind} declares {inj.field_type}"]
        return []

    if inj.field_desc is not None:
        actual = find_field()
        if actual is None:
            return [f"{inj.owner}#{inj.name} field missing"]
        if actual != inj.field_desc:
            return [f"{inj.owner}#{inj.name} is {actual}, target says {inj.field_desc}"]
        return []

    found = find_method()
    if found is None:
        return [f"{inj.owner}#{inj.name}{inj.desc or ''} missing — no method of that name at all"]
    cls, overloads = found

    if inj.desc and inj.desc not in overloads:
        return [f"{inj.owner}#{inj.name}{inj.desc} missing — present with {sorted(overloads)}"]

    if inj.member_params is not None:
        shapes = {tuple(simple_name(t) for t in arg_types(d)): d for d in overloads}
        if tuple(inj.member_params) not in shapes:
            return [f"{inj.owner}#{inj.name} takes {sorted(shapes)} "
                    f"but the @{inj.kind} declares {inj.member_params}"]
        return []

    errors: list[str] = []
    # A name-only target may be overloaded, so every candidate has to satisfy the remaining checks.
    candidates = [inj.desc] if inj.desc else sorted(overloads)

    if inj.ordinal is not None and inj.captured:
        for desc in candidates:
            count = sum(1 for t in arg_types(desc) if t == inj.captured)
            if count <= inj.ordinal:
                errors.append(
                    f"{inj.owner}#{inj.name}{desc}: ordinal {inj.ordinal} of type {inj.captured} "
                    f"but only {count} such argument(s)")

    if inj.own_target and inj.handler:
        for desc in candidates:
            target_static = overloads.get(desc)
            if target_static is not None and target_static != inj.handler.static and inj.name != "<init>":
                errors.append(
                    f"{inj.owner}#{inj.name}{desc} is {'static' if target_static else 'non-static'} "
                    f"but handler {inj.handler.name} is "
                    f"{'static' if inj.handler.static else 'non-static'}")

            declared = inj.handler.target_params
            if inj.kind == "Inject" and inj.mirror and declared is not None:
                expected = [simple_name(t) for t in arg_types(desc)]
                if declared != expected:
                    errors.append(f"{inj.owner}#{inj.name}{desc}: handler {inj.handler.name} declares "
                                  f"{declared} but the target takes {expected}")

    if inj.constant and not errors:
        try:
            forms = constant_forms(inj.constant)
        except ValueError:
            forms = None
        if forms:
            for desc in candidates:
                body = cls.code.get((inj.name, desc), "")
                hits = sum(len(re.findall(f, body)) for f in forms)
                if hits == 0:
                    errors.append(f"{inj.owner}#{inj.name}{desc}: constant {inj.constant} is never "
                                  f"loaded in that method")
                elif inj.expect is not None and hits != inj.expect:
                    errors.append(f"{inj.owner}#{inj.name}{desc}: constant {inj.constant} loaded "
                                  f"{hits}x but the injection expects {inj.expect}")

    if inj.sites and inj.at_value in SITE_VALUES and not errors:
        bodies = site_bodies(inj.node, inj.sites)
        # No bodies means the enclosing method lives in a class these jars don't hold (a loader
        # class); the `method =` selector's own Injection already reports it if it is a MC one.
        if bodies and not any(any(f in body for f in site_forms(inj, owner))
                              for owner, body in bodies):
            where = ", ".join(f"{o.rsplit('.', 1)[-1]}#{n}{d or ''}" for o, n, d in inj.sites)
            errors.append(f"{inj.owner}#{inj.name}{inj.desc or inj.field_desc or ''} is never "
                          f"referenced in {where} — the injection point does not exist there")

    return errors


# ─────────────────────────────────────────────────────────────────────────────── driver ──

def mixin_config(node: str) -> Path | None:
    """The processed config if there is one, else the Stonecutter-generated template."""
    base = ROOT / "versions" / node / "build"
    for candidate in (base / "resources/main" / MIXIN_CONFIG,
                      base / "generated/stonecutter/main/resources" / MIXIN_CONFIG):
        if candidate.exists():
            return candidate
    return None


def node_injections(node: str) -> tuple[list[Injection], list[str]]:
    mc = node.rsplit("-", 1)[0]
    config = mixin_config(node)
    if config is None:
        return [], [f"{node}: no generated {MIXIN_CONFIG} — run :{node}:processResources"]

    # The template still carries `${java}`, which is not valid JSON on its own but always sits
    # inside a string, so a straight parse works either way.
    cfg = json.loads(config.read_text())
    pkg = cfg["package"].replace(".", "/")
    entries = list(cfg.get("mixins", [])) + list(cfg.get("client", [])) + list(cfg.get("server", []))
    if not entries:
        return [], [f"{node}: mixin config lists no mixins"]

    src = ROOT / "versions" / node / "build/generated/stonecutter/main/java"
    injections: list[Injection] = []
    problems: list[str] = []
    seen: set[str] = set()
    for entry in entries:
        # A nested mixin (`…$PalettedPermutationsAccessor`) lives in its outer class's file, and the
        # client list repeats what the common list already named.
        file_entry = entry.split("$", 1)[0]
        if file_entry in seen:
            continue
        seen.add(file_entry)
        path = src / pkg / (file_entry.replace(".", "/") + ".java")
        if not path.exists():
            problems.append(f"{node}: {entry} listed in {MIXIN_CONFIG} but {path} is missing")
            continue
        found, issues = parse_mixin_source(node, mc, path)
        injections += found
        problems += [f"{node}: {p}" for p in issues]
    return injections, problems


def main(argv: list[str]) -> int:
    nodes = argv or sorted(p.name for p in (ROOT / "versions").iterdir() if p.is_dir())

    failures: list[str] = []
    problems: list[str] = []
    total = 0

    for node in nodes:
        injections, issues = node_injections(node)
        problems += issues
        errors: list[str] = []
        for inj in injections:
            total += 1
            for err in check(inj):
                errors.append(f"  {inj.source} [{inj.kind}] {err}")
        status = "OK" if not errors else f"{len(errors)} PROBLEM(S)"
        print(f"{node:<20} {len(injections):>4} injections  {status}")
        for err in errors:
            print(err)
        failures += [f"{node}: {e.strip()}" for e in errors]

    print(f"\n{len(nodes)} nodes, {total} injection points checked")
    if problems:
        print(f"\n{len(problems)} setup problem(s):")
        for p in problems:
            print(f"  {p}")
    if failures:
        print(f"\n{len(failures)} missing target(s)")
        return 1
    print("all targets resolve" if not problems else "all targets resolve (with setup problems above)")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
