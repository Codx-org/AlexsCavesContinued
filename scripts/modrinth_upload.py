#!/usr/bin/env python3
"""Publish the Alex's Caves Continued build matrix to Modrinth — one version per node.

Reads the built jars under ``versions/*/build/libs/`` (the 58-node stonecutter matrix)
and creates one Modrinth version per node, numbered ``<mod_version>+<loader>-<mc>``,
type ``release``, with the changelog taken from ``modrinth-changelog.md``. The mod
version is read from ``stonecutter.properties.toml`` (``mod.version``).

Auth: set ``MODRINTH_TOKEN`` in the environment, or drop the token in a ``.mr_token``
file next to this script (gitignored). The PAT needs **Read user data** and
**Create versions** scopes.

Usage:
  python3 scripts/modrinth_upload.py --check               # validate token only
  python3 scripts/modrinth_upload.py --list                # show what would upload
  python3 scripts/modrinth_upload.py --only 1.20.1-forge   # single node (test upload)
  python3 scripts/modrinth_upload.py                        # upload all not-yet-present
  python3 scripts/modrinth_upload.py --force                # ignore already-present check

Notes / API gotchas (learned the hard way, see DEVELOPMENT.md):
  - The create-version POST is multipart; the ``data`` JSON MUST be sent as a *file*
    part (``-F data=@file.json;type=application/json``), never inline — inline mangles
    JSON on the changelog's newlines/quotes, and ``;type=`` is ignored on inline values.
  - If the file part fails to attach, Modrinth still creates the version but silently
    clears loaders/game_versions (they don't stick without a file).
  - ``GET /v2/project/{id}/version`` is CACHED and under-reports straight after a bulk
    upload. Never conclude a version is broken from it — re-read ``GET /v2/version/{id}``.
  - This tree emits THREE jars per node (mod, -sources, -javadoc). Both are skipped by
    name, and JAR_RE pins the exact expected filename so a stray ``-SNAPSHOT`` can never
    be parsed as a game version.
"""
import sys, os, re, json, time, glob, subprocess, urllib.request, urllib.error

HERE = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(HERE)  # scripts/ -> repo root
API = "https://api.modrinth.com/v2"
PROJECT_ID = "cO2CvXug"  # modrinth.com/mod/alexs-caves-continued
CODXLIB_PROJECT_ID = "6oyMM4yX"  # modrinth.com/mod/codxlib — required at runtime
UA = "alexscaves-continued-publisher/1.0 (+https://github.com/Codx-org/AlexsCavesContinued)"


def mod_version():
    toml = os.path.join(PROJECT_ROOT, "stonecutter.properties.toml")
    with open(toml) as f:
        for line in f:
            s = line.strip()
            if s.startswith("mod.version"):
                return s.split("=", 1)[1].strip().strip('"')
    raise SystemExit("mod.version not found in stonecutter.properties.toml")


MOD_VERSION = mod_version()


def token():
    tok = os.environ.get("MODRINTH_TOKEN")
    if tok:
        return tok.strip()
    p = os.path.join(HERE, ".mr_token")
    if os.path.exists(p):
        with open(p) as f:
            return f.read().strip()
    raise SystemExit("No token: set MODRINTH_TOKEN or create scripts/.mr_token")


def api_get(path):
    req = urllib.request.Request(API + path, headers={"Authorization": token(), "User-Agent": UA})
    with urllib.request.urlopen(req) as r:
        return json.load(r)


def check():
    try:
        u = api_get("/user")
        print(f"OK  token valid — user '{u.get('username')}' (id {u.get('id')})")
        return u
    except urllib.error.HTTPError as e:
        print(f"FAIL  HTTP {e.code}: {e.read().decode()[:200]}")
        sys.exit(1)


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
            "version_number": f"{MOD_VERSION}+{loader}-{mc}",
            "name": f"Alex's Caves Continued {MOD_VERSION} — {_LOADER_NAME[loader]} {mc}",
        })
    out.sort(key=lambda n: (_vkey(n["mc"]), _LOADER_ORDER.get(n["loader"], 9)))
    return out


def changelog():
    with open(os.path.join(PROJECT_ROOT, "modrinth-changelog.md")) as f:
        return f.read()


# --- multipart POST ------------------------------------------------------
class HttpFail(Exception):
    def __init__(self, code, body):
        self.code = code
        self.body = body
        super().__init__(f"HTTP {code}: {body}")


def post_version(node, cl):
    data = {
        "name": node["name"],
        "version_number": node["version_number"],
        "project_id": PROJECT_ID,
        "file_parts": ["file"],
        "primary_file": "file",
        "game_versions": [node["mc"]],
        "loaders": [node["loader"]],
        # CodxLib is a separately-distributed required dependency: without the matching
        # jar the mod fails at launch with NoClassDefFoundError. Project-level (no
        # version_id) so pruning a codxlib version can never orphan these.
        "dependencies": [{"project_id": CODXLIB_PROJECT_ID, "dependency_type": "required"}],
        "version_type": "release",
        "featured": False,
        "changelog": cl,
        "status": "listed",
    }
    # Attach the data JSON as a file part — inline -F values mangle JSON (the changelog's
    # newlines/quotes) and don't honor ;type=. File parts do.
    data_path = os.path.join(HERE, ".mr_data.json")
    with open(data_path, "w") as f:
        json.dump(data, f)
    cmd = [
        "curl", "-sS", "-X", "POST", API + "/version",
        "-H", f"Authorization: {token()}",
        "-H", f"User-Agent: {UA}",
        "-w", "\n__HTTP__%{http_code}",
        "-F", f"data=@{data_path};type=application/json",
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


# --- main ----------------------------------------------------------------
def existing_version_numbers():
    return {v.get("version_number") for v in api_get(f"/project/{PROJECT_ID}/version")}


_FLAGS_WITH_VALUE = {"--only"}
_FLAGS = {"--check", "--list", "--force"} | _FLAGS_WITH_VALUE


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
    ns = nodes()
    if "--only" in args:
        key = args[args.index("--only") + 1]
        ns = [n for n in ns if key in n["path"] or key in n["file"]]
    if "--list" in args:
        for n in ns:
            print(f"{n['version_number']:34} <- {n['file']}")
        print(f"\n{len(ns)} node(s)  (mod_version={MOD_VERSION})")
        return
    if not ns:
        raise SystemExit(f"No jars found for mod_version {MOD_VERSION} under versions/*/build/libs/ "
                         "— build the matrix first (MOD_IS_RELEASE=true).")
    check()
    existing = set() if "--force" in args else existing_version_numbers()
    cl = changelog()
    ok, skip, fail = 0, 0, 0
    for i, n in enumerate(ns, 1):
        if n["version_number"] in existing:
            print(f"[{i}/{len(ns)}] SKIP (exists) {n['version_number']}")
            skip += 1
            continue
        try:
            v = post_version(n, cl)
            print(f"[{i}/{len(ns)}] OK   {n['version_number']}  -> version id {v.get('id')}")
            ok += 1
        except HttpFail as e:
            print(f"[{i}/{len(ns)}] FAIL {n['version_number']}  HTTP {e.code}: {e.body}")
            fail += 1
            if e.code in (401, 403):
                print("\nABORT: auth/scope error on first write — token lacks the "
                      "Create-versions scope (or org permission). Nothing further attempted.")
                break
        time.sleep(0.7)  # Modrinth rate limit ~300/min
    print(f"\nDone. uploaded={ok} skipped={skip} failed={fail}")


if __name__ == "__main__":
    main()
