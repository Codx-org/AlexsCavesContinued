import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import java.io.File

/**
 * Data-pack migrations that Stonecutter cannot express.
 *
 * Stonecutter only preprocesses source files — a `//? if` block inside a `.json` is copied through
 * verbatim (verified), and vanilla parses data-pack JSON with a strict Gson reader that rejects
 * comments. So the era-dependent parts of the shipped data pack are rewritten here, after
 * `processResources` has staged them, instead of being duplicated per MC version.
 *
 * Everything below is a no-op on files that are already in the target shape, so re-running is safe.
 */
object DataPackMigration {

	private val json = Json { prettyPrint = true }

	/**
	 * MC 1.20.5 replaced the `{"item": …, "nbt": "<snbt>"}` item-stack JSON with the
	 * component-based `{"id": …, "components": {…}}`. That hits two places in this mod's data:
	 * every crafting `result`, and every advancement `display.icon`.
	 */
	fun migrateTo1205(resourcesRoot: File): Int {
		val data = resourcesRoot.resolve("data")
		if (!data.isDirectory) return 0
		var changed = 0
		data.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
			val dirs = file.invariantSeparatorsPath
			// Both spellings are matched so this stays correct whichever side of the 1.21
			// singular-folder rename it runs on.
			val transform: (JsonObject) -> JsonObject = when {
				// capsid_recipes is this mod's own recipe type, but its result goes through
				// ItemStack.CODEC too (see CapsidRecipe.Deserializer).
				dirs.contains("/recipe/") || dirs.contains("/recipes/") ||
					dirs.contains("/capsid_recipes/") -> ::migrateRecipe
				dirs.contains("/advancement/") || dirs.contains("/advancements/") -> ::migrateAdvancement
				dirs.contains("/loot_table/") || dirs.contains("/loot_tables/") -> ::migrateLootTable
				else -> return@forEach
			}
			val original = runCatching { json.parseToJsonElement(file.readText()) as? JsonObject }
				.getOrNull() ?: return@forEach
			val migrated = transform(original)
			if (migrated != original) {
				file.writeText(json.encodeToString(JsonObject.serializer(), migrated))
				changed++
			}
		}
		return changed
	}

	/**
	 * MC 1.21 renamed every data-pack registry folder to the singular form of its registry key.
	 * Nothing inside the files changes — only where they live.
	 */
	fun migrateTo121(resourcesRoot: File): Int {
		val data = resourcesRoot.resolve("data")
		if (!data.isDirectory) return 0
		var moved = 0
		data.listFiles().orEmpty().filter { it.isDirectory }.forEach { namespace ->
			singularFolders.forEach { (plural, singular) ->
				moved += relocate(namespace.resolve(plural), namespace.resolve(singular))
			}
			// tags/<registry> was pluralised one level deeper.
			val tags = namespace.resolve("tags")
			singularTagFolders.forEach { (plural, singular) ->
				moved += relocate(tags.resolve(plural), tags.resolve(singular))
			}
		}
		return moved
	}

	/**
	 * Alex's Mobs' armour: material name -> (texture base name, equipment layer type).
	 *
	 * The texture name differs from the material name for over half of them (the material is named
	 * after the mob, the texture after the item), which is why this is a table and not a rule. The
	 * layer type is the slot the material's one and only item occupies — everything renders through
	 * the humanoid armour layer, including the tarantula-hawk elytra, whose custom model is a
	 * HumanoidModel handed to Forge's `getHumanoidArmorModel` hook.
	 */
	private val armorEquipment = listOf(
		Triple("roadrunner", "roadrunner_boots", "humanoid"),
		Triple("crocodile", "crocodile_chestplate", "humanoid"),
		Triple("centipede", "centipede_leggings", "humanoid_leggings"),
		Triple("moose", "moose_headgear", "humanoid"),
		Triple("raccoon", "frontier_cap", "humanoid"),
		Triple("sombrero", "sombrero", "humanoid"),
		Triple("spiked_turtle_shell", "spiked_turtle_shell", "humanoid"),
		Triple("fedora", "fedora", "humanoid"),
		Triple("emu", "emu_leggings", "humanoid_leggings"),
		Triple("tarantula_hawk_elytra", "tarantula_hawk_elytra", "humanoid"),
		Triple("froststalker", "froststalker_helmet", "humanoid"),
		Triple("rocky_roller", "rocky_chestplate", "humanoid"),
		Triple("flying_fish", "flying_fish_boots", "humanoid"),
		Triple("novelty_hat", "novelty_hat", "humanoid"),
		Triple("kimono", "unsettling_kimono", "humanoid"),
	)

	/**
	 * MC 1.21.2 deleted Forge's `getArmorTexture` hook along with `ArmorMaterial.Layer`. An armour
	 * texture is now named indirectly: the ArmorMaterial carries an equipment-model id, resolved to
	 * `assets/<ns>/models/equipment/<id>.json`, whose layers name textures under
	 * `assets/<ns>/textures/entity/equipment/<layer type>/<texture>.png`.
	 *
	 * Alex's Mobs keeps its armour textures at `textures/armor/<item>.png` — one file per item,
	 * used for both armour layers. Rather than duplicate them into the source tree for the sake of
	 * the upper nodes, the model JSON and the relocated texture are both derived here, so
	 * `textures/armor/` stays the single source of truth.
	 */
	fun migrateEquipmentTo12102(resourcesRoot: File, modId: String): Int {
		val assets = resourcesRoot.resolve("assets/$modId")
		if (!assets.isDirectory) return 0
		val models = assets.resolve("models/equipment")
		var written = 0
		armorEquipment.forEach { (material, texture, layer) ->
			val source = assets.resolve("textures/armor/$texture.png")
			if (!source.isFile) return@forEach
			val relocated = assets.resolve("textures/entity/equipment/$layer/$texture.png")
			relocated.parentFile.mkdirs()
			source.copyTo(relocated, overwrite = true)
			models.mkdirs()
			models.resolve("$material.json").writeText(
				"""{"layers":{"$layer":[{"texture":"$modId:$texture"}]}}"""
			)
			written++
		}
		return written
	}

	/**
	 * 1.21.4 moved equipment definitions out of the model tree: `assets/<ns>/models/equipment/<id>.json`
	 * became `assets/<ns>/equipment/<id>.json`. Verified by listing the shipped client jars — 1.21.2
	 * has only `assets/minecraft/models/equipment/`, 1.21.4 has only `assets/minecraft/equipment/`.
	 *
	 * [migrateEquipmentTo12102] writes the 1.21.2 layout on every node from 1.21.2 up, so this runs
	 * after it and moves the whole folder on the nodes that want the newer one. Missing the move is
	 * silent in the worst way: an armour item whose `asset_id` resolves to nothing does not warn and
	 * does not draw a missing texture — **the layer is skipped and the armour is simply invisible**.
	 * That is report #38, seen as "the crocodile chestplate is equipped but not visible".
	 *
	 * On Fabric the twelve items with a hand-built model are drawn by `FabricArmorRenderers`, which
	 * names its own texture, so only the three model-less ones (crocodile chestplate, centipede and
	 * emu leggings) went missing. Forge and NeoForge have no such renderer and take the vanilla
	 * armour layer for all fifteen.
	 */
	fun relocateEquipmentTo1214(resourcesRoot: File, modId: String): Int {
		val assets = resourcesRoot.resolve("assets/$modId")
		if (!assets.isDirectory) return 0
		return relocate(assets.resolve("models/equipment"), assets.resolve("equipment"))
	}

	/**
	 * 1.21.5 changed how an advancement tab's background is addressed. It used to be the texture file
	 * itself — `"minecraft:textures/gui/advancements/backgrounds/stone.png"` — and is now a bare id,
	 * `"minecraft:gui/advancements/backgrounds/stone"`, which the client expands back into
	 * `textures/<path>.png` when it loads it. Read out of the shipped `story/root.json` in each jar
	 * rather than recalled: 1.21.4 still has the old form, 1.21.5 has the new one.
	 *
	 * Alex's Mobs' own tab names `alexsmobs:textures/advancement_background.png`, which the expansion
	 * turns into `alexsmobs:textures/textures/advancement_background.png.png` — the exact string the
	 * client logged as missing. A missing background is not fatal, so from 1.21.5 up the advancement
	 * screen just drew the magenta-and-black missing texture behind every Alex's Mobs advancement
	 * (report #39).
	 *
	 * The rewrite is the inverse of the client's expansion — drop a leading `textures/` and a trailing
	 * `.png` — so a value already in the new form is left alone.
	 */
	fun migrateAdvancementBackgroundsTo1215(resourcesRoot: File): Int {
		val data = resourcesRoot.resolve("data")
		if (!data.isDirectory) return 0
		var changed = 0
		data.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
			val root = runCatching { json.parseToJsonElement(file.readText()) }.getOrNull() as? JsonObject
				?: return@forEach
			val display = root["display"] as? JsonObject ?: return@forEach
			val background = (display["background"] as? JsonPrimitive)?.takeIf { it.isString }?.content
				?: return@forEach
			val namespace = background.substringBefore(':', missingDelimiterValue = "")
			val path = background.substringAfter(':')
			if (!path.startsWith("textures/") || !path.endsWith(".png")) return@forEach
			val rewritten = path.removePrefix("textures/").removeSuffix(".png")
			file.writeText(
				json.encodeToString(
					JsonElement.serializer(),
					root.replacing(
						"display",
						display.replacing(
							"background",
							JsonPrimitive(if (namespace.isEmpty()) rewritten else "$namespace:$rewritten"),
						),
					),
				)
			)
			changed++
		}
		return changed
	}

	/**
	 * `spawnEgg("<mob>", ACEntityRegistry.<MOB>, 0X<background>, 0X<highlight>, <biome tab>)`
	 *
	 * The two colours are only ever spelled at the registration site, and from 1.21.4 the client needs
	 * them in JSON rather than on the item — `SpawnEggItem`'s constructor stopped taking them, which is
	 * why `ACItemRegistry.spawnEgg` still carries the parameters unused there. The registered id is
	 * `spawn_egg_<mob>`, built by that helper; the call itself names the mob alone, so the prefix is
	 * re-applied here rather than captured.
	 */
	private val spawnEggRegistration = Regex(
		"""spawnEgg\("([a-z0-9_]+)",\s*[^,]+,\s*0[Xx]([0-9A-Fa-f]+)\s*,\s*0[Xx]([0-9A-Fa-f]+)"""
	)

	/** The parent MC 1.21.4 deleted along with the whole BER-item mechanism. */
	private const val BUILTIN_ENTITY = "builtin/entity"

	/**
	 * MC 1.21.4 stopped resolving an item's model by convention.
	 *
	 * Up to 1.21.3 the client looked for `assets/<ns>/models/item/<id>.json` and that was the whole
	 * contract. 1.21.4 introduced **item model definitions**: `assets/<ns>/items/<id>.json`, a
	 * `ClientItem` naming an [ItemModel] — the old model file is still needed, but it is now only
	 * reachable *through* a definition. An item with no definition is not an error, it just renders
	 * as the missing-model cube, and the client logs one `Missing item model for location <id>` per
	 * item at every resource reload.
	 *
	 * Alex's Caves was authored against 1.20.1 and has ~750 items with nothing but the legacy model, so
	 * on every node from 1.21.4 up **every item in the mod rendered as the missing model**. Rather
	 * than commit that many near-identical files that only the ≥1.21.4 nodes read, one definition is
	 * derived here per legacy item model file (⚠️ do not write that path with a glob — a `slash-star`
	 * inside a KDoc opens a NESTED Kotlin block comment and swallows the rest of the file):
	 *
	 *     {"model":{"type":"minecraft:model","model":"<ns>:item/<id>"}}
	 *
	 * A number of those models are override targets (`limestone_spear_throwing`, `dinosaur_nugget_1`,
	 * `holocoder_bound`, …) that no item is registered under. They keep getting a definition anyway —
	 * writing one is cheaper than deciding which ids are items, and an unreferenced definition is
	 * never looked up and never validated.
	 *
	 * Four shapes need more than that base, and all four are things the mod used to do from Java:
	 *
	 * - **Spawn eggs.** `SpawnEggItem` no longer carries colours (its 1.21.4+ constructor takes only
	 *   `Item.Properties`); the two tints that used to be Java arguments are `minecraft:constant` tint
	 *   sources on the definition, applied to `item/template_spawn_egg`'s two layers. They are read
	 *   back out of [spawnEggSource], which is where the mod spells them, so the registration stays the
	 *   single source of truth.
	 * - **The five dynamic tints** `RegisterColorHandlersEvent.Item` used to install ([DYNAMIC_TINTS]).
	 *   The `colorIn` index each lambda guarded on is the entry's *position* in the `tints` list, so a
	 *   tint on layer 1 is preceded by a `minecraft:constant` `-1` — the "leave this layer alone" the
	 *   old `colorIn != 1 ? -1 : …` returned.
	 * - **The 23 models parented to `builtin/entity`**, which 1.21.4 deleted along with the ISTER that
	 *   drew them. Both renderers are alive again as `minecraft:special` model renderers
	 *   (`ACItemSpecialRenderer`), so those become a special definition whose `base` is the model's own
	 *   file — kept, not emptied, because the `base` is exactly where a special model reads its display
	 *   transforms from and every one of the 23 authored a `display` block. See [stripDeadParent].
	 *   The two [LIVE_ICON_ITEMS] dispatch to the vendored Citadel renderer instead.
	 * - **The 11 models with an `overrides` list**, whose mechanism 1.21.4 also deleted. Each becomes a
	 *   `minecraft:range_dispatch` over the mod's own `alexscaves:legacy` property, carrying the same
	 *   thresholds and naming the same value `ItemProperties.register` used to — see
	 *   [dispatchOverrides]. Both eras read the value out of `ACItemPredicates`.
	 */
	fun writeItemModelDefinitions(resourcesRoot: File, modId: String, spawnEggSource: File?): Int {
		val assets = resourcesRoot.resolve("assets/$modId")
		val models = assets.resolve("models/item")
		if (!models.isDirectory) return 0
		val eggTints: Map<String, Pair<String, String>> = spawnEggSource
			?.takeIf { it.isFile }
			?.let { source ->
				spawnEggRegistration.findAll(source.readText()).associate { match ->
					val (mob, background, highlight) = match.destructured
					"spawn_egg_$mob" to (background.toInt(16).toString() to highlight.toInt(16).toString())
				}
			}
			.orEmpty()
		val files = models.listFiles().orEmpty().filter { it.isFile && it.extension == "json" }.sortedBy { it.name }
		// Computed over the whole directory first: an override target is reached by id, and four of
		// them (the three `*_spear_throwing`, `ortholance_charging`) are themselves ISTER models.
		val dead = files.filter { it.readText().contains(BUILTIN_ENTITY) }.map { it.nameWithoutExtension }.toSet()
		val definitions = assets.resolve("items")
		definitions.mkdirs()
		var written = 0
		files.forEach { model ->
			val id = model.nameWithoutExtension
			val body = dispatchOverrides(model, modId, dead, eggTints)
				?: itemModelBody(modId, id, dead, eggTints)
			definitions.resolve("$id.json").writeText("""{"model":$body}""")
			written++
			if (id in dead) stripDeadParent(model)
		}
		return written
	}

	/** One item's model definition body, before any `overrides` list is folded in around it. */
	private fun itemModelBody(
		modId: String,
		id: String,
		dead: Set<String>,
		eggTints: Map<String, Pair<String, String>>,
	): String = when {
		id in LIVE_ICON_ITEMS ->
			"""{"type":"minecraft:special","base":"$modId:item/$id","model":{"type":"$modId:icon"}}"""
		id in dead ->
			"""{"type":"minecraft:special","base":"$modId:item/$id","model":{"type":"$modId:item_renderer"}}"""
		else -> """{"type":"minecraft:model","model":"$modId:item/$id"${tintList(modId, id, eggTints)}}"""
	}

	/**
	 * The `,"tints":[…]` suffix for one item, or `""` if it has none.
	 *
	 * A tint entry applies to the layer at its own index, which is what the old handler's `colorIn`
	 * meant, so a source on layer 1 is padded with the `minecraft:constant` `-1` (opaque white, i.e.
	 * untinted) that the old lambda returned for every other index.
	 */
	private fun tintList(modId: String, id: String, eggTints: Map<String, Pair<String, String>>): String {
		val entries = DYNAMIC_TINTS[id]?.let { (layer, source) ->
			"""{"type":"minecraft:constant","value":-1},""".repeat(layer) +
				"""{"type":"$modId:tint","source":"$source"}"""
		} ?: eggTints[id]?.let { (background, highlight) ->
			"""{"type":"minecraft:constant","value":$background},""" +
				"""{"type":"minecraft:constant","value":$highlight}"""
		} ?: return ""
		return ""","tints":[$entries]"""
	}

	/**
	 * Folds a model's `overrides` list into a `minecraft:range_dispatch`, or `null` if it has none.
	 *
	 * The translation is one-to-one — same thresholds, same target models, same property name — because
	 * `alexscaves:legacy` exists precisely to answer the nine names `ItemProperties.register` used to
	 * (see `ACItemModelShims.Legacy`, which resolves each of them through the shared
	 * `ACItemPredicates`). The overridden model itself becomes the `fallback`, which is what an entry
	 * below the lowest threshold selected before.
	 *
	 * Every list in this mod is keyed on a single property, so a nested dispatch is never needed; a
	 * mixed list is a hard error rather than a silently dropped predicate, and so is a name the shim
	 * cannot answer — the alternative symptom is an item that renders wrong only in one state.
	 *
	 * ⚠️ Upstream's `extinction_spear` override names `limestone_spear_throwing`, not its own throwing
	 * model. That is rewritten verbatim: all three `*_spear_throwing` models carry byte-identical
	 * `display` blocks (the ISTER draws the geometry), so the mismatch has never been visible, and
	 * "correcting" it here would make this pass a behaviour change rather than a port.
	 */
	private fun dispatchOverrides(
		model: File,
		modId: String,
		dead: Set<String>,
		eggTints: Map<String, Pair<String, String>>,
	): String? {
		val id = model.nameWithoutExtension
		val text = model.readText()
		if (!text.contains(""""overrides"""")) return null
		val root = runCatching { json.parseToJsonElement(text) as? JsonObject }.getOrNull() ?: return null
		val overrides = (root["overrides"] as? JsonArray)?.takeIf { it.isNotEmpty() } ?: return null
		val entries = overrides.map { entry ->
			val over = entry as? JsonObject ?: error("Malformed override in models/item/$id.json")
			val predicate = (over["predicate"] as? JsonObject)?.entries?.singleOrNull()
				?: error("models/item/$id.json has an override with no single predicate")
			val target = (over["model"] as? JsonPrimitive)?.content?.substringAfterLast('/')
				?: error("models/item/$id.json has an override with no model")
			Triple(predicate.key, (predicate.value as JsonPrimitive).content, target)
		}
		val property = entries.map { it.first }.distinct().singleOrNull()
			?: error("models/item/$id.json mixes override properties: ${entries.map { it.first }}")
		require(property in LEGACY_PROPERTIES) {
			"models/item/$id.json overrides on '$property', which ACItemModelShims.Legacy cannot answer"
		}
		val cases = entries
			.sortedBy { it.second.toFloat() }
			.joinToString(",") { (_, threshold, target) ->
				"""{"threshold":$threshold,"model":${itemModelBody(modId, target, dead, eggTints)}}"""
			}
		return """{"type":"minecraft:range_dispatch","property":"$modId:legacy","name":"$property",""" +
			""""entries":[$cases],"fallback":${itemModelBody(modId, id, dead, eggTints)}}"""
	}

	/** The template MC 1.21.5 deleted when it gave every vanilla spawn egg its own painted sprite. */
	private const val SPAWN_EGG_TEMPLATE = "item/template_spawn_egg"

	/**
	 * MC 1.21.5 deleted `item/template_spawn_egg` and both of the greyscale textures it layered.
	 *
	 * Vanilla stopped tinting: each of its ~80 eggs now has a hand-painted sprite and a plain
	 * `item/generated` model. A mod's eggs, whose colours are two ints at the registration site,
	 * have nothing to move to — and every model in this mod that named the deleted parent resolves
	 * to nothing, so all 43 spawn eggs would render as the missing-model cube on every node from
	 * 1.21.5 up.
	 *
	 * Tinting itself still works — `minecraft:constant` is unchanged and [writeItemModelDefinitions]
	 * already writes the pair of tints onto the definition. Only the two layers went away, so they
	 * ship in this mod's own namespace (`textures/item/spawn_egg{,_overlay}.png`, copied from 1.21.4)
	 * and the model is re-pointed at them. Below 1.21.5 the vanilla parent is left alone.
	 */
	fun retemplateSpawnEggs(resourcesRoot: File, modId: String): Int {
		val models = resourcesRoot.resolve("assets/$modId/models/item")
		if (!models.isDirectory) return 0
		var rewritten = 0
		models.listFiles().orEmpty().filter { it.isFile && it.extension == "json" }.forEach { model ->
			if (!model.readText().contains(SPAWN_EGG_TEMPLATE)) return@forEach
			model.writeText(
				"""{"parent":"minecraft:item/generated","textures":""" +
					"""{"layer0":"$modId:item/spawn_egg","layer1":"$modId:item/spawn_egg_overlay"}}"""
			)
			rewritten++
		}
		return rewritten
	}

	/**
	 * Strips the dead `builtin/entity` parent from one item model, leaving everything else alone.
	 *
	 * 1.21.4 deleted that parent along with the ISTER mechanism, and the model manager loads every file
	 * under `models/` whether or not a definition points at it, so an unresolvable parent is a
	 * resolution failure logged per file. What the file is *for* has not changed: it is the `base` of
	 * the `minecraft:special` definition written for it, and a special model reads its display
	 * transforms from that base — which is why the parent alone comes out and the rest of the file
	 * stays.
	 *
	 * All 23 of this mod's ISTER models author a `display` block (checked, not assumed), so emptying
	 * them the way the sibling repo does would silently reset every hand-tuned in-hand and GUI pose to
	 * the identity. A leftover `textures` or `gui_light` key is harmless: with no `elements` and no
	 * parent the model bakes to no quads, which is exactly what it did under `builtin/entity`.
	 *
	 * ⚠️ This is the one pass in here that is **not** idempotent — a stripped model no longer looks
	 * dead, so a second run over the same staged directory would demote all 23 to plain
	 * `minecraft:model` definitions. It relies on `processResources` re-copying every model from source
	 * whenever it re-executes, which a Gradle `Copy` does; nothing may be re-ordered so that these
	 * `doLast` actions can run against a directory this one already touched.
	 */
	private fun stripDeadParent(model: File) {
		val root = runCatching { json.parseToJsonElement(model.readText()) as? JsonObject }.getOrNull() ?: return
		if (!root.containsKey("parent")) return
		model.writeText(json.encodeToString(JsonObject.serializer(), root.without("parent")))
	}

	/**
	 * The two vendored Citadel display items, drawn by `CitadelItemstackRenderer` rather than the mod's
	 * own `ACItemstackRenderer`.
	 *
	 * They are **registered items**, not the inert override targets they sit next to in the legacy model
	 * directory: `icon_item` draws whatever item or entity its `custom_data` names and `effect_item`
	 * draws a mob-effect icon, which is how all 30 of this mod's advancement icons are rendered. Both
	 * type ids are registered by `ACItemModelShims`; the split exists only because a
	 * `minecraft:special` definition names exactly one renderer and these are a different object from
	 * the 21 hand-held 3D items.
	 */
	private val LIVE_ICON_ITEMS = setOf("icon_item", "effect_item")

	/**
	 * The five item tints `RegisterColorHandlersEvent.Item` used to install, as `layer to source`.
	 *
	 * The layer is the `colorIn` index the old lambda guarded on — `tints[layer]` in the definition —
	 * and the source name is the `ACItemModelShims.Tint` branch that computes the same colour. Kept in
	 * lockstep with `ClientProxy#onItemColors`, which is the &lt;1.21.4 half of the same five.
	 */
	private val DYNAMIC_TINTS = mapOf(
		"cave_tablet" to (1 to "biome"),
		"cave_codex" to (1 to "biome"),
		"gazing_pearl" to (0 to "pearl"),
		"jelly_bean" to (0 to "jelly_bean"),
		"biome_treat" to (1 to "biome_treat"),
	)

	/**
	 * The nine values `ACItemModelShims.Legacy` can answer, i.e. every `ItemProperties.register` name
	 * `ClientProxy#clientInit` installs below 1.21.4. An `overrides` list keyed on anything else is a
	 * build failure — see [dispatchOverrides].
	 */
	private val LEGACY_PROPERTIES =
		setOf("bound", "nugget", "throwing", "active", "tooting", "charging", "totem", "cast", "open")

	/**
	 * Removes the render-state mixins from the mod's mixin config on nodes that cannot compile them.
	 *
	 * They mix into `EntityRenderState`, which only exists from 1.21.2, so below that their source
	 * package is excluded from the compile (see `ModPlatformPlugin.configureJava`) and no `.class`
	 * reaches the jar. But **Fletching Table populates the config's `mixins` array itself**, by
	 * scanning for `@Mixin`-annotated sources — and that scan does not honour the source-set
	 * `exclude`, so it lists them on every node, including the ones that dropped them. A mixin
	 * config naming an absent class is a hard load failure, not a warning:
	 *
	 *     InvalidMixinException: The specified mixin '…renderstate.EntityRenderStateMixin'
	 *     was not found
	 *
	 * — the game never reaches mod loading. (This is also why the whole config's `client` list turns
	 * up duplicated into `mixins` in the built jar: same scan, and harmless because the classes are
	 * present, just dist-cleaned on a server.)
	 *
	 * So the entries are pruned here rather than added: the `mixins`/`client` arrays are rewritten
	 * without them, dropping the now-dangling comma if the array empties out.
	 */
	fun pruneRenderStateMixins(resourcesRoot: File, modId: String): Int =
		pruneMixinEntries(resourcesRoot, modId, listOf("renderstate.EntityRenderStateMixin", "renderstate.EntityRendererMixin"))

	/**
	 * The general form of the above: drop [entries] from the mixin config's arrays, for any node
	 * whose source set excludes the classes that back them.
	 */
	fun pruneMixinEntries(resourcesRoot: File, modId: String, entries: List<String>): Int {
		val config = resourcesRoot.resolve("$modId.mixins.json")
		if (!config.isFile) return 0
		val original = config.readText()
		var text = original
		var removed = 0
		entries.forEach { entry ->
			// Match the quoted entry plus whatever separator sits on either side of it, so the
			// remaining array stays valid JSON whether the entry was first, last or in the middle.
			val pattern = Regex(""",\s*"${Regex.escape(entry)}"|"${Regex.escape(entry)}"\s*,|"${Regex.escape(entry)}"""")
			if (pattern.containsMatchIn(text)) {
				text = pattern.replace(text, "")
				removed++
			}
		}
		if (text == original) return 0
		config.writeText(text)
		return removed
	}

	/**
	 * Drops every entry in a whole mixin **package** from the config's arrays.
	 *
	 * [pruneMixinEntries] names classes one by one, which is right when the reason a class is absent
	 * is specific to that class. `mixin.fabric.**` is the other shape: the entire package exists only
	 * because Fabric has no event bus, and it is excluded wholesale from the compile on Forge and
	 * NeoForge — so listing the classes here would just be a second place to forget to update when
	 * one is added. [prefix] is matched against the entry as written in the config, i.e. relative to
	 * the config's `package` (`"fabric."`).
	 *
	 * Rewritten via the parser rather than spliced, so it is idempotent and cannot leave a dangling
	 * comma; returns the number of entries removed across both arrays.
	 */
	fun pruneMixinPackage(resourcesRoot: File, modId: String, prefix: String): Int {
		val config = resourcesRoot.resolve("$modId.mixins.json")
		if (!config.isFile) return 0
		val root = runCatching { json.parseToJsonElement(config.readText()) }.getOrNull() as? JsonObject ?: return 0

		var removed = 0
		val rebuilt = buildJsonObject {
			root.forEach { (key, value) ->
				val array = value as? JsonArray
				if ((key == "mixins" || key == "client") && array != null) {
					val keep = array.filterNot { (it as? JsonPrimitive)?.content?.startsWith(prefix) == true }
					removed += array.size - keep.size
					put(key, JsonArray(keep))
				} else {
					put(key, value)
				}
			}
		}
		if (removed == 0) return 0
		config.writeText(json.encodeToString(JsonObject.serializer(), rebuilt))
		return removed
	}

	/**
	 * Mixin packages in this mod whose targets are CLIENT classes, and which Fletching Table
	 * nevertheless lists in the common `mixins` array — see [partitionClientMixins].
	 *
	 * The `mixin.client` package is obvious from the name. `mixin.renderstate` is the non-obvious
	 * one: it mixes into `EntityRenderer` / `EntityRenderState`, which are just as client-only, and
	 * it is the reason a `>=1.21.2` node's dist-cleaner noise is nine lines rather than five.
	 *
	 * `mixin.fabric.client` needs its own entry rather than riding on `client.`: these are matched
	 * with `startsWith` against the package path relative to `mixin`, so a nested package does not
	 * match its own leaf name. It is the Fabric-only half of the dispatcher (`mixin.fabric` proper
	 * is server/common and must stay in `mixins`), and it exists only on Fabric — the other two
	 * loaders drop the whole `fabric.` prefix in [pruneMixinPackage] before this pass would see it.
	 *
	 * (Written without a trailing glob on purpose — a `/` followed by two asterisks inside a KDoc
	 * opens a NESTED Kotlin block comment and swallows the rest of the file.)
	 */
	private val clientMixinPackages = listOf("client.", "renderstate.", "fabric.client.")

	/**
	 * Moves every client-only entry out of the mixin config's `mixins` array and into `client`.
	 *
	 * **Fabric only, and it is not cosmetic — it is the difference between a dedicated server that
	 * boots and one that does not.** Fletching Table populates `mixins` by scanning for `@Mixin`
	 * sources, and that scan knows nothing about dists, so client mixins land in the common list
	 * (the `client` list is authored separately, which is why four of them appear twice — see
	 * [pruneRenderStateMixins]). On Forge and NeoForge that is harmless: `RuntimeDistCleaner` /
	 * `NeoForgeDevDistCleaner` refuse to hand a client class to the transformer on a server, which
	 * is exactly where this repo's documented benign `/ERROR]` lines come from.
	 *
	 * **Fabric has no dist cleaner.** An entry under `mixins` is applied on both dists, so a
	 * dedicated server would try to apply e.g. `client.GuiMixin` to `net.minecraft.client.gui.Gui`
	 * — a class that is not on a server's classpath at all — and mixin aborts the launch. The
	 * `client` array is precisely the mechanism for saying "client dist only", so this makes it
	 * the only place those entries appear.
	 *
	 * Returns the number of entries moved. The config is re-emitted rather than spliced, so the
	 * pass is idempotent and cannot leave a dangling comma.
	 */
	fun partitionClientMixins(resourcesRoot: File, modId: String): Int {
		val config = resourcesRoot.resolve("$modId.mixins.json")
		if (!config.isFile) return 0
		val root = runCatching { json.parseToJsonElement(config.readText()) }.getOrNull() as? JsonObject ?: return 0
		fun arrayAt(key: String) = (root[key] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content }.orEmpty()

		val common = arrayAt("mixins")
		val client = arrayAt("client")
		val (moved, keep) = common.partition { entry ->
			entry in client || clientMixinPackages.any { entry.startsWith(it) }
		}
		if (moved.isEmpty()) return 0

		val rebuilt = buildJsonObject {
			root.forEach { (key, value) ->
				when (key) {
					"mixins" -> put(key, JsonArray(keep.map { JsonPrimitive(it) }))
					"client" -> put(key, JsonArray((client + moved.filterNot { it in client }).map { JsonPrimitive(it) }))
					else -> put(key, value)
				}
			}
		}
		config.writeText(json.encodeToString(JsonObject.serializer(), rebuilt))
		return moved.size
	}

	/**
	 * MC 1.21 turned looting into an enchantment *effect*, and deleted the two loot symbols that
	 * named the looting level directly: the `looting_enchant` function and the
	 * `random_chance_with_looting` condition. Both have exact replacements that name the enchantment
	 * explicitly, so this is a shape rewrite and not a behaviour change.
	 *
	 * It is not optional: an unknown loot function or condition id fails the *whole* table to parse
	 * (logged, not thrown), so leaving them in silently deletes every affected mob's entire drop
	 * table — 55 entries across 41 tables here.
	 */
	fun migrateLootTo121(resourcesRoot: File): Int {
		val data = resourcesRoot.resolve("data")
		if (!data.isDirectory) return 0
		var changed = 0
		data.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
			val dirs = file.invariantSeparatorsPath
			if (!dirs.contains("/loot_table/") && !dirs.contains("/item_modifier/")) return@forEach
			val original = runCatching { json.parseToJsonElement(file.readText()) }.getOrNull()
				?: return@forEach
			val migrated = migrateLootingNode(original)
			if (migrated != original) {
				file.writeText(json.encodeToString(JsonElement.serializer(), migrated))
				changed++
			}
		}
		return changed
	}

	private const val LOOTING = "minecraft:looting"

	private fun migrateLootingNode(node: JsonElement): JsonElement = when (node) {
		is JsonArray -> JsonArray(node.map(::migrateLootingNode))
		is JsonObject -> {
			val mapped = JsonObject(LinkedHashMap<String, JsonElement>().also { out ->
				node.forEach { (key, value) -> out[key] = migrateLootingNode(value) }
			})
			when {
				mapped.idOf("function") == "minecraft:looting_enchant" -> lootingEnchant(mapped)
				mapped.idOf("condition") == "minecraft:random_chance_with_looting" ->
					randomChanceWithLooting(mapped)
				else -> mapped
			}
		}
		else -> node
	}

	/** The `minecraft:` prefix is optional in data-pack ids, and these tables leave it off. */
	private fun JsonObject.idOf(key: String): String? =
		(this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
			?.let { if (it.contains(':')) it else "minecraft:$it" }

	/** `looting_enchant` -> `enchanted_count_increase`; `count` and `limit` carry over unchanged. */
	private fun lootingEnchant(node: JsonObject): JsonObject = buildJsonObject {
		put("function", JsonPrimitive("minecraft:enchanted_count_increase"))
		// The enchantment used to be implicit in the function's name.
		put("enchantment", JsonPrimitive(LOOTING))
		node.forEach { (key, value) -> if (key != "function") put(key, value) }
	}

	private fun randomChanceWithLooting(node: JsonObject): JsonObject {
		val chance = node["chance"] as? JsonPrimitive ?: return node
		val multiplier = (node["looting_multiplier"] as? JsonPrimitive)?.doubleOrNull ?: return node
		val base = chance.doubleOrNull?.let { round6(it + multiplier) } ?: return node
		return buildJsonObject {
			put("condition", JsonPrimitive("minecraft:random_chance_with_enchanted_bonus"))
			put("unenchanted_chance", chance)
			// The old condition was a flat `chance + level * multiplier`. A linear level-based value
			// whose base is the level-1 result reproduces that exactly for every level >= 1, and
			// level 0 is what unenchanted_chance covers — so the drop rates are unchanged.
			put("enchanted_chance", buildJsonObject {
				put("type", JsonPrimitive("minecraft:linear"))
				put("base", JsonPrimitive(base))
				put("per_level_above_first", JsonPrimitive(multiplier))
			})
			put("enchantment", JsonPrimitive(LOOTING))
			node.forEach { (key, value) ->
				if (key != "condition" && key != "chance" && key != "looting_multiplier") put(key, value)
			}
		}
	}

	/** Keeps 0.2 + 0.1 from being written out as 0.30000000000000004. */
	private fun round6(value: Double): Double = Math.round(value * 1_000_000.0) / 1_000_000.0

	private val singularFolders = mapOf(
		"advancements" to "advancement",
		"recipes" to "recipe",
		"loot_tables" to "loot_table",
		"structures" to "structure",
		"predicates" to "predicate",
		"item_modifiers" to "item_modifier",
		"functions" to "function",
	)

	private val singularTagFolders = mapOf(
		"blocks" to "block",
		"items" to "item",
		"entity_types" to "entity_type",
		"fluids" to "fluid",
		"game_events" to "game_event",
		"functions" to "function",
	)

	/**
	 * MC 1.21.2 dropped the carving-step split from a biome's `carvers`.
	 *
	 * Up to 1.21.1 the field was a map keyed by `GenerationStep.Carving` (`"air"` and, historically,
	 * `"liquid"`), each value a `HolderSet<ConfiguredWorldCarver<?>>`. 1.21.2's
	 * `BiomeGenerationSettings.CODEC` reads `carvers` as one flat `ConfiguredWorldCarver.LIST_CODEC`,
	 * so the map is now the HolderSet itself — and an object that is neither a `#tag` string, nor an
	 * array, nor a `{"type": …}` gets the three-way "Failed to parse either" the log shows.
	 *
	 * The field is `promotePartial`, so a failure is *not* silent here: every carver drops out and the
	 * enclosing biome then fails `RegistryDataLoader`, which is fatal. All six of this mod's biomes
	 * carry the field — three with the vanilla cave/canyon trio under `"air"`, three with an empty
	 * object — so the flattening is a straight concatenation of the map's values in order.
	 */
	fun flattenBiomeCarversTo1212(resourcesRoot: File): Int {
		val data = resourcesRoot.resolve("data")
		if (!data.isDirectory) return 0
		var changed = 0
		data.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
			if (!file.invariantSeparatorsPath.contains("/worldgen/biome/")) return@forEach
			val original = runCatching { json.parseToJsonElement(file.readText()) as? JsonObject }
				.getOrNull() ?: return@forEach
			// Only the map form is rewritten; a file already flattened has an array (or a tag string)
			// here and is left alone, which is what keeps a re-run a no-op.
			val carvers = original["carvers"] as? JsonObject ?: return@forEach
			val flattened = JsonArray(carvers.values.flatMap { step ->
				when (step) {
					is JsonArray -> step.toList()
					is JsonPrimitive -> listOf(step)
					else -> emptyList()
				}
			})
			val migrated = JsonObject(original.toMutableMap().apply { put("carvers", flattened) })
			file.writeText(json.encodeToString(JsonObject.serializer(), migrated))
			changed++
		}
		return changed
	}

	/**
	 * MC 1.21.4 turned a biome's background music into a weighted list.
	 *
	 * `BiomeSpecialEffects.CODEC` reads `music` as
	 * `SimpleWeightedRandomList.wrappedCodecAllowingEmpty(Music.CODEC)` — a JSON *array* of
	 * `{"data": <Music>, "weight": <int>}` — where every version up to 1.21.3 read a bare `Music`
	 * object. `Music` itself is unchanged, so the migration is purely the wrapper.
	 *
	 * Not optional: the field is a plain `optionalFieldOf` on a codec that fails hard, so all six of
	 * this mod's biomes died with *"Not a json array: {"max_delay":…}"* and took the whole
	 * `RegistryDataLoader` pass with them — the same fatal shape as
	 * [flattenBiomeCarversTo1212].
	 *
	 * Idempotent: an already-wrapped file has an array here and is skipped. Weight 1 is the only
	 * sensible choice for a one-entry list — it is a relative weight, so any positive value plays
	 * that track always.
	 */
	fun wrapBiomeMusicTo1214(resourcesRoot: File): Int {
		val data = resourcesRoot.resolve("data")
		if (!data.isDirectory) return 0
		var changed = 0
		data.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
			if (!file.invariantSeparatorsPath.contains("/worldgen/biome/")) return@forEach
			val original = runCatching { json.parseToJsonElement(file.readText()) as? JsonObject }
				.getOrNull() ?: return@forEach
			val effects = original["effects"] as? JsonObject ?: return@forEach
			val music = effects["music"] as? JsonObject ?: return@forEach
			val wrapped = JsonArray(
				listOf(
					JsonObject(
						mapOf(
							"data" to music,
							"weight" to JsonPrimitive(1),
						)
					)
				)
			)
			val migratedEffects = JsonObject(effects.toMutableMap().apply { put("music", wrapped) })
			val migrated = JsonObject(original.toMutableMap().apply { put("effects", migratedEffects) })
			file.writeText(json.encodeToString(JsonObject.serializer(), migrated))
			changed++
		}
		return changed
	}

	/**
	 * MC 1.21.5 swapped `patch_pumpkin` and `patch_sugar_cane` in vanilla's own vegetal-decoration
	 * step, and all six of this mod's biomes list them the other way round.
	 *
	 * A biome does not declare an absolute feature order — vanilla derives one global order per step
	 * by topologically sorting the adjacent pairs every biome contributes. Two biomes that disagree
	 * about a pair make that graph cyclic, and `FeatureSorter` throws: *"Feature order cycle found,
	 * involved sources: [… minecraft:windswept_savanna …, … alexscaves:candy_cavity …]"*, wrapped in
	 * a `ReportedException: Exception generating new chunk`. It is fatal on the first chunk, not
	 * logged, so the server never finishes loading a world.
	 *
	 * The whole disagreement is that one pair, so this reorders it rather than rewriting the list:
	 * anywhere a step names both and has sugar cane first, the two swap. Checked by rebuilding the
	 * same adjacency graph over vanilla's 1.21.5 biome JSONs plus this mod's — that pair is the only
	 * cycle in it. Idempotent, since a swapped list no longer matches.
	 */
	fun orderBiomeFeaturesTo1215(resourcesRoot: File): Int {
		val data = resourcesRoot.resolve("data")
		if (!data.isDirectory) return 0
		val pumpkin = "minecraft:patch_pumpkin"
		val sugarCane = "minecraft:patch_sugar_cane"
		var changed = 0
		data.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
			if (!file.invariantSeparatorsPath.contains("/worldgen/biome/")) return@forEach
			val original = runCatching { json.parseToJsonElement(file.readText()) as? JsonObject }
				.getOrNull() ?: return@forEach
			val steps = original["features"] as? JsonArray ?: return@forEach
			var touched = false
			val migratedSteps = steps.map { step ->
				val entries = (step as? JsonArray)?.map { it } ?: return@map step
				val names = entries.map { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content }
				val atPumpkin = names.indexOf(pumpkin)
				val atSugarCane = names.indexOf(sugarCane)
				if (atPumpkin < 0 || atSugarCane < 0 || atSugarCane > atPumpkin) return@map step
				touched = true
				JsonArray(entries.toMutableList().apply {
					val held = this[atPumpkin]
					this[atPumpkin] = this[atSugarCane]
					this[atSugarCane] = held
				})
			}
			if (!touched) return@forEach
			val migrated = JsonObject(original.toMutableMap().apply { put("features", JsonArray(migratedSteps)) })
			file.writeText(json.encodeToString(JsonObject.serializer(), migrated))
			changed++
		}
		return changed
	}

	/**
	 * MC 1.21.11 emptied a biome's `effects` into a new top-level `attributes` map.
	 *
	 * `BiomeSpecialEffects` is down to the five colour fields the *world* asks for — `water_color`
	 * and the three colour overrides plus `grass_color_modifier`. Everything the **client** read off
	 * a biome became an `EnvironmentAttribute`: a registry of typed, positional, spatially
	 * interpolated values that `Camera#attributeProbe` blends across a biome boundary, which is the
	 * same rework that deleted `ClientLevel#getSkyColor` / `#getSkyDarken` (see
	 * `mixin.client.EnvironmentAttributeProbeMixin`). So the six keys this mod sets move house:
	 *
	 * | `effects` key | attribute | shape change |
	 * |---|---|---|
	 * | `fog_color` | `minecraft:visual/fog_color` | none, packed int |
	 * | `sky_color` | `minecraft:visual/sky_color` | none |
	 * | `water_fog_color` | `minecraft:visual/water_fog_color` | none |
	 * | `music` | `minecraft:audio/background_music` | wrapped as `{"default": <Music>}` |
	 * | `ambient_sound` + `mood_sound` + `additions_sound` | `minecraft:audio/ambient_sounds` | folded into one `{"loop", "mood", "additions"}` record |
	 * | `particle` | `minecraft:visual/ambient_particles` | a **list**, and `options` is spelled `particle` |
	 *
	 * `Music`, `AmbientMoodSettings` and `AmbientAdditionsSettings` kept every field name, so only
	 * the nesting moves. All six attributes are positional (none of them calls `notPositional()` in
	 * `EnvironmentAttributes`), which is what `EnvironmentAttributeMap.CODEC_ONLY_POSITIONAL` — the
	 * codec a biome uses — demands; a non-positional one fails the whole map with *"The following
	 * attributes cannot be positional"*.
	 *
	 * ⚠️ This is the **silent** kind of break, not the fatal kind: a `RecordCodecBuilder` ignores
	 * keys it does not know, so an unmigrated biome parses fine and simply renders with vanilla's
	 * default sky, fog and ambience, and plays no music. Nothing is logged.
	 *
	 * `music` is accepted in either spelling — the bare `Music` object every version up to 1.21.3
	 * uses, or the one-entry weighted list [wrapBiomeMusicTo1214] produces — so this pass does not
	 * depend on whether that one ran first. It is gated off above 1.21.11 anyway; accepting both
	 * costs three lines and removes the ordering question.
	 *
	 * Idempotent: a file whose `effects` holds none of the seven moved keys is skipped, and an
	 * `attributes` object that is already there is merged into rather than replaced.
	 */
	fun migrateBiomeAttributesTo12111(resourcesRoot: File): Int {
		val data = resourcesRoot.resolve("data")
		if (!data.isDirectory) return 0
		val moved = setOf(
			"fog_color", "sky_color", "water_fog_color", "music", "music_volume",
			"ambient_sound", "mood_sound", "additions_sound", "particle",
		)
		var changed = 0
		data.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
			if (!file.invariantSeparatorsPath.contains("/worldgen/biome/")) return@forEach
			val original = runCatching { json.parseToJsonElement(file.readText()) as? JsonObject }
				.getOrNull() ?: return@forEach
			val effects = original["effects"] as? JsonObject ?: return@forEach
			if (effects.keys.none { it in moved }) return@forEach

			val attributes = ((original["attributes"] as? JsonObject)?.toMutableMap() ?: mutableMapOf())
			effects["fog_color"]?.let { attributes["minecraft:visual/fog_color"] = it }
			effects["sky_color"]?.let { attributes["minecraft:visual/sky_color"] = it }
			effects["water_fog_color"]?.let { attributes["minecraft:visual/water_fog_color"] = it }
			effects["music_volume"]?.let { attributes["minecraft:audio/music_volume"] = it }

			// Either the bare Music object or the 1.21.4 weighted list's first entry — a one-entry
			// list is all this mod's biomes ever carry, and BackgroundMusic has no weighting.
			val music = when (val node = effects["music"]) {
				is JsonObject -> node
				is JsonArray -> (node.firstOrNull() as? JsonObject)?.get("data") as? JsonObject
				else -> null
			}
			if (music != null) {
				attributes["minecraft:audio/background_music"] =
					JsonObject(mapOf("default" to music))
			}

			val loop = effects["ambient_sound"]
			val mood = effects["mood_sound"]
			val additions = effects["additions_sound"]
			if (loop != null || mood != null || additions != null) {
				attributes["minecraft:audio/ambient_sounds"] = buildJsonObject {
					loop?.let { put("loop", it) }
					mood?.let { put("mood", it) }
					additions?.let { put("additions", JsonArray(listOf(it))) }
				}
			}

			(effects["particle"] as? JsonObject)?.let { particle ->
				val options = particle["options"] ?: particle["particle"]
				if (options != null) {
					attributes["minecraft:visual/ambient_particles"] = JsonArray(
						listOf(
							buildJsonObject {
								put("particle", options)
								particle["probability"]?.let { put("probability", it) }
							}
						)
					)
				}
			}

			val migrated = JsonObject(
				original.toMutableMap().apply {
					put("effects", JsonObject(effects.filterKeys { it !in moved }))
					put("attributes", JsonObject(attributes))
				}
			)
			file.writeText(json.encodeToString(JsonObject.serializer(), migrated))
			changed++
		}
		return changed
	}

	/**
	 * MC 1.21.5 gave `minecraft:smithing_trim` a fourth required field, `pattern`.
	 *
	 * The trim pattern used to be read off the template item at craft time, through the item's
	 * `trim_pattern` data component; 1.21.5 made the recipe name the pattern directly and the
	 * template ingredient purely an ingredient. The field is `fieldOf`, not optional, so the recipe
	 * fails to parse — *"No key pattern in MapLike[…]"* — which is logged per file rather than
	 * thrown, i.e. the polarity trim silently becomes uncraftable.
	 *
	 * The value is not spelled anywhere in the recipe, so it is recovered the way the game used to:
	 * every `trim_pattern/<id>.json` names the item that applies it in `template_item`, which gives a
	 * template-item -> pattern-id map to look the recipe's own `template` up in. Both ingredient
	 * spellings are accepted, since whether the >=1.21.2 pass has already flattened `{"item": …}` to
	 * a bare string depends only on which of the two ran first.
	 */
	fun addTrimPatternsTo1215(resourcesRoot: File): Int {
		val data = resourcesRoot.resolve("data")
		if (!data.isDirectory) return 0
		val patternOfTemplateItem = mutableMapOf<String, String>()
		data.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
			val path = file.invariantSeparatorsPath
			val marker = "/trim_pattern/"
			if (!path.contains(marker)) return@forEach
			val namespace = path.substringBefore(marker).substringAfterLast('/')
			val declared = runCatching { json.parseToJsonElement(file.readText()) as? JsonObject }
				.getOrNull()?.idOf("template_item") ?: return@forEach
			patternOfTemplateItem[declared] = "$namespace:${file.nameWithoutExtension}"
		}
		if (patternOfTemplateItem.isEmpty()) return 0
		var changed = 0
		data.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
			val original = runCatching { json.parseToJsonElement(file.readText()) as? JsonObject }
				.getOrNull() ?: return@forEach
			if (original.idOf("type") != "minecraft:smithing_trim" || original.containsKey("pattern")) return@forEach
			val templateItem = original.idOf("template")
				?: (original["template"] as? JsonObject)?.idOf("item")
				?: return@forEach
			val pattern = patternOfTemplateItem[templateItem] ?: return@forEach
			val migrated = JsonObject(original.toMutableMap().apply { put("pattern", JsonPrimitive(pattern)) })
			file.writeText(json.encodeToString(JsonObject.serializer(), migrated))
			changed++
		}
		return changed
	}

	/**
	 * MC 1.21.2 rewrote the recipe `Ingredient` JSON. `Ingredient` became a `HolderSet<Item>`, whose
	 * codec accepts only a **string** (`"minecraft:paper"` for a single item, `"#forge:rods/wooden"`
	 * for a tag) or a **JSON array** of those — the old `{"item": …}` / `{"tag": …}` object forms are
	 * gone. An unrecognised ingredient shape fails the *whole* recipe to parse (logged, not thrown), so
	 * every crafting/cooking recipe in this mod silently vanished on >= 1.21.2 until this ran.
	 *
	 * Only the ingredient-bearing fields are touched (`ingredient`, `ingredients`, `key`, and the
	 * smithing `base`/`addition`/`template`), never `result`, so this is independent of the 1.20.5
	 * result migration.
	 */
	fun migrateIngredientsTo1212(resourcesRoot: File): Int {
		val data = resourcesRoot.resolve("data")
		if (!data.isDirectory) return 0
		var changed = 0
		data.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
			val dirs = file.invariantSeparatorsPath
			// capsid_recipes is this mod's own recipe type; its `ingredients` go through Ingredient.CODEC too.
			if (!dirs.contains("/recipe/") && !dirs.contains("/recipes/") &&
				!dirs.contains("/capsid_recipes/")) return@forEach
			val original = runCatching { json.parseToJsonElement(file.readText()) as? JsonObject }
				.getOrNull() ?: return@forEach
			val migrated = migrateRecipeIngredients(original)
			if (migrated != original) {
				file.writeText(json.encodeToString(JsonObject.serializer(), migrated))
				changed++
			}
		}
		return changed
	}

	/**
	 * MC 1.21.2 rebuilt the whole shader asset layout around the frame graph. Three moves, all of them
	 * read out of the shipped client jar rather than recalled:
	 *
	 *  * post **chains** left `assets/<ns>/shaders/post/<n>.json` for `assets/<ns>/post_effect/<n>.json`
	 *    (`ShaderManager.POST_CHAIN_ID_CONVERTER = FileToIdConverter.json("post_effect")`);
	 *  * post **programs** left `shaders/program/` for `shaders/post/` — so the folder this mod's chains
	 *    live in is exactly the folder its programs must move into, and the order below matters;
	 *  * every `vertex`/`fragment`/`program` id became a **pathed** id relative to `shaders/`:
	 *    `"sobel"` is now `"minecraft:post/sobel"`, `"alexscaves:rendertype_sepia"` is now
	 *    `"alexscaves:core/rendertype_sepia"` (`CompiledShader.Type.idConverter()` is
	 *    `FileToIdConverter("shaders", ".vsh"/".fsh")`). `shaders/core/` itself did not move.
	 *
	 * The chain schema changed with it: `targets` is a map rather than a list, a pass names its
	 * `program`/`output` instead of `name`/`outtarget`, and both the implicit `intarget` sampler and the
	 * explicit `auxtargets` became one `inputs` list of `{sampler_name, target, use_depth_buffer}`.
	 * `PostPass.TargetInput.bindTo` binds `<sampler_name>Sampler` **and** sets `<sampler_name>Size`, so
	 * the input that used to be `DiffuseSampler` has to be named `In` — that is what feeds the `InSize`
	 * the vertex shaders divide by — and `DiffuseSampler` is renamed to `InSampler` in the program JSON
	 * and in the GLSL to match.
	 *
	 * The mod's own `"final"` target is mapped to `minecraft:main`: it was a chain-owned temp target the
	 * mod pulled back out with `getTempTarget("final")`, and 1.21.2 has no such thing —
	 * `PostEffectRegistry` now owns that texture itself and hands it to `process` as the chain's one
	 * external target, which `PostChain.MAIN_TARGET_ID` names `minecraft:main`.
	 *
	 * A declared target no pass references is dropped (`watcher_perspective` declares `largeBlur2` and
	 * never uses it); `PostChain.addToFrame` would otherwise create a frame-graph resource for it.
	 *
	 * Every step keys off the shape it finds, so re-running is a no-op: a chain is recognised by its
	 * `passes` array, a program by having `vertex`+`fragment`, and an id that already contains a `/` is
	 * left alone — which is what keeps `ac_lightmap.json`'s hand-written `minecraft:core/blit_screen`
	 * from becoming `minecraft:core/core/blit_screen`.
	 */
	fun migrateShadersTo1212(resourcesRoot: File, modId: String): Int {
		val assets = resourcesRoot.resolve("assets/$modId")
		if (!assets.isDirectory) return 0
		val shaders = assets.resolve("shaders")
		var changed = 0

		// 1. Chains out of shaders/post/ first — that folder is where the programs are about to land.
		shaders.resolve("post").listFiles().orEmpty().filter { it.isFile && it.extension == "json" }
			.forEach { file ->
				val root = runCatching { json.parseToJsonElement(file.readText()) as? JsonObject }
					.getOrNull() ?: return@forEach
				if (root["passes"] !is JsonArray) return@forEach
				val destination = assets.resolve("post_effect/${file.name}")
				destination.parentFile.mkdirs()
				destination.writeText(json.encodeToString(JsonObject.serializer(), migratePostChain(root)))
				file.delete()
				changed++
			}

		// 2. Programs into it, rewritten in place afterwards.
		val relocated = relocate(shaders.resolve("program"), shaders.resolve("post"))
		if (relocated > 0) {
			shaders.resolve("post").walkTopDown().filter { it.isFile }.forEach { file ->
				when (file.extension) {
					"json" -> {
						val root = runCatching { json.parseToJsonElement(file.readText()) as? JsonObject }
							.getOrNull() ?: return@forEach
						file.writeText(
							json.encodeToString(
								JsonObject.serializer(),
								renameScreenSizeUniform(migrateShaderProgram(root, "post")),
							)
						)
					}
					"vsh", "fsh", "glsl" -> file.writeText(renameScreenSize(renameDiffuseSampler(file.readText())))
				}
				changed++
			}
		}

		// 3. Core programs stay put and only need their ids pathed.
		shaders.resolve("core").listFiles().orEmpty().filter { it.isFile && it.extension == "json" }
			.forEach { file ->
				val root = runCatching { json.parseToJsonElement(file.readText()) as? JsonObject }
					.getOrNull() ?: return@forEach
				val migrated = migrateShaderProgram(root, "core", stripFixedFunctionState = false)
				if (migrated != root) {
					file.writeText(json.encodeToString(JsonObject.serializer(), migrated))
					changed++
				}
			}

		return changed
	}

	/** `"sobel"` -> `"minecraft:post/sobel"`; an id that is already pathed is returned unchanged. */
	private fun qualifyShaderId(value: String, folder: String): String {
		val namespace = value.substringBefore(':', missingDelimiterValue = "minecraft")
		val path = value.substringAfter(':')
		return if (path.contains('/')) value else "$namespace:$folder/$path"
	}

	private fun migratePostChain(chain: JsonObject): JsonObject {
		// A target name is a ResourceLocation from 1.21.2 on, and a path may only hold
		// [a-z0-9/._-]. watcher_perspective's `largeBlur` is not a legal one, so the chain would
		// have failed to decode outright — camelCase becomes snake_case rather than being
		// lowercased, so `largeBlur` and a hypothetical `largeblur` stay distinct names.
		fun sanitise(id: String): String {
			val namespace = id.substringBefore(':', missingDelimiterValue = "")
			val path = id.substringAfter(':')
				.replace(Regex("(?<=[a-z0-9])(?=[A-Z])"), "_")
				.lowercase()
				.replace(Regex("[^a-z0-9/._-]"), "_")
			return if (namespace.isEmpty()) path else "$namespace:$path"
		}

		fun target(id: String) = if (id == "final") "minecraft:main" else sanitise(id)

		val passes = (chain["passes"] as? JsonArray).orEmpty().filterIsInstance<JsonObject>()
		val referenced = LinkedHashSet<String>()
		val migratedPasses = passes.map { pass ->
			val inputs = buildList {
				(pass["intarget"] as? JsonPrimitive)?.content?.let { intarget ->
					referenced += target(intarget)
					add(buildJsonObject {
						put("sampler_name", JsonPrimitive("In"))
						put("target", JsonPrimitive(target(intarget)))
					})
				}
				(pass["auxtargets"] as? JsonArray).orEmpty().filterIsInstance<JsonObject>().forEach { aux ->
					val name = (aux["name"] as? JsonPrimitive)?.content ?: return@forEach
					val id = (aux["id"] as? JsonPrimitive)?.content ?: return@forEach
					// `<target>:depth` sampled the depth attachment; it is a flag on the input now.
					val depth = id.endsWith(":depth")
					val resolved = target(id.removeSuffix(":depth"))
					referenced += resolved
					add(buildJsonObject {
						put("sampler_name", JsonPrimitive(name.removeSuffix("Sampler")))
						put("target", JsonPrimitive(resolved))
						if (depth) put("use_depth_buffer", JsonPrimitive(true))
					})
				}
			}
			val output = (pass["outtarget"] as? JsonPrimitive)?.content?.let(::target)
			if (output != null) referenced += output
			buildJsonObject {
				(pass["name"] as? JsonPrimitive)?.content?.let {
					put("program", JsonPrimitive(qualifyShaderId(it, "post")))
				}
				put("inputs", JsonArray(inputs))
				if (output != null) put("output", JsonPrimitive(output))
				(pass["uniforms"] as? JsonArray)?.let { put("uniforms", it) }
			}
		}

		val internal = (chain["targets"] as? JsonArray).orEmpty()
			.filterIsInstance<JsonPrimitive>()
			.map { target(it.content) }
			.filter { it != "minecraft:main" && it in referenced }
			.distinct()

		return buildJsonObject {
			put("targets", JsonObject(internal.associateWith { JsonObject(emptyMap()) }))
			put("passes", JsonArray(migratedPasses))
		}
	}

	/**
	 * `ShaderProgramConfig`'s codec has only `vertex`, `fragment`, `samplers`, `uniforms` and `defines`.
	 * The dropped `blend`/`attributes` are ignored rather than rejected, but a post program's blend
	 * state genuinely is gone — 1.21.2 leaves it to the caller — so they come out of the post programs
	 * rather than being left behind as a claim the file no longer makes. Core programs keep theirs: a
	 * `RenderType` carries its own transparency state and those files are otherwise untouched.
	 */
	private fun migrateShaderProgram(
		program: JsonObject,
		folder: String,
		stripFixedFunctionState: Boolean = true,
	): JsonObject {
		val out = LinkedHashMap(program)
		listOf("vertex", "fragment").forEach { field ->
			(program[field] as? JsonPrimitive)?.takeIf { it.isString }?.let {
				out[field] = JsonPrimitive(qualifyShaderId(it.content, folder))
			}
		}
		if (stripFixedFunctionState) {
			out.remove("blend")
			out.remove("attributes")
			(program["samplers"] as? JsonArray)?.let { samplers ->
				out["samplers"] = JsonArray(samplers.map { sampler ->
					val name = ((sampler as? JsonObject)?.get("name") as? JsonPrimitive)?.content
					if (name == "DiffuseSampler") {
						JsonObject(LinkedHashMap(sampler as JsonObject).also {
							it["name"] = JsonPrimitive("InSampler")
						})
					} else {
						sampler
					}
				})
			}
		}
		return JsonObject(out)
	}

	/** The one sampler 1.21.2 renamed, as a whole word so `DiffuseDepthSampler` is left alone. */
	private fun renameDiffuseSampler(source: String): String =
		source.replace(Regex("\\bDiffuseSampler\\b"), "InSampler")

	/**
	 * `ScreenSize` was a **builtin** post uniform, set per pass by `PostPass` — `javap` on 1.20.1's
	 * `PostPass.class` yields `ProjMat`, `InSize`, `OutSize`, `Time`, `ScreenSize`, `DiffuseSampler` —
	 * and 1.21.2 deleted it: `addToFrame` sets only `OutSize`, and each input contributes
	 * `<name>Sampler`/`<name>Size`. So from 1.21.2 the two shaders that read it (`submarine_light`,
	 * `watcher_blur`) get whatever their program JSON declares as a default, i.e. `[1, 1]` — a 1:1
	 * aspect ratio — and from 1.21.5 [migrateShadersTo1215] drops the declaration outright (it filters
	 * every name ending in `Size`), leaving `ScreenSize.x / ScreenSize.y` as `0 / 0`. Both effects go
	 * to NaN there and paint the screen black.
	 *
	 * `OutSize` is the exact replacement: it is the size of the target the pass draws into, every
	 * target in all three of this mod's chains is full-window, and it is free on every version from
	 * 1.21.2 (a builtin through 1.21.5, a `SamplerInfo` member on 1.21.6). Both files already declare
	 * an unused `uniform vec2 OutSize;`, so the old declaration is **dropped** rather than renamed —
	 * renaming it would be a redefinition.
	 *
	 * Keyed off the old spelling, so re-running is a no-op.
	 */
	private fun renameScreenSize(source: String): String {
		val word = Regex("(?<![A-Za-z0-9_])ScreenSize(?![A-Za-z0-9_])")
		if (!word.containsMatchIn(source)) return source
		val declaresOutSize = source.lineSequence().any { it.trim() == "uniform vec2 OutSize;" }
		val stripped = source.lineSequence()
			.filterNot { declaresOutSize && it.trim() == "uniform vec2 ScreenSize;" }
			.joinToString("\n")
		return word.replace(stripped, "OutSize")
	}

	/** The [renameScreenSize] counterpart in a post-program JSON's `uniforms` list. */
	private fun renameScreenSizeUniform(program: JsonObject): JsonObject {
		val uniforms = program["uniforms"] as? JsonArray ?: return program
		val names = uniforms.filterIsInstance<JsonObject>()
			.mapNotNull { (it["name"] as? JsonPrimitive)?.content }
		if ("ScreenSize" !in names) return program
		val out = LinkedHashMap(program)
		out["uniforms"] = JsonArray(uniforms.mapNotNull { uniform ->
			val name = ((uniform as? JsonObject)?.get("name") as? JsonPrimitive)?.content
			when {
				name != "ScreenSize" -> uniform
				"OutSize" in names -> null
				else -> JsonObject(LinkedHashMap(uniform as JsonObject).also {
					it["name"] = JsonPrimitive("OutSize")
				})
			}
		})
		return JsonObject(out)
	}

	/**
	 * MC 1.20.5 rewrote the core-shader interface underneath this mod's eight render-type shaders, and
	 * the two changes it made are both **client-only link failures** — invisible to every `runServer`,
	 * which is why they survived the whole Forge/NeoForge walk:
	 *
	 *  * `fog.glsl`'s `fog_distance` lost its `mat4` first parameter (`fog_distance(mat4, vec3, int)`
	 *    on 1.20.4, `fog_distance(vec3, int)` from 1.20.6 — read out of each node's own
	 *    `client-extra` jar, the change lands in 1.20.5). It took `IViewRotMat` with it, so the two
	 *    shaders that declared that uniform are declaring one nothing supplies;
	 *  * vanilla's entity vertex shaders stopped writing `out vec4 normal`. A fragment shader with a
	 *    matching `in` and no vertex-side `out` is a link error in the core profile, and
	 *    `rendertype_red_ghost.fsh` has one — it never used it.
	 *
	 * Only fragment shaders with no sibling `.vsh` are touched for the second: `bubbled` and
	 * `ferrouslime_gel` ship their own vertex stage, which still writes `normal`, so their `in` is
	 * matched and correct.
	 *
	 * Both steps key off the old spelling, so re-running is a no-op.
	 */
	fun migrateCoreShadersTo1205(resourcesRoot: File, modId: String): Int {
		val core = resourcesRoot.resolve("assets/$modId/shaders/core")
		if (!core.isDirectory) return 0
		var changed = 0

		core.listFiles().orEmpty().filter { it.isFile && it.extension == "vsh" }.forEach { file ->
			val source = file.readText()
			var out = source.replace(
				Regex("fog_distance\\(\\s*ModelViewMat\\s*,\\s*IViewRotMat\\s*\\*\\s*(\\w+)\\s*,"),
				"fog_distance($1,",
			)
			// Only once nothing references it any more — the declaration is the last thing to go.
			if (!Regex("(?<![A-Za-z0-9_])IViewRotMat(?![A-Za-z0-9_])")
					.containsMatchIn(out.lineSequence().filterNot { it.trimStart().startsWith("uniform") }.joinToString("\n"))
			) {
				out = out.lineSequence().filterNot { it.trimStart() == "uniform mat3 IViewRotMat;" }.joinToString("\n")
			}
			if (out != source) {
				file.writeText(out)
				changed++
			}
		}

		core.listFiles().orEmpty().filter { it.isFile && it.extension == "fsh" }.forEach { file ->
			if (core.resolve("${file.nameWithoutExtension}.vsh").isFile) return@forEach
			val source = file.readText()
			val out = source.lineSequence().filterNot { it.trimStart() == "in vec4 normal;" }.joinToString("\n")
			if (out != source) {
				file.writeText(out)
				changed++
			}
		}

		return changed
	}

	/**
	 * The uniform declarations 1.21.6 folded into a vanilla std140 block, and the include that
	 * declares each block now. `FogStart`/`FogEnd`/`FogShape` map to `fog.glsl` only because that is
	 * the include the files carrying them already have — those three names are simply **gone**, which
	 * is why the fog rewrite below has to run before the declarations are stripped.
	 */
	private val ubo1216Uniforms = mapOf(
		"uniform mat4 ModelViewMat;" to "dynamictransforms.glsl",
		"uniform vec4 ColorModulator;" to "dynamictransforms.glsl",
		"uniform mat4 TextureMat;" to "dynamictransforms.glsl",
		"uniform mat4 ProjMat;" to "projection.glsl",
		"uniform float GameTime;" to "globals.glsl",
		"uniform vec3 Light0_Direction;" to "light.glsl",
		"uniform vec3 Light1_Direction;" to "light.glsl",
		"uniform vec4 FogColor;" to "fog.glsl",
		"uniform float FogStart;" to "fog.glsl",
		"uniform float FogEnd;" to "fog.glsl",
		"uniform int FogShape;" to "fog.glsl",
	)

	/** Emitted in vanilla's own order, so a migrated file reads like one of vanilla's. */
	private val include1216Order = listOf("light.glsl", "fog.glsl", "dynamictransforms.glsl", "projection.glsl", "globals.glsl")

	/**
	 * The 1.21.6 replacement for `ac_lightmap.fsh`'s eleven scalar uniforms.
	 *
	 * Declared **without** an instance name, unlike vanilla's own `lightmap.fsh` (`} lightmapInfo;`),
	 * so the members stay in global scope and the shader body below it is untouched. The order is the
	 * order `LightTextureMixin` writes the buffer in, and both `vec3`s come last because
	 * `Std140Builder#putVec3` advances a full 16 bytes — a scalar declared after one would land in
	 * that padding rather than in the next slot.
	 */
	private val lightmapUboBlock = """
		layout(std140) uniform LightmapInfo {
		    float AmbientLightFactor;
		    float SkyFactor;
		    float BlockFactor;
		    int UseBrightLightmap;
		    float NightVisionFactor;
		    float DarknessScale;
		    float DarkenWorldFactor;
		    float BrightnessFactor;
		    float ACAmbientLight;
		    vec3 SkyLightColor;
		    vec3 ACLightColor;
		};
	""".trimIndent()

	/**
	 * MC 1.21.6 replaced every scalar shader uniform with a std140 uniform **block**, and rewrote fog
	 * from one distance to two. Both are client-only GLSL link failures — a `runServer` sees nothing —
	 * and they hit all thirteen of this mod's core shaders:
	 *
	 *  * `ModelViewMat`, `ColorModulator` and `TextureMat` now come from `DynamicTransforms`, `ProjMat`
	 *    from `Projection`, `GameTime` from `Globals`, `Light0/1_Direction` from `Lighting` and the fog
	 *    ranges from `Fog`. Each block is declared by an include (`dynamictransforms.glsl`,
	 *    `projection.glsl`, `globals.glsl`, `light.glsl`, `fog.glsl`) **without** an instance name, so
	 *    its members are in global scope and a shader that stops declaring the uniform and starts
	 *    importing the include needs no other edit. Redeclaring one is a redefinition error.
	 *  * `fog.glsl` deleted `linear_fog`, `linear_fog_fade` and `fog_distance(vec3, int)`. A vertex
	 *    stage now writes **two** distances (`fog_spherical_distance` / `fog_cylindrical_distance`) and
	 *    the fragment stage passes both to `apply_fog(...)` or `total_fog_value(...)`; `FogShape` is
	 *    gone, the two shapes being separate values rather than a switch. `FogStart`/`FogEnd` became
	 *    `FogEnvironmentalStart`/`End` plus `FogRenderDistanceStart`/`End`.
	 *
	 * `rendertype_sepia.fsh` and `rendertype_red_ghost.fsh` have no sibling `.vsh` — they are paired
	 * with vanilla's `core/entity.vsh` by the pipelines in `ACInternalShaders` — so their `in` has to
	 * follow vanilla's renamed `out`, not a vertex stage of this mod's own.
	 *
	 * `ac_lightmap.fsh` is special-cased: its eleven uniforms become one block, since
	 * `LightTextureMixin` now hands the whole thing over as a single mapped buffer.
	 *
	 * Every step keys off the old spelling, so re-running is a no-op.
	 */
	fun migrateCoreShadersTo1216(resourcesRoot: File, modId: String): Int {
		val core = resourcesRoot.resolve("assets/$modId/shaders/core")
		if (!core.isDirectory) return 0
		var changed = 0

		core.listFiles().orEmpty().filter { it.isFile && (it.extension == "vsh" || it.extension == "fsh") }.forEach { file ->
			val source = file.readText()
			var out = source

			if (file.name == "ac_lightmap.fsh") {
				val names = Regex("^uniform (?:float|int|vec3) (\\w+);$", RegexOption.MULTILINE)
				if (names.containsMatchIn(out)) {
					var first = true
					out = out.lineSequence().mapNotNull { line ->
						if (!names.matches(line.trim())) return@mapNotNull line
						if (first) {
							first = false
							lightmapUboBlock
						} else {
							null
						}
					}.joinToString("\n")
				}
			} else {
				// Fog first: FogShape/FogStart/FogEnd have to stop being referenced before their
				// declarations can go, and the varying rename below reads the old call shape.
				out = out.replace(
					Regex("""(\h*)vertexDistance\s*=\s*fog_distance\(\s*(.+?)\s*,\s*FogShape\s*\)\s*;"""),
				) { m ->
					val indent = m.groupValues[1]
					val pos = m.groupValues[2]
					"${indent}sphericalVertexDistance = fog_spherical_distance($pos);\n" +
						"${indent}cylindricalVertexDistance = fog_cylindrical_distance($pos);"
				}
				out = out.replace(
					Regex("""linear_fog\(\s*(.+?)\s*,\s*vertexDistance\s*,\s*FogStart\s*,\s*FogEnd\s*,\s*FogColor\s*\)"""),
				) { m ->
					"apply_fog(${m.groupValues[1]}, sphericalVertexDistance, cylindricalVertexDistance, " +
						"FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor)"
				}
				out = out.replace(
					Regex("""linear_fog_fade\(\s*vertexDistance\s*,\s*FogStart\s*,\s*FogEnd\s*\)"""),
				) {
					"(1.0 - total_fog_value(sphericalVertexDistance, cylindricalVertexDistance, " +
						"FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd))"
				}
				out = out.replace(
					Regex("""^(\h*)(in|out) float vertexDistance;$""", RegexOption.MULTILINE),
				) { m ->
					val (indent, dir) = m.groupValues[1] to m.groupValues[2]
					"$indent$dir float sphericalVertexDistance;\n$indent$dir float cylindricalVertexDistance;"
				}

				val needed = linkedSetOf<String>()
				out = out.lineSequence().filterNot { line ->
					val include = ubo1216Uniforms[line.trim()] ?: return@filterNot false
					needed += include
					true
				}.joinToString("\n")

				val missing = include1216Order.filter { it in needed && !out.contains("#moj_import <$it>") && !out.contains("#moj_import <minecraft:$it>") }
				if (missing.isNotEmpty()) {
					val lines = out.lines().toMutableList()
					val anchor = lines.indexOfLast { it.trimStart().startsWith("#moj_import") }
						.takeIf { it >= 0 }
						?: lines.indexOfFirst { it.trimStart().startsWith("#version") }
					lines.addAll(anchor + 1, missing.map { "#moj_import <$it>" })
					// A file that had no imports at all gets a blank line to sit under #version.
					if (out.lineSequence().none { it.trimStart().startsWith("#moj_import") }) {
						lines.add(anchor + 1, "")
					}
					out = lines.joinToString("\n")
				}
			}

			// Stripping whole declaration lines leaves the blank lines that framed them stacked up.
			out = out.replace(Regex("\n{3,}"), "\n\n")
			if (out != source) {
				file.writeText(out)
				changed++
			}
		}

		return changed
	}

	private data class PostUniform(val name: String, val type: String, val values: List<Float>?)

	private data class PostProgram(val vertex: String, val fragment: String, val uniforms: List<PostUniform>)

	/**
	 * The three vanilla post programs this mod's chains name, as 1.21.5 leaves them.
	 *
	 * The uniform lists are **not** the 1.21.4 program JSONs' — they are what each program's 1.21.5
	 * GLSL still reads, minus what the pipeline supplies for free. `color_convolve.fsh` const-folded
	 * `Gray` and `Saturation` away, and a uniform that is declared but unused is optimised out of the
	 * compiled program, which `PostChain.createPass` treats as a hard error ("Uniform 'X' does not
	 * exist for …"). So over-declaring here is fatal, not harmless.
	 */
	private val vanillaPostPrograms = mapOf(
		"minecraft:post/blit" to PostProgram(
			"minecraft:post/blit",
			"minecraft:post/blit",
			listOf(PostUniform("ColorModulate", "vec4", listOf(1.0f, 1.0f, 1.0f, 1.0f))),
		),
		"minecraft:post/entity_outline" to PostProgram(
			"minecraft:post/sobel",
			"minecraft:post/entity_sobel",
			emptyList(),
		),
		"minecraft:post/color_convolve" to PostProgram(
			"minecraft:post/sobel",
			"minecraft:post/color_convolve",
			listOf(
				PostUniform("RedMatrix", "vec3", listOf(1.0f, 0.0f, 0.0f)),
				PostUniform("GreenMatrix", "vec3", listOf(0.0f, 1.0f, 0.0f)),
				PostUniform("BlueMatrix", "vec3", listOf(0.0f, 0.0f, 1.0f)),
			),
		),
	)

	/** Uniforms `PostEffectRegistry` pushes per frame, which must therefore ship without `values`. */
	private val runtimePostUniforms = setOf("Time")

	/** Supplied by `RenderPipelines.POST_PROCESSING_SNIPPET`, so a pass may never redeclare them. */
	private val builtinPostUniforms = setOf("ProjMat", "OutSize")

	/**
	 * MC 1.21.5 deleted the post-**program** JSON. A pass names its `vertex_shader` and
	 * `fragment_shader` directly, and the pipeline it is compiled into is built from the pass itself:
	 * `POST_PROCESSING_SNIPPET` contributes `ProjMat` and `OutSize`, each declared input contributes
	 * `<name>Sampler` and `<name>Size`, and everything else has to be listed in the pass's `uniforms`
	 * — which grew a mandatory `type` (`float`/`vec2`/`vec3`/`vec4`/`int`/`ivec3`/`matrix4x4`), the
	 * `count` having gone with the program file.
	 *
	 * So this pass folds each program JSON into every pass that referenced it: the program's
	 * vertex/fragment ids become the pass's two fields, and the program's uniform defaults become the
	 * pass's declarations, with the pass's own `uniforms` values layered on top. That is exactly what
	 * 1.21.4's `PostPass` did at runtime — it seeded a uniform from the program and then let the chain
	 * override it — so a `blit` pass that declared nothing still has to ship
	 * `ColorModulate = [1,1,1,1]` here, or it multiplies the frame by zero.
	 *
	 * Two things make this narrower than "copy the program's uniform list across":
	 *
	 *  * **A declared uniform the compiled program does not have is fatal.** `PostChain.createPass`
	 *    throws `ShaderManager.CompilationException`, and the chain is dead. GLSL optimises out a
	 *    uniform that is declared and never read, so `watcher_blur.fsh`'s five unused declarations
	 *    (`Scissor`, `Vignette`, `InSize`, `OutSize`, `ProjMat`) must not be carried over — hence the
	 *    use check against the program's own GLSL, and hence [vanillaPostPrograms] listing what
	 *    1.21.5's shaders actually read rather than what their old JSONs declared.
	 *  * **A uniform driven at runtime must ship without `values`.** `PostPass.addToFrame` applies the
	 *    `Consumer<RenderPass>` handed to `PostChain.process` *before* the pass's own JSON uniforms, so
	 *    a default would overwrite it every frame. Vanilla's own `blur.json` does the same with
	 *    `Radius`. Here that is `Time` — see `PostEffectRegistry.processEffects`.
	 *
	 * `sugar_rush`'s `Saturation: 1.65` is dropped as a consequence of the first point: 1.21.5 baked
	 * the value into `color_convolve.fsh` as a constant `1.8`, and there is no uniform left to set.
	 *
	 * Recognised by the pass shape (`program` present), and the program JSONs are deleted once folded
	 * in — 1.21.5's `ShaderManager` never reads them — so re-running finds nothing to do.
	 */
	fun migrateShadersTo1215(resourcesRoot: File, modId: String): Int {
		val assets = resourcesRoot.resolve("assets/$modId")
		val postEffects = assets.resolve("post_effect")
		if (!postEffects.isDirectory) return 0
		val shaders = assets.resolve("shaders")
		val programDir = shaders.resolve("post")

		val programs = LinkedHashMap(vanillaPostPrograms)
		programDir.listFiles().orEmpty().filter { it.isFile && it.extension == "json" }.forEach { file ->
			val root = runCatching { json.parseToJsonElement(file.readText()) as? JsonObject }
				.getOrNull() ?: return@forEach
			val vertex = (root["vertex"] as? JsonPrimitive)?.content ?: return@forEach
			val fragment = (root["fragment"] as? JsonPrimitive)?.content ?: return@forEach
			val glsl = listOf(vertex to "vsh", fragment to "fsh").joinToString("\n") { (id, extension) ->
				val source = shaders.resolve("${id.substringAfter(':')}.$extension")
				if (id.substringBefore(':', "minecraft") == modId && source.isFile) source.readText() else ""
			}
			val uniforms = (root["uniforms"] as? JsonArray).orEmpty().filterIsInstance<JsonObject>()
				.mapNotNull { uniform ->
					val name = (uniform["name"] as? JsonPrimitive)?.content ?: return@mapNotNull null
					if (name in builtinPostUniforms || name.endsWith("Sampler") || name.endsWith("Size")) {
						return@mapNotNull null
					}
					if (!usesPostUniform(glsl, name)) return@mapNotNull null
					val type = postUniformType(
						(uniform["type"] as? JsonPrimitive)?.content,
						(uniform["count"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 1,
					) ?: return@mapNotNull null
					PostUniform(
						name,
						type,
						(uniform["values"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.doubleOrNull?.toFloat() },
					)
				}
			programs["$modId:post/${file.nameWithoutExtension}"] = PostProgram(vertex, fragment, uniforms)
		}

		var changed = 0
		postEffects.listFiles().orEmpty().filter { it.isFile && it.extension == "json" }.forEach { file ->
			val root = runCatching { json.parseToJsonElement(file.readText()) as? JsonObject }
				.getOrNull() ?: return@forEach
			val passes = (root["passes"] as? JsonArray).orEmpty().filterIsInstance<JsonObject>()
			if (passes.none { it["program"] != null }) return@forEach

			val migrated = passes.map { pass ->
				val programId = (pass["program"] as? JsonPrimitive)?.content
					?: error("Post pass in ${file.name} names no program")
				val program = programs[programId]
					?: error("Post pass in ${file.name} names unknown program $programId")
				// Whatever the pass reads through an input is already a pipeline uniform.
				val fromInputs = (pass["inputs"] as? JsonArray).orEmpty().filterIsInstance<JsonObject>()
					.mapNotNull { ((it["sampler_name"] as? JsonPrimitive)?.content) }
					.flatMap { listOf("${it}Sampler", "${it}Size") }
					.toSet()
				val overrides = (pass["uniforms"] as? JsonArray).orEmpty().filterIsInstance<JsonObject>()
					.mapNotNull { uniform ->
						val name = (uniform["name"] as? JsonPrimitive)?.content ?: return@mapNotNull null
						val values = (uniform["values"] as? JsonArray)
							?.mapNotNull { (it as? JsonPrimitive)?.doubleOrNull?.toFloat() }
							?: return@mapNotNull null
						name to values
					}.toMap()
				val uniforms = program.uniforms
					.filterNot { it.name in fromInputs }
					.map { declared ->
						if (declared.name in runtimePostUniforms) declared.copy(values = null)
						else overrides[declared.name]?.let { declared.copy(values = it) } ?: declared
					}
				buildJsonObject {
					put("vertex_shader", JsonPrimitive(program.vertex))
					put("fragment_shader", JsonPrimitive(program.fragment))
					(pass["inputs"] as? JsonArray)?.let { put("inputs", it) }
					(pass["output"] as? JsonPrimitive)?.let { put("output", it) }
					if (uniforms.isNotEmpty()) {
						put("uniforms", JsonArray(uniforms.map { uniform ->
							buildJsonObject {
								put("name", JsonPrimitive(uniform.name))
								put("type", JsonPrimitive(uniform.type))
								uniform.values?.let { values ->
									put("values", JsonArray(values.map { JsonPrimitive(it) }))
								}
							}
						}))
					}
				}
			}

			val out = buildJsonObject {
				(root["targets"] as? JsonObject)?.let { put("targets", it) }
				put("passes", JsonArray(migrated))
			}
			file.writeText(json.encodeToString(JsonObject.serializer(), out))
			changed++
		}

		if (changed > 0) {
			programDir.listFiles().orEmpty().filter { it.isFile && it.extension == "json" }
				.forEach { it.delete() }
		}
		return changed
	}

	/** True when [glsl] reads [name] somewhere other than its own `uniform` declaration. */
	/**
	 * MC 1.21.5 changed the quad a post pass draws, which no compiler and no log can tell you about:
	 * the effect simply stops appearing.
	 *
	 * Up to 1.21.4 `PostPass.addToFrame` built its own buffer with the corners `(0,0)` and
	 * `(width,height)` and drew it under an orthographic projection of the same size, so a vertex
	 * stage wrote `ProjMat * vec4(Position.xy, 0, 1)` and recovered the texture coordinate from the
	 * clip-space result (`outPos.xy * 0.5 + 0.5`). 1.21.5 draws `RenderSystem.getQuadVertexBuffer()`
	 * instead — the unit quad, corners `(0,0)` and `(1,1)` — so the same shader emits a quad one pixel
	 * across. Vanilla's own `blit.vsh`/`sobel.vsh` were rewritten to scale by `OutSize` and to take the
	 * texture coordinate straight from the vertex, which is what this writes.
	 *
	 * All three of this mod's post vertex stages carry the identical pre-1.21.5 body, and none of them
	 * declares `OutSize` — it is a free pipeline uniform from 1.21.2 (`POST_PROCESSING_SNIPPET`) and a
	 * `SamplerInfo` member on 1.21.6, so the declaration is added here and folded into the block by
	 * [migrateShadersTo1216].
	 *
	 * Keyed off the old body, so re-running is a no-op.
	 */
	fun migratePostVertexTo1215(resourcesRoot: File, modId: String): Int {
		val post = resourcesRoot.resolve("assets/$modId/shaders/post")
		if (!post.isDirectory) return 0
		var changed = 0

		post.listFiles().orEmpty().filter { it.isFile && it.extension == "vsh" }.forEach { file ->
			val source = file.readText()
			var out = source.replace(
				"ProjMat * vec4(Position.xy, 0.0, 1.0)",
				"ProjMat * vec4(Position.xy * OutSize, 0.0, 1.0)",
			)
			out = out.replace(
				Regex("""texCoord\s*=\s*outPos\.xy\s*\*\s*0\.5\s*\+\s*0\.5\s*;"""),
				"texCoord = Position.xy;",
			)
			if (out != source && out.lineSequence().none { it.trim() == "uniform vec2 OutSize;" }) {
				val lines = out.lines().toMutableList()
				// Vanilla declares OutSize first, and [migrateShadersTo1216] reads the pair as one
				// block in that order.
				val anchor = lines.indexOfFirst { it.trim() == "uniform vec2 InSize;" }
				if (anchor < 0) error("${file.name} scales by OutSize but declares no InSize to sit beside")
				lines.add(anchor, "uniform vec2 OutSize;")
				out = lines.joinToString("\n")
			}
			if (out != source) {
				file.writeText(out)
				changed++
			}
		}

		return changed
	}

	private data class PostBlockMember(val name: String, val glsl: String)

	private data class PostUniformBlock(val block: String, val members: List<PostBlockMember>)

	/**
	 * The std140 block each surviving post uniform lands in on 1.21.6, and the member order the block
	 * is laid out in.
	 *
	 * The first three are vanilla's own — its `blit.fsh` and `color_convolve.fsh` already declare them
	 * under exactly these names, and those two shaders are not this mod's to rewrite, so the chains
	 * that name them have to agree. The last two are this mod's, and the order is arbitrary but must
	 * be the same on both sides: [migrateShadersTo1216] writes the GLSL declaration and the JSON value
	 * list from this one table, and `PostPass` fills the buffer by walking the JSON list in order.
	 *
	 * **A block is emitted whole or not at all.** Declaring a subset in GLSL would move the members
	 * that follow it to the wrong std140 offsets, so a shader that reads only `Radius` still declares
	 * `BlurDir` in front of it.
	 */
	private val postUniformBlocks1216 = listOf(
		PostUniformBlock("BlitConfig", listOf(PostBlockMember("ColorModulate", "vec4"))),
		PostUniformBlock(
			"BlurConfig",
			listOf(PostBlockMember("BlurDir", "vec2"), PostBlockMember("Radius", "float")),
		),
		PostUniformBlock(
			"ColorConfig",
			listOf(
				PostBlockMember("RedMatrix", "vec3"),
				PostBlockMember("GreenMatrix", "vec3"),
				PostBlockMember("BlueMatrix", "vec3"),
			),
		),
		PostUniformBlock(
			"HologramConfig",
			listOf(PostBlockMember("Frequency", "vec2"), PostBlockMember("WobbleAmount", "vec2")),
		),
		PostUniformBlock("PerspectiveConfig", listOf(PostBlockMember("_FOV", "float"))),
	)

	/** `PostChain.createPass` adds this one itself, from the pass's output target and its inputs. */
	private val samplerInfoBlock = """
		layout(std140) uniform SamplerInfo {
		    vec2 OutSize;
		    vec2 InSize;
		};
	""".trimIndent()

	/**
	 * `Time` stops being a uniform on 1.21.6 and is derived from the vanilla `Globals` block instead.
	 *
	 * `GlobalSettingsUniform` writes `GameTime` as `((gameTime % 24000) + partialTick) / 24000`, i.e. a
	 * 0..1 sawtooth over 24000 ticks, and `PostEffectRegistry` used to push a 0..1 sawtooth over 20.
	 * 24000 / 1200 = 20, so this is the same ramp — and it is a better one, since it no longer drifts
	 * with how often the effect happens to be processed.
	 */
	private val hologramTimeDefine = """

		// PostChain has no per-frame uniform push on 1.21.6 — a PostPass builds its uniform buffers
		// once, in its constructor — so the clock comes out of the vanilla Globals block, which
		// bindDefaultUniforms supplies to every post pass for free.
		#define Time (fract(GameTime * 1200.0))
	""".trimIndent()

	private val postUniformDeclaration = Regex("""^\s*uniform\s+(\w+)\s+(\w+)\s*;\s*$""")

	/**
	 * MC 1.21.6 did to the post shaders what it did to the core ones: every scalar uniform became a
	 * member of a std140 block, and a block is bound by name from the pipeline the pass is compiled
	 * into. `PostChain.createPass` builds that pipeline from the pass JSON — `SamplerInfo` always, one
	 * `<name>Sampler` per input, and **one block per key of the pass's `uniforms` map** — so the JSON
	 * and the GLSL have to agree on block names, member order and member types, all three.
	 *
	 * Three consequences shape this pass:
	 *
	 *  * **The flat 1.21.5 uniform list becomes a map of blocks.** `UniformValue`'s codec is a plain
	 *    `Type.CODEC.dispatch`, so an element is `{"type": "vec2", "value": [1, 0]}` — there is no
	 *    `name` field at all, and the list order *is* the std140 layout. [postUniformBlocks1216] is
	 *    the single source of both sides.
	 *  * **`ProjMat` moves to `projection.glsl`.** `POST_PROCESSING_SNIPPET` still declares
	 *    `Projection`, so the include is all that is needed; `InSize`/`OutSize` move into
	 *    `SamplerInfo`, which `createPass` declares unconditionally.
	 *  * **`Time` cannot be a uniform any more** — see [hologramTimeDefine]. Reading `GameTime` means
	 *    declaring `Globals`, and the only way to get that name onto the pipeline is a key in the
	 *    pass's `uniforms` map. An **empty** list is exactly right: `createPass` calls `withUniform`
	 *    for every key, while `PostPass`'s constructor skips an empty one, so the block is declared and
	 *    nothing overwrites what `bindDefaultUniforms` bound.
	 *
	 * A declaration the shader body never reads is dropped rather than carried into a block —
	 * `watcher_blur.fsh` alone has four (`ProjMat`, `InSize`, `Scissor`, `Vignette`) — since GLSL
	 * optimises an unread uniform out and `GlProgram` then cannot find the block to bind.
	 *
	 * Recognised by the pass's `uniforms` still being an array, and by the old declaration spelling in
	 * the GLSL, so re-running is a no-op.
	 */
	fun migrateShadersTo1216(resourcesRoot: File, modId: String): Int {
		val assets = resourcesRoot.resolve("assets/$modId")
		if (!assets.isDirectory) return 0
		var changed = 0

		assets.resolve("shaders/post").listFiles().orEmpty()
			.filter { it.isFile && (it.extension == "vsh" || it.extension == "fsh") }
			.forEach { file ->
				val source = file.readText()
				val out = rewritePostShaderTo1216(source)
				if (out != source) {
					file.writeText(out)
					changed++
				}
			}

		assets.resolve("post_effect").listFiles().orEmpty().filter { it.isFile && it.extension == "json" }
			.forEach { file ->
				val root = runCatching { json.parseToJsonElement(file.readText()) as? JsonObject }
					.getOrNull() ?: return@forEach
				val migrated = blockPostChainUniformsTo1216(root, file.name)
				if (migrated != root) {
					file.writeText(json.encodeToString(JsonObject.serializer(), migrated))
					changed++
				}
			}

		return changed
	}

	private fun rewritePostShaderTo1216(source: String): String {
		// A sampler is still a plain uniform on 1.21.6 and is bound by name, so it stays put.
		fun declaration(line: String): Pair<String, String>? {
			val match = postUniformDeclaration.matchEntire(line) ?: return null
			val (type, name) = match.destructured
			return if (type.startsWith("sampler")) null else type to name
		}

		val lines = source.lines()
		val declared = lines.mapNotNull { declaration(it) }.associate { (type, name) -> name to type }
		if (declared.isEmpty()) return source

		val body = lines.filter { declaration(it) == null }.joinToString("\n")
		fun used(name: String) = declared.containsKey(name) &&
			Regex("(?<![A-Za-z0-9_])" + Regex.escape(name) + "(?![A-Za-z0-9_])").containsMatchIn(body)

		val usesProjMat = used("ProjMat")
		val usesSamplerInfo = used("OutSize") || used("InSize")
		val usesTime = used("Time")
		val blocks = postUniformBlocks1216.filter { block -> block.members.any { used(it.name) } }

		val generated = buildList {
			if (usesSamplerInfo) add(samplerInfoBlock)
			blocks.forEach { block ->
				add(
					"layout(std140) uniform ${block.block} {\n" +
						block.members.joinToString("\n") { "    ${it.glsl} ${it.name};" } +
						"\n};"
				)
			}
		}

		var spliced = false
		val out = lines.mapNotNull { line ->
			if (declaration(line) == null) return@mapNotNull line
			if (spliced || generated.isEmpty()) null else generated.joinToString("\n\n").also { spliced = true }
		}.toMutableList()

		val imports = buildList {
			if (usesProjMat) add("#moj_import <minecraft:projection.glsl>")
			if (usesTime) add("#moj_import <minecraft:globals.glsl>")
		}.filterNot { out.contains(it) }
		var anchor = out.indexOfLast { it.trimStart().startsWith("#moj_import") }
		val fresh = anchor < 0
		if (fresh) anchor = out.indexOfFirst { it.trimStart().startsWith("#version") }
		out.addAll(anchor + 1, imports)
		if (usesTime) out.addAll(anchor + 1 + imports.size, hologramTimeDefine.lines())
		// A file that had no imports at all gets a blank line back under #version, the way vanilla's
		// own post shaders are laid out.
		if (fresh && imports.isNotEmpty()) out.add(anchor + 1, "")

		// Stripping whole declaration lines leaves the blank lines that framed them stacked up.
		return out.joinToString("\n").replace(Regex("\n{3,}"), "\n\n")
	}

	private fun blockPostChainUniformsTo1216(chain: JsonObject, file: String): JsonObject {
		val passes = (chain["passes"] as? JsonArray).orEmpty().filterIsInstance<JsonObject>()
		if (passes.none { it["uniforms"] is JsonArray }) return chain
		val known = postUniformBlocks1216.flatMap { it.members }.associateBy { it.name }

		val migrated = passes.map { pass ->
			val flat = (pass["uniforms"] as? JsonArray).orEmpty().filterIsInstance<JsonObject>()
				.mapNotNull { uniform ->
					val name = (uniform["name"] as? JsonPrimitive)?.content ?: return@mapNotNull null
					name to uniform
				}.toMap()
			// The only runtime uniform, and the only one that leaves rather than moving.
			val needsGlobals = flat.keys.any { it in runtimePostUniforms }
			val values = flat.filterKeys { it !in runtimePostUniforms }
			values.keys.firstOrNull { it !in known }?.let {
				error("Post uniform $it in $file has no 1.21.6 block in postUniformBlocks1216")
			}

			val uniforms = buildJsonObject {
				postUniformBlocks1216.forEach { block ->
					if (block.members.none { it.name in values }) return@forEach
					put(block.block, JsonArray(block.members.map { member ->
						val declaration = values[member.name]
							?: error("Post pass in $file sets ${block.block} but not its member ${member.name}")
						val type = (declaration["type"] as? JsonPrimitive)?.content
							?: error("Post uniform ${member.name} in $file declares no type")
						val numbers = (declaration["values"] as? JsonArray)
							?.mapNotNull { (it as? JsonPrimitive)?.doubleOrNull?.toFloat() }
							?: error("Post uniform ${member.name} in $file ships no values")
						buildJsonObject {
							// `UniformValue.CODEC` is a plain dispatch on `type` and never reads a
							// name — the position in the list is the std140 slot. Vanilla's own
							// chains carry one anyway, purely so the file can be read, and so does
							// this.
							put("name", JsonPrimitive(member.name))
							put("type", JsonPrimitive(type))
							put("value", when (type) {
								"float" -> JsonPrimitive(numbers.single())
								"int" -> JsonPrimitive(numbers.single().toInt())
								"ivec3" -> JsonArray(numbers.map { JsonPrimitive(it.toInt()) })
								else -> JsonArray(numbers.map { JsonPrimitive(it) })
							})
						}
					}))
				}
				if (needsGlobals) put("Globals", JsonArray(emptyList()))
			}

			val out = LinkedHashMap(pass)
			if (uniforms.isEmpty()) out.remove("uniforms") else out["uniforms"] = uniforms
			JsonObject(out)
		}

		val out = LinkedHashMap(chain)
		out["passes"] = JsonArray(migrated)
		return JsonObject(out)
	}

	private fun usesPostUniform(glsl: String, name: String): Boolean {
		val word = Regex("(?<![A-Za-z0-9_])" + Regex.escape(name) + "(?![A-Za-z0-9_])")
		return glsl.lineSequence()
			.filterNot { it.trimStart().startsWith("uniform") }
			.any { word.containsMatchIn(it) }
	}

	/** `{"type": "float", "count": 2}` -> `"vec2"`; the 1.21.5 `UniformType` serialized names. */
	private fun postUniformType(type: String?, count: Int): String? = when (type) {
		"matrix4x4" -> "matrix4x4"
		"int" -> when (count) {
			1 -> "int"
			3 -> "ivec3"
			else -> null
		}
		"float", null -> when (count) {
			1 -> "float"
			2 -> "vec2"
			3 -> "vec3"
			4 -> "vec4"
			else -> null
		}
		else -> null
	}

	private val ingredientFields = listOf("ingredient", "ingredients", "base", "addition", "template")

	private fun migrateRecipeIngredients(recipe: JsonObject): JsonObject {
		val out = LinkedHashMap(recipe)
		ingredientFields.forEach { field ->
			recipe[field]?.let { out[field] = convertIngredient(it) }
		}
		// crafting_shaped: `key` is a char -> ingredient map.
		(recipe["key"] as? JsonObject)?.let { key ->
			out["key"] = JsonObject(key.mapValues { convertIngredient(it.value) })
		}
		return JsonObject(out)
	}

	/**
	 * Vanilla item tags that vanilla itself deleted at 1.21.2 have no bootstrap `TagKey`, so an
	 * `Ingredient` referencing `#<that tag>` cannot be decoded during `SimpleJsonResourceReloadListener`
	 * `prepare()` (the reload's tag-bound provider doesn't know an unregistered tag) — it fails with
	 * "Missing tag" and the whole recipe silently drops. This mod's only such reference is
	 * `minecraft:music_discs`, consumed solely by the `music_disc_daze` capsid recipe; the tag contains
	 * exactly these two mod discs. `Ingredient.CODEC` accepts a bare array of item ids as a direct
	 * holder set (no tag lookup), so we inline the members here and sidestep the binding-timing problem
	 * entirely. (`data/minecraft/tags/item/music_discs.json` is still shipped for the <1.21.2 path, which
	 * uses the object `{"tag":…}` form vanilla still resolves there.)
	 */
	private val expandedTags = mapOf(
		"minecraft:music_discs" to listOf("alexsmobs:music_disc_daze", "alexsmobs:music_disc_thime")
	)

	/**
	 * `{"item": x}` -> `"x"`, `{"tag": y}` -> `"#y"` (or an inlined item array for an expanded tag),
	 * an array -> each element converted; else unchanged.
	 */
	private fun convertIngredient(node: JsonElement): JsonElement = when (node) {
		is JsonArray -> JsonArray(node.map(::convertIngredient))
		is JsonObject -> {
			val item = (node["item"] as? JsonPrimitive)?.takeIf { it.isString }
			val tag = (node["tag"] as? JsonPrimitive)?.takeIf { it.isString }
			when {
				node.size == 1 && item != null -> item
				node.size == 1 && tag != null ->
					expandedTags[tag.content]?.let { members ->
						JsonArray(members.map { JsonPrimitive(it) })
					} ?: JsonPrimitive("#" + tag.content)
				else -> node
			}
		}
		else -> node
	}

	private fun migrateRecipe(recipe: JsonObject): JsonObject = when (val result = recipe["result"]) {
		is JsonObject -> recipe.replacing("result", toComponentStack(result))
		// Cooking recipes (smelting/smoking/campfire) used to name their result as a bare item id;
		// 1.20.5 made every recipe result a full item stack.
		is JsonPrimitive -> if (result.isString) {
			recipe.replacing("result", buildJsonObject { put("id", result) })
		} else recipe
		else -> recipe
	}

	private fun migrateAdvancement(advancement: JsonObject): JsonObject {
		var out = advancement
		val display = out["display"] as? JsonObject
		val icon = display?.get("icon") as? JsonObject
		if (display != null && icon != null) {
			out = out.replacing("display", display.replacing("icon", toComponentStack(icon)))
		}
		val criteria = out["criteria"] as? JsonObject ?: return out
		return out.replacing("criteria", JsonObject(LinkedHashMap<String, JsonElement>().also { map ->
			criteria.forEach { (name, criterion) ->
				map[name] = (criterion as? JsonObject)?.let { c ->
					(c["conditions"] as? JsonObject)
						?.let { c.replacing("conditions", migrateItemPredicateFields(it)) } ?: c
				} ?: criterion
			}
		}))
	}

	/**
	 * Criterion condition fields whose value is an `ItemPredicate` (or a list of them).
	 *
	 * An allowlist for the same reason [entityPredicateFields] is one — `items` also names a plain
	 * id list elsewhere, and a shape heuristic would rewrite those too.
	 */
	private val itemPredicateFields = setOf("item", "items")

	/**
	 * MC 1.20.5 rebuilt `ItemPredicate`: the mutually exclusive `item`/`tag` pair became a single
	 * `items` holder set, spelled `"#tag"` for a tag and a bare id (or an array of ids) for items.
	 *
	 * The dangerous half is that the old spellings are not *rejected* — `ItemPredicate.CODEC` is a
	 * record codec of optional fields, so an unknown key is simply dropped and what is left decodes
	 * as a predicate with **no** conditions, which matches every stack. So `alexsmobs:banana`'s
	 * `{"tag": "alexsmobs:bananas"}` turned into "the player has any item at all" and the
	 * `inventory_changed` trigger granted "Gone Bananas" the instant a world was entered (report
	 * #31). `alexsmobs:mantis_shrimp_bucket`'s `{"item": "minecraft:water_bucket"}` had the same
	 * shape and granted on interacting with a mantis shrimp holding anything.
	 *
	 * Nothing logs, nothing fails to load, and no gate can see it — the advancement still exists and
	 * still fires, it just fires on the wrong thing.
	 *
	 * Idempotent: an already-migrated predicate has neither legacy key. Vanilla kept `count`,
	 * `components` and `predicates` alongside `items`, so those ride through untouched.
	 */
	private fun migrateItemPredicateFields(conditions: JsonObject): JsonObject =
		JsonObject(LinkedHashMap<String, JsonElement>().also { out ->
			conditions.forEach { (key, value) ->
				out[key] = if (key in itemPredicateFields) migrateItemPredicate(value) else value
			}
		})

	private fun migrateItemPredicate(node: JsonElement): JsonElement = when (node) {
		is JsonArray -> JsonArray(node.map(::migrateItemPredicate))
		is JsonObject -> {
			val item = (node["item"] as? JsonPrimitive)?.takeIf { it.isString }
			val tag = (node["tag"] as? JsonPrimitive)?.takeIf { it.isString }
			when {
				node.containsKey("items") -> node
				item != null -> node.without("item").replacing("items", item)
				tag != null -> node.without("tag").replacing("items", JsonPrimitive("#" + tag.content))
				else -> node
			}
		}
		else -> node
	}

	/** Items vanilla renamed in 1.20.5. Only the ones this mod's data actually references. */
	private val renamedItems = mapOf("minecraft:scute" to "minecraft:turtle_scute")

	/**
	 * Loot tables were componentised alongside item stacks: the NBT-shaped functions grew
	 * `custom_data` names, potions became their own function, and a nested-table entry names its
	 * target with `value` rather than `name`.
	 */
	private fun migrateLootTable(node: JsonObject): JsonObject {
		val out = LinkedHashMap<String, JsonElement>()
		val isNestedTableEntry = node.idOf("type") == "minecraft:loot_table"
		node.forEach { (key, value) ->
			val newKey = if (key == "name" && isNestedTableEntry) "value" else key
			out[newKey] = when {
				key == "name" && value is JsonPrimitive && value.isString ->
					JsonPrimitive(renamedItems[value.content] ?: value.content)
				else -> migrateLootNode(value)
			}
		}
		return migrateLootFunction(JsonObject(out))
	}

	private fun migrateLootNode(node: JsonElement): JsonElement = when (node) {
		is JsonObject -> migrateLootTable(node)
		is JsonArray -> JsonArray(node.map(::migrateLootNode))
		else -> node
	}

	private fun migrateLootFunction(node: JsonObject): JsonObject {
		val tag = (node["tag"] as? JsonPrimitive)?.takeIf { it.isString }?.content
		// idOf, not a raw read: this mod's loot tables spell most function ids without the
		// `minecraft:` prefix ("function": "set_nbt"), which an exact match silently skips.
		return when (node.idOf("function")) {
			"minecraft:set_nbt" -> {
				val parsed = tag?.let { Snbt.parse(it) as? JsonObject } ?: return node
				// The `Potion` tag became the potion_contents component, which set_custom_data
				// cannot reach — vanilla split it out into its own function.
				val potion = (parsed["Potion"] as? JsonPrimitive)?.takeIf { it.isString }
				if (potion != null && parsed.size == 1) {
					buildJsonObject {
						put("function", JsonPrimitive("minecraft:set_potion"))
						put("id", potion)
					}
				} else {
					// set_custom_data's tag is still SNBT (TagParser.LENIENT_CODEC), so it is
					// carried over as-is; only the function name moved.
					node.replacing("function", JsonPrimitive("minecraft:set_custom_data"))
				}
			}
			"minecraft:copy_nbt" -> node.replacing("function", JsonPrimitive("minecraft:copy_custom_data"))
			else -> node
		}
	}

	/** `{"item": x, "nbt": "<snbt>"}` -> `{"id": x, "components": {"minecraft:custom_data": {…}}}`. */
	private fun toComponentStack(stack: JsonObject): JsonObject {
		val item = stack["item"] ?: return stack
		return buildJsonObject {
			put("id", item)
			stack.forEach { (key, value) ->
				when (key) {
					"item", "nbt" -> {}
					else -> put(key, value)
				}
			}
			val nbt = (stack["nbt"] as? JsonPrimitive)?.takeIf { it.isString }?.content
			if (nbt != null) {
				// CustomData.CODEC is CompoundTag.CODEC.xmap(…) — it takes a JSON object, not an
				// SNBT string (checked against the 1.20.6 jar), so the tag is converted here.
				put("components", buildJsonObject { put("minecraft:custom_data", Snbt.parse(nbt)) })
			}
		}
	}

	private fun JsonObject.replacing(key: String, value: JsonElement): JsonObject =
		JsonObject(LinkedHashMap(this).also { it[key] = value })

	private fun JsonObject.without(key: String): JsonObject =
		JsonObject(LinkedHashMap(this).also { it.remove(key) })

	// ------------------------------------------------------- 26.2 entity predicates

	/**
	 * Fields of the legacy flat `EntityPredicate` record, mapped to the id they are registered under
	 * in 26.2's `ENTITY_SUB_PREDICATE_TYPE` registry.
	 *
	 * Every one of those registered codecs is an `xmap` over exactly the codec the old flat field
	 * used (verified against 26.2's `EntitySubPredicates.bootstrap` and each predicate class), so
	 * apart from the `type` -> `entity_type` rename this is a pure key rewrite — no value shape
	 * changes. `location`/`stepping_on`/`movement_affected_by` used to be inlined by the
	 * `LocationWrapper` sub-codec and `components`/`predicates` by `DataComponentMatchers`, but at
	 * the same names, so they map straight through too.
	 */
	private val entitySubPredicateKeys = mapOf(
		"type" to "entity_type",
		"distance" to "distance",
		"movement" to "movement",
		"location" to "location",
		"stepping_on" to "stepping_on",
		"movement_affected_by" to "movement_affected_by",
		"effects" to "effects",
		"nbt" to "nbt",
		"flags" to "flags",
		"equipment" to "equipment",
		"periodic_tick" to "periodic_tick",
		"vehicle" to "vehicle",
		"passenger" to "passenger",
		"targeted_entity" to "targeted_entity",
		"team" to "team",
		"slots" to "slots",
		"components" to "components",
		"predicates" to "predicates",
	)

	/** The sub-predicate values that are themselves an `EntityPredicate`, so recurse into them. */
	private val nestedEntityPredicateKeys = setOf("vehicle", "passenger", "targeted_entity")

	/**
	 * Advancement-criterion condition fields whose value is an `EntityPredicate`.
	 *
	 * Deliberately an allowlist rather than a shape heuristic: sibling fields of a criterion carry
	 * other predicate types with overlapping key names (`minecraft:effects_changed` has an `effects`
	 * field that is a `MobEffectsPredicate`, and an `ItemPredicate` has `components`/`predicates`),
	 * so anything structural would rewrite them too. `entity`, `child` and `player` are what this
	 * mod uses; the rest are the other vanilla trigger fields, listed so a new advancement does not
	 * quietly go unmigrated.
	 */
	private val entityPredicateFields = setOf(
		"entity", "player", "child", "parent", "partner", "victim", "attacker", "source",
		"zombie", "villager", "lightning", "bystander", "projectile", "shooter", "owner",
		"source_entity", "direct_entity",
	)

	/**
	 * MC 26.2 rewrote `EntityPredicate` from a flat record into
	 * `Codec.dispatchedMap(BuiltInRegistries.ENTITY_SUB_PREDICATE_TYPE.byNameCodec(), c -> c)` —
	 * every key of the object is now a sub-predicate *registry id*, and the value is that
	 * sub-predicate's own payload.
	 *
	 * So the old `"type"` field is read as a sub-predicate named `minecraft:type`, which does not
	 * exist, and the whole file fails to decode:
	 *
	 *     Couldn't parse data file 'alexsmobs:alexsmobs/alligator_snapping_turtle' …
	 *     Unknown registry key in ResourceKey[minecraft:root / minecraft:entity_sub_predicate_type]:
	 *     minecraft:type
	 *
	 * That is logged and not thrown, so the server still reaches `Done (` while 42 of this mod's
	 * advancements silently do not exist. `type` becomes `entity_type`; every other legacy field
	 * keeps its name (see [entitySubPredicateKeys]).
	 *
	 * The one structural change is `type_specific`, which used to be a nested dispatch on its own
	 * `type` field and is now flattened into the outer map under `type_specific/<type>` — e.g.
	 * `{"type_specific": {"type": "player", "looking_at": …}}` becomes
	 * `{"type_specific/player": {"looking_at": …}}`.
	 *
	 * Entity predicates are reached from two places, both of which this mod uses: an advancement
	 * criterion's condition fields, and the `predicate` of a `minecraft:entity_properties` loot
	 * condition (which is how the two spyglass advancements express "looking at a bison"). Both are
	 * matched by context rather than by shape — see [entityPredicateFields].
	 *
	 * Idempotent: only the legacy spellings are rewritten, and every key except `type` and
	 * `type_specific` is its own replacement.
	 */
	fun migrateEntityPredicatesTo262(resourcesRoot: File): Int {
		val data = resourcesRoot.resolve("data")
		if (!data.isDirectory) return 0
		var changed = 0
		data.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
			val original = runCatching { json.parseToJsonElement(file.readText()) }.getOrNull()
				?: return@forEach
			val migrated = rewriteEntityPredicateHosts(original)
			if (migrated != original) {
				file.writeText(json.encodeToString(JsonElement.serializer(), migrated))
				changed++
			}
		}
		return changed
	}

	/** Walks a whole document, migrating every entity predicate it can positively identify. */
	private fun rewriteEntityPredicateHosts(node: JsonElement): JsonElement = when (node) {
		is JsonArray -> JsonArray(node.map(::rewriteEntityPredicateHosts))
		is JsonObject -> {
			val mapped = JsonObject(LinkedHashMap<String, JsonElement>().also { out ->
				node.forEach { (key, value) -> out[key] = rewriteEntityPredicateHosts(value) }
			})
			when {
				// A loot condition. `predicate` names an ItemPredicate on match_tool and a
				// DamageSourcePredicate on damage_source_properties, so the id has to be checked.
				mapped.idOf("condition") == "minecraft:entity_properties" ->
					(mapped["predicate"] as? JsonObject)
						?.let { mapped.replacing("predicate", migrateEntityPredicate(it)) } ?: mapped
				// An advancement criterion.
				mapped["trigger"] is JsonPrimitive && mapped["conditions"] is JsonObject ->
					mapped.replacing("conditions", migrateCriterionConditions(mapped["conditions"] as JsonObject))
				else -> mapped
			}
		}
		else -> node
	}

	private fun migrateCriterionConditions(conditions: JsonObject): JsonObject =
		JsonObject(LinkedHashMap<String, JsonElement>().also { out ->
			conditions.forEach { (key, value) ->
				// A JsonArray here is a ContextAwarePredicate (a list of loot conditions), which the
				// entity_properties branch above has already handled.
				out[key] = if (key in entityPredicateFields && value is JsonObject)
					migrateEntityPredicate(value) else value
			}
		})

	private fun migrateEntityPredicate(predicate: JsonObject): JsonObject =
		JsonObject(LinkedHashMap<String, JsonElement>().also { out ->
			predicate.forEach { (key, value) ->
				when {
					key == "type_specific" && value is JsonObject -> {
						// The nested dispatch key moves into the outer map's key and out of the value.
						val type = (value["type"] as? JsonPrimitive)?.takeIf { it.isString }?.content
							?.substringAfter(':')
						if (type == null) {
							out[key] = value
						} else {
							out["type_specific/$type"] = migrateTypeSpecific(type, value)
						}
					}
					key in nestedEntityPredicateKeys && value is JsonObject ->
						out[entitySubPredicateKeys.getValue(key)] = migrateEntityPredicate(value)
					else -> out[entitySubPredicateKeys[key] ?: key] = value
				}
			}
		})

	/** Strips the dispatch key, and migrates the one sub-predicate field that nests a predicate. */
	private fun migrateTypeSpecific(type: String, value: JsonObject): JsonObject =
		JsonObject(LinkedHashMap<String, JsonElement>().also { out ->
			value.forEach { (key, nested) ->
				when {
					key == "type" -> {}
					// PlayerPredicate#lookingAt is an EntityPredicate.
					type == "player" && key == "looking_at" && nested is JsonObject ->
						out[key] = migrateEntityPredicate(nested)
					else -> out[key] = nested
				}
			}
		})

	// ---------------------------------------------------------------- worldgen

	/**
	 * MC 1.20.5 turned the int-provider codecs into `MapCodec`s, which changes their JSON shape.
	 *
	 * `IntProvider` is a `Codec.dispatch("type", …)`, and DFU's `KeyDispatchCodec` inlines the
	 * dispatched fields only when the element codec is a `MapCodecCodec`. Up to 1.20.4 `UniformInt.
	 * CODEC` was `RecordCodecBuilder.create(…).validate(…)` — the `validate` wrapper erases the
	 * MapCodec-ness — so the fields had to be nested under `value`. 1.20.5 rebuilt it as
	 * `RecordCodecBuilder.mapCodec(…).validate(…)`, which stays a MapCodec, so the same nesting is
	 * now a hard parse error and the whole registry load fails:
	 *
	 *     Failed to parse alexscaves:worldgen/placed_feature/acid_lake.json …
	 *     Not a number: {"type":"minecraft:uniform","value":{"max_inclusive":5,"min_inclusive":1}};
	 *     No key max_inclusive in MapLike[…]
	 *
	 * 39 of this mod's placed features are affected (38 `count` placements plus one `weighted_list`
	 * entry). Matching is on the exact `{type, value}` two-key shape a legacy dispatch always
	 * produces, and only for the int-provider types — the neighbouring height providers were already
	 * MapCodecs in 1.20.1 and are written inline in the very same files.
	 *
	 * Idempotent: an already-flattened provider has no `value` key to move.
	 */
	fun flattenIntProvidersTo1205(resourcesRoot: File): Int =
		rewriteWorldgen(resourcesRoot, ::flattenIntProviderNode)

	/** The int-provider types this mod uses. Anything else keeps its wrapper — see the note above. */
	private val wrappedIntProviders = setOf("minecraft:uniform")

	private fun flattenIntProviderNode(node: JsonElement): JsonElement = when (node) {
		is JsonArray -> JsonArray(node.map(::flattenIntProviderNode))
		is JsonObject -> {
			val mapped = JsonObject(LinkedHashMap<String, JsonElement>().also { out ->
				node.forEach { (key, value) -> out[key] = flattenIntProviderNode(value) }
			})
			val value = mapped["value"]
			if (mapped.keys == setOf("type", "value") && mapped.idOf("type") in wrappedIntProviders &&
				value is JsonObject
			) {
				JsonObject(LinkedHashMap<String, JsonElement>().also { out ->
					out["type"] = mapped.getValue("type")
					value.forEach { (key, nested) -> out[key] = nested }
				})
			} else {
				mapped
			}
		}
		else -> node
	}

	/**
	 * MC 1.20.3 renamed the block `minecraft:grass` to `minecraft:short_grass` (`grass_block` was
	 * always a separate id and is untouched). This mod names it once, as a weighted state provider
	 * entry in the primordial-caves grass patch, where the stale id is fatal rather than cosmetic:
	 *
	 *     Unknown registry key in ResourceKey[minecraft:root / minecraft:block]: minecraft:grass
	 *
	 * Applied to the whole data tree, not just worldgen, so a loot table or tag naming the block
	 * would be caught too — the id is unambiguous.
	 */
	fun renameShortGrassTo1203(resourcesRoot: File): Int {
		val data = resourcesRoot.resolve("data")
		if (!data.isDirectory) return 0
		var changed = 0
		data.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
			val text = file.readText()
			// A whole-token replace: "minecraft:grass_block" and "minecraft:grass_path" must not match,
			// and the closing quote is what guarantees that.
			val renamed = text.replace("\"minecraft:grass\"", "\"minecraft:short_grass\"")
			if (renamed != text) {
				file.writeText(renamed)
				changed++
			}
		}
		return changed
	}

	/**
	 * Renames `minecraft:chain` to `minecraft:iron_chain`, from 1.21.9.
	 *
	 * The copper update gave chains a second metal, so vanilla's single `chain` became the pair
	 * `iron_chain` / `copper_chain` — a plain rename of the existing block *and* item, confirmed
	 * against `Blocks`/`Items` in the 1.21.9 jar. Six of this mod's files name it: two crafting
	 * recipes for hanging signs, the quarry smasher recipe, the boundroid loot table, and both
	 * ferromagnetic tags. Nothing gains the copper chain — it is not iron, so it is not
	 * ferromagnetic, and the recipes are upstream's.
	 *
	 * Whole-token like the short-grass rename above: the closing quote keeps
	 * `minecraft:chain_command_block` out of it.
	 */
	fun renameIronChainTo1219(resourcesRoot: File): Int {
		val data = resourcesRoot.resolve("data")
		if (!data.isDirectory) return 0
		var changed = 0
		data.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
			val text = file.readText()
			val renamed = text.replace("\"minecraft:chain\"", "\"minecraft:iron_chain\"")
			if (renamed != text) {
				file.writeText(renamed)
				changed++
			}
		}
		return changed
	}

	/**
	 * Drops the two `exclusion_zone`s from this mod's structure sets, on every node.
	 *
	 * `StructurePlacement.ExclusionZone#otherSet` is a single `Holder<StructureSet>` decoded by a
	 * `RegistryFileCodec` built with `allowInline = false`, i.e. one structure-set id and nothing
	 * else. Upstream wrote a *tag* there (`#alexscaves:licowitch_tower_generates_far_from`, four
	 * entries), which vanilla has never been able to read. On 1.20.1 the field sits behind a lenient
	 * `optionalFieldOf`, so the decode error is swallowed and the exclusion zone silently does not
	 * exist; from 1.20.5, where optional fields became strict, the same file is a fatal
	 * "Inline definitions not allowed here" that takes the whole registry load down with it.
	 *
	 * So this is not a behaviour change — dropping the field is what 1.20.1 already does at runtime,
	 * and doing it unconditionally keeps every node's worldgen identical to the baseline's. Picking
	 * one of the tag's four entries instead would be the only parseable alternative, and it would
	 * make the upper nodes generate differently from the version this mod is ported from.
	 */
	fun dropUnreadableExclusionZones(resourcesRoot: File): Int {
		val data = resourcesRoot.resolve("data")
		if (!data.isDirectory) return 0
		val sets = data.walkTopDown()
			.filter { it.isFile && it.extension == "json" && it.parentFile.name == "structure_set" }
		var changed = 0
		sets.forEach { file ->
			val original = runCatching { json.parseToJsonElement(file.readText()) as? JsonObject }
				.getOrNull() ?: return@forEach
			val placement = original["placement"] as? JsonObject ?: return@forEach
			if ("exclusion_zone" !in placement) return@forEach
			val migrated = original.replacing("placement", placement.without("exclusion_zone"))
			file.writeText(json.encodeToString(JsonObject.serializer(), migrated))
			changed++
		}
		return changed
	}

	/** Runs [transform] over every worldgen JSON under `data/`, returning the number rewritten. */
	private fun rewriteWorldgen(resourcesRoot: File, transform: (JsonElement) -> JsonElement): Int {
		val data = resourcesRoot.resolve("data")
		if (!data.isDirectory) return 0
		var changed = 0
		data.walkTopDown()
			.filter { it.isFile && it.extension == "json" && it.invariantSeparatorsPath.contains("/worldgen/") }
			.forEach { file ->
				val original = runCatching { json.parseToJsonElement(file.readText()) }.getOrNull()
					?: return@forEach
				val migrated = transform(original)
				if (migrated != original) {
					file.writeText(json.encodeToString(JsonElement.serializer(), migrated))
					changed++
				}
			}
		return changed
	}

	// ---------------------------------------------------------------- NeoForge

	/**
	 * The source tree is written for Forge, so everything Forge-namespaced has to be re-pointed for
	 * a NeoForge node. Three separate things, only the last of which is version-dependent:
	 *
	 *  - NeoForge reads its registries out of `data/<ns>/neoforge/…` and its global loot modifiers
	 *    out of `data/neoforge/loot_modifiers/…`. Left under `forge/` they are simply never read —
	 *    silently, with no log line — so the biome/structure modifiers (i.e. all mob spawning) and
	 *    the four global loot modifiers do nothing.
	 *  - Its loot condition is registered as `neoforge:loot_table_id`.
	 *  - NeoForge 1.20.5 moved the cross-mod convention tags from `forge:` to the loader-neutral
	 *    `c:` namespace, renaming a handful of them on the way.
	 *
	 * @param conventionTags whether this node wants the `c:` tag namespace (NeoForge >= 1.20.5).
	 * @param indexedLootModifiers whether this node still reads the `global_loot_modifiers.json`
	 *   index (NeoForge < 26). NeoForge 26's `LootModifierManager` dropped it: it is a plain
	 *   `SimpleJsonResourceReloadListener` over every json under `loot_modifiers` in every namespace,
	 *   decoded with `IGlobalLootModifier.DIRECT_CODEC`. So the index file is itself scanned as a
	 *   modifier and fails to parse ("No key type in MapLike[…]"), and shipping it does nothing but
	 *   log that error — the four modifiers are picked up directly, ordered by their `priority`
	 *   field (optional, default 1000). Forge 26 still reads the index, so this is NeoForge-only.
	 */
	fun migrateNeoForge(resourcesRoot: File, conventionTags: Boolean, indexedLootModifiers: Boolean = true): Int {
		val data = resourcesRoot.resolve("data")
		if (!data.isDirectory) return 0
		var changed = 0

		if (!indexedLootModifiers) {
			val index = data.resolve("forge/loot_modifiers/global_loot_modifiers.json")
			if (index.isFile && index.delete()) {
				changed++
				index.parentFile.takeIf { it.listFiles().isNullOrEmpty() }?.delete()
			}
		}

		// data/forge/tags -> data/c/tags, everything else under data/forge -> data/neoforge.
		// Before 1.20.5 the convention tags still lived in `forge:`, so they stay put there.
		data.resolve("forge").listFiles().orEmpty().forEach { dir ->
			val target = when {
				dir.name != "tags" -> "neoforge"
				conventionTags -> "c"
				else -> return@forEach
			}
			changed += relocate(dir, data.resolve(target).resolve(dir.name))
		}
		data.resolve("forge").takeIf { it.isDirectory && it.listFiles().isNullOrEmpty() }?.delete()

		// data/<namespace>/forge/<registry> -> data/<namespace>/neoforge/<registry>
		data.listFiles().orEmpty().filter { it.isDirectory && it.name != "forge" }.forEach { namespace ->
			val forgeDir = namespace.resolve("forge")
			if (!forgeDir.isDirectory) return@forEach
			forgeDir.listFiles().orEmpty().forEach { registry ->
				changed += relocate(registry, namespace.resolve("neoforge").resolve(registry.name))
			}
			forgeDir.delete()
		}

		data.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
			val original = file.readText()
			var text = forgeNamespace.replace(original) { m ->
				if (m.groupValues[1] in nonTagForgeIds) "neoforge:" + m.groupValues[1] else m.value
			}
			if (conventionTags) {
				text = forgeNamespace.replace(text) { "c:" + (renamedTags[it.groupValues[1]] ?: it.groupValues[1]) }
			}
			if (text != original) {
				file.writeText(text)
				changed++
			}
		}
		return changed
	}

	/** Matches a `forge:` resource location but not the `neoforge:` one it is a suffix of. */
	private val forgeNamespace = Regex("""(?<![A-Za-z0-9_])forge:([a-z0-9_./-]+)""")

	/**
	 * The Forge-namespaced ids in this tree that are **not** tags. Two passes read this set and they
	 * want it for opposite reasons: [migrateNeoForge] re-points exactly these because NeoForge
	 * re-registered them under its own namespace, and [migrateConventionTags] leaves exactly these
	 * alone because the `c:` sweep is a **tag** sweep — Forge 26 moved its tags and kept every one of
	 * these where it was (`ForgeMod`'s `<clinit>` on 65.1.0 still spells the three holder-set types
	 * `forge:`). Everything else written `forge:` in this tree is a convention tag, which is why this
	 * is a whitelist rather than a blanket rename.
	 *
	 *  - `loot_table_id` is the global-loot-modifier condition (the four cave-tablet modifiers).
	 *  - `and` / `not` are HolderSetType ids. `underground_cabin.json` builds its biome set out of
	 *    them, and an unknown one is fatal, not skipped: the whole `RegistryDataLoader` pass dies with
	 *    *"Unknown registry key in ResourceKey[minecraft:root / neoforge:holder_set_type]: forge:and"*
	 *    and the server never starts. `or` is here for symmetry — NeoForgeMod registers all three.
	 *
	 * Matching goes through [forgeNamespace] rather than a plain string replace so that the greedy
	 * path group makes `forge:and` a non-match inside a hypothetical `forge:andesite` tag.
	 */
	private val nonTagForgeIds = setOf("loot_table_id", "and", "or", "not")

	/**
	 * Convention tags this mod references that did not keep their path when they moved to `c:`.
	 * Anything absent from this map keeps its path (`ores`, `seeds`, `eggs`, `ingots/iron`, …), as do
	 * the tags this mod defines itself (`heart`, `armors/boots`, `crops/rice`, …) — those are only ever
	 * read back by this mod, so they just follow the definition into `c:`.
	 */
	private val renamedTags = mapOf(
		"sand" to "sands",
		"string" to "strings",
		"glass" to "glass_blocks",
		// Matching is exact-path, so every sub-tag of a renamed tag has to be listed as well.
		"glass/colorless" to "glass_blocks/colorless",
		"gravel" to "gravels",
		"concrete" to "concretes",
		"is_dense/overworld" to "is_dense_vegetation/overworld",
		"is_coniferous" to "is_tree/coniferous",
	)

	/**
	 * **Forge 26 followed NeoForge into the `c:` namespace.** Its `Tags` class is almost entirely
	 * `cTag(...)` now — only a handful of genuinely Forge-specific entries (`enderman_place_on_blacklist`,
	 * `needs_wood_tool`, …) are still `forgeTag(...)` — and the tag names match NeoForge's exactly,
	 * renames included (`sands`, `strings`, `glass_blocks`, `gravels`, `is_dense_vegetation/overworld`,
	 * `is_tree/coniferous`, `dyes/green` via `DyeColor#getTag`). So a Forge >= 26 node needs the same
	 * convention-tag pass a NeoForge >= 1.20.5 node gets — and ONLY that half: `forge:loot_table_id`,
	 * `data/forge/loot_modifiers/global_loot_modifiers.json` and `data/<ns>/forge/<registry>` are all
	 * still read under `forge:` there, which is why this cannot just call [migrateNeoForge].
	 *
	 * Without it every `#forge:` reference silently resolves to nothing. Found by the boot gate on
	 * `26.1.2-forge` (Milestone 13): 25 `Couldn't load tag` lines (`forge:ores`, `forge:seeds`,
	 * `forge:sand`, `forge:is_sandy`, …, cascading into every `*_spawns` tag) and 11
	 * `Couldn't parse data file` lines (`Missing tag: 'forge:rods/wooden'`), i.e. most of the mod's
	 * spawning and a tenth of its recipes, gone. Compiles and boots clean either way.
	 */
	fun migrateConventionTags(resourcesRoot: File): Int {
		val data = resourcesRoot.resolve("data")
		if (!data.isDirectory) return 0
		var changed = relocate(data.resolve("forge/tags"), data.resolve("c/tags"))
		data.resolve("forge").takeIf { it.isDirectory && it.listFiles().isNullOrEmpty() }?.delete()

		data.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
			val original = file.readText()
			val text = forgeNamespace.replace(original) { match ->
				val path = match.groupValues[1]
				// Only the convention TAGS moved. Forge's loot-modifier condition and its three
				// holder-set types are still registered under `forge:` on 26 — and rewriting the
				// latter is FATAL rather than silent, exactly as it was on NeoForge: an unknown
				// holder-set type takes the whole RegistryDataLoader pass down and the server never
				// starts. See nonTagForgeIds.
				if (path in nonTagForgeIds) match.value else "c:" + (renamedTags[path] ?: path)
			}
			if (text != original) {
				file.writeText(text)
				changed++
			}
		}
		return changed
	}

	/**
	 * **MC 26.2 gave `MobCategory` a sixth constructor parameter, and FML matches an enum extension
	 * by DESCRIPTOR.**
	 *
	 * The new argument is the short code the F3 mob-count readout prints (`MO`, `C`, `UWC`, …),
	 * inserted straight after the serialized name. Java-side both arms of `ACMobCategories` pass it
	 * through the `!mc262-mobcategory-*` replacement rules — but `META-INF/enumextensions.json` is a
	 * resource, so nothing preprocesses it, and its `constructor` string still named the five-argument
	 * form. FML resolves the entry by that exact descriptor and refuses the whole extension when it
	 * misses: *"Invalid, non-existant or disallowed constructor '(Ljava/lang/String;IZZI)V' for field
	 * 'ALEXSCAVES_CAVE_CREATURE'"*, thrown while `MobCategory` is being transformed — which then
	 * poisons `EntityTypes`' initialiser (`NoClassDefFoundError: Could not initialize class`) and the
	 * server dies with *"Couldn't find Minecraft server thread"*. A boot failure two exceptions
	 * removed from its cause, and the mod's own name appears only in the first one.
	 *
	 * Only NeoForge reads this file at all (Forge never adopted the mechanism and keeps its patched
	 * `MobCategory.create`), so this is scoped exactly as the manifest key is — see
	 * `Loader.NeoForge.enumExtensionsKey`.
	 *
	 * Idempotent: it matches the old descriptor, which the rewritten text no longer contains.
	 */
	fun retargetEnumExtensionsTo1262(resourcesRoot: File): Int {
		val file = resourcesRoot.resolve("META-INF/enumextensions.json")
		if (!file.isFile) return 0
		val original = file.readText()
		val old = """"constructor": "(Ljava/lang/String;IZZI)V""""
		val text = original.replace(old, """"constructor": "(Ljava/lang/String;Ljava/lang/String;IZZI)V"""")
		if (text == original) return 0
		file.writeText(text)
		return original.windowed(old.length).count { it == old }
	}

	/**
	 * **Fabric has no composite-`HolderSet` mechanism at all**, so a biome set built out of Forge's
	 * `and`/`or`/`not` has to be flattened away rather than re-namespaced.
	 *
	 * Vanilla's `HolderSetCodec` reads exactly two shapes — a `"#tag"` string or a list of ids — and
	 * both Forge and NeoForge widen it with a `holder_set_type` registry so a set can also be an
	 * object with a `"type"`. Fabric API does not, and there is nothing to add one to: the widening
	 * is a loader patch on the vanilla codec, not an extension point. So on Fabric the object form is
	 * simply not a set, and the failure is the fatal kind — `underground_cabin.json` reported
	 * *"Not a string: {"type":"c:and", …}"*, which left `alexscaves:underground_cabin` unbound in
	 * `minecraft:worldgen/structure` and killed the whole registry load before the server started.
	 * (`c:` rather than `forge:` in that message because [migrateConventionTags] used to rewrite the
	 * type ids as well; it no longer does — see nonTagForgeIds — but the shape is unreadable either
	 * way.)
	 *
	 * **What flattening costs.** The one composite in this tree is
	 * `and(#alexscaves:has_underground_cabins, not(#alexscaves:has_no_underground_cabins))`, i.e.
	 * "the vanilla stronghold biomes, except this mod's six cave biomes". The negative clause is
	 * defensive: `has_underground_cabins` is `#minecraft:stronghold_biased_to`, this mod ships no
	 * override of that vanilla tag, and its own cave biomes are therefore not in it — so the
	 * intersection equals the positive tag on its own and the cabins generate in exactly the same
	 * places. What is genuinely lost is only the guard against a *third party* adding an AC cave
	 * biome to a vanilla surface-biome tag, and no shape available on this loader can express it.
	 *
	 * **Deliberately not a lenient pass.** Anything it cannot flatten to a single surviving positive
	 * member is a hard build failure naming the file, because the alternative — dropping the field,
	 * or picking a member — is a worldgen change nobody would see until it shipped. A second
	 * composite added later fails the Fabric build rather than silently generating somewhere else.
	 */
	fun flattenCompositeHolderSets(resourcesRoot: File): Int {
		val data = resourcesRoot.resolve("data")
		if (!data.isDirectory) return 0
		var changed = 0
		data.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
			val original = runCatching { json.parseToJsonElement(file.readText()) }.getOrNull() ?: return@forEach
			val flattened = rewriteHolderSets(original, file)
			if (flattened != original) {
				file.writeText(json.encodeToString(JsonElement.serializer(), flattened))
				changed++
			}
		}
		return changed
	}

	/**
	 * The composite `HolderSet` types Forge and NeoForge register, by path. `any` is listed so that
	 * it is *rejected* explicitly rather than walked into as an ordinary object.
	 */
	private val compositeHolderSetTypes = setOf("and", "or", "not", "any")

	/** The composite type's path, or null when [node] is not a composite `HolderSet` at all. */
	private fun compositeType(node: JsonElement): String? {
		val type = ((node as? JsonObject)?.get("type") as? JsonPrimitive)?.takeIf { it.isString }?.content ?: return null
		val colon = type.indexOf(':')
		if (colon < 0) return null
		val namespace = type.substring(0, colon)
		val path = type.substring(colon + 1)
		// Every namespace the id can have worn by the time this runs: as authored, as NeoForge
		// re-points it, and as the convention sweep used to rewrite it.
		return path.takeIf { namespace in setOf("forge", "neoforge", "c") && it in compositeHolderSetTypes }
	}

	private fun rewriteHolderSets(node: JsonElement, file: File): JsonElement = when {
		compositeType(node) != null -> positiveOf(node, file)
			?: error("Cannot flatten the composite HolderSet in ${file.name}: it resolves to nothing at all")
		node is JsonArray -> JsonArray(node.map { rewriteHolderSets(it, file) })
		node is JsonObject -> JsonObject(node.mapValues { rewriteHolderSets(it.value, file) })
		else -> node
	}

	/**
	 * The single positive member a composite reduces to, or null for a pure negative — which is what
	 * makes `and(X, not(Y))` come out as `X` and lets a bare `not` be dropped by its parent.
	 */
	private fun positiveOf(node: JsonElement, file: File): JsonElement? {
		val type = compositeType(node) ?: return rewriteHolderSets(node, file)
		val obj = node as JsonObject
		return when (type) {
			"not" -> null
			"and", "or" -> {
				val values = obj["values"] as? JsonArray
					?: error("Composite HolderSet `$type` in ${file.name} has no `values` array")
				val kept = values.mapNotNull { positiveOf(it, file) }
				kept.singleOrNull()
					?: error(
						"Cannot flatten `$type` of ${kept.size} positive members in ${file.name} on Fabric — " +
							"vanilla's HolderSet codec reads one tag or one list of ids, and nothing on this " +
							"loader composes them. Express the set as a single tag instead."
					)
			}
			else -> error("Composite HolderSet type `$type` in ${file.name} has no Fabric equivalent")
		}
	}

	/**
	 * **`c:` is a namespace, not a library — on Fabric NOBODY is obliged to define the tag you read.**
	 *
	 * On Forge and NeoForge the loader itself ships every `Tags` entry, so a `#forge:`/`#c:` reference
	 * always resolves. On Fabric the convention tags come from an *optional* Fabric API module, and which
	 * ones exist depends on the fabric-api build: v1 (the only module below 1.20.6) defines 156 `c:` tags
	 * and none of the seven below; v2 grew the rest in over a year of releases, so `c:is_sandy` first
	 * appears somewhere between the 1.21 and 1.21.1 pins. Referencing one that does not exist is not a
	 * crash — it is a logged `Couldn't load tag`, and the referencing tag loads EMPTY, which then cascades
	 * (`c:sands` alone takes out `alexsmobs:am_spawns` and the fifteen `*_spawns` tags built on it).
	 * The boot gate caught it on `1.20.1-fabric`; `scripts/verify_convention_tags.py` diffs every Fabric
	 * node's references against its pinned fabric-api jar so the next one is caught before a run.
	 *
	 * So the mod defines them itself, on **every** Fabric node — not just the ones whose pinned fabric-api
	 * is missing them. A shipped jar meets whatever fabric-api the player installed, which may be older
	 * than the pin, and tag JSONs *merge*: where the module already defines the tag the two are unioned,
	 * and the values here are copied from fabric-api's own v2 definitions (flattened past the `#c:sands/…`
	 * sub-tag indirection, which is itself version-dependent), so the union is the module's own set.
	 *
	 * Written in the PLURAL folders and before [migrateTo121], so the singular rename picks them up.
	 */
	fun backfillFabricConventionTags(resourcesRoot: File): Int {
		val tags = resourcesRoot.resolve("data/c/tags")
		var written = 0
		fabricConventionBackfill.forEach { (path, values) ->
			val file = tags.resolve("$path.json")
			if (file.exists()) return@forEach
			file.parentFile.mkdirs()
			file.writeText(values.joinToString(
				separator = ",\n    ",
				prefix = "{\n  \"values\": [\n    ",
				postfix = "\n  ]\n}\n",
			) { "\"$it\"" })
			written++
		}
		return written
	}

	/** Tag path (under `data/c/tags/`) to the vanilla ids fabric-api's own v2 module puts in it. */
	private val fabricConventionBackfill = mapOf(
		"blocks/sands" to listOf("minecraft:sand", "minecraft:red_sand"),
		"blocks/gravels" to listOf("minecraft:gravel"),
		"items/seeds" to listOf(
			"minecraft:wheat_seeds", "minecraft:beetroot_seeds", "minecraft:melon_seeds",
			"minecraft:pumpkin_seeds", "minecraft:torchflower_seeds", "minecraft:pitcher_pod",
		),
		"items/crops/carrot" to listOf("minecraft:carrot"),
		"worldgen/biome/is_sandy" to listOf(
			"minecraft:desert", "minecraft:badlands", "minecraft:wooded_badlands",
			"minecraft:eroded_badlands", "minecraft:beach",
		),
		"worldgen/biome/is_swamp" to listOf("minecraft:swamp", "minecraft:mangrove_swamp"),
		"worldgen/biome/is_snowy" to listOf(
			"minecraft:snowy_beach", "minecraft:snowy_plains", "minecraft:ice_spikes",
			"minecraft:snowy_taiga", "minecraft:grove", "minecraft:snowy_slopes",
			"minecraft:jagged_peaks", "minecraft:frozen_peaks",
		),
		// The eight below (plus is_snowy and is_swamp above) are what the cave map's biome colours
		// read through the Fabric `Tags` stand-in, and the shear tag is the nuclear bomb's
		// wire-cutting check. Same policy as the rest of this map — v2's own values, flattened past
		// its `#c:` sub-tag indirection so a node whose module predates a sub-tag still resolves.
		//
		// Two vanilla tag references survive the flattening (`#minecraft:is_ocean`,
		// `#minecraft:is_taiga`, `#minecraft:is_mountain`): those are vanilla's, not the module's, so
		// they exist on every node by definition. And `minecraft:pale_garden` — which v2 puts in
		// is_rare and is_spooky — is deliberately dropped: it does not exist below 1.21.4, an entry
		// naming an absent biome takes the whole tag out, and on the nodes where it does exist the
		// module's own copy supplies it through the merge.
		"worldgen/biome/is_aquatic" to listOf("#minecraft:is_ocean", "#minecraft:is_river"),
		"worldgen/biome/is_desert" to listOf("minecraft:desert"),
		"worldgen/biome/is_mountain" to listOf(
			"#minecraft:is_mountain", "minecraft:frozen_peaks", "minecraft:jagged_peaks",
			"minecraft:stony_peaks", "minecraft:snowy_slopes", "minecraft:meadow",
			"minecraft:grove", "minecraft:cherry_grove",
		),
		"worldgen/biome/is_tree/coniferous" to listOf("#minecraft:is_taiga", "minecraft:grove"),
		"worldgen/biome/is_rare" to listOf(
			"minecraft:sunflower_plains", "minecraft:flower_forest",
			"minecraft:old_growth_birch_forest", "minecraft:old_growth_spruce_taiga",
			"minecraft:bamboo_jungle", "minecraft:sparse_jungle", "minecraft:eroded_badlands",
			"minecraft:savanna_plateau", "minecraft:windswept_savanna", "minecraft:ice_spikes",
			"minecraft:windswept_gravelly_hills", "minecraft:mushroom_fields",
			"minecraft:deep_dark",
		),
		"worldgen/biome/is_mushroom" to listOf("minecraft:mushroom_fields"),
		"worldgen/biome/is_spooky" to listOf("minecraft:dark_forest", "minecraft:deep_dark"),
		"worldgen/biome/is_plains" to listOf("minecraft:plains", "minecraft:sunflower_plains"),
		"items/tools/shear" to listOf("minecraft:shears"),
	)

	/**
	 * **Fabric only.** Removes the seven files under `data/<modId>/loot_modifiers/` and the
	 * `data/forge/loot_modifiers/global_loot_modifiers.json` index that names them.
	 *
	 * There is no global-loot-modifier system on this loader — no registry, no manager, no id on the
	 * table — so nothing reads any of the eight. They are not merely inert: the index names a Forge
	 * registry key, and each modifier file is keyed by a `forge:` serializer id and carries a
	 * `forge:loot_table_id` condition, none of which exists here. Shipping them would be dead weight
	 * that reads like a working feature.
	 *
	 * The feature itself is not lost. Every one of the seven carries nothing but `forge:loot_table_id`
	 * conditions, so their whole content is a (table id -> modifier) mapping; `ACFabricLootModifiers`
	 * is that mapping written in Java, resolved against the live tables from
	 * `LootTableEvents.ALL_LOADED` and applied by `mixin.fabric.LootTableModifierMixin` at the tail of
	 * `LootTable#getRandomItems(LootContext)` — the same method Forge rewrites. ⚠️ **The two are
	 * therefore coupled**: adding, removing or re-pointing a modifier file means editing that class in
	 * the same commit, or this loader silently diverges from the other two.
	 *
	 * Returns the number of files deleted.
	 */
	fun dropForgeLootModifiers(resourcesRoot: File, modId: String): Int {
		val data = resourcesRoot.resolve("data")
		if (!data.isDirectory) return 0
		var dropped = 0
		listOf(
			data.resolve("$modId/loot_modifiers"),
			data.resolve("forge/loot_modifiers"),
		).forEach { root ->
			if (!root.isDirectory) return@forEach
			root.walkTopDown().filter { it.isFile }.toList().forEach { if (it.delete()) dropped++ }
			root.walkBottomUp().filter { it.isDirectory }.forEach { it.delete() }
		}
		data.resolve("forge").takeIf { it.isDirectory && it.listFiles().isNullOrEmpty() }?.delete()
		return dropped
	}

	/**
	 * Removes the two datapack-registry villager trades and the vanilla trade tags that pull them in.
	 *
	 * The `minecraft:villager_trade` registry arrives in 26, along with the deletion of every way a mod
	 * could add a trade from code. Below it the same two offers come from `ACVillagerTradeEvents`, and
	 * these files would be an unknown registry directory: harmless, but shipped dead weight naming a
	 * loot function (`minecraft:exploration_map`) whose arguments only make sense on 26. So they are
	 * authored once for the top of the range and dropped here, the mirror image of the source-set
	 * exclusion `ModPlatformPlugin.configureJava` applies to the Java half.
	 *
	 * Returns the number of files deleted.
	 */
	fun dropVillagerTradeData(resourcesRoot: File, modId: String): Int {
		var dropped = 0
		listOf(
			resourcesRoot.resolve("data/$modId/villager_trade"),
			resourcesRoot.resolve("data/minecraft/tags/villager_trade"),
		).forEach { root ->
			if (!root.exists()) return@forEach
			root.walkTopDown().filter { it.isFile }.toList().forEach { if (it.delete()) dropped++ }
			root.walkBottomUp().filter { it.isDirectory }.forEach { it.delete() }
		}
		return dropped
	}

	/** Moves [from] onto [to], merging into an existing directory. Returns the number of files moved. */
	private fun relocate(from: File, to: File): Int {
		if (!from.exists()) return 0
		var moved = 0
		from.walkTopDown().filter { it.isFile }.toList().forEach { file ->
			val destination = to.resolve(file.toRelativeString(from))
			destination.parentFile.mkdirs()
			file.copyTo(destination, overwrite = true)
			file.delete()
			moved++
		}
		from.walkBottomUp().filter { it.isDirectory }.forEach { it.delete() }
		return moved
	}

	// ------------------------------------------------------- 26.2 worldgen features

	/**
	 * **26.2 deleted `minecraft:random_patch`, and a patch is expressed as PLACEMENT now.**
	 *
	 * `Feature.RANDOM_PATCH` is gone from the feature registry along with `FLOWER`,
	 * `NO_BONEMEAL_FLOWER`, `DRIPSTONE_CLUSTER`, `POINTED_DRIPSTONE`, `FOREST_ROCK` and `ICE_SPIKE`,
	 * and an unknown feature type is fatal rather than skipped — the whole `RegistryDataLoader` pass
	 * dies with *"Unknown registry key in ResourceKey[minecraft:root / minecraft:worldgen/feature]:
	 * minecraft:random_patch"* and the server never starts. Four of this mod's configured features
	 * are random patches (`flytrap`, `underweed`, `primordial_caves_grass`,
	 * `forlorn_hollows_brown_mushroom`).
	 *
	 * Vanilla's own `VegetationFeatures`/`VegetationPlacements` show what replaced it: the inner
	 * `simple_block` becomes the configured feature outright, and the three fields the patch carried
	 * move onto the **placed** feature that referenced it —
	 *
	 *  - `tries` -> `minecraft:count`, which duplicates the position that many times;
	 *  - `xz_spread` / `y_spread` -> `minecraft:random_offset` over `TrapezoidInt.triangle(range)`,
	 *    i.e. `{"type":"minecraft:trapezoid","min":-range,"max":range,"plateau":0}`, whose `sample`
	 *    is `nextInt(max + 1) - nextInt(max + 1)` — byte-for-byte the offset `RandomPatchFeature`
	 *    used to compute inline, so the scatter is unchanged;
	 *  - the patch's own inner `placement` list (here one `block_predicate_filter` per feature),
	 *    appended last so it still filters each scattered position rather than the patch origin.
	 *
	 * The three are appended to the **end** of the existing placement list, after `biome`. That is
	 * deliberate and it is what the old shape did: the biome check ran once, at the patch origin,
	 * and only then did the patch scatter. Appending earlier would biome-test every scattered
	 * position instead, which is a different feature.
	 *
	 * A placed feature whose `feature` is an inline object rather than an id is left alone — none
	 * exists in this tree, and rewriting one would need the modifiers to go somewhere there is no
	 * room for.
	 */
	fun unrollRandomPatchesTo1262(resourcesRoot: File): Int {
		val data = resourcesRoot.resolve("data")
		if (!data.isDirectory) return 0
		var changed = 0
		val unrolled = mutableMapOf<String, List<JsonElement>>()

		worldgenEntries(data, "configured_feature").forEach { (id, file) ->
			val root = runCatching { json.parseToJsonElement(file.readText()) as? JsonObject }
				.getOrNull() ?: return@forEach
			if (root.idOf("type") != "minecraft:random_patch") return@forEach
			val config = root["config"] as? JsonObject ?: return@forEach
			val patch = config["feature"] as? JsonObject ?: return@forEach
			val inner = patch["feature"] as? JsonObject ?: return@forEach

			val modifiers = mutableListOf<JsonElement>()
			modifiers += buildJsonObject {
				put("type", JsonPrimitive("minecraft:count"))
				put("count", JsonPrimitive(intAt(config, "tries") ?: 96))
			}
			modifiers += buildJsonObject {
				put("type", JsonPrimitive("minecraft:random_offset"))
				put("xz_spread", triangleInt(intAt(config, "xz_spread") ?: 7))
				put("y_spread", triangleInt(intAt(config, "y_spread") ?: 3))
			}
			modifiers += (patch["placement"] as? JsonArray).orEmpty()

			unrolled[id] = modifiers
			file.writeText(json.encodeToString(JsonElement.serializer(), inner))
			changed++
		}
		if (unrolled.isEmpty()) return changed

		worldgenEntries(data, "placed_feature").forEach { (_, file) ->
			val root = runCatching { json.parseToJsonElement(file.readText()) as? JsonObject }
				.getOrNull() ?: return@forEach
			val extra = unrolled[root.idOf("feature")] ?: return@forEach
			val placement = (root["placement"] as? JsonArray).orEmpty()
			val migrated = root.replacing("placement", JsonArray(placement + extra))
			file.writeText(json.encodeToString(JsonObject.serializer(), migrated))
			changed++
		}
		return changed
	}

	/**
	 * **26.2 made `minecraft:lake`'s three block predicates explicit**, where they had been hard
	 * coded inside `LakeFeature#place` since 1.17. Same failure mode as the random patch above, one
	 * step later: *"No key can_replace_with_barrier … can_replace_with_air_or_fluid … can_place_feature
	 * in MapLike[…]"* takes the registry pass down. Two files here (`acid_lake`,
	 * `primordial_caves_lake`).
	 *
	 * The values written are exactly what the deleted hard-coded logic did, read off vanilla 26.2's
	 * own `MiscOverworldFeatures.LAKE_LAVA`, so this is behaviour-preserving rather than a guess.
	 * Only a key that is absent is filled, so a file that already spells one keeps it.
	 */
	fun fillLakePredicatesTo1262(resourcesRoot: File): Int = rewriteWorldgen(resourcesRoot) { root ->
		val feature = root as? JsonObject ?: return@rewriteWorldgen root
		if (feature.idOf("type") != "minecraft:lake") return@rewriteWorldgen root
		val config = feature["config"] as? JsonObject ?: return@rewriteWorldgen root
		var filled = config
		lakePredicates.forEach { (key, value) -> if (key !in filled) filled = filled.replacing(key, value) }
		if (filled === config) root else feature.replacing("config", filled)
	}

	private val lakePredicates: Map<String, JsonElement> = mapOf(
		"can_place_feature" to buildJsonObject { put("type", JsonPrimitive("minecraft:true")) },
		"can_replace_with_air_or_fluid" to notMatchingTag("minecraft:features_cannot_replace"),
		"can_replace_with_barrier" to notMatchingTag("minecraft:lava_pool_stone_cannot_replace"),
	)

	private fun notMatchingTag(tag: String): JsonObject = buildJsonObject {
		put("type", JsonPrimitive("minecraft:not"))
		put("predicate", buildJsonObject {
			put("type", JsonPrimitive("minecraft:matching_block_tag"))
			put("tag", JsonPrimitive(tag))
		})
	}

	/** `TrapezoidInt.triangle(range)` in its inline (>= 1.20.5) JSON form. */
	private fun triangleInt(range: Int): JsonObject = buildJsonObject {
		put("type", JsonPrimitive("minecraft:trapezoid"))
		put("min", JsonPrimitive(-range))
		put("max", JsonPrimitive(range))
		put("plateau", JsonPrimitive(0))
	}

	private fun intAt(node: JsonObject, key: String): Int? =
		(node[key] as? JsonPrimitive)?.content?.toIntOrNull()

	/**
	 * Every file under `data/<namespace>/worldgen/<registry>/`, paired with the id it is registered
	 * under. Sub-directories count towards the path, as they do for every other registry.
	 */
	private fun worldgenEntries(data: File, registry: String): List<Pair<String, File>> {
		val marker = "/worldgen/$registry/"
		return data.walkTopDown()
			.filter { it.isFile && it.extension == "json" }
			.mapNotNull { file ->
				val rel = file.relativeTo(data).invariantSeparatorsPath
				val at = rel.indexOf(marker)
				if (at < 0) null
				else rel.substring(0, at) + ":" + rel.substring(at + marker.length).removeSuffix(".json") to file
			}
			.toList()
	}
}

