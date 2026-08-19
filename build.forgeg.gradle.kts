// Forge via Architectury Loom (builds Forge on modern Gradle 9 — no ForgeGradle, so it
// avoids the FG6/Gradle-8 wall). Applied ONLY to Forge nodes; NeoForge (MDG) keeps its own.
plugins {
	id("mod-platform")
	id("dev.architectury.loom")  // version pinned in settings.gradle.kts resolutionStrategy
}

stonecutter {
	val (version, loader) = current.project.split('-', limit = 2)
	properties.tags(version, loader)
}

platform {
	loader = "forge"
	dependencies {
		required("minecraft") {
			// Exact range, NOT a bare version: "1.20.1" is a Maven *soft* requirement that Forge
			// reads as "[1.20.1,)", so the jar claims to run on every later MC and Modrinth's
			// upload auto-detect cannot pin a game version. See exactMcRange — it also pads
			// two-component versions ("1.21" -> "[1.21.0]") so Modrinth doesn't read the range
			// as the semver X-range "1.21.x".
			// Exact is only the DEFAULT — a node whose MC patch releases are API-identical can
			// widen it with `deps.minecraft-range` in its toml section (26.1.2 does). See
			// declaredMcRange: widen there BEFORE tagging a store, never after.
			forgeLikeVersionRange = declaredMcRange(fabricLike = false)
		}
		required("forge") {
			forgeLikeVersionRange.set("[1,)")
		}
		required("codxlib") {
			forgeLikeVersionRange.set("[1.3,)")
		}
		// NOTE: no Citadel dependency — the subset Alex's Caves uses is bundled into the mod
		// under com.github.alexmodguy.alexscaves.citadel (see docs/notes/citadel.md).
	}
}

// Alex's Caves ships Forge access transformers (widens ~44 vanilla members).
// Stonecutter node projects live in versions/<node>/, but the ACTIVE node compiles the
// root src/ directly and never gets a generated copy — so fall back to the root file.
val accessTransformerFile = file("src/main/resources/META-INF/accesstransformer.cfg")
	.takeIf { it.exists() }
	?: rootProject.file("src/main/resources/META-INF/accesstransformer.cfg")

loom {
	silentMojangMappingsLicense()
	forge {
		mixinConfig("${prop("mod.id")}.mixins.json")
		accessTransformer(accessTransformerFile)
	}
	// Lets a test harness drive the dev client without editing this file — e.g.
	// AC_CLIENT_ARGS="--quickPlayMultiplayer 127.0.0.1:25565" to join a local dedicated
	// server straight from the launch, skipping the title screen. Whitespace-separated;
	// appended, so nothing loom sets is lost. Mirrors the hook in build.neoforge.gradle.kts.
	runs.named("client") {
		System.getenv("AC_CLIENT_ARGS")?.split(Regex("\\s+"))?.filter { it.isNotBlank() }
			?.forEach { programArg(it) }
	}
}

repositories {
	mavenLocal()   // CodxLib per-node jars
	mavenCentral()
	maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
	maven("https://cursemaven.com") {
		name = "CurseMaven"
		content { includeGroup("curse.maven") }
	}
	maven("https://maven.blamejared.com") { name = "BlameJared (JEI)" }
}

