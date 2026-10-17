plugins {
	id("mod-platform")
	id("net.neoforged.moddev")
}

stonecutter {
	val (version, loader) = current.project.split('-', limit = 2)
	properties.tags(version, loader)
}

platform {
	loader = "neoforge"
	dependencies {
		required("minecraft") {
			// Exact range, NOT a bare version: "1.20.4" is a Maven *soft* requirement that
			// NeoForge reads as "[1.20.4,)", so the jar claims to run on every later MC and
			// Modrinth's upload auto-detect cannot pin a game version. See exactMcRange —
			// it also pads two-component versions ("1.21" -> "[1.21.0]") so Modrinth doesn't
			// read the range as the semver X-range "1.21.x".
			// Exact is only the DEFAULT — see declaredMcRange / `deps.minecraft-range`.
			forgeLikeVersionRange = declaredMcRange(fabricLike = false)
		}
		required("neoforge") {
			// "[1,)" — any NeoForge — is right wherever the loader API this mod calls has been
			// stable across the whole point-release line of that MC version, which is every node
			// but one. `deps.neoforge-min` overrides it per node for the case where it is not:
			// 26.3 is still a BETA line, and 26.3.0.4-beta DELETED five APIs that 26.3.0.1-beta
			// still had (AbstractContainerScreen#getGuiLeft/#getGuiTop, NeoForgeRenderTypes
			// #getUnlitTranslucent, FlowerPotBlock#addPlant, ModifyDefaultComponentsEvent#modify's
			// Consumer overloads, and the whole neoforge.items package). This jar is compiled
			// against the survivors, so on .1-beta it would install cleanly and then throw
			// NoSuchMethodError in a screen, a render type and a registry callback. A declared
			// floor turns that into the loader's own "requires NeoForge x or above" message.
			forgeLikeVersionRange.set(propOrNull("deps.neoforge-min") ?: "[1,)")
		}
		required("codxlib") {
			// >=1.3.6, not >=1.3: /acc menu is built on CodxLib's api.ui.menu chest-menu toolkit,
			// and 1.3.3 ships no api/ui/menu classes at all — an older CodxLib is a
			// NoClassDefFoundError the first time an operator runs the command. 1.3.6 is also the
			// build-time pin (deps.codxlib) and the first CodxLib on BOTH Modrinth and CurseForge.
			forgeLikeVersionRange.set("[1.3.6,)")
		}
		// NOTE: no Citadel dependency — the subset Alex's Caves uses is bundled into the mod
		// under com.github.alexmodguy.alexscaves.citadel (see docs/notes/citadel.md).
	}
}

// NeoForm 26.3-1's decompiled vanilla source does not recompile under the NeoFormRuntime that
// MDG 2.0.141 defaults to (which runs JST 2.0.6): NeoForge's own access transformer widens BOTH
// `HolderSet$Named contents()` and the anonymous `HolderSet$1 contents()` that overrides it, and
// that JST applies only the first — so the override is left `protected` against a now-`public`
// parent and javac rejects the whole game with "attempting to assign weaker access privileges;
// was public". It fails inside createMinecraftArtifacts, i.e. before any of this mod's source is
// compiled. NFRT 2.0.31 (JST 2.0.11) applies both entries. Pinned for 26.3 ONLY — the other 17
// NeoForge nodes are verified against the default runtime, and a blanket bump would re-open all
// of them for a fault that exists on one. The runtime is a separate plugin's extension
// (net.neoforged.nfrtgradle), applied by moddev, so it is configured here rather than in neoForge {}.
if (prop("deps.minecraft") == "26.3") {
	extensions.configure<net.neoforged.nfrtgradle.NeoFormRuntimeExtension> {
		version.set("2.0.31")
	}
}

neoForge {
	version = prop("deps.neoforge")

	// Alex's Caves ships Forge access transformers, written in SRG names. NeoForge dropped SRG
	// in 1.20.2 and only understands Mojang names, so NeoForge nodes get the parallel
	// accesstransformer_mojmap.cfg (processResources below renames it into place in the jar).
	// The ACTIVE Stonecutter node compiles root src/ and gets no generated copy in
	// versions/<node>/, so fall back to the root file.
	accessTransformers.from(
		file("src/main/resources/META-INF/accesstransformer_mojmap.cfg")
			.takeIf { it.exists() }
			?: rootProject.file("src/main/resources/META-INF/accesstransformer_mojmap.cfg")
	)

	runs {
		register("client") {
			client()
			gameDirectory = file("run/")
			ideName = "NeoForge Client (${stonecutter.current.version})"
			programArgument("--username=Dev")
			// Lets a test harness drive the dev client without editing this file — e.g.
			// AC_CLIENT_ARGS="--quickPlayMultiplayer 127.0.0.1:25565" to join a local dedicated
			// server straight from the launch, skipping the title screen. Whitespace-separated;
			// appended, so nothing MDG sets is lost (unlike Gradle's own --args, which replaces).
			System.getenv("AC_CLIENT_ARGS")?.split(Regex("\\s+"))?.filter { it.isNotBlank() }
				?.forEach { programArgument(it) }
		}
		register("server") {
			server()
			gameDirectory = file("run/")
			ideName = "NeoForge Server (${stonecutter.current.version})"
		}
	}

	mods {
		register(prop("mod.id")) {
			sourceSet(sourceSets["main"])
		}
	}
}

