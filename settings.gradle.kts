pluginManagement {
	repositories {
		mavenLocal()
		mavenCentral()
		gradlePluginPortal()
		maven("https://maven.fabricmc.net/") { name = "Fabric" }
		maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
		maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
		maven("https://maven.architectury.dev/") { name = "Architectury" }  // architectury-loom (Forge on Gradle 9)
		maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
		maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
		maven("https://maven.parchmentmc.org") { name = "ParchmentMC" }
	}
	// architectury-loom has no plugin marker on the maven; map the id → artifact so the
	// plugins { id("dev.architectury.loom") version "..." } request resolves (incl. snapshots).
	resolutionStrategy {
		eachPlugin {
			if (requested.id.id.startsWith("dev.architectury.loom")) {
				useModule("dev.architectury:architectury-loom:1.17-SNAPSHOT")
			}
		}
	}
	includeBuild("build-logic")
}

plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
	id("dev.kikugie.stonecutter") version "0.9.2"
}

rootProject.name = "AlexsCavesContinued"

// Alex's Caves is a Forge mod (upstream stopped at MC 1.20.1 / Forge, version 2.0.2). This tree
// carries that exact source forward across MC versions on Forge + NeoForge + Fabric, using the
// harness proven on AlexsMobsContinued.
//
// The target is the FULL 58-node matrix — the same set codxlib ships, so every node of every
// codx mod has a companion library build. 58 is not 20 MC versions × 3 loaders; the loaders
// genuinely differ in what exists upstream:
//   Forge     — no upstream build at all for 1.20.2 / 1.20.3 / 1.20.5 / 1.21.2
//               (1.20.3's userdev resolves but its bootstrap-dev:2.0.0 is gone from the maven)
//   NeoForge  — no usable modern bundle below 1.20.4 (1.20.1 NeoForge is the legacy toolchain),
//               and none for 1.20.5
//   Fabric    — the only loader that reaches 1.20.2 / 1.20.3 / 1.20.5
// That leaves 18 Forge + 18 NeoForge + 22 Fabric = 58.
// A version gets a node only once it actually compiles, so the tree never carries a
// known-broken node. Uncomment each line as its port lands.
stonecutter {
	create(rootProject) {
		fun forge(version: String) =
			version("$version-forge", version).apply { buildscript = "build.forgeg.gradle.kts" }

		fun neoforge(version: String) =
			version("$version-neoforge", version).apply { buildscript = "build.neoforge.gradle.kts" }

		// MC 26.x ships UNOBFUSCATED — there is no SRG namespace, so its Forge nodes build on
		// arch-loom's no-remap variant. The node id stays "<mc>-forge", so every `//? if forge`
		// gate in the shared source applies unchanged. NeoForge stays on MDG either way.
		fun forgeNoRemap(version: String) =
			version("$version-forge", version).apply { buildscript = "build.forgenr.gradle.kts" }

		// Fabric, same story: on the unobfuscated 26.x line there is nothing to remap.
		fun fabricNoRemap(version: String) =
			version("$version-fabric", version).apply { buildscript = "build.fabricnr.gradle.kts" }

		// …and below 26.1 the game is obfuscated again, so Fabric goes back to classic loom with
		// a mappings tree (build.fabric.gradle.kts).
		fun fabric(version: String) =
			version("$version-fabric", version).apply { buildscript = "build.fabric.gradle.kts" }

		// ── ported ────────────────────────────────────────────────────────────
		forge("1.20.1")   // upstream baseline: Alex's Caves 2.0.2
		forge("1.20.4");   neoforge("1.20.4")
		forge("1.20.6");   neoforge("1.20.6")     // ← 1.20.5 DataComponents break
		forge("1.21");     neoforge("1.21")       // ← data folders renamed to singular
		forge("1.21.1");   neoforge("1.21.1")
		                   neoforge("1.21.2")     // Forge: no 1.21.2 build upstream
		forge("1.21.3");   neoforge("1.21.3")     // ← first Forge node ≥1.21.2
		forge("1.21.4");   neoforge("1.21.4")     // ← item model definitions
		forge("1.21.5");   neoforge("1.21.5")     // ← CompoundTag→Optional + RenderPipeline rewrite
		forge("1.21.6");   neoforge("1.21.6")     // ← ValueInput/ValueOutput; Forge ships EventBus 7
		forge("1.21.7");   neoforge("1.21.7")
		forge("1.21.8");   neoforge("1.21.8")
		forge("1.21.9");   neoforge("1.21.9")     // ← SubmitNodeCollector submission pipeline
		forge("1.21.10");  neoforge("1.21.10")    // ← entityInside gained an "actually inside" flag
		forge("1.21.11");  neoforge("1.21.11")    // ← ResourceLocation→Identifier, 37 package moves
		forgeNoRemap("26.1");   neoforge("26.1")  // ← unobfuscated 26.x line; Java 25
		forgeNoRemap("26.1.1"); neoforge("26.1.1")
		forgeNoRemap("26.1.2"); neoforge("26.1.2")
		forgeNoRemap("26.2");   neoforge("26.2")  // ← Gui→Hud, EntityType→EntityTypes constants

		// ── planned (uncomment as each version is ported) ─────────────────────

		fabric("1.20.1")                          // ← first Fabric node; access widener + loader seam
		fabric("1.20.2")
		fabric("1.20.3")
		fabric("1.20.4")
		fabric("1.20.5")
		fabric("1.20.6")
		fabric("1.21")
		fabric("1.21.1")
		fabric("1.21.2")
		fabric("1.21.3")
		fabric("1.21.4")
		fabric("1.21.5")
		fabric("1.21.6")
		fabric("1.21.7")
		fabric("1.21.8")
		fabric("1.21.9")
		fabric("1.21.10")
		fabric("1.21.11")
		fabricNoRemap("26.1")
		fabricNoRemap("26.1.1")
		fabricNoRemap("26.1.2")
		fabricNoRemap("26.2")

		vcsVersion = "1.20.1-forge"
	}
}

// arch-loom must know its platform BEFORE its plugin applies. Every project is configured
// on any task, so Forge nodes must declare loom.platform=forge or their loom{forge{}} block
// aborts the whole build. NeoForge uses MDG and ignores this.
gradle.beforeProject {
	if (name.endsWith("-forge")) {
		extensions.extraProperties["loom.platform"] = "forge"
	}
}
