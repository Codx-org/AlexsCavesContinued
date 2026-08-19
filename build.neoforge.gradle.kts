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
			forgeLikeVersionRange.set("[1,)")
		}
		required("codxlib") {
			forgeLikeVersionRange.set("[1.3,)")
		}
		// NOTE: no Citadel dependency — the subset Alex's Caves uses is bundled into the mod
		// under com.github.alexmodguy.alexscaves.citadel (see docs/notes/citadel.md).
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
}

// javac reports at most 100 errors by default, which makes "how far off is this node?" a lie
// during a version migration.
tasks.withType<JavaCompile>().configureEach {
	options.compilerArgs.addAll(listOf("-Xmaxerrs", "9999"))
}

// Alex's Caves carries essentially no javadoc comments — see build.forgeg.gradle.kts.
tasks.named<Javadoc>("javadoc") { isEnabled = false }