repositories {
	mavenLocal()   // CodxLib per-node jars
	mavenCentral()
	maven("https://cursemaven.com") {
		name = "CurseMaven"
		content { includeGroup("curse.maven") }
	}
	maven("https://maven.blamejared.com") { name = "BlameJared (JEI)" }
}

// maven.neoforged.net answers a path it does not host with HTTP 200 and an EMPTY BODY rather
// than a 404, and Gradle treats unparseable metadata as a hard failure instead of falling
// through to the next repository. That is fatal for a DYNAMIC version, which has to list
// versions before it can pick one: NeoForge 20.4/20.6 pull net.minecraftforge:unsafe:0.2.0,
// whose POM asks for org.apache.logging.log4j:{log4j-api,log4j-core}:2.11.+, and
// `createMinecraftArtifacts` dies with "Premature end of file" on maven-metadata.xml. Neither
// artifact has ever lived on that maven, so keeping the whole log4j group away from it costs
// nothing and sends the lookup to Maven Central, which answers. MDG adds its own instance of
// the repository to this project, hence the filter is applied by URL after the fact.
repositories.withType<MavenArtifactRepository>().configureEach {
	if (url.toString().contains("maven.neoforged.net")) {
		content { excludeGroup("org.apache.logging.log4j") }
	}
}

dependencies {
	// JEI is optional at runtime; only compat/jei/** compiles against it. Same split-API
	// artifacts as the Forge nodes, just the neoforge flavour of the loader-specific one.
	// A node without a deps.jei pin has no JEI for its MC version (JEI published nothing at all
	// for 1.21.2/1.21.3); the convention plugin drops compat/jei from the compile there.
	val jei = propOrNull("deps.jei")
	if (jei != null) {
		val jeiMc = prop("deps.jei-mc")
		compileOnly("mezz.jei:jei-$jeiMc-common-api:$jei")
		compileOnly("mezz.jei:jei-$jeiMc-neoforge-api:$jei")
	}
	// NOTE: the full JEI jar is deliberately NOT on the dev runtime classpath (see the Forge
	// buildscript for why). JEI compat is compile-only.

	// CodxLib — per-node jar from mavenLocal (codx:codxlib:<ver>-neoforge+<mc>). NeoForge/MDG is
	// Mojmap end to end, so the published jar needs no remapping.
	implementation("codx:codxlib:${prop("deps.codxlib")}-neoforge+${prop("deps.minecraft")}")

	// MixinExtras — bundled by the loader at runtime, absent from the compile classpath.
	// See build.forgeg.gradle.kts for the full rationale.
	compileOnly("io.github.llamalad7:mixinextras-common:0.3.5")
	annotationProcessor("io.github.llamalad7:mixinextras-common:0.3.5")
}

tasks.named("createMinecraftArtifacts") {
	dependsOn(tasks.named("stonecutterGenerate"))
}

// Ship the Mojang-named AT as the jar's META-INF/accesstransformer.cfg and drop the SRG one.
tasks.named<ProcessResources>("processResources") {
	exclude("META-INF/accesstransformer.cfg")
	rename("accesstransformer_mojmap.cfg", "accesstransformer.cfg")
	// The source file opens with a commented header, and accesstransformers 8.2.x (Forge 26.2's)
	// logs "Invalid access transformer line" for a bare "#". Ship only the entries: drop comment
	// and blank lines. Matched under both names since the rename may already have run.
	filesMatching(listOf("META-INF/accesstransformer_mojmap.cfg", "META-INF/accesstransformer.cfg")) {
		filter { line -> if (line.isBlank() || line.trimStart().startsWith("#")) null else line }
	}
}

// javac reports at most 100 errors by default, which makes "how far off is this node?" a lie
// during a version migration.
tasks.withType<JavaCompile>().configureEach {
	options.compilerArgs.addAll(listOf("-Xmaxerrs", "9999"))
}

// Alex's Caves carries essentially no javadoc comments — see build.forgeg.gradle.kts.
tasks.named<Javadoc>("javadoc") { isEnabled = false }
