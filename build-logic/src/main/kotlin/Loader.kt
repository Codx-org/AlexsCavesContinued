@file:Suppress("unused")

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import net.peanuuutz.tomlkt.Toml
import org.gradle.api.NamedDomainObjectContainer
import java.util.*

private val JSON = Json { prettyPrint = true; encodeDefaults = true; explicitNulls = false }
private val TOML = Toml { }

sealed class Loader(val id: String) {
	abstract val modManifestPath: String
	abstract val excludedResources: List<String>

	open val isFabricLike: Boolean = false

	// Path/exclusions can depend on the MC version (see NeoForge's legacy manifest name).
	// Defaults fall back to the static values above.
	open fun manifestPathFor(ctx: Context): String = modManifestPath
	open fun excludedResourcesFor(ctx: Context): List<String> = excludedResources

	abstract fun generateManifest(ctx: Context): String

	object Fabric : Loader("fabric") {
		override val isFabricLike = true
		override val modManifestPath = "fabric.mod.json"

		// NOTE: pack.mcmeta is deliberately NOT excluded here (codxlib's copy was, because it
		// ships no data/ or assets/). This mod does, so a Fabric node without pack.mcmeta
		// loads none of its own recipes, loot tables, tags or advancements — and says nothing.
		// The access transformers are Forge-only; Fabric reads the generated .accesswidener.
		override val excludedResources = listOf(
			"META-INF/mods.toml", "META-INF/neoforge.mods.toml",
			"META-INF/accesstransformer.cfg", "META-INF/accesstransformer_mojmap.cfg",
			"aw/*.cfg", ".cache"
		)

		override fun generateManifest(ctx: Context): String {
			val manifest = FabricManifest(
				id = ctx.modId,
				name = ctx.modName,
				version = ctx.baseVersion,
				authors = ctx.authors,
				contributors = ctx.contributors,
				contact = mapOf(
					"sources" to ctx.sourcesUrl, "issues" to ctx.issuesUrl, "homepage" to ctx.homepageUrl
				),
				custom = ctx.discordUrl.takeIf { it.isNotEmpty() }?.let { url ->
					buildJsonObject {
						putJsonObject("modmenu") {
							putJsonObject("links") {
								put("modmenu.discord", url)
							}
						}
					}
				},
				description = ctx.description,
				icon = ctx.fabricIcon,
				license = ctx.licenseName,
				// Both read from stonecutter.properties.toml (mod.fabric.*) rather than being
				// derived from the mod id: an entrypoint class name is not predictable, and
				// getting it wrong loads the mod with no init at all — silently.
				accessWidener = ctx.fabricAccessWidener.takeIf { it.isNotEmpty() },
				entrypoints = buildMap {
					put("main", listOf(ctx.fabricMainEntrypoint.ifEmpty {
						error("Missing 'mod.fabric.entrypoint_main' in stonecutter.properties.toml")
					}))
					ctx.fabricClientEntrypoint.takeIf { it.isNotEmpty() }?.let { put("client", listOf(it)) }
				},
				mixins = listOf("${ctx.modId}.mixins.json"),
				depends = ctx.extension.dependencies.required.associate { it.modid.get() to it.fabricLikeVersionRange.get() },
				recommends = ctx.extension.dependencies.optional.associate { it.modid.get() to it.fabricLikeVersionRange.get() },
				breaks = ctx.extension.dependencies.incompatible.associate { it.modid.get() to it.fabricLikeVersionRange.get() },
				provides = ctx.extension.dependencies.embeds.map { it.modid.get() }
			)
			return JSON.encodeToString(manifest)
		}
	}

	sealed class ForgeLike(id: String) : Loader(id) {
		override val excludedResources = listOf(
			"fabric.mod.json", "aw/*.accesswidener", ".cache"
		)

