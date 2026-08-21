#!/usr/bin/env python3
"""Publish the Alex's Caves Continued matrix to CurseForge — one file per node.

Mirrors ``scripts/modrinth_upload.py``: reads the built jars under
``versions/*/build/libs/`` (the 58-node stonecutter matrix) and uploads one file per
node with the changelog from ``modrinth-changelog.md``.

CurseForge's *upload* API is a different beast from Modrinth's:
  - Auth is a header ``X-Api-Token`` (upload tokens: legacy.curseforge.com/account/api-tokens).
  - ``gameVersions`` are **numeric ids**, not strings. The MC version *and* the mod loader
    are both entries in ``GET /api/game/versions`` (different ``gameVersionTypeID``s), so
    each upload sends ``[<mc id>, <loader id>]``. We resolve them by name at runtime
    instead of hardcoding, because CF adds ids for every new MC release.
  - There is **no endpoint to list a project's existing files** (that lives in the separate
    Core API, which needs its own key), so re-runs can't ask the server what's already
    there. We keep a local ledger at ``scripts/.cf_uploaded.json`` and skip nodes recorded
    in it. ``--force`` ignores the ledger.

Usage:
  python3 scripts/curseforge_upload.py --check                 # validate token
  python3 scripts/curseforge_upload.py --versions              # dump CF's MC/loader ids
  python3 scripts/curseforge_upload.py --list                  # what would upload (+ id mapping)
  python3 scripts/curseforge_upload.py --only 1.20.1-forge     # single node (test upload)
  python3 scripts/curseforge_upload.py                         # upload all not-yet-recorded
  python3 scripts/curseforge_upload.py --force                 # ignore the local ledger

Config:
  Project id  -- numeric, from the project page sidebar. Set ``CURSEFORGE_PROJECT_ID`` or
                 put it in ``scripts/.cf_project_id`` (gitignored). It is NOT the slug.
  Token       -- ``CURSEFORGE_TOKEN`` env or ``scripts/.cf_token`` (gitignored).

The **required CodxLib relation is NOT set here.** CurseForge relations carry no version
and are a project-level setting on the web UI (Relations tab -> Required Dependency ->
codxlib); the upload API has no field for them. The runtime floor is enforced by each
jar's own manifest, not by the store.
"""
import sys, os, re, json, time, glob, subprocess, urllib.request, urllib.error

HERE = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(HERE)  # scripts/ -> repo root
API = "https://minecraft.curseforge.com/api"
UA = "alexscaves-continued-publisher/1.0 (+https://github.com/Codx-org/AlexsCavesContinued)"
LEDGER = os.path.join(HERE, ".cf_uploaded.json")


def mod_version():
    toml = os.path.join(PROJECT_ROOT, "stonecutter.properties.toml")
    with open(toml) as f:
        for line in f:
            s = line.strip()
            if s.startswith("mod.version"):
                return s.split("=", 1)[1].strip().strip('"')
    raise SystemExit("mod.version not found in stonecutter.properties.toml")


MOD_VERSION = mod_version()


def _read_cfg(env, filename, what):
    v = os.environ.get(env)
    if v:
        return v.strip()
    p = os.path.join(HERE, filename)
    if os.path.exists(p):
        with open(p) as f:
            return f.read().strip()
    raise SystemExit(f"No {what}: set {env} or create scripts/{filename}")


def token():
    return _read_cfg("CURSEFORGE_TOKEN", ".cf_token", "token")


def project_id():
    pid = _read_cfg("CURSEFORGE_PROJECT_ID", ".cf_project_id", "project id")
    if not pid.isdigit():
        raise SystemExit(f"Project id must be numeric (got '{pid}') — use the id from the "
                         "project page sidebar, not the URL slug.")
    return pid


def api_get(path):
    req = urllib.request.Request(API + path,
                                 headers={"X-Api-Token": token(), "User-Agent": UA})
    with urllib.request.urlopen(req) as r:
        return json.load(r)


def check():
    try:
        v = api_get("/game/versions")
        print(f"OK  token valid — CurseForge returned {len(v)} game-version entries")
        return v
    except urllib.error.HTTPError as e:
        print(f"FAIL  HTTP {e.code}: {e.read().decode()[:200]}")
        sys.exit(1)


# --- CurseForge game-version id resolution -------------------------------
_LOADER_CF_NAME = {"fabric": "Fabric", "forge": "Forge", "neoforge": "NeoForge"}


_MC_TYPE_SLUG_RE = re.compile(r"^minecraft-\d")

# Alex's Caves Continued is a content mod: blocks, items, entities and worldgen all have to
# exist on both sides, so every upload claims both. A Mods-class project *must* tag at least
# one ("errorCode 1021 — You must select at least one version from the environment group").
_ENVIRONMENTS = ("client", "server")