dependencies {
	minecraft("com.mojang:minecraft:${prop("deps.minecraft")}")
	mappings(loom.officialMojangMappings())
	"forge"("net.minecraftforge:forge:${prop("deps.minecraft")}-${prop("deps.forge")}")

	// JEI is optional at runtime; only compat/jei/** compiles against it. A node without a
	// deps.jei pin has no JEI for its MC version at all (JEI published nothing for 1.21.2/1.21.3
	// and no Forge flavour after 1.21.1) — the convention plugin drops compat/jei from the
	// compile there, so there is nothing to resolve.
	val jei = propOrNull("deps.jei")
	if (jei != null) {
		val jeiMc = prop("deps.jei-mc")
		modCompileOnly("mezz.jei:jei-$jeiMc-common-api:$jei")
		modCompileOnly("mezz.jei:jei-$jeiMc-forge-api:$jei")
	}
	// NOTE: the full JEI jar is deliberately NOT on the dev runtime classpath. From 1.20.2 on it
	// and the *-api jars both export mezz.jei.api.*, and Forge's stricter module resolution
	// aborts the dev launch with "Modules jei and jei.…api export package … to module minecraft".
	// JEI compat is compile-only; test it by dropping a JEI jar into the run's mods folder.

	// Forge 51.0.x (MC 1.21) ships a userdev POM that forgets jopt-simple, even though
	// cpw.mods.modlauncher's module-info `requires jopt.simple`, so its dev server aborts
	// with "Module jopt.simple not found" (Forge 52+ include it). Add it to loom's Forge
	// runtime library set so it lands on the dev module path. Harmless on nodes that
	// already provide it (duplicate classpath entry).
	"forgeRuntimeLibrary"("net.sf.jopt-simple:jopt-simple:5.0.4")

	// CodxLib — per-node jar from mavenLocal (codx:codxlib:<ver>-forge+<mc>). Consume as a MOD
	// (modImplementation) so loom remaps it. Classic Forge (<1.20.6) runs on SRG, so its published
	// codxlib jar is SRG-named; a plain `implementation` leaves it un-remapped and the game hits
	// NoSuchMethodError on SRG names at the Mojmap dev runtime.
	modImplementation("codx:codxlib:${prop("deps.codxlib")}-forge+${prop("deps.minecraft")}")

	// MixinExtras (@Local / @ModifyExpressionValue / @WrapOperation) — compile-time half.
	// NeoForge and Fabric Loader bundle it at runtime; neither puts it on the COMPILE classpath,
	// so it is declared compileOnly here (plain compileOnly, not modCompileOnly: it is a library,
	// not a mod, and nothing in it needs remapping). Pinned at the 0.3.x floor the oldest bundled
	// loader ships; later runtimes are backwards compatible with 0.3.x-generated annotations.
	compileOnly("io.github.llamalad7:mixinextras-common:0.3.5")
	annotationProcessor("io.github.llamalad7:mixinextras-common:0.3.5")

	// …and the RUNTIME half, which Forge — unlike the other two loaders — does not always give us.
	// ⚠️ Forge started bundling MixinExtras at build **60.1.11** (MC 1.21.10): every userdev
	// config.json from 47.4.21 through 59.0.5 lists none, and 60.1.11 / 61.1.0 / 62+ list
	// `io.github.llamalad7:mixinextras-forge:0.5.3`. That is a Forge-BUILD boundary, not an MC one,
	// so it is tested against deps.forge's major rather than deps.minecraft.
	// Without it the two annotation families fail DIFFERENTLY, and only one of them is loud:
	//   * `@Local` on an @Inject handler is a hard crash at mixin apply — Mixin sees an extra
	//     handler parameter it cannot account for ("Invalid descriptor … Expected (…CallbackInfo)V
	//     but found (…CallbackInfo;Lcom/mojang/blaze3d/vertex/PoseStack;)V"). Killed the dev
	//     clients on 1.21.8-forge and 1.21.9-forge, on client.GameRendererMixin.
	//   * `@ModifyExpressionValue` is SILENT: an unknown annotation is not an injector, so the
	//     handler is simply never applied. ItemStackAttributeModifiersMixin is in the COMMON
	//     mixin list, so on Forge 1.21.3–1.21.9 it has been quietly doing nothing, on servers too
	//     — which is exactly why the 58-node server sweep passed while the clients did not.
	// Jar-in-jar is the shape MixinExtras itself documents for Forge, and the shape Forge 60+
	// uses internally (its own copy arrives through JarInJarDependencyLocator). Two mods shipping
	// it is fine: JarJar dedupes by coordinate and keeps the highest version.
	// `mixinextras-forge` is itself a tiny Forge MOD (mods.toml + mixinextras.init.mixins.json,
	// whose config plugin calls MixinExtrasBootstrap.init) wrapping the real MixinExtras jar in
	// META-INF/jars — so it has to be LOADED, not merely be on the classpath. `include` ships it
	// jar-in-jar for players; the plain `implementation` is what makes FML find it in the dev run.
	// Confirmed on 1.21.6-forge / 1.21.7-forge: the log shows `ModDiscoverer` finding
	// `mixinextras-forge-0.4.1.jar` of type GAMELIBRARY via ClasspathLocator, then the nested
	// `MixinExtras-0.4.1.jar` via JarInJarDependencyLocator, then `(MixinExtras|Service)
	// Initializing MixinExtras via com.llamalad7.mixinextras.service.MixinExtrasServiceImpl`.
	// modRuntimeOnly is NOT needed and would only add a pointless remap of a plain library.
	if ((propOrNull("deps.forge")?.substringBefore('.')?.toIntOrNull() ?: 0) < 60) {
		include("io.github.llamalad7:mixinextras-forge:0.4.1")
		implementation("io.github.llamalad7:mixinextras-forge:0.4.1")
	}
}

// javac reports at most 100 errors by default, which makes "how far off is this node?" a lie
// during a version migration (the real count on a fresh node is in the thousands).
tasks.withType<JavaCompile>().configureEach {
	options.compilerArgs.addAll(listOf("-Xmaxerrs", "9999"))
}

// Alex's Caves carries essentially no javadoc comments, so generating javadoc for ~1000 files
// costs minutes per node and emits 100 "no comment" warnings for zero value.
// mod-platform still wires javadocJar for publishing; it just packs nothing.
tasks.named<Javadoc>("javadoc") { isEnabled = false }

// modlauncher `requires jopt.simple` — the automatic module name of jopt-simple 5.0.4.
// jopt-simple 6.0-alpha-* ships a real module-info named `joptsimple` (no dot), which does
// NOT satisfy that requires. Forge 51's constraints try to pull 6.0-alpha-3, so pin 5.0.4.
configurations.configureEach {
	resolutionStrategy { force("net.sf.jopt-simple:jopt-simple:5.0.4") }
}
