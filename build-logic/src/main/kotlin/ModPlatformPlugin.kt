@file:Suppress("unused", "DuplicatedCode")

import dev.kikugie.fletching_table.extension.FletchingTableExtension
import dev.kikugie.stonecutter.StonecutterExperimentalAPI
import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.extensions.stdlib.toDefaultLowerCase
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.plugins.ide.idea.model.IdeaModel
import javax.inject.Inject

val Project.sc: StonecutterBuildExtension
	get() = extensions.getByType<StonecutterBuildExtension>()

@OptIn(StonecutterExperimentalAPI::class)
fun Project.prop(name: String): String = (project.sc.properties.get<String>(name))

/**
 * For pins that only some nodes have. Stonecutter throws rather than returning null for an absent
 * key, which is the right default — a missing `deps.forge` should fail loudly — so opting out is
 * per-call-site.
 */
@OptIn(StonecutterExperimentalAPI::class)
fun Project.propOrNull(name: String): String? =
	runCatching { project.sc.properties.get<String>(name) }.getOrNull()

/**
 * The exact-one-version Maven range to declare for `minecraft`, e.g. `[1.20.1]` / `[1.21.0]`.
 *
 * Two things conspire here:
 *
 *  1. A **bare** version (`"1.21"`) is a Maven *soft* requirement, which Forge/NeoForge read as
 *     `[1.21,)` — the jar then claims to run on every later MC. Hence the brackets.
 *  2. Modrinth's upload auto-detect (`apps/frontend/src/helpers/infer/version-ranges.ts`) turns
 *     `[X]` into the **semver range** `X` and feeds it to node-semver's `satisfies`. A
 *     two-component range like `1.21` is a semver *X-range* meaning `1.21.x`, so it preselects
 *     1.21 **and** 1.21.1 … 1.21.11. Padding to three components (`[1.21.0]`) makes it an exact
 *     semver version that matches only MC 1.21.
 *
 * Padding is safe for the loaders: Maven's `ComparableVersion` normalises trailing zero
 * components, so `1.21` and `1.21.0` compare **equal** and `[1.21.0]` is satisfied by MC 1.21.
 */
fun exactMcRange(mc: String): String {
	val padded = when (mc.count { it == '.' }) {
		0 -> "$mc.0.0"
		1 -> "$mc.0"
		else -> mc
	}
	return "[$padded]"
}

/**
 * The MC range a node's manifest declares. **Exact by default** (see [exactMcRange]); a node may
 * widen it with an optional `deps.minecraft-range` key in its `stonecutter.properties.toml` section.
 *
 * One node per MC version is the tree's normal shape, but MC sometimes ships patch releases that
 * are API-identical, and then a single node genuinely serves several. `26.1.2` is the first: it
 * runs 26.1, 26.1.1 and 26.1.2.
 *
 * ⚠️ **This is the authority, not the store listing.** A store can advertise a file against any MC
 * version it likes, but the loader reads *this* range out of the jar — so tagging a file 26.1 on
 * Modrinth/CurseForge without widening the range here ships a jar the launcher installs and the
 * loader then refuses, which is the same failure the `fabric-loader` floor bug produced. Widen
 * here **first**, then tag the store to match.
 *
 * The value is written in the syntax of the section's own loader family, because the two are not
 * interchangeable: Forge/NeoForge want a Maven range (`[26.1, 26.1.3)`) and Fabric wants semver
 * (`>=26.1 <=26.1.2`). Sections are already per-loader, so one key name covers both.
 */
fun Project.declaredMcRange(fabricLike: Boolean): String =
	propOrNull("deps.minecraft-range")
		?: if (fabricLike) prop("deps.minecraft") else exactMcRange(prop("deps.minecraft"))

fun Project.env(variable: String): String? = providers.environmentVariable(variable).orNull

fun Project.envTrue(variable: String): Boolean = env(variable)?.toDefaultLowerCase() == "true"

fun RepositoryHandler.strictMaven(
	url: String, vararg groups: String, configure: MavenArtifactRepository.() -> Unit = {}
) = exclusiveContent {
	forRepository { maven(url) { configure() } }
	filter { groups.forEach(::includeGroup) }
}

abstract class GenerateModManifestTask : DefaultTask() {
	@get:Input
	abstract val content: Property<String>

	@get:OutputFile
	abstract val outputFile: RegularFileProperty

	@TaskAction
	fun generate() {
		val file = outputFile.get().asFile
		file.parentFile.mkdirs()
		file.writeText(content.get())
	}
}

abstract class ModPlatformPlugin @Inject constructor() : Plugin<Project> {
	override fun apply(project: Project) = with(project) {
		val inferredLoader = Loader.of(project.buildFile.name.substringAfter('.').replace(".gradle.kts", ""))

		val extension = extensions.create("platform", ModPlatformExtension::class.java).apply {
			loader.convention(inferredLoader.id)
		}

		when (inferredLoader) {
			is Loader.Fabric, is Loader.Forge -> {
				// arch-loom builds both; remapped output is remapJar/remapSourcesJar. On the
				// NO-REMAP variant (unobfuscated 26.x) those tasks don't exist → fall back to
				// jar/sourcesJar. Resolved lazily so the Loom tasks are created by query time.
				extension.jarTask.convention(providers.provider {
					if (tasks.findByName("remapJar") != null) "remapJar" else "jar"
				})
				extension.sourcesJarTask.convention(providers.provider {
					if (tasks.findByName("remapSourcesJar") != null) "remapSourcesJar" else "sourcesJar"
				})
			}
			else -> {
				extension.jarTask.convention("jar")
				extension.sourcesJarTask.convention("sourcesJar")
			}
		}

		listOf("org.jetbrains.kotlin.jvm", "com.google.devtools.ksp", "dev.kikugie.fletching-table").forEach {
			apply(
				plugin = it
			)
		}

		afterEvaluate {
			val ctx = Context(
				project = this,
				extension = extension,
				loader = Loader.of(extension.loader.get()),
				stonecutter = project.sc
			)
			configureProject(ctx)
		}
	}

	private fun Project.configureProject(ctx: Context) {
		listOf("java", "me.modmuss50.mod-publish-plugin", "idea").forEach { apply(plugin = it) }

		version = ctx.fullVersion
		ctx.extension.requiredJava.set(ctx.javaVersion)

		if (ctx.loader.isFabricLike) {
			ctx.extension.dependencies {
				required("java") { fabricLikeVersionRange = ">=${ctx.javaVersion.majorVersion}" }
			}
		}

		configureFletchingTable(ctx)
		registerGenerateManifestTask(ctx)
		configureJarTask(ctx)
		configureIdea()
		configureProcessResources(ctx)
		configureJava(ctx)
		registerBuildAndCollectTask(ctx)

		configureModPublishing(ctx)

		if (envTrue("PUB_MAVEN_ENABLE")) {
			configureMavenPublishing(ctx)
		}
	}

