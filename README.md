# Alex's Caves Continued

A multiversion continuation of **[Alex's Caves](https://github.com/AlexModGuy/AlexsCaves)** by
Alexthe668. Upstream shipped `2.0.2` for Forge on Minecraft 1.20.1 only; this fork carries that
same content forward across **58 builds** — Minecraft `1.20.1` through `26.2`, on **Fabric, Forge
and NeoForge** — without changing what the mod is.

Six rare cave biomes hidden under the Overworld, and everything that lives in them.

- **Modrinth:** <https://modrinth.com/mod/alexs-caves-continued>
- **Issues:** <https://github.com/Codx-org/AlexsCavesContinued/issues>

## Requirements

**[CodxLib](https://modrinth.com/mod/codxlib) is a required dependency** and must be installed
alongside this mod — download the build matching your Minecraft version and loader. Without it the
game will refuse to start with a missing-dependency error.

Fabric additionally needs the Fabric API build pinned for your Minecraft version.

## Supported versions

58 builds: **22 Fabric + 18 Forge + 18 NeoForge**. The loaders do not cover the same set, because
upstream toolchains do not exist for every combination:

| Missing | Why |
|---|---|
| `1.20.2`, `1.20.3`, `1.20.5` on Forge and NeoForge | No usable upstream build for either loader. Fabric reaches all three. |
| `1.20.1` on NeoForge | 1.20.1 NeoForge is the legacy Forge-fork toolchain, not modern moddev. |
| `1.21.2` on Forge | Forge published no 1.21.2 build. |

## Relationship to upstream

This is a **continuation, not a rewrite**. The gameplay, assets, worldgen and balance are
Alexthe668's; the work here is the port. Two structural differences are worth knowing about:

- **Citadel is vendored, not depended on.** Upstream requires
  [Citadel](https://github.com/AlexModGuy/Citadel), which only ever shipped for a fraction of this
  version range and never for Fabric. The subset Alex's Caves actually uses — 93 classes — is
  bundled here, package-relocated from `com.github.alexthe666.citadel` to
  `com.github.alexmodguy.alexscaves.citadel` so that a player who has the real Citadel installed for
  another mod does not end up with two copies of one class name. Citadel is LGPL-3.0, which permits
  this. See [`docs/notes/citadel.md`](docs/notes/citadel.md).
- **The build is [Stonecutter](https://stonecutter.kikugie.dev/)-based.** One source tree with
  version-gated arms and replacement rules produces all 58 jars, rather than a branch per version.

## Building

Each node targets the Java version its Minecraft version needs — 17 below 1.20.5, 21 from
1.20.5, 25 on the 26.x line — selected automatically, so building the whole matrix needs a
JDK 25 available to Gradle's toolchain resolution.

```bash
# one node
./gradlew :1.21.1-neoforge:build

# every node — must be ONE invocation, the daemon is single-use
TASKS=(${(f)"$(ls versions/ | sed 's|^|:|; s|$|:build|')"})
MOD_IS_RELEASE=true ./gradlew "${TASKS[@]}" --continue
```

Without `MOD_IS_RELEASE=true` the artifacts are marked `-SNAPSHOT`. Each node emits three jars into
`versions/<node>/build/libs/` — the mod jar, `-sources` and `-javadoc`.

CodxLib is resolved from the local Maven repository during development, so it must be installed
there first (`cd ../codxlib && python3 scripts/install_maven_local.py`).

Verification scripts live in [`scripts/`](scripts/): `verify_mixins.py` checks every mixin
injection point on every node against the real bytecode, and `aw_check.py` validates the access
widener.

## Licence

**GNU Lesser General Public License v3.0**, the same licence as upstream Alex's Caves and Citadel.

The full terms are the GNU GPL v3 ([`COPYING`](COPYING)) as supplemented by the additional
permissions of the GNU LGPL v3 ([`COPYING.LESSER`](COPYING.LESSER)).

- Alex's Caves © Alexthe668 — <https://github.com/AlexModGuy/AlexsCaves>
- Citadel © Alexthe668 — <https://github.com/AlexModGuy/Citadel>
- Continuation and multiversion port © the Alex's Caves Continued contributors