/**
 * Just enough SNBT to convert the string-form tags in this mod's data pack into JSON. Type suffixes
 * are dropped: every numeric tag here is read back through `CompoundTag#getInt`/`getString`, and
 * NBT's numeric tags are mutually convertible, so the distinction does not survive anyway.
 */
private object Snbt {
	fun parse(text: String): JsonElement = Reader(text).let {
		val value = it.value()
		it.skipWhitespace()
		require(it.atEnd()) { "Trailing input in SNBT: $text" }
		value
	}

	private class Reader(private val text: String) {
		private var pos = 0

		fun atEnd() = pos >= text.length

		fun skipWhitespace() {
			while (!atEnd() && text[pos].isWhitespace()) pos++
		}

		fun value(): JsonElement {
			skipWhitespace()
			return when (text[pos]) {
				'{' -> compound()
				'[' -> list()
				'"', '\'' -> JsonPrimitive(quoted())
				else -> scalar()
			}
		}

		private fun compound(): JsonObject {
			expect('{')
			val entries = LinkedHashMap<String, JsonElement>()
			skipWhitespace()
			while (text[pos] != '}') {
				skipWhitespace()
				val key = if (text[pos] == '"' || text[pos] == '\'') quoted() else unquoted()
				skipWhitespace()
				expect(':')
				entries[key] = value()
				skipWhitespace()
				if (text[pos] == ',') pos++ else break
				skipWhitespace()
			}
			expect('}')
			return JsonObject(entries)
		}