	private fun Project.configureJava(ctx: Context) {
		extensions.configure<JavaPluginExtension>("java") {
			withSourcesJar()
			withJavadocJar()
			// Select a real per-node toolchain (17/21/25) so javac runs on the
			// matching JDK — Gradle auto-provisions (foojay) or uses a system JDK —
			// instead of the daemon JVM. Fixes "invalid source release: 25".
			toolchain {
				languageVersion.set(
					org.gradle.jvm.toolchain.JavaLanguageVersion.of(ctx.javaVersion.majorVersion.toInt())
				)
			}
		}

		// JEI does not exist for every MC version this tree covers — it skipped 1.21.2 and 1.21.3
		// entirely, and stopped publishing a Forge flavour after 1.21.1. compat/jei/** is only ever
		// reached through JEI's own @JeiPlugin classpath scan (nothing in the mod references it), so
		// on a node with no `deps.jei` pin it is simply left out of the compile.
		if (propOrNull("deps.jei") == null) {
			extensions.getByType<org.gradle.api.tasks.SourceSetContainer>()
				.named("main").get().java.exclude("**/compat/jei/**")
			logger.lifecycle("No JEI for this version — compat/jei is excluded from the build")
		}

		// The Fabric-only half of the mod: entrypoints and everything that speaks the Fabric API.
		// It lives inside the shared source tree rather than in its own Gradle source set because
		// Stonecutter projects root src/ wholesale — a second source set would need its own
		// projection. Excluding by path is equivalent and costs nothing.
		//
		// Note this is a LOADER gate, not a version one: net.fabricmc.** is simply absent from a
		// Forge/NeoForge node's classpath, so leaving it in is a compile error, not dead code.
		if (ctx.loader !is Loader.Fabric) {
			extensions.getByType<org.gradle.api.tasks.SourceSetContainer>()
				.named("main").get().java.exclude(
					"**/alexscaves/fabric/**",
					// The other half of the same story, in the one place it could not live under
					// alexscaves/fabric/: a mixin's package must sit under the config's declared
					// `package` (com.github.alexmodguy.alexscaves.mixin) or Mixin cannot resolve it.
					// mixin/fabric/** exists purely because Fabric has no event bus — Forge and
					// NeoForge fire every one of those hooks from @SubscribeEvent — so on those two
					// loaders the classes are not merely redundant, they would fire ServerEvents a
					// second time. Excluded here and pruned back out of the mixin config in
					// processResources, because Fletching Table's @Mixin scan ignores this exclude
					// (see DataPackMigration.pruneMixinPackage).
					"**/mixin/fabric/**",
				)
		}

		// MC 1.20.5 replaced the MapDecoration.Type enum with the MapDecorationType registry, which
		// is exactly the extension point MapDecorationTypeMixin existed to fake — it appends a
		// constant to the enum's $VALUES — and MapDecorationMixin's loader-patched render(int) hook
		// went with it. Both target a type that no longer exists, so from 1.20.5 the marker is an
		// ordinary registered object (see ACVanillaMapUtil) and these two are out of the build.
		// A whole-file Stonecutter gate is not available: their bodies already carry commented-out
		// //? arms from the 1.20.2 wave, and those cannot nest. VanillaMapDecorationRenderer — the
		// hand-rolled quad the mixin hops to, split out of ClientEvents so it could be excluded as a
		// file — goes the same way for the same reason.
		//
		// MapRendererMapInstanceMixin goes with them. It exists purely to hand that quad the pose
		// stack, buffer source and light vanilla is drawing the map with, via three statics on
		// ClientEvents; with VanillaMapDecorationRenderer out of the build nothing reads them, so from
		// 1.20.5 it was an injection that only cost a target to chase on every MC bump. 1.21.9 turned
		// that cost into a load failure — MapRenderer#render now takes a SubmitNodeCollector — which
		// is when it was noticed.
		if (ctx.stonecutter.eval(ctx.currentMcVersion, ">=1.20.5")) {
			extensions.getByType<org.gradle.api.tasks.SourceSetContainer>()
				.named("main").get().java.exclude(
					"**/mixin/MapDecorationMixin.java",
					"**/mixin/MapDecorationTypeMixin.java",
					"**/mixin/client/MapRendererMapInstanceMixin.java",
					"**/client/render/VanillaMapDecorationRenderer.java",
				)
		}

		// Fabric-only source-set excludes go here, for files that a Stonecutter gate cannot express
		// (typically: a body already full of non-nesting `//?` blocks, or a Forge-family API with no
		// Fabric analogue at all). Populated during the Fabric milestone; empty until then.

		// client/render/compat/** re-implements the pre-1.21.2 entity-renderer API on top of the
		// render-state architecture 1.21.2 introduced. It extends classes that do not exist below
		// 1.21.2 (EntityRenderState & friends), so it cannot compile there — and nothing below
		// 1.21.2 references it, because the Stonecutter import rules that point the renderers
		// at it are themselves gated on >=1.21.2.
		// mixin/renderstate/** is the same story: it mixes into EntityRenderState, which does not
		// exist below 1.21.2. Excluding it here keeps the .class files out of the jar, but
		// alexscaves.mixins.json still declares them (that config is hand-maintained and does not
		// honour this
		// exclude), and a config naming an absent class is a hard load failure — so they are pruned
		// back out in processResources (see DataPackMigration.pruneRenderStateMixins).
		if (!ctx.stonecutter.eval(ctx.currentMcVersion, ">=1.21.2")) {
			extensions.getByType<org.gradle.api.tasks.SourceSetContainer>()
				.named("main").get().java.exclude(
					"**/client/render/compat/**",
					"**/mixin/renderstate/**",
					// The 1.21.2 sky, which mixes into SkyRenderer — a class that did not exist
					// before it. Its opposite number, LevelRendererSkyMixin, is excluded above 1.21.2
					// in the block below. Both are pruned from the mixin config in processResources.
					"**/mixin/client/SkyRendererMixin.java",
					// The 1.21.2 home of the primordial-armour saturation bonus. FoodProperties is
					// not a Consumable listener below it, so the redirected call is not there.
					"**/mixin/FoodPropertiesMixin.java",
					// 1.21.2 split minecart movement into MinecartBehavior implementations and moved
					// getPos/getPosOffs onto the classic one; below it the class does not exist and
					// AbstractMinecartMixin still makes those two injections itself.
					"**/mixin/OldMinecartBehaviorMixin.java",
				)
		}
		if (ctx.stonecutter.eval(ctx.currentMcVersion, ">=1.21.2")) {
			extensions.getByType<org.gradle.api.tasks.SourceSetContainer>()
				.named("main").get().java.exclude("**/mixin/client/LevelRendererSkyMixin.java")
		}
		// The ValueInput/ValueOutput bridge (see ACCompat) — both targets arrived with 1.21.6.
		if (!ctx.stonecutter.eval(ctx.currentMcVersion, ">=1.21.6")) {
			extensions.getByType<org.gradle.api.tasks.SourceSetContainer>()
				.named("main").get().java.exclude(
					"**/mixin/TagValueInputAccessor.java",
					"**/mixin/TagValueOutputAccessor.java",
					// Substitutes ACClientCompat#setImmediateTint for the colour modulator that
					// RenderType#draw hardcodes. Below 1.21.6 RenderSystem#setShaderColor still
					// exists and the facade uses it, so there is nothing for this to do — and the
					// method it modifies does not exist to be modified.
					"**/mixin/client/CompositeRenderTypeMixin.java",
					// The cave book's picture-in-picture port. GuiRenderState, PictureInPictureRenderer
					// and PictureInPictureRenderState all arrived with 1.21.6, and below it the book
					// is still drawn inline on the screen's own pose stack.
					"**/client/gui/book/CaveBookRenderState.java",
					"**/client/gui/book/CaveBookPipRenderer.java",
					"**/mixin/client/GuiRenderStateAccessor.java",
						// The chunk-layer render stages. 1.21.6 replaced LevelRenderer#renderSectionLayer
						// with ChunkSectionsToRender#renderGroup over the new ChunkSectionLayerGroup;
						// below it neither type exists and LevelRenderStageMixin holds that anchor.
						"**/mixin/client/ChunkSectionsToRenderMixin.java",
				)
		}
		// The non-NeoForge half of that port. NeoForge registers the renderer through
		// RegisterPictureInPictureRenderersEvent (see ClientProxy); Forge ships no patch for
		// GuiRenderer at all and Fabric has no such event either, so both take the mixin, which
		// targets the vanilla class and needs nothing from a loader.
		if (ctx.loader is Loader.NeoForge || !ctx.stonecutter.eval(ctx.currentMcVersion, ">=1.21.6")) {
			extensions.getByType<org.gradle.api.tasks.SourceSetContainer>()
				.named("main").get().java.exclude("**/mixin/client/GuiRendererMixin.java")
		}
		// 1.21.11 deleted Level#getTimeOfDay(float) — and with it DimensionType#fixedTime() and
		// ClientLevel#dayTime() — so this redirect has no anchor left to name. Nothing is lost:
		// the sun, moon and star angles are EnvironmentAttributes now, read through
		// EnvironmentAttributeProbe#getValue(attr, partialTick) over time-based layers, which
		// interpolates by partial tick natively. That is exactly what Citadel's lerp added, so
		// vanilla subsumes the feature rather than dropping it.
		if (ctx.stonecutter.eval(ctx.currentMcVersion, ">=1.21.11")) {
			extensions.getByType<org.gradle.api.tasks.SourceSetContainer>()
				.named("main").get().java.exclude("**/mixin/client/citadel/SkyTimeOfDayMixin.java")
		}
		// The mirror image of the exclusion above: EnvironmentAttributeProbe is what 1.21.11 replaced
		// ClientLevel#getSkyColor / #getSkyDarken with, so this mixin's target class does not exist
		// below it. ClientLevelMixin keeps the two old injections for those nodes.
		if (!ctx.stonecutter.eval(ctx.currentMcVersion, ">=1.21.11")) {
			extensions.getByType<org.gradle.api.tasks.SourceSetContainer>()
				.named("main").get().java.exclude("**/mixin/client/EnvironmentAttributeProbeMixin.java")
		}

		// Client classes whose SUPERTYPES were deleted in MC 26.1, with no successor to port
		// them onto. A file whose whole body is a Stonecutter block is not an option here — each
		// already contains /* */-commented arms from earlier waves, and those cannot nest.
		//
		//   {Tabula,Vanilla,Baked}*   the vanilla-block-model half of the vendored Tabula loader.
		//                             @Deprecated(since = "2.6.2") upstream and entirely unreachable
		//                             here (only loadTabulaModel/getCubeByName/getAllCubes are
		//                             live); its BlockElement / ItemTransform(s) / UnbakedModel
		//                             dependencies are all gone or moved in 26.1.
		//
		// None is a mixin, so unlike the renderstate pair above there is nothing to prune back out
		// of the mixin config. Their few call sites are gated <26 in source.
		// Alex's Caves' OWN 26-incompatible classes get appended here as the 26.x wave lands.
		if (ctx.stonecutter.eval(ctx.currentMcVersion, ">=26")) {
			extensions.getByType<org.gradle.api.tasks.SourceSetContainer>()
				.named("main").get().java.exclude(
					"**/citadel/client/model/container/TabulaModelBlock.java",
					"**/citadel/client/model/container/VanillaTabulaModel.java",
					"**/citadel/client/model/container/BakedTabulaModel.java",
					// LightTexture is gone: 26.1 split it into Lightmap (the texture and its upload)
					// and LightmapRenderStateExtractor (everything that used to be computed inline).
					// This mixin cannot follow it — it names one target class, and its body is a
					// re-implementation of updateLightTexture that 26.1 does not have to re-implement.
					// The pair below replaces it; ACLightmapAdditions holds what all three share.
					"**/mixin/client/LightTextureMixin.java",
					// 26 turned villager trades into datapack registry entries: there is no
					// VillagerTrades.ItemListing to implement and no loader event to add one from.
					// The two cabin-map trades ship as data/alexscaves/villager_trade/*.json instead,
					// pulled into vanilla's cartographer/level_2 and wandering_trader/common trade
					// tags; DataPackMigration.dropVillagerTradeData removes those files below 26.
					"**/server/event/ACVillagerTradeEvents.java",
					"**/server/entity/util/VillagerUndergroundCabinMapTrade.java",
					// ...and the whole Fabric side of the same feature: the two stand-ins for the
					// loader events that class listened to, the dispatcher that posts them and the
					// two mixins that hand vanilla the merged result. All five name
					// VillagerTrades.ItemListing, which 26 deleted with the rest of the code-side
					// trade API, so none of them can compile here — and none of them is needed,
					// because on 26 the trades come from the datapack on every loader alike.
					"**/fabric/forge/event/village/VillagerTradesEvent.java",
					"**/fabric/forge/event/village/WandererTradesEvent.java",
					"**/fabric/event/ACFabricVillagerTrades.java",
					"**/mixin/fabric/VillagerTradesTableMixin.java",
					"**/mixin/fabric/WandererTradesTableMixin.java",
				)
		}
		// ...and the mirror image. Both target classes arrived with 26.1.
		if (!ctx.stonecutter.eval(ctx.currentMcVersion, ">=26")) {
			extensions.getByType<org.gradle.api.tasks.SourceSetContainer>()
				.named("main").get().java.exclude(
					"**/mixin/client/LightmapMixin.java",
					"**/mixin/client/LightmapRenderStateExtractorMixin.java",
					// 26 moved day time out of ServerLevel#tickTime into a WorldClock, whose
					// ServerClockManager$ClockInstance is where a CELESTIAL tick-rate modifier now
					// applies. That class does not exist below 26; Server/ClientLevelMixin keep the
					// old @ModifyConstant on those nodes.
					"**/mixin/citadel/ServerClockInstanceMixin.java",
					"**/mixin/client/citadel/ClientClockManagerMixin.java",
					// 26 dropped the ItemDisplayContext from SpecialModelRenderer#submit, so this
					// republishes it from the one place that still holds it. Every earlier version
					// passes it as a parameter and has nothing for this to do — and below 1.21.4
					// ItemStackRenderState does not exist at all. See ACItemDisplayContexts.
					"**/mixin/client/ItemStackRenderStateMixin.java",
				)
		}
		// 26.2 finished the deferred-rendering move: the chunk layers are no longer drawn from a
		// method a mod can inject into, and there is nothing left to hand a render-stage listener at
		// that point anyway — ChunkSectionsToRender#renderGroup runs during the REPLAY pass, after
		// FeatureRenderDispatcher#prepareFrame has consumed the frame's SubmitNodeCollector, so a
		// stage fired from it could only draw into a collector that is already spent. It also reads
		// LevelRenderer#getTicks(), which 26.2 deleted. Both block-layer stages move into
		// LevelRenderStageMixin's single submitFeatures anchor instead; see ACRenderContext for what
		// collapsing the stages onto one moment in the frame costs.
		if (ctx.stonecutter.eval(ctx.currentMcVersion, ">=26.2")) {
			extensions.getByType<org.gradle.api.tasks.SourceSetContainer>()
				.named("main").get().java.exclude("**/mixin/client/ChunkSectionsToRenderMixin.java")
		}
	}