def version_index():
    """{'mc': {name -> id}, 'loader': {lowername -> id}, 'env': {slug -> id}} from CF.

    The same MC version name appears under *several* version types — e.g. '1.20.2' exists
    as id 10236 under type 'minecraft-1-20', 10326 under an unnamed legacy type, and
    10864 under 'addons'. Only the ``minecraft-<digit>…`` types are valid for a project in
    the **Mods** class; sending an id from another type fails the upload with
    ``errorCode 1009 — Invalid game version ID: <id> belongs to an invalid dependency``.
    So filter by version *type*, never by name alone — matching on name and taking
    whatever the API happens to return first is how we hit exactly that error.

    Mod loaders live under the 'modloader' type. Resolved by name so new MC releases need
    no code change.
    """
    types = {t["id"]: t for t in api_get("/game/version-types")}
    mc_type_ids, loader_type_ids, env_type_ids = set(), set(), set()
    for tid, t in types.items():
        slug = (t.get("slug") or "").lower()
        if slug == "modloader":
            loader_type_ids.add(tid)
        elif slug == "environment":
            env_type_ids.add(tid)
        elif _MC_TYPE_SLUG_RE.match(slug):
            mc_type_ids.add(tid)
    mc, loader, env = {}, {}, {}
    for v in api_get("/game/versions"):
        name = v.get("name", "")
        tid = v.get("gameVersionTypeID")
        if tid in loader_type_ids:
            loader.setdefault(name.lower(), v["id"])
        elif tid in env_type_ids:
            env.setdefault(name.lower(), v["id"])
        elif tid in mc_type_ids and re.fullmatch(r"\d+(\.\d+)*", name):
            mc.setdefault(name, v["id"])
    return {"mc": mc, "loader": loader, "env": env}


# --- discover jars -------------------------------------------------------
JAR_RE = re.compile(r"alexscaves-" + re.escape(MOD_VERSION) + r"-(fabric|forge|neoforge)\+(.+)\.jar$")


def _vkey(mc):
    # numeric ascending sort: "1.20.1" < "1.21" < "1.21.10" < "26.1" < "26.2"
    return tuple(int(x) for x in re.findall(r"\d+", mc))


_LOADER_ORDER = {"fabric": 0, "forge": 1, "neoforge": 2}
_LOADER_NAME = {"fabric": "Fabric", "forge": "Forge", "neoforge": "NeoForge"}


def nodes():
    out = []
    pat = os.path.join(PROJECT_ROOT, "versions", "*", "build", "libs", "*.jar")
    for p in glob.glob(pat):
        b = os.path.basename(p)
        if b.endswith("-sources.jar") or b.endswith("-javadoc.jar"):
            continue
        m = JAR_RE.match(b)
        if not m:
            continue
        loader, mc = m.group(1), m.group(2)
        out.append({
            "path": p, "file": b, "loader": loader, "mc": mc,
            "key": f"{mc}-{loader}",
            "name": f"Alex's Caves Continued {MOD_VERSION} — {_LOADER_NAME[loader]} {mc}",
        })
    out.sort(key=lambda n: (_vkey(n["mc"]), _LOADER_ORDER.get(n["loader"], 9)))
    return out


def changelog():
    """The shared root changelog — the same text the Modrinth uploader sends."""
    with open(os.path.join(PROJECT_ROOT, "modrinth-changelog.md")) as f:
        return f.read()


# --- upload --------------------------------------------------------------
class HttpFail(Exception):
    def __init__(self, code, body):
        self.code = code
        self.body = body
        super().__init__(f"HTTP {code}: {body}")


def upload(node, cl, game_version_ids):
    metadata = {
        "changelog": cl,
        "changelogType": "markdown",
        "displayName": node["name"],
        "gameVersions": game_version_ids,
        "releaseType": "release",
    }
    # --form-string (not -F): the changelog is markdown, and -F would treat a leading
    # '@' or '<' in a value as a file reference. CurseForge parses this part as JSON text,
    # which is exactly what its own documented curl example sends.
    cmd = [
        "curl", "-sS", "-X", "POST", f"{API}/projects/{project_id()}/upload-file",
        "-H", f"X-Api-Token: {token()}",
        "-H", f"User-Agent: {UA}",
        "-w", "\n__HTTP__%{http_code}",
        "--form-string", "metadata=" + json.dumps(metadata),
        "-F", f"file=@{node['path']};type=application/java-archive;filename={node['file']}",
    ]
    p = subprocess.run(cmd, capture_output=True, text=True)
    out = p.stdout
    code = "000"
    if "__HTTP__" in out:
        out, code = out.rsplit("__HTTP__", 1)
        code = code.strip()
    if code != "200":
        raise HttpFail(int(code) if code.isdigit() else 0, out.strip()[:300])
    return json.loads(out)


def load_ledger():
    if os.path.exists(LEDGER):
        with open(LEDGER) as f:
            return json.load(f)
    return {}


def save_ledger(d):
    with open(LEDGER, "w") as f:
        json.dump(d, f, indent=2, sort_keys=True)