		override fun generateManifest(ctx: Context): String {
			val forgeDeps = mutableListOf<ForgeDependency>()

			fun addDeps(container: NamedDomainObjectContainer<Dependency>, type: String) {
				container.forEach {
					forgeDeps.add(
						ForgeDependency(
							modId = it.modid.get(),
							side = it.environment.get().uppercase(Locale.getDefault()),
							versionRange = it.forgeLikeVersionRange.get(),
							mandatory = type == "required",
							type = type
						)
					)
				}
			}

			addDeps(ctx.extension.dependencies.required, "required")
			addDeps(ctx.extension.dependencies.optional, "optional")
			addDeps(ctx.extension.dependencies.incompatible, "incompatible")

			val manifest = ForgeManifest(
				license = ctx.licenseName,
				issueTrackerURL = ctx.issuesUrl,
				mods = listOf(
					ForgeMod(
						modId = ctx.modId,
						displayName = ctx.modName,
						version = ctx.baseVersion,
						displayURL = ctx.homepageUrl,
						modUrl = ctx.homepageUrl,
						// Same asset the Fabric manifest names (mod.fabric.icon) — this tree kept the
					// logo where the upstream Forge manifest pointed, not at Fabric's
					// assets/<modid>/icon.png convention, so one property covers both.
					logoFile = ctx.fabricIcon,
						authors = ctx.authors.joinToString(", "),
						credits = "${ctx.authors.joinToString(", ")} Contributors: ${ctx.contributors.joinToString(", ")}",
						description = ctx.description
					)
				),
				dependencies = mapOf(ctx.modId to forgeDeps),
				mixins = listOf(ForgeMixin("${ctx.modId}.mixins.json")),
				// Left empty on purpose: Forge auto-loads META-INF/accesstransformer.cfg (the
				// default location, which is where loom puts this mod's AT), and naming it here
				// as well makes Forge read it twice.
				accessTransformers = emptyList()
			)

			// Spliced into the [[mods]] table rather than modelled as a field on ForgeMod: it would
			// have to be a nullable field, which the encoder still emits, and every other loader
			// would then carry an empty key. Placement matters — FML reads it off the *mod's*
			// config section (LoadingModList calls IModInfo.getConfig().getConfigElement(
			// "enumExtensions")), so a bare top-level key parses fine and is simply never seen.
			val toml = TOML.encodeToString(manifest)
			val key = enumExtensionsKey(ctx)
			return if (key.isEmpty()) toml else toml.replaceFirst("[[mods]]\n", "[[mods]]\n$key")
		}

		/**
		 * The `enumExtensions` line, on the nodes whose loader understands it.
		 *
		 * This mod adds two MobCategory constants (see ACMobCategories). Up to 1.20.6 that was a
		 * plain `MobCategory.create` call and needed nothing in the manifest; NeoForge 1.21 replaced
		 * the patched-in factory with FML's declarative enum extension, which is driven entirely by
		 * this key. Forge never adopted it, and NeoForge below 1.21 rejects unknown manifest keys,
		 * so it is emitted for exactly one loader-and-version combination.
		 */
		open fun enumExtensionsKey(ctx: Context): String = ""
	}

	object NeoForge : ForgeLike("neoforge") {
		override val modManifestPath = "META-INF/neoforge.mods.toml"
		override val excludedResources = (super.excludedResources + "META-INF/mods.toml") + "pack.mcmeta"

		// NeoForge renamed META-INF/mods.toml -> META-INF/neoforge.mods.toml at MC 1.20.5
		// (NeoForge 20.5). Older NeoForge (20.4 and below) reads ONLY the legacy filename,
		// so on those nodes emit META-INF/mods.toml instead — otherwise the manifest is
		// never found and the mod is silently absent at runtime (bit us on 1.20.4-neoforge).
		private fun isLegacy(ctx: Context) = !ctx.stonecutter.eval(ctx.currentMcVersion, ">=1.20.5")

		override fun manifestPathFor(ctx: Context) =
			if (isLegacy(ctx)) "META-INF/mods.toml" else "META-INF/neoforge.mods.toml"

		override fun excludedResourcesFor(ctx: Context) =
			if (isLegacy(ctx)) (super.excludedResources + "META-INF/neoforge.mods.toml") + "pack.mcmeta"
			else excludedResources

		override fun enumExtensionsKey(ctx: Context) =
			if (ctx.stonecutter.eval(ctx.currentMcVersion, ">=1.21")) "enumExtensions=\"META-INF/enumextensions.json\"\n"
			else ""
	}

	object Forge : ForgeLike("forge") {
		override val modManifestPath = "META-INF/mods.toml"
		override val excludedResources = super.excludedResources + "META-INF/neoforge.mods.toml"
		val mixinConfigAttribute = "MixinConfigs"
	}

	companion object {
		fun of(id: String): Loader = when (id) {
			"fabric" -> Fabric
			"neoforge" -> NeoForge
			// "fabricnr"/"forgenr" = the arch-loom NO-REMAP buildscripts for the unobfuscated
			// 26.x nodes; "forgeg" = the arch-loom Forge script for classic nodes. All resolve
			// to the same loader (gating is driven by the node id, not the buildscript name).
			"fabricnr" -> Fabric
			"forge", "forgeg", "forgenr" -> Forge
			else -> error("Unknown loader: '$id'")
		}
	}
}
