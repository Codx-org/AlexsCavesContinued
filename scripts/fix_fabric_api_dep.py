#!/usr/bin/env python3
"""Add the missing Fabric API required-dependency to every fabric version on Modrinth.

Every fabric jar this tree builds declares a hard ``fabric-api`` floor in its generated
``fabric.mod.json``, but the uploader only ever attached CodxLib, so Modrinth showed no
Fabric API dependency and launchers did not offer to install it. This walks the project's
versions, and for any fabric-loader version whose dependency list is missing Fabric API,
PATCHes the full list back on (CodxLib + Fabric API, both project-level).

Idempotent: a version that already lists Fabric API is skipped. Re-reads each version with
GET /v2/version/{id} before deciding — the project listing is cached and under-reports.

Usage:
  python3 scripts/fix_fabric_api_dep.py --dry-run
  python3 scripts/fix_fabric_api_dep.py [--version 1.0.1]
"""
import sys, os, json, time, urllib.request, urllib.error

HERE = os.path.dirname(os.path.abspath(__file__))
API = "https://api.modrinth.com/v2"
PROJECT_ID = "cO2CvXug"
CODXLIB = "6oyMM4yX"
FABRIC_API = "P7dR8mSH"
UA = "alexscaves-continued-publisher/1.0 (+https://github.com/Codx-org/AlexsCavesContinued)"

def token():
    t = os.environ.get("MODRINTH_TOKEN")
    if t: return t.strip()
    with open(os.path.join(HERE, ".mr_token")) as f:
        return f.read().strip()

TOK = token()

def req(method, path, body=None):
    url = API + path
    data = json.dumps(body).encode() if body is not None else None
    r = urllib.request.Request(url, data=data, method=method)
    r.add_header("Authorization", TOK)
    r.add_header("User-Agent", UA)
    if data is not None:
        r.add_header("Content-Type", "application/json")
    with urllib.request.urlopen(r, timeout=60) as resp:
        raw = resp.read()
        return json.loads(raw) if raw else None

def main():
    dry = "--dry-run" in sys.argv
    want = None
    if "--version" in sys.argv:
        want = sys.argv[sys.argv.index("--version") + 1]

    listing = req("GET", f"/project/{PROJECT_ID}/version")
    fabric = [v for v in listing if "fabric" in v["loaders"]]
    if want:
        fabric = [v for v in fabric if v["version_number"].startswith(want)]
    print(f"{len(listing)} versions listed, {len(fabric)} fabric" + (f" matching {want}" if want else ""))

    fixed = ok = failed = 0
    for i, stub in enumerate(fabric, 1):
        vid = stub["id"]
        v = req("GET", f"/version/{vid}")          # fresh read; the listing is cached
        deps = v.get("dependencies") or []
        if any(d.get("project_id") == FABRIC_API for d in deps):
            print(f"[{i}/{len(fabric)}] ok   {v['version_number']}")
            ok += 1
            continue
        new = [d for d in deps if d.get("project_id") != FABRIC_API]
        new.append({"project_id": FABRIC_API, "version_id": None,
                    "file_name": None, "dependency_type": "required"})
        if dry:
            print(f"[{i}/{len(fabric)}] WOULD PATCH {v['version_number']}")
            fixed += 1
            continue
        try:
            req("PATCH", f"/version/{vid}", {"dependencies": new})
            back = req("GET", f"/version/{vid}")
            if any(d.get("project_id") == FABRIC_API for d in back.get("dependencies") or []):
                print(f"[{i}/{len(fabric)}] FIXED {v['version_number']}")
                fixed += 1
            else:
                print(f"[{i}/{len(fabric)}] FAILED (not readable back) {v['version_number']}")
                failed += 1
        except urllib.error.HTTPError as e:
            print(f"[{i}/{len(fabric)}] FAILED {v['version_number']}: HTTP {e.code} {e.read()[:200]}")
            failed += 1
        time.sleep(0.3)

    print(f"\nDone. already-ok={ok} fixed={fixed} failed={failed}")
    return 1 if failed else 0

if __name__ == "__main__":
    sys.exit(main())