	private fun Project.registerGenerateManifestTask(ctx: Context) {
		val manifestOutputDir = layout.buildDirectory.dir("generated/modManifest")
		val generateTask = tasks.register<GenerateModManifestTask>("generateModManifest") {
			content.set(ctx.loader.generateManifest(ctx))
			outputFile.set(layout.buildDirectory.file("generated/modManifest/${ctx.loader.manifestPathFor(ctx)}"))
		}

		the<JavaPluginExtension>().sourceSets.named("main") { resources.srcDir(manifestOutputDir) }
		tasks.named<ProcessResources>("processResources") { dependsOn(generateTask) }
	}

	// Data-pack format per MC version (from each version.json `pack_version.data`).
	private fun packFormatFor(mc: String): Int = when (mc) {
		"1.20.1" -> 15
		"1.20.2" -> 18
		"1.20.3", "1.20.4" -> 26
		"1.20.5", "1.20.6" -> 41
		"1.21", "1.21.1" -> 48
		"1.21.2", "1.21.3" -> 57
		"1.21.4" -> 61
		"1.21.5" -> 71
		"1.21.6" -> 80
		"1.21.7", "1.21.8" -> 81
		"1.21.9", "1.21.10" -> 88
		"1.21.11" -> 94
		"26.1", "26.1.1", "26.1.2" -> 101
		"26.2" -> 107
		else -> 48
	}
	private fun packMinorFor(mc: String): Int = when (mc) {
		"1.21.11", "26.1", "26.1.1", "26.1.2", "26.2" -> 1
		else -> 0
	}
	// mcmeta schema changed at 1.21.9 (data-format 88): <=1.21.8 needs a `pack_format` int and
	// rejects min_format/max_format; >=1.21.9 requires min_format/max_format as [major, minor].
	private fun packMetaFieldsFor(mc: String): String {
		val f = packFormatFor(mc)
		val m = packMinorFor(mc)
		return if (f <= 81) "\"pack_format\": $f"
		else "\"pack_format\": $f, \"min_format\": [$f, $m], \"max_format\": [$f, $m]"
	}