		private fun list(): JsonArray {
			expect('[')
			// Typed array prefixes (B;, I;, L;) carry no meaning once this is JSON.
			if (pos + 1 < text.length && text[pos + 1] == ';') pos += 2
			val items = mutableListOf<JsonElement>()
			skipWhitespace()
			while (text[pos] != ']') {
				items += value()
				skipWhitespace()
				if (text[pos] == ',') pos++ else break
				skipWhitespace()
			}
			expect(']')
			return JsonArray(items)
		}

		private fun quoted(): String {
			val quote = text[pos++]
			val out = StringBuilder()
			while (text[pos] != quote) {
				if (text[pos] == '\\') pos++
				out.append(text[pos++])
			}
			pos++
			return out.toString()
		}

		private fun unquoted(): String {
			val start = pos
			while (!atEnd() && (text[pos].isLetterOrDigit() || text[pos] in "_-.+")) pos++
			require(pos > start) { "Empty SNBT token at $pos in $text" }
			return text.substring(start, pos)
		}

		private fun scalar(): JsonElement {
			val token = unquoted()
			return when {
				token == "true" -> JsonPrimitive(true)
				token == "false" -> JsonPrimitive(false)
				else -> {
					val number = token.dropLastWhile { it in "bBsSlLfFdD" }
					number.toLongOrNull()?.let { return JsonPrimitive(it) }
					number.toDoubleOrNull()?.let { return JsonPrimitive(it) }
					JsonPrimitive(token)
				}
			}
		}

		private fun expect(char: Char) {
			skipWhitespace()
			require(!atEnd() && text[pos] == char) { "Expected '$char' at $pos in $text" }
			pos++
		}
	}
}
