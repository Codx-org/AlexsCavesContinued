#!/bin/zsh
# vprobe.sh <fully.qualified.Class> [grep-ERE]
#
# javaps one vanilla/loader-patched class out of EVERY NeoForge node whose MDG artifacts are
# staged, so a signature's version boundary can be read in one command instead of one node at
# a time. Written 2026-08-22 during the 1.0.1 triage — see docs/notes/1.0.1-triage.md.
#
# ⚠️ These are the *patched* jars, i.e. NeoForge's view. Forge and Fabric can differ (a loader
# patch is not vanilla); re-probe the other loader whenever the member might be one.
# ⚠️ Not every node is staged. A version missing from the output was never built here, not
# "the member is absent" — the two are printed differently on purpose.
set -u
cls="$1"; pat="${2:-.}"
cd "${0:A:h}/.."
for n in $(ls -d versions/*-neoforge 2>/dev/null | sort -V); do
  jar=$(ls $n/build/moddev/artifacts/*.jar 2>/dev/null | grep -v -e sources -e merged -e client-extra | head -1)
  [[ -z "$jar" ]] && continue
  node="${n#versions/}"; node="${node%-neoforge}"
  out=$(javap -p -classpath "$jar" "$cls" 2>/dev/null | grep -E "$pat")
  if [[ -z "$out" ]]; then
    printf '%-8s  (no match / class absent)\n' "$node"
  else
    print -r -- "$out" | while read -r l; do printf '%-8s  %s\n' "$node" "$l"; done
  fi
done