	private fun Project.configureProcessResources(ctx: Context) {
		tasks.named<ProcessResources>("processResources") {
			dependsOn(tasks.named("stonecutterGenerate"), "kspKotlin")
			// Forge bundles an older Mixin library than Fabric/NeoForge for the same MC (e.g. Forge
			// 50.x on 1.20.6 does not recognise JAVA_21), so pin Forge's mixin compatibilityLevel to
			// JAVA_17 — a level every bundled Mixin across 1.20.1–26.2 accepts.
			//
			// A plain `filter`, NOT `expand`: Gradle's expand() runs the file through Groovy's
			// SimpleTemplateEngine, which treats EVERY `$` as an interpolation. A nested mixin is
			// addressed as "Outer$Inner" (this mod has SpriteResourceLoaderMixin$PalettedPermutations-
			// Accessor), so expand() dies with "Missing property (PalettedPermutationsAccessor)" — and
			// the Groovy escape `\$` is not legal JSON, so there is nothing to escape it with.
			val mixinJava = if (ctx.loader is Loader.Forge) "JAVA_17" else "JAVA_${ctx.javaVersion.majorVersion}"
			filesMatching("*.mixins.json") {
				filter { line: String -> line.replace("\${java}", mixinJava) }
			}
			// pack.mcmeta must carry a per-version pack_format int (MC < 1.21.11 requires it and
			// rejects the min_format/max_format-only schema). Forge keeps pack.mcmeta in the jar,
			// so stamp the right value from ${pack_format}.
			filesMatching(listOf("pack.mcmeta", "**/pack.mcmeta")) {
				expand("pack_meta" to packMetaFieldsFor(ctx.currentMcVersion))
			}
			exclude(ctx.loader.excludedResourcesFor(ctx))

			// Fletching Table fills the mixin config's `mixins` array from an @Mixin source scan that
			// ignores the source-set exclude, so it lists mixin/renderstate/** even on the nodes that
			// cannot compile it. A config naming an absent class is a hard load failure — prune them.
			if (!ctx.stonecutter.eval(ctx.currentMcVersion, ">=1.21.2")) doLast {
				val pruned = DataPackMigration.pruneRenderStateMixins(destinationDir, ctx.modId)
				logger.lifecycle("Pruned $pruned render-state mixins from ${ctx.modId}.mixins.json")
			}

			// Any Alex's Caves mixin whose TARGET CLASS disappears on a given MC version gets the
			// same treatment: excluded from the compile in configureJava, then pruned here with
			// DataPackMigration.pruneMixinEntries(destinationDir, ctx.modId, listOf(...)) so the
			// config never names an absent class. Populated per wave; empty on the baseline.
			val vanishedMixins = buildList {
				// MapDecoration.Type became the MapDecorationType registry in 1.20.5, taking both
				// mixins that extended the enum with it (see ACVanillaMapUtil).
				if (ctx.stonecutter.eval(ctx.currentMcVersion, ">=1.20.5")) {
					add("MapDecorationMixin")
					add("MapDecorationTypeMixin")
					// Its only consumer, VanillaMapDecorationRenderer, went with them.
					add("client.MapRendererMapInstanceMixin")
				}
				// 1.21.2 emptied LevelRenderer#renderSky out into SkyRenderer, so exactly one of the
				// two sky mixins compiles on any given node — see the source-set excludes above.
				if (ctx.stonecutter.eval(ctx.currentMcVersion, ">=1.21.2")) {
					add("client.LevelRendererSkyMixin")
				} else {
					add("client.SkyRendererMixin")
					// Its target exists on every version; what does not exist below 1.21.2 is the
					// FoodData call it redirects, so the mixin is 1.21.2-and-up only.
					add("FoodPropertiesMixin")
					// OldMinecartBehavior arrived with the 1.21.2 minecart rewrite.
					add("OldMinecartBehaviorMixin")
				}
				// The two halves of the ValueInput/ValueOutput bridge — see ACCompat. Their targets
				// arrived with 1.21.6, and the source files are excluded from the compile above.
				if (!ctx.stonecutter.eval(ctx.currentMcVersion, ">=1.21.6")) {
					add("TagValueInputAccessor")
					add("TagValueOutputAccessor")
					// RenderType$CompositeRenderType#draw only gained the hardcoded colour
					// modulator this mixin rewrites in 1.21.6 — see ACClientCompat#setImmediateTint.
					add("client.CompositeRenderTypeMixin")
					// GuiGraphics only started recording into a GuiRenderState in 1.21.6 — see the
					// cave book's picture-in-picture port.
					add("client.GuiRenderStateAccessor")
						// ChunkSectionsToRender is the 1.21.6 successor to renderSectionLayer — see
						// ACLevelRenderStage. Below it the anchor is in LevelRenderStageMixin instead.
						add("client.ChunkSectionsToRenderMixin")
					}
				// And its non-NeoForge half, which NeoForge replaces with a registration event.
				// Kept on Forge AND Fabric from 1.21.6 — neither loader offers a way into the
				// renderer map, and the mixin's target is plain vanilla on both.
				if (ctx.loader is Loader.NeoForge || !ctx.stonecutter.eval(ctx.currentMcVersion, ">=1.21.6")) {
					add("client.GuiRendererMixin")
				}
				// Citadel's day-time lerp. 1.21.11 deleted Level#getTimeOfDay(float), the call this
				// redirects, along with DimensionType#fixedTime() and ClientLevel#dayTime() — and the
				// celestial angles are partial-tick-interpolated EnvironmentAttributes now, so vanilla
				// does the lerp itself. See the source-set exclude above.
				if (ctx.stonecutter.eval(ctx.currentMcVersion, ">=1.21.11")) {
					add("client.citadel.SkyTimeOfDayMixin")
				} else {
					// The other side of the same change: the probe that replaced ClientLevel's sky
					// getters does not exist yet, so the mixin that carries the cave-biome sky
					// override on 1.21.11 has no target below it.
					add("client.EnvironmentAttributeProbeMixin")
				}
				// 26.1 split LightTexture into Lightmap + LightmapRenderStateExtractor, so exactly one
				// side of this pair compiles on any given node — see the source-set excludes above.
				if (ctx.stonecutter.eval(ctx.currentMcVersion, ">=26")) {
					add("client.LightTextureMixin")
					// The Fabric trade-table pair, whose source files leave the compile with the
					// rest of the code-side trade API above. Listed here as well as pruned by
					// package on the other two loaders, because on Fabric the fabric. prefix stays.
					add("fabric.VillagerTradesTableMixin")
					add("fabric.WandererTradesTableMixin")
				} else {
					add("client.LightmapMixin")
					add("client.LightmapRenderStateExtractorMixin")
					// The display-context republisher 26 made necessary — see the source-set
					// exclude above.
					add("client.ItemStackRenderStateMixin")
					// The world clock is 26's replacement for the day-time counter — see the
					// source-set exclude above.
					add("citadel.ServerClockInstanceMixin")
					add("client.citadel.ClientClockManagerMixin")
				}
				// The chunk-layer anchor is gone again at the far end of the range, for the opposite
				// reason it was missing below 1.21.6: there renderGroup did not exist yet, here it runs
				// after the frame's collector has been consumed. See the source-set exclude above.
				if (ctx.stonecutter.eval(ctx.currentMcVersion, ">=26.2")) {
					add("client.ChunkSectionsToRenderMixin")
				}
			}
			if (vanishedMixins.isNotEmpty()) doLast {
				val pruned = DataPackMigration.pruneMixinEntries(destinationDir, ctx.modId, vanishedMixins)
				logger.lifecycle("Pruned $pruned absent-target mixins from ${ctx.modId}.mixins.json")
			}

			// And the same story once more for mixin/fabric/**, which is excluded from the compile on
			// Forge and NeoForge (see configureJava). Prefix-pruned rather than listed class by class:
			// the whole package is Fabric-only by construction, so there is nothing to keep in sync.
			if (ctx.loader !is Loader.Fabric) doLast {
				val pruned = DataPackMigration.pruneMixinPackage(destinationDir, ctx.modId, "fabric.")
				logger.lifecycle("Pruned $pruned Fabric-only mixins from ${ctx.modId}.mixins.json")
			}

			// That same scan also puts CLIENT mixins in the common `mixins` array. On Forge/NeoForge
			// the dist cleaner blocks those classes on a server (that is where this repo's benign
			// `/ERROR]` lines come from), but FABRIC HAS NO DIST CLEANER — a client mixin left in
			// `mixins` is applied on a dedicated server, whose classpath has no client classes at
			// all, and mixin aborts the launch. The `client` array already says "client dist only".
			if (ctx.loader is Loader.Fabric) doLast {
				val moved = DataPackMigration.partitionClientMixins(destinationDir, ctx.modId)
				logger.lifecycle("Moved $moved client-only mixins into ${ctx.modId}.mixins.json's client list")
			}

			// Data-pack shapes that changed with the MC version. Stonecutter cannot do this —
			// it leaves `//?` markers in JSON, which vanilla's strict parser rejects.
			//
			// Unconditional: upstream wrote a structure-set TAG where vanilla only ever accepted a
			// single id, so the field has never been read. See dropUnreadableExclusionZones.
			doLast {
				val dropped = DataPackMigration.dropUnreadableExclusionZones(destinationDir)
				logger.lifecycle("Dropped $dropped unreadable structure-set exclusion zones")
			}
			// 1.20.3 renamed the block minecraft:grass to minecraft:short_grass.
			if (ctx.stonecutter.eval(ctx.currentMcVersion, ">=1.20.3")) doLast {
				val renamed = DataPackMigration.renameShortGrassTo1203(destinationDir)
				logger.lifecycle("Renamed minecraft:grass to minecraft:short_grass in $renamed files")
			}
			// 1.21.9 split the chain in two, so minecraft:chain is now minecraft:iron_chain.
			if (ctx.stonecutter.eval(ctx.currentMcVersion, ">=1.21.9")) doLast {
				val renamed = DataPackMigration.renameIronChainTo1219(destinationDir)
				logger.lifecycle("Renamed minecraft:chain to minecraft:iron_chain in $renamed files")
			}
			val migrateTo1205 = ctx.stonecutter.eval(ctx.currentMcVersion, ">=1.20.5")
			// 1.20.5 made the int-provider codecs MapCodecs, which drops their `value` wrapper.
			if (migrateTo1205) doLast {
				val flattened = DataPackMigration.flattenIntProvidersTo1205(destinationDir)
				logger.lifecycle("Flattened int providers in $flattened worldgen files")
			}
			if (migrateTo1205) doLast {
				val changed = DataPackMigration.migrateTo1205(destinationDir)
				logger.lifecycle("Migrated $changed data-pack files to the 1.20.5 item-stack format")
				// 1.20.5 also dropped fog_distance's matrix parameter and stopped writing `normal`
				// out of the entity vertex shaders. Both are GLSL link failures — a dead render
				// type on the client and nothing at all in a server log. See
				// DataPackMigration.migrateCoreShadersTo1205.
				val relinked = DataPackMigration.migrateCoreShadersTo1205(destinationDir, ctx.modId)
				logger.lifecycle("Relinked $relinked core shaders against the 1.20.5 GLSL interface")
			}
			// 1.20.5 turned LocationPredicate's `structure`/`biome` and the advancement
			// BlockPredicate's `tag` into holder sets. Both records are all-optionalFieldOf, so
			// the old keys are dropped in silence and what is left matches EVERYWHERE — ten
			// advancements granted on world entry. See DataPackMigration.
			if (migrateTo1205) doLast {
				val changed = DataPackMigration.migrateAdvancementPredicatesTo1205(destinationDir)
				logger.lifecycle("Migrated $changed advancement location predicates to the 1.20.5 holder-set format")
			}
			// The data pack is authored Forge-side; NeoForge reads its own namespaces.
			if (ctx.loader is Loader.NeoForge) doLast {
				val changed = DataPackMigration.migrateNeoForge(
					destinationDir,
					conventionTags = migrateTo1205,
					indexedLootModifiers = !ctx.stonecutter.eval(ctx.currentMcVersion, ">=26"),
				)
				logger.lifecycle("Re-namespaced $changed data-pack files for NeoForge")
			}
			// Forge 26 moved the convention tags to `c:` as well — the tag half of the pass above,
			// and only that half. See DataPackMigration.migrateConventionTags.
			//
			// Fabric takes the same pass on EVERY node: `c:` IS the Fabric convention namespace and
			// always has been, so a Fabric jar shipping data/forge/tags loads none of them. The other
			// halves of the NeoForge pass are deliberately skipped — biome/structure modifiers and
			// global loot modifiers are Forge-family datapack mechanisms with no Fabric reader at
			// all, so re-namespacing them would buy nothing; both become Java-side work instead.
			val cTags = ctx.loader is Loader.Fabric ||
				(ctx.loader is Loader.Forge && ctx.stonecutter.eval(ctx.currentMcVersion, ">=26"))
			if (cTags) doLast {
				val changed = DataPackMigration.migrateConventionTags(destinationDir)
				logger.lifecycle("Re-namespaced $changed data-pack files into the c: convention tags")
			}
			// 26.2 widened MobCategory's constructor, and this file names it by descriptor. Only
			// NeoForge reads it — see Loader.NeoForge.enumExtensionsKey and
			// DataPackMigration.retargetEnumExtensionsTo1262.
			if (ctx.loader is Loader.NeoForge && ctx.stonecutter.eval(ctx.currentMcVersion, ">=26.2")) doLast {
				val retargeted = DataPackMigration.retargetEnumExtensionsTo1262(destinationDir)
				logger.lifecycle("Retargeted $retargeted enum extensions at the 26.2 MobCategory constructor")
			}
			// Fabric's HolderSet codec reads a tag or a list, and nothing more — the composite
			// biome set the cabin structure is authored with has no reader here at all, and an
			// unreadable one is fatal. See DataPackMigration.flattenCompositeHolderSets.
			if (ctx.loader is Loader.Fabric) doLast {
				val flattened = DataPackMigration.flattenCompositeHolderSets(destinationDir)
				logger.lifecycle("Flattened composite HolderSets in $flattened data-pack files")
			}
			// …and on Fabric the `c:` tags the mod READS are only as complete as the player's
			// fabric-api, so seven of them have to be defined here. See the long note on
			// DataPackMigration.backfillFabricConventionTags.
			if (ctx.loader is Loader.Fabric) doLast {
				val written = DataPackMigration.backfillFabricConventionTags(destinationDir)
				logger.lifecycle("Backfilled $written c: convention tags Fabric API may not define")
			}
			// Fabric has no global-loot-modifier system at all, so the seven modifier files and the
			// index that names them are Forge-format data nothing on this loader reads. The feature is
			// carried by fabric.loot.ACFabricLootModifiers + mixin.fabric.LootTableModifierMixin
			// instead; see the note on dropForgeLootModifiers for why the two must be edited together.
			if (ctx.loader is Loader.Fabric) doLast {
				val dropped = DataPackMigration.dropForgeLootModifiers(destinationDir, ctx.modId)
				logger.lifecycle("Dropped $dropped Forge loot-modifier data files Fabric cannot read")
			}
			// Last, so the two passes above still see the folder names they were written against.
			if (ctx.stonecutter.eval(ctx.currentMcVersion, ">=1.21")) doLast {
				val moved = DataPackMigration.migrateTo121(destinationDir)
				logger.lifecycle("Moved $moved data-pack files into the 1.21 singular folders")
				// After the move, so it can look for the singular loot_table/ folder.
				val relooted = DataPackMigration.migrateLootTo121(destinationDir)
				logger.lifecycle("Rewrote looting functions/conditions in $relooted loot tables")
			}
			// 1.21.2 resolves armour textures through an equipment model instead of Forge's
			// deleted getArmorTexture hook; both the model and its texture are derived from
			// the textures/armor/ files the older nodes use directly.
			if (ctx.stonecutter.eval(ctx.currentMcVersion, ">=1.21.2")) doLast {
				val written = DataPackMigration.migrateEquipmentTo12102(destinationDir, ctx.modId)
				logger.lifecycle("Wrote $written equipment models for the 1.21.2 armour textures")
				// 1.21.2 replaced the `{"item": …}` / `{"tag": …}` recipe ingredient objects with a
				// bare string / `#tag` string (Ingredient is a HolderSet<Item> now).
				val reing = DataPackMigration.migrateIngredientsTo1212(destinationDir)
				logger.lifecycle("Rewrote ingredients in $reing recipes to the 1.21.2 string format")
				// 1.21.2 also moved post chains to assets/<ns>/post_effect/, moved post programs from
				// shaders/program/ to shaders/post/, and made every vertex/fragment id pathed. A chain
				// that fails to load is logged and not thrown, so all seven of this mod's effects would
				// simply never draw — see DataPackMigration.migrateShadersTo1212.
				val reshaded = DataPackMigration.migrateShadersTo1212(destinationDir, ctx.modId)
				logger.lifecycle("Rewrote $reshaded shader assets into the 1.21.2 layout")
				// 1.21.2 dropped the carving-step map from a biome's `carvers`, which is now one flat
				// HolderSet. A biome that fails to parse takes the whole registry load down — see
				// DataPackMigration.flattenBiomeCarversTo1212.
				val recarved = DataPackMigration.flattenBiomeCarversTo1212(destinationDir)
				logger.lifecycle("Flattened carvers in $recarved biomes to the 1.21.2 shape")
			}
			// 1.21.4 made an item's model indirect: assets/<ns>/items/<id>.json is now what binds an
			// item to a model, and the legacy models/item/<id>.json alone renders nothing. Logged
			// per item and not thrown, so every item in the mod silently became the missing-model
			// cube — see DataPackMigration.writeItemModelDefinitions.
			if (ctx.stonecutter.eval(ctx.currentMcVersion, ">=1.21.4")) doLast {
				val written = DataPackMigration.writeItemModelDefinitions(
					destinationDir,
					ctx.modId,
					// The spawn-egg tints are only spelled at their registration site. Read from the
					// root source set, not the node's projection — it is not version-gated.
					rootProject.file("src/main/java/com/github/alexmodguy/alexscaves/server/item/ACItemRegistry.java"),
				)
				logger.lifecycle("Wrote $written item model definitions for the 1.21.4 item format")
				// 1.21.4 turned a biome's `music` into a weighted list. A biome that fails to parse
				// takes the whole registry load down — see DataPackMigration.wrapBiomeMusicTo1214.
				// Skipped from 1.21.11, where the field leaves `effects` entirely and the weighting
				// is gone again: wrapping it there would only be undone by the ≥1.21.11 pass.
				if (!ctx.stonecutter.eval(ctx.currentMcVersion, ">=1.21.11")) {
					val remixed = DataPackMigration.wrapBiomeMusicTo1214(destinationDir)
					logger.lifecycle("Wrapped music in $remixed biomes into the 1.21.4 weighted list")
				}
				// 1.21.4 also moved equipment definitions out of the model tree. An armour item whose
				// asset_id resolves to nothing is not a missing texture, it is no layer at all — the
				// piece renders invisible, silently. Runs after the ≥1.21.2 pass that writes them.
				val requipped = DataPackMigration.relocateEquipmentTo1214(destinationDir, ctx.modId)
				logger.lifecycle("Moved $requipped equipment definitions into the 1.21.4 equipment/ folder")
			}
			// 1.21.5 deleted item/template_spawn_egg and its two greyscale layers, which every one
			// of this mod's 43 spawn-egg models parents to — see DataPackMigration.retemplateSpawnEggs.
			if (ctx.stonecutter.eval(ctx.currentMcVersion, ">=1.21.5")) doLast {
				val retemplated = DataPackMigration.retemplateSpawnEggs(destinationDir, ctx.modId)
				logger.lifecycle("Re-pointed $retemplated spawn-egg models at this mod's own egg layers")
				// 1.21.5 also made an advancement tab's background a bare id rather than the texture
				// file, and the client's expansion of the old value names a file that cannot exist —
				// so the tab drew the missing texture. Expect exactly 1: this mod has one root.
				val rebacked = DataPackMigration.migrateAdvancementBackgroundsTo1215(destinationDir)
				logger.lifecycle("Rewrote $rebacked advancement backgrounds to the 1.21.5 bare-id form")
				// 1.21.5 deleted the post-program JSON: a pass names its two shaders itself and
				// declares every uniform the pipeline is built from. Runs after the ≥1.21.2 pass
				// that produced the files it folds together — see
				// DataPackMigration.migrateShadersTo1215.
				val refolded = DataPackMigration.migrateShadersTo1215(destinationDir, ctx.modId)
				logger.lifecycle("Folded post programs into $refolded post chains for 1.21.5")
				// 1.21.5 also swapped the quad a post pass draws for the unit quad, which silently
				// collapses every pre-1.21.5 vertex stage to one pixel — see
				// DataPackMigration.migratePostVertexTo1215. Expect 3, one per post vertex shader.
				val rescaled = DataPackMigration.migratePostVertexTo1215(destinationDir, ctx.modId)
				logger.lifecycle("Rewrote $rescaled post vertex shaders onto the 1.21.5 unit quad")
				// 1.21.5 swapped patch_pumpkin and patch_sugar_cane in vanilla's own biomes, which
				// makes the global feature order cyclic and kills the first chunk generated — see
				// DataPackMigration.orderBiomeFeaturesTo1215. Expect 6, one per cave biome.
				val reordered = DataPackMigration.orderBiomeFeaturesTo1215(destinationDir)
				logger.lifecycle("Reordered the vegetal-decoration step in $reordered biomes for 1.21.5")
				// 1.21.5 made a smithing_trim recipe name its trim pattern itself rather than read it
				// off the template item — see DataPackMigration.addTrimPatternsTo1215. Expect 1.
				val retrimmed = DataPackMigration.addTrimPatternsTo1215(destinationDir)
				logger.lifecycle("Named the trim pattern in $retrimmed smithing_trim recipes for 1.21.5")
			}
			// 1.21.6 replaced every scalar shader uniform with a std140 block and split the fog
			// distance in two. Both are client-only GLSL link failures, invisible to runServer —
			// see DataPackMigration.migrateCoreShadersTo1216. Expect 13, one per core shader.
			if (ctx.stonecutter.eval(ctx.currentMcVersion, ">=1.21.6")) doLast {
				val reuniformed = DataPackMigration.migrateCoreShadersTo1216(destinationDir, ctx.modId)
				logger.lifecycle("Rewrote $reuniformed core shaders onto the 1.21.6 uniform blocks")
				// The post shaders got the same treatment, and their chains have to name the blocks
				// the pipeline is built from — see DataPackMigration.migrateShadersTo1216. Runs after
				// the ≥1.21.5 pass whose flat uniform list it groups. Expect 14: 7 of this mod's 8
				// post shaders (bumpy.fsh declares no uniform but its sampler) and all 7 chains.
				val reblocked = DataPackMigration.migrateShadersTo1216(destinationDir, ctx.modId)
				logger.lifecycle("Rewrote $reblocked post shader assets onto the 1.21.6 uniform blocks")
			}
			// 1.21.9 deleted every post-pass vertex shader but rotscale and drew the fullscreen
			// quad from gl_VertexID instead, so a chain naming minecraft:post/sobel or
			// minecraft:post/blit as its VERTEX stage cannot compile — logged and nulled below 26.2,
			// a blanked frame from it. Runs after the ≥1.21.6 pass whose SamplerInfo block it
			// reuses — see DataPackMigration.migratePostShadersTo1219. Expect 15: 7 chains, 3 of
			// this mod's post vertex shaders deleted and the 5 fragment shaders it rewrites.
			if (ctx.stonecutter.eval(ctx.currentMcVersion, ">=1.21.9")) doLast {
				val requadded = DataPackMigration.migratePostShadersTo1219(destinationDir, ctx.modId)
				logger.lifecycle("Rewrote $requadded post shader assets onto the 1.21.9 screen quad")
			}
			// …and 26.2 split the post pipeline's binds into two groups, which turns the chain-level
			// `Globals` declaration the ≥1.21.6 pass emits from the thing that BINDS the block into a
			// duplicate of a bind group 0 entry — fatal, and the 26.2 black main menu. Must run after
			// that pass, and must never run below 26.2, where the same line is what makes GameTime
			// resolve at all — see DataPackMigration.dropPostChainGlobalsTo1262. Expect 1 (hologram).
			if (ctx.stonecutter.eval(ctx.currentMcVersion, ">=26.2")) doLast {
				val unglobbed = DataPackMigration.dropPostChainGlobalsTo1262(destinationDir, ctx.modId)
				logger.lifecycle("Dropped the duplicate Globals bind from $unglobbed post chains for 26.2")
			}
			// 1.21.11 emptied a biome's `effects` into a top-level `attributes` map — everything the
			// client reads off a biome is an EnvironmentAttribute now. An unmigrated biome parses
			// clean and silently renders with vanilla's sky, fog and ambience — see
			// DataPackMigration.migrateBiomeAttributesTo12111. Expect 6, one per cave biome.
			if (ctx.stonecutter.eval(ctx.currentMcVersion, ">=1.21.11")) doLast {
				val reattributed = DataPackMigration.migrateBiomeAttributesTo12111(destinationDir)
				logger.lifecycle("Moved effects into attributes in $reattributed biomes for 1.21.11")
			}
			// 26.2 made EntityPredicate a dispatched map over the sub-predicate registry, so the
			// flat `"type": …` field no longer decodes. Logged and not thrown, i.e. the advancement
			// just vanishes — see DataPackMigration.migrateEntityPredicatesTo262.
			if (ctx.stonecutter.eval(ctx.currentMcVersion, ">=26.2")) doLast {
				val repred = DataPackMigration.migrateEntityPredicatesTo262(destinationDir)
				logger.lifecycle("Rewrote entity predicates in $repred files to the 26.2 dispatched map")
			}
			// Feature.RANDOM_PATCH was deleted at 26.1, NOT at 26.2 — the two passes below were
			// written together during the 26.2 wave and share nothing but that wave. An unreadable
			// worldgen entry is fatal, not skipped: it takes the whole RegistryDataLoader pass down
			// ("Unknown registry key … minecraft:worldgen/feature: minecraft:random_patch", then
			// "Unbound values in registry … configured_feature"), which is exactly how 26.1 was
			// caught — all four 26.1.x NeoForge nodes died on it while 26.2 booted clean. Expect 8
			// (4 configured + 4 placed).
			if (ctx.stonecutter.eval(ctx.currentMcVersion, ">=26.1")) doLast {
				val unrolled = DataPackMigration.unrollRandomPatchesTo1262(destinationDir)
				logger.lifecycle("Unrolled random patches into placement in $unrolled files for 26.1+")
			}
			// minecraft:lake's three block predicates became required at 26.2 and only there — a
			// 26.1 boot parses both lake features with no complaint. Expect 2.
			if (ctx.stonecutter.eval(ctx.currentMcVersion, ">=26.2")) doLast {
				val lakes = DataPackMigration.fillLakePredicatesTo1262(destinationDir)
				logger.lifecycle("Filled the 26.2 lake predicates in $lakes features")
			}
			// …and the mirror image of the >=26 source-set exclusion in configureJava: below 26 the
			// two cabin-map trades come from ACVillagerTradeEvents, so their datapack-registry form
			// is dead weight. Expect 4 — two trades and the two vanilla trade tags naming them.
			if (!ctx.stonecutter.eval(ctx.currentMcVersion, ">=26")) doLast {
				val untraded = DataPackMigration.dropVillagerTradeData(destinationDir, ctx.modId)
				logger.lifecycle("Dropped $untraded pre-26 villager trade data files")
			}
		}
	}