def ledger_key(n):
    """Ledger keys are ``<version>/<node>``, so a new release is never mistaken for a
    node that was already uploaded under an older one.

    (CodxLib 1.3.6 wrote bare node keys into its own ledger and had to grow this prefix
    later; this tree has it from the first release.)
    """
    return f"{MOD_VERSION}/{n['key']}"


# --- main ----------------------------------------------------------------
_FLAGS_WITH_VALUE = {"--only"}
_FLAGS = {"--check", "--versions", "--list", "--force"} | _FLAGS_WITH_VALUE


def _reject_unknown_flags(args):
    """A bare run of this script uploads every node. There is no dry-run mode, so an
    unrecognised argument must NOT fall through to that -- `--help` did exactly that
    once on the AlexsMobsContinued copy and put 24 versions live before it was killed.
    Use `--list` to inspect."""
    i = 0
    while i < len(args):
        a = args[i]
        if a in _FLAGS_WITH_VALUE:
            i += 2
            continue
        if a not in _FLAGS:
            raise SystemExit(
                f"Unrecognised argument {a!r}. There is no --help and no dry run: a bare "
                f"invocation UPLOADS. Known flags: {' '.join(sorted(_FLAGS))}. "
                "Use --list to see what would be uploaded.")
        i += 1


def main():
    args = sys.argv[1:]
    _reject_unknown_flags(args)
    if "--check" in args:
        check(); return
    if "--versions" in args:
        idx = version_index()
        print("Mod loaders:")
        for k, v in sorted(idx["loader"].items()):
            print(f"  {k:22} {v}")
        print(f"\nMinecraft versions ({len(idx['mc'])}):")
        for k in sorted(idx["mc"], key=_vkey):
            print(f"  {k:12} {idx['mc'][k]}")
        return

    ns = nodes()
    if "--only" in args:
        key = args[args.index("--only") + 1]
        ns = [n for n in ns if key in n["path"] or key in n["file"]]
    if not ns:
        raise SystemExit(f"No jars found for mod_version {MOD_VERSION} under "
                         "versions/*/build/libs/ — build the matrix first.")

    idx = version_index()
    env_ids = [idx["env"][e] for e in _ENVIRONMENTS if e in idx["env"]]
    if not env_ids:
        raise SystemExit("CurseForge returned no 'environment' versions — a Mods-class "
                         "project requires at least one (errorCode 1021).")
    missing, ready = [], []
    for n in ns:
        mc_id = idx["mc"].get(n["mc"])
        loader_id = idx["loader"].get(_LOADER_CF_NAME[n["loader"]].lower())
        if mc_id is None or loader_id is None:
            n["why"] = ("MC version not in CurseForge's taxonomy" if mc_id is None
                        else f"loader '{n['loader']}' not in CurseForge's taxonomy")
            missing.append(n)
        else:
            n["ids"] = [mc_id, loader_id] + env_ids
            ready.append(n)

    if "--list" in args:
        for n in ready:
            print(f"{n['key']:22} ids={n['ids']}  <- {n['file']}")
        for n in missing:
            print(f"{n['key']:22} SKIP — {n['why']}")
        print(f"\n{len(ready)} uploadable, {len(missing)} unsupported  (mod_version={MOD_VERSION})")
        return

    if missing:
        print(f"WARNING: {len(missing)} node(s) have no CurseForge game version and will be "
              "skipped:")
        for n in missing:
            print(f"  {n['key']:22} — {n['why']}")
        print()

    check()
    # --force ignores the ledger for the skip decision but must NOT discard it — an
    # emptied dict would be written back over the record of every earlier release.
    force = "--force" in args
    ledger = load_ledger()
    cl = changelog()
    ok, skip, fail = 0, 0, 0
    for i, n in enumerate(ready, 1):
        lkey = ledger_key(n)
        if not force and lkey in ledger:
            print(f"[{i}/{len(ready)}] SKIP (already uploaded, file id {ledger[lkey]}) {lkey}")
            skip += 1
            continue
        try:
            r = upload(n, cl, n["ids"])
            fid = r.get("id")
            print(f"[{i}/{len(ready)}] OK   {n['key']}  -> file id {fid}")
            ledger[lkey] = fid
            save_ledger(ledger)
            ok += 1
        except HttpFail as e:
            print(f"[{i}/{len(ready)}] FAIL {n['key']}  HTTP {e.code}: {e.body}")
            fail += 1
            if e.code in (401, 403):
                print("\nABORT: auth error on a write — the upload token is invalid or has no "
                      "access to this project. Nothing further attempted.")
                break
        time.sleep(1.0)  # CurseForge upload rate limit
    print(f"\nDone. uploaded={ok} skipped={skip} failed={fail} unsupported={len(missing)}")
    if ok:
        print(f"Ledger: {LEDGER}")


if __name__ == "__main__":
    main()