	private fun Project.configureJarTask(ctx: Context) {
		val generateTask = tasks.named("generateModManifest")
		tasks.withType<Jar>().configureEach {
			archiveBaseName.set(ctx.modId)
			dependsOn(generateTask)
			if (ctx.loader is Loader.Forge) {
				manifest.attributes(ctx.loader.mixinConfigAttribute to "${ctx.modId}.mixins.json")
			}
		}
	}

	private fun Project.configureIdea() {
		extensions.configure<IdeaModel>("idea") {
			module {
				isDownloadJavadoc = true
				isDownloadSources = true
			}
		}
	}

	private fun Project.configureFletchingTable(ctx: Context) {
		extensions.configure<FletchingTableExtension> {
			mixins.create("main") { mixin("default", "${ctx.modId}.mixins.json") }
			j52j.register("main") { extension("json", "**/*.json5") }
		}
	}

	private fun Project.registerBuildAndCollectTask(ctx: Context) {
		tasks.register<Copy>("buildAndCollect") {
			from(
				tasks.named(ctx.extension.jarTask.get()),
				tasks.named(ctx.extension.sourcesJarTask.get()),
				tasks.named("javadocJar")
			)
			into(rootProject.layout.buildDirectory.file("libs/${ctx.basicVersion}"))
			dependsOn("build")
			group = "build"
		}
	}
}
