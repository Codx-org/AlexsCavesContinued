@file:OptIn(dev.kikugie.stonecutter.StonecutterExperimentalAPI::class)

plugins {
	alias(libs.plugins.stonecutter)
	alias(libs.plugins.dotenv)
	alias(libs.plugins.loom.back.compat).apply(false)
	alias(libs.plugins.neoforged.moddev).apply(false)
	alias(libs.plugins.jsonlang.postprocess).apply(false)
	alias(libs.plugins.mod.publish.plugin).apply(false)
	alias(libs.plugins.kotlin.jvm).apply(false)
	alias(libs.plugins.devtools.ksp).apply(false)
	alias(libs.plugins.fletching.table).apply(false)
	alias(libs.plugins.legacyforge.moddev).apply(false)
}

stonecutter active "1.20.1-forge"

tasks.register("runActiveClient") {
	group = "stonecutter"
	description = "Run client of the active Stonecutter version"
	dependsOn(stonecutter.current!!.project + ":runClient")
}

tasks.register("runActiveServer") {
	group = "stonecutter"
	description = "Run server of the active Stonecutter version"
	dependsOn(stonecutter.current!!.project + ":runServer")
}

stonecutter parameters {
	// ⚠️ Every name a `//? if <loader>` gate can mention must appear here — an unlisted one is an
	// "unknown constant" failure, not a silent false.
	constants.match(current.project.substringAfterLast('-'), "neoforge", "forge", "fabric")
	swaps["mod_version"] = "\"${properties.get<String>("mod.version")}\";"
	swaps["mod_id"] = "\"${properties.get<String>("mod.id")}\";"
	swaps["mod_name"] = "\"${properties.get<String>("mod.name")}\";"
	swaps["mod_group"] = "\"${properties.get<String>("mod.group")}\";"
	swaps["minecraft"] = "\"${current.version}\";"

	// ── Forge → NeoForge package renames ────────────────────────────────────────
	// The source of truth in src/ is the ORIGINAL Forge source, so every NeoForge node gets the
	// whole net.minecraftforge.* namespace rewritten at generation time. Doing it here keeps
	// hundreds of files free of loader conditionals; only genuine API divergences (networking,
	// capabilities, registry handles) get //? if forge / //? if neoforge.
	//
	// ⚠️ Rules are BIDIRECTIONAL: replace(a, b) under a false condition applies b -> a. Every rule
	// must therefore be a no-op in reverse on the nodes it is not meant for. See the four
	// `replacements.string` semantics written up in AlexsMobsContinued/docs/notes/stonecutter.md
	// before adding any rule here.
	//
	// ⚠️ The second argument to string(id, …) is `reversible` and must be `true`. Passing `false`
	// does not mean "one-way" — it applies the rule BACKWARDS.
	//
	// ⚠️ Never make a non-Forge node the ACTIVE version: activation rewrites root src/ in place,
	// and these rules' reverse direction would corrupt it. The active node stays 1.20.1-forge.
	//
	// Rules are ordered longest-prefix-first: the catch-all must run last, after the three
	// namespaces that did NOT move under net.neoforged.neoforge.
	if (current.project.endsWith("-neoforge")) replacements {
		string("!nf-distmarker", true) { replace("net.minecraftforge.api.distmarker.", "net.neoforged.api.distmarker.") }
		string("!nf-eventbus", true) { replace("net.minecraftforge.eventbus.api.", "net.neoforged.bus.api.") }
		string("!nf-fml", true) { replace("net.minecraftforge.fml.", "net.neoforged.fml.") }
		string("!nf-rest", true) { replace("net.minecraftforge.", "net.neoforged.neoforge.") }

		// Classes NeoForge renamed but kept API-compatible. Longest name first so a shorter rule
		// cannot eat a prefix of a longer one (ForgeHooksClient before ForgeHooks).
		string("!nf-cls-hooksclient", true) { replace("ForgeHooksClient", "ClientHooks") }
		string("!nf-cls-hooks", true) { replace("ForgeHooks", "CommonHooks") }
		string("!nf-cls-spawnegg", true) { replace("ForgeSpawnEggItem", "DeferredSpawnEggItem") }
		string("!nf-cls-configspec", true) { replace("ForgeConfigSpec", "ModConfigSpec") }
		string("!nf-cls-shearable", true) { replace("IForgeShearable", "IShearable") }
		string("!nf-cls-registries", true) { replace("ForgeRegistries", "NeoForgeRegistries") }
		string("!nf-cls-eventfactory", true) { replace("ForgeEventFactory", "EventHooks") }
		string("!nf-cls-rendertypes", true) { replace("ForgeRenderTypes", "NeoForgeRenderTypes") }
		string("!nf-cls-soundtype", true) { replace("ForgeSoundType", "DeferredSoundType") }
		string("!nf-cls-flowingfluid", true) { replace("ForgeFlowingFluid", "BaseFlowingFluid") }
		string("!nf-cls-gui", true) { replace("ForgeGui", "ExtendedGui") }
		string("!nf-cls-forgemod", true) { replace("ForgeMod", "NeoForgeMod") }
		string("!nf-cls-minecraftforge", true) { replace("MinecraftForge", "NeoForge") }
	}

	// ── Forge → Fabric seam ─────────────────────────────────────────────────────
	// NeoForge above is a namespace RENAME: every type still exists, so a string swap is the whole
	// job. Fabric is not — most of net.minecraftforge.* has no counterpart at all. So this group is
	// a redirect onto a RELOCATED COMPAT NAMESPACE: stand-in classes vendored under
	// com.github.alexmodguy.alexscaves.fabric.**, compiled only on Fabric nodes (ModPlatformPlugin
	// excludes **/alexscaves/fabric/** everywhere else), which the shared source reaches through
	// these rules. Same precedent as the vendored Citadel: a type the mod needs on every node,
	// supplied by the loader where one exists and by the mod where none does.
	//
	// ⚠️ Never map a Forge type onto a Fabric API type unless it is a literal 1:1 rename. Anything
	// with a behavioural difference gets a vendored class or a `//? if fabric` source gate, so the
	// difference is visible in code rather than hidden in a build script.
	//
	// Rule keys come in two shapes, and which one to use is not a style choice:
	//   • the bare fully-qualified name — for types the source uses UNQUALIFIED everywhere, so the
	//     rule only ever rewrites an import line;
	//   • `import <fqn>` — for the event stubs, whose own javadoc quotes the Forge names it stands
	//     in for; keying on the import keeps the prose intact, and stops a broad package-prefix
	//     rule from racing a narrower bare-FQN one (matching is plain substring and the
	//     earlier-STARTING match consumes the span).
	//
	// ⚠️ This group is behind a Kotlin `if`, so it gets NO reverse pass — bidirectionality applies
	// to a rule whose *condition* is false, not to a block that was never registered. Shared source
	// is therefore always authored in the FORGE spelling and rewritten to Fabric here.
	if (current.project.endsWith("-fabric")) replacements {
		// @OnlyIn(Dist.CLIENT) → @Environment(EnvType.CLIENT). The single largest cluster in the
		// node's error list (106 of the first 764) and the cheapest: this tree has exactly one
		// spelling of the annotation, 71 sites across 53 files, and no bare-FQN uses.
		//
		// The class rule is keyed on the WHOLE annotation, deliberately. A rule on `Dist.` alone
		// would fire inside `lookDist.y` — matching is boundary-checked on neither edge — and one
		// on `Dist` alone could not tell the enum from the parameter name.
		string("!fab-onlyin-import", true) {
			replace("import net.minecraftforge.api.distmarker.OnlyIn;", "import net.fabricmc.api.Environment;")
		}
		string("!fab-dist-import", true) {
			replace("import net.minecraftforge.api.distmarker.Dist;", "import net.fabricmc.api.EnvType;")
		}
		string("!fab-onlyin-class", true) {
			replace("@OnlyIn(Dist.CLIENT)", "@Environment(EnvType.CLIENT)")
		}
		// Deferred registration → the vendored immediate one. Fabric registries have no deferred
		// phase at all, so there is nothing in the loader to rename onto; see the class javadoc for
		// what the stand-in reproduces and for the flush-ORDER caveat that comes with going
		// immediate. Keyed on the bare fully-qualified name because two dozen files import it and
		// four (ACDataSerializers, ACVanillaMapUtil, ACPlatform, Citadel) also spell it out inline —
		// one rule covers both, and the shorter token `DeferredRegister` is never rewritten, so this
		// cannot touch `ACDeferredRegister`, which wraps it and needs no change.
		string("!fab-deferredregister", true) {
			replace("net.minecraftforge.registries.DeferredRegister",
				"com.github.alexmodguy.alexscaves.fabric.registries.DeferredRegister")
		}
		// The declarative config spec → the vendored TOML-backed stand-in. Fabric ships no config
		// subsystem of its own, so this is the same shape as the register above: reproduce the
		// slice of the API the mod uses under the mod's own package rather than migrate 58 option
		// declarations and 106 read sites on all 58 nodes to buy something 22 of them need. See the
		// stand-in's javadoc for what it does and does not reproduce.
		//
		// Keyed on the bare fully-qualified name, which touches only the three import lines (both
		// spec classes and AlexsCaves) — every other mention in this tree is the short type name,
		// and the short name is deliberately never a rule key here, so nothing else can be caught.
		string("!fab-forgeconfigspec", true) {
			replace("net.minecraftforge.common.ForgeConfigSpec",
				"com.github.alexmodguy.alexscaves.fabric.forge.common.ForgeConfigSpec")
		}
		// ── The event bus ──────────────────────────────────────────────────
		// Two Forge types that look like one subject and are not, so they get two destinations.
		//
		// The GAME bus is only ever reached as MinecraftForge.EVENT_BUS, never named by type, and it
		// becomes a real dispatching bus (fabric/event/ACEventBus) — the mod publishes eight events
		// of its own as well as receiving ~60, and a direct-call dispatcher would answer only the
		// second half. The MOD bus is only ever named IEventBus, always in a parameter or a local
		// that the mod class threads through, and it becomes the fabric/ModBus token, because on
		// this loader registration is immediate and there is nothing to schedule.
		//
		// Rule order inside the pair is what keeps them apart: an import line matches both the
		// fully-qualified rule and the bare one, and the earlier-STARTING match consumes the span,
		// so the FQ rule always wins there and the bare rule only ever sees a use site.
		string("!fab-ieventbus-fqn", true) {
			replace("net.minecraftforge.eventbus.api.IEventBus",
				"com.github.alexmodguy.alexscaves.fabric.ModBus")
		}
		string("!fab-ieventbus", true) {
			replace("IEventBus", "ModBus")
		}
		string("!fab-minecraftforge", true) {
			replace("net.minecraftforge.common.MinecraftForge",
				"com.github.alexmodguy.alexscaves.fabric.forge.common.MinecraftForge")
		}
		// The bus-6 core types, relocated wholesale. The Event rule also covers EventPriority by
		// prefix, which lands on the right target since both move to the same package — and it
		// cannot reach the bus-7 spellings that share the prefix (`api.bus.EventBus`,
		// `api.event.MutableEvent`), because neither continues `api.Event`.
		string("!fab-eb-event", true) {
			replace("net.minecraftforge.eventbus.api.Event",
				"com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Event")
		}
		string("!fab-eb-subscribe", true) {
			replace("net.minecraftforge.eventbus.api.SubscribeEvent",
				"com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.SubscribeEvent")
		}
		string("!fab-eb-cancelable", true) {
			replace("net.minecraftforge.eventbus.api.Cancelable",
				"com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Cancelable")
		}
		// ── The common-side event TYPES ────────────────────────────────────
		// Seventeen stand-ins under fabric/forge/event/**, each shaped from the handlers that read
		// it rather than from the loader's real API — a getter nothing calls is a getter the
		// dispatcher would have to invent a value for. What fires them is a separate concern
		// (fabric/event/**, built on Fabric API callbacks and mixin/fabric/**); these rules only
		// move the declarations.
		//
		// EVERY rule below is keyed on `import ` and that is load-bearing twice over: the stubs'
		// own javadoc quotes the Forge names they stand in for, and the tree still names several
		// UNSTUBBED types from the same packages inline — CustomPayloadEvent.Context,
		// SaplingGrowTreeEvent, GatherComponentsEvent, BrewingRecipeRegisterEvent. Keeping those
		// spelled `net.minecraftforge` is deliberate:
		// grepping the generated Fabric tree for that string stays an honest to-do list, where a
		// broad package-prefix rule would rewrite them into a namespace that has no such class and
		// hide them among the stubs' own errors.
		//
		// The four package-shaped rules end in a dot so they carry the star import at
		// CommonEvents.java (`event.entity.living.*` alone supplies eight of the types); the three
		// `entity.` types are listed one by one, since a rule on `event.entity.` would start at the
		// same offset as the two sub-package rules and there is no defined winner between them.
		string("!fab-ev-living", true) {
			replace("import net.minecraftforge.event.entity.living.",
				"import com.github.alexmodguy.alexscaves.fabric.forge.event.entity.living.")
		}
		string("!fab-ev-player", true) {
			replace("import net.minecraftforge.event.entity.player.",
				"import com.github.alexmodguy.alexscaves.fabric.forge.event.entity.player.")
		}
		string("!fab-ev-attributes", true) {
			replace("import net.minecraftforge.event.entity.EntityAttributeCreationEvent",
				"import com.github.alexmodguy.alexscaves.fabric.forge.event.entity.EntityAttributeCreationEvent")
		}
		string("!fab-ev-dimension", true) {
			replace("import net.minecraftforge.event.entity.EntityTravelToDimensionEvent",
				"import com.github.alexmodguy.alexscaves.fabric.forge.event.entity.EntityTravelToDimensionEvent")
		}
		string("!fab-ev-spawnplacement", true) {
			replace("import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent",
				"import com.github.alexmodguy.alexscaves.fabric.forge.event.entity.SpawnPlacementRegisterEvent")
		}
		string("!fab-ev-server", true) {
			replace("import net.minecraftforge.event.server.",
				"import com.github.alexmodguy.alexscaves.fabric.forge.event.server.")
		}
		string("!fab-ev-village", true) {
			replace("import net.minecraftforge.event.village.",
				"import com.github.alexmodguy.alexscaves.fabric.forge.event.village.")
		}
		string("!fab-ev-anvil", true) {
			replace("import net.minecraftforge.event.AnvilUpdateEvent",
				"import com.github.alexmodguy.alexscaves.fabric.forge.event.AnvilUpdateEvent")
		}
		string("!fab-ev-tick", true) {
			replace("import net.minecraftforge.event.TickEvent",
				"import com.github.alexmodguy.alexscaves.fabric.forge.event.TickEvent")
		}
		string("!fab-ev-registercommands", true) {
			replace("import net.minecraftforge.event.RegisterCommandsEvent",
				"import com.github.alexmodguy.alexscaves.fabric.forge.event.RegisterCommandsEvent")
		}
		// ── the two hook facades ──────────────────────────────────────────
		// Unlike everything above these two are not events the mod LISTENS to but the factories it
		// FIRES from, so the stand-ins reproduce a return value rather than a type. All ten methods
		// answer the branch Forge takes when nobody is listening, which on this loader is not an
		// approximation: none of the eight events they post has a @SubscribeEvent handler anywhere in
		// this mod, so the real bus would dispatch to an empty list and reach the same branch. See the
		// two classes' javadoc.
		//
		// Both are keyed on the bare fully-qualified name, since every one of the sixteen call sites
		// spells the class out inline and none of them imports it. Neither can race a neighbour: the
		// `!fab-ev-*` rules above all begin with `import `, and every other
		// `net.minecraftforge.common.` rule in this group diverges from ForgeHooks at the character
		// after the last dot.
		string("!fab-hook-eventfactory", true) {
			replace("net.minecraftforge.event.ForgeEventFactory",
				"com.github.alexmodguy.alexscaves.fabric.forge.event.ForgeEventFactory")
		}
		string("!fab-hook-forgehooks", true) {
			replace("net.minecraftforge.common.ForgeHooks",
				"com.github.alexmodguy.alexscaves.fabric.forge.common.ForgeHooks")
		}
		// ── FML, and the two hooks that are not events ─────────────────────
		// The mod-lifecycle package is the one place where a stand-in is a behavioural claim rather
		// than a rename: enqueueWork runs its Runnable in place, because Fabric loads mods serially
		// on one thread and there is no other thread to hand back to. The stubs' javadoc argues
		// that; see fabric/forge/fml/event/lifecycle/ParallelDispatchEvent.
		//
		// Keyed on `import ` for the same reason the event rules are: AlexsCaves' commented
		// forge >=1.21.6 arm names FMLCommonSetupEvent fully qualified to reach its static getBus,
		// and that arm must stay legible as Forge's own API rather than be rewritten into a
		// namespace where the method does not exist.
		string("!fab-fml-lifecycle", true) {
			replace("import net.minecraftforge.fml.event.lifecycle.",
				"import com.github.alexmodguy.alexscaves.fabric.forge.fml.event.lifecycle.")
		}
		// @Mod and its nested @Mod.EventBusSubscriber are inert on Fabric but cannot simply be
		// deleted — both carrier classes already sit under four- and five-armed gate chains, and a
		// sixth arm to remove one annotation costs more than a SOURCE-retention stand-in.
		string("!fab-fml-mod", true) {
			replace("net.minecraftforge.fml.common.Mod",
				"com.github.alexmodguy.alexscaves.fabric.forge.fml.common.Mod")
		}
		// Both use sites are fully qualified, so this rule rewrites the calls themselves, not an
		// import. The stand-in keeps the doubled Supplier so a dedicated server still never loads
		// ClientProxy — Fabric has no RuntimeDistCleaner to catch it if it did.
		string("!fab-fml-distexecutor", true) {
			replace("net.minecraftforge.fml.DistExecutor",
				"com.github.alexmodguy.alexscaves.fabric.forge.fml.DistExecutor")
		}
		// Three imports plus Pathfinding's fully-qualified use, which this loader now takes on every
		// version (the arm it shares with >=1.21.5).
		string("!fab-server-lifecyclehooks", true) {
			replace("net.minecraftforge.server.ServerLifecycleHooks",
				"com.github.alexmodguy.alexscaves.fabric.forge.server.ServerLifecycleHooks")
		}
		// The lazy SoundType. Keyed on the FULL name and deliberately not on the
		// net.minecraftforge.common.util. package — LogicalSidedProvider shares it and is gated out
		// rather than stubbed.
		string("!fab-forgesoundtype", true) {
			replace("net.minecraftforge.common.util.ForgeSoundType",
				"com.github.alexmodguy.alexscaves.fabric.forge.common.util.ForgeSoundType")
		}
		// ── the client half ────────────────────────────────────────────────────
		// Two star imports (ClientProxy, ClientEvents) and three single-type ones, all of which the
		// stand-in package answers in full. The only other single-type import in that package is
		// RenderLevelStageEvent, which is already `//? if !fabric`-gated at both sites, so this rule
		// reaches it only inside a comment — hence no guard rule is needed to hold the "an unstubbed
		// Forge type keeps its Forge spelling, so grepping the generated tree is an honest to-do
		// list" policy. Keyed on the import statement so a fully-qualified BODY use of an unstubbed
		// member of the same package (AddGuiOverlayLayersEvent, ModelEvent.BakeFluidModels) is left
		// alone; the three that are live on this loader get their own rules below.
		string("!fab-cl-event", true) {
			replace("import net.minecraftforge.client.event.",
				"import com.github.alexmodguy.alexscaves.fabric.forge.client.event.")
		}
		// Live only below 1.20.5, which is where both imports are gated to and where the HUD is
		// still a list of named overlays rather than a layered draw.
		string("!fab-cl-overlay", true) {
			replace("import net.minecraftforge.client.gui.overlay.",
				"import com.github.alexmodguy.alexscaves.fabric.forge.client.gui.overlay.")
		}
		// The three client events this tree names fully qualified from a body on an arm this loader
		// takes. Each is spelled out because the arm chains around them are keyed on how the event's
		// shape moved, not on where its package is, so no import exists to catch instead.
		//
		// On an import line these overlap the rule above, and the earlier-STARTING match consumes
		// the span — `import ` begins first — so the two kinds cannot disagree about a single line.
		string("!fab-cl-rendernametag", true) {
			replace("net.minecraftforge.client.event.RenderNameTagEvent",
				"com.github.alexmodguy.alexscaves.fabric.forge.client.event.RenderNameTagEvent")
		}
		string("!fab-cl-renderliving", true) {
			replace("net.minecraftforge.client.event.RenderLivingEvent",
				"com.github.alexmodguy.alexscaves.fabric.forge.client.event.RenderLivingEvent")
		}
		string("!fab-cl-screen", true) {
			replace("net.minecraftforge.client.event.ScreenEvent",
				"com.github.alexmodguy.alexscaves.fabric.forge.client.event.ScreenEvent")
		}
		// The per-item client-extension object, 23 imports and one cast apiece, and its fluid sibling
		// two lines down. Both are named in full rather than by their shared package, because that
		// package holds nothing else and naming the two types says so.
		//
		// The two keys diverge one character after `common.`, so at any given offset only one of them
		// can match — they are not the overlapping pair the ToolAction note below describes, which is
		// two keys where one is a PREFIX of the other.
		string("!fab-cl-itemext", true) {
			replace("net.minecraftforge.client.extensions.common.IClientItemExtensions",
				"com.github.alexmodguy.alexscaves.fabric.forge.client.extensions.common.IClientItemExtensions")
		}
		string("!fab-cl-fluidext", true) {
			replace("net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions",
				"com.github.alexmodguy.alexscaves.fabric.forge.client.extensions.common.IClientFluidTypeExtensions")
		}
		// The one loader render type this tree draws with — ten call sites, all of them glow, all
		// reading it through the short type name, so only the ten imports move. Named in full rather
		// than by `net.minecraftforge.client.`, which would also swallow the two unstubbed client
		// helpers still spelled that way in bodies; and it cannot collide with the event rule above,
		// whose key diverges from this one at the character after `client.`.
		string("!fab-cl-rendertypes", true) {
			replace("net.minecraftforge.client.ForgeRenderTypes",
				"com.github.alexmodguy.alexscaves.fabric.forge.client.ForgeRenderTypes")
		}
		// The delegating baked-model base and the per-position model-data bag it carries. Two rules
		// rather than one on `net.minecraftforge.client.model.`, because that package also holds the
		// unstubbed geometry API and this tree's policy is to leave what has no stand-in spelled
		// `net.minecraftforge` so the generated Fabric tree stays an honest to-do list. Neither can
		// collide with the other or with the render-types rule above: all three keys diverge one
		// character after the last package segment they share.
		string("!fab-cl-bakedmodelwrapper", true) {
			replace("net.minecraftforge.client.model.BakedModelWrapper",
				"com.github.alexmodguy.alexscaves.fabric.forge.client.model.BakedModelWrapper")
		}
		string("!fab-cl-modeldata", true) {
			replace("net.minecraftforge.client.model.data.ModelData",
				"com.github.alexmodguy.alexscaves.fabric.forge.client.model.data.ModelData")
		}
		// The client hook bag. Only one of its methods is reached from this tree and only below
		// 1.21.2 — see the stand-in — but the import that names it is ungated in the armour-layer
		// mixin, so the rule has to be too. It also rewrites the one `handleCameraTransforms` call
		// in ACClientCompat, which sits in an arm no Fabric node takes (the `fabric` arm of
		// applyItemTransform is declared before the version arms, so it always wins there); the
		// stand-in deliberately does not declare that method, since a rewritten call inside a
		// disabled arm is comment text.
		string("!fab-cl-forgehooks", true) {
			replace("net.minecraftforge.client.ForgeHooksClient",
				"com.github.alexmodguy.alexscaves.fabric.forge.client.ForgeHooksClient")
		}
		// The multipart base class. Not under fabric/forge/**: the vendored copy is three members
		// read out of the 1.20.1 jar rather than a stand-in for an event or a registration phase, so
		// it sits beside the other vendored entity code. Named in full rather than by its package —
		// `net.minecraftforge.entity` holds nothing else this tree imports, and naming the type says
		// so. ACMultipartOwner's return type is rewritten by this same rule.
		string("!fab-partentity", true) {
			replace("net.minecraftforge.entity.PartEntity",
				"com.github.alexmodguy.alexscaves.fabric.entity.PartEntity")
		}
		// The convention-tag constants — eleven of them, all read through the short type name, so
		// only the two import lines move. The stand-in declares ids rather than delegating to Fabric
		// API's own constants, and its javadoc says why; what matters here is that it lives under the
		// package mirror, beside the config spec, because both stand in for a type of the same
		// loader package.
		string("!fab-tags", true) {
			replace("net.minecraftforge.common.Tags",
				"com.github.alexmodguy.alexscaves.fabric.forge.common.Tags")
		}
		// The brewing trio — the recipe interface, its ingredient-pair implementation and the static
		// registry below 1.20.5. A package rule rather than three type rules, because all three
		// stand-ins exist and the package holds nothing else, so there is no unstubbed type left
		// behind for the trailing dot to swallow. It cannot collide with the five other
		// `net.minecraftforge.common.` rules: every one of them diverges from this at the character
		// after that dot.
		string("!fab-brewing", true) {
			replace("net.minecraftforge.common.brewing.",
				"com.github.alexmodguy.alexscaves.fabric.forge.common.brewing.")
		}
		// Global loot modifiers. A package-prefix rule is safe here for the same reason it is for
		// brewing: IGlobalLootModifier is the only type this tree uses out of that package (there is
		// no LootModifier base class and no LootTableIdCondition anywhere in src/), so every name the
		// rule can reach has a stand-in. It cannot collide with the other net.minecraftforge.common.
		// rules — Tags, ToolAction, SoundAction, MinecraftForge, ForgeConfigSpec and brewing all
		// diverge from it at the character straight after that dot.
		string("!fab-loot", true) {
			replace("net.minecraftforge.common.loot.",
				"com.github.alexmodguy.alexscaves.fabric.forge.common.loot.")
		}
		// EntityType.Builder's three Forge-patched setters. The first two are plain aliases of
		// vanilla methods that write the very same field — setUpdateInterval is updateInterval and
		// setTrackingRange is clientTrackingRange — so they are renames and nothing about the
		// resulting entity type changes. (ACEntityRegistry calls both spellings on four of its
		// types, which is upstream sloppiness rather than a difference: the last call wins, and it
		// wins identically once they read the same name.)
		string("!fab-entitytype-updateinterval", true) {
			replace(".setUpdateInterval(", ".updateInterval(")
		}
		string("!fab-entitytype-trackingrange", true) {
			replace(".setTrackingRange(", ".clientTrackingRange(")
		}
		// The third has no vanilla counterpart and needs none. Forge's velocityUpdates flag already
		// defaults to true, all 44 call sites in this tree pass true, and vanilla's own
		// EntityType.trackDeltas() returns true for everything outside a hardcoded blacklist of
		// eight vanilla types (read out of the 1.20.1 bytecode) — so the call is a no-op on every
		// loader and the rule drops it from the builder chain. It is replaced by a single SPACE
		// rather than by nothing, for two reasons: Stonecutter refuses an empty replacement
		// outright ("Substituting an empty string is an irreversible operation"), and a space is
		// the one filler that cannot break a call site which a later wave moves inside a
		// commented-out `//?` arm — a `/* … */` replacement would close the arm's own comment.
		// The literal argument is part of the key on purpose: a future call site passing false
		// would not match, and would surface as a compile error rather than being silently
		// dropped.
		string("!fab-entitytype-velocity", true) {
			replace(".setShouldReceiveVelocityUpdates(true)", " ")
		}
		// Entity#getStepHeight() is a pure rename. Both patched jars add it as the accessor for a
		// step height they made overridable; vanilla has carried `public float maxUpStep()` unchanged
		// on every version in this range (javap'd on 1.20.1, 1.20.6, 1.21, 1.21.5 and 26.2), and the
		// patched accessor is what the loaders' own movement code calls in its place. The rule is a
		// plain substring rewrite, so it covers the fifteen entities that DECLARE the override and
		// GumWormEntity's `super.getStepHeight()` as readily as the ten call sites — which is the
		// whole point, since an override has to change name together with what it overrides.
		// AbstractPathJob's two pre-existing `entity.maxUpStep()` calls are untouched: a replacement
		// group behind a Kotlin `if (eval(...))` never gets Stonecutter's reverse pass.
		string("!fab-stepheight", true) {
			replace("getStepHeight()", "maxUpStep()")
		}
		// Screen#getMinecraft(). Both patched jars add it; vanilla keeps the client on a protected
		// field, so the five reads in PageRenderer — which is not in Screen's package — go through
		// the access widener instead. Keyed with the leading dot so it can only match a call.
		string("!fab-screen-minecraft", true) {
			replace(".getMinecraft()", ".minecraft")
		}
		// AbstractContainerScreen#getGuiLeft()/#getGuiTop(), the same shape one class down: patched
		// accessors for two protected fields the access widener opens instead. The spelunkery table's
		// word buttons are the only readers in the Fabric source set.
		string("!fab-gui-left", true) {
			replace(".getGuiLeft()", ".leftPos")
		}
		string("!fab-gui-top", true) {
			replace(".getGuiTop()", ".topPos")
		}
		// TextureAtlasSprite#getPixelRGBA(frame, x, y), which both patched jars add and vanilla does
		// not replace — the pixels live on the sprite's contents, whose source image is private. The
		// rule folds the receiver into the first argument of ACClientCompat#spritePixel, which is why
		// it keys on the variable name as well: BlockColorFinder is the only caller in the tree, its
		// four call sites all read frame 0 off a local called `image`, and a future caller spelled any
		// other way should surface as a compile error rather than be quietly missed.
		string("!fab-sprite-pixel", true) {
			replace("image.getPixelRGBA(0, ",
				"com.github.alexmodguy.alexscaves.client.ACClientCompat.spritePixel(image, ")
		}
		// The tool-action token and its constants — ONE rule for BOTH types, on purpose. Matching is
		// plain substring, so this key is a prefix of the plural class's, and the two stand-ins share
		// a package: the trailing `s` the rule leaves behind reassembles into exactly the name the
		// second rule would have produced. Declaring that second rule would instead be two matches
		// starting at the same offset, which is the one overlap Stonecutter does not resolve.
		string("!fab-toolaction", true) {
			replace("net.minecraftforge.common.ToolAction",
				"com.github.alexmodguy.alexscaves.fabric.forge.common.ToolAction")
		}
		// The fluid-sound token and its constants — ONE rule for BOTH, for exactly the reason the
		// tool-action rule above is one. Safe to key on the bare FQN rather than on `import ` because
		// no BROAD `net.minecraftforge.common.` rule exists to race it: every rule in this group that
		// touches that package names a type (Tags, ToolAction, MinecraftForge, ForgeConfigSpec), and
		// none of those is a prefix of this one, so no two of them can start at the same offset.
		string("!fab-soundaction", true) {
			replace("net.minecraftforge.common.SoundAction",
				"com.github.alexmodguy.alexscaves.fabric.forge.common.SoundAction")
		}
		// The fluid package, by PREFIX rather than by type — the one place in this group where that
		// is the right shape. Four of its types are named here (FluidType, FluidStack,
		// ForgeFlowingFluid, FluidInteractionRegistry) and the package holds nothing else this tree
		// touches, so four rules would be four ways to forget the fifth. It also has to be a prefix
		// rule to reach ACPlatform's two fluid-type accessors, which spell the return type fully
		// qualified inside a loader arm and so have no import line to rewrite.
		string("!fab-fluids", true) {
			replace("net.minecraftforge.fluids.",
				"com.github.alexmodguy.alexscaves.fabric.forge.fluids.")
		}
		// The one *arity* difference in the ACDestroyedItem story. Vanilla's hook is
		// Item#onDestroyed(ItemEntity); the loaders add a second parameter and call THAT instead, so
		// RadioactiveOnDestroyedBlockItem overrides the two-argument form and chains up to its super.
		// The override itself is legalised on Fabric by ACDestroyedItem declaring the same signature,
		// but the `super.` call has no two-argument target there and must drop back to vanilla's
		// arity. Unique in the tree, and no reverse pass to worry about (Kotlin-guarded group).
		string("!fab-item-ondestroyed-super", true) {
			replace("super.onDestroyed(itemEntity, damageSource)",
				"super.onDestroyed(itemEntity)")
		}

		// Vanilla has no no-argument CreativeModeTab.builder() — its Builder takes the row and
		// column a *vanilla* tab occupies, which a mod tab never knows. Forge adds the no-arg
		// factory; Fabric API's FabricItemGroup.builder() is the same thing under another name
		// (it also puts the tab on the extra page vanilla's two rows cannot hold). The six
		// withTabsBefore calls that go with it are Forge-only Builder methods and are gated out
		// one line at a time in ACCreativeTabRegistry; Fabric orders tabs by registration order,
		// which is already the order those calls spell out.
		//
		// ⚠️ Renamed and rehomed at MC 26: the whole `fabric-item-group-api-v1` module is gone from
		// fabric-api 0.145.1+26.1 and the factory is `api.creativetab.v1.FabricCreativeModeTab` in
		// `fabric-creative-tab-api-v1` now. Same shape — a static builder() handing back a vanilla
		// CreativeModeTab.Builder — so only the owner moves. Folded in as a version-dependent val
		// rather than added as a second rule, because replacements never chain: a rule keyed on what
		// this one produces would match against the ORIGINAL file text and silently never fire.
		val fabTabBuilder = if (eval(current.version, ">=26"))
			"net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab.builder()"
		else "net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup.builder()"
		string("!fab-creativetab-builder", true) {
			replace("CreativeModeTab.builder()", fabTabBuilder)
		}

		// Vanilla's ShaderInstance(ResourceProvider, String, VertexFormat) concatenates the name
		// straight into "shaders/core/<name>.json", so it cannot carry a namespace; the
		// (…, ResourceLocation, …) overload the eight calls below use is a Forge addition. Fabric API
		// ships the identical constructor on a public final subclass — and its own ShaderProgramMixin
		// hands namespaced paths to instances of exactly that class and to nothing else, so the
		// subclass is not an alternative to it but the only way in.
		string("!fab-shaderinstance-ctor", true) {
			replace("new ShaderInstance(",
				"new net.fabricmc.fabric.impl.client.rendering.FabricShaderProgram(")
		}

		// Vanilla ItemBlockRenderTypes has no setRenderLayer at all — its two maps are built from a
		// hardcoded vanilla list in <clinit> and never added to. Fabric API's BlockRenderLayerMap is
		// the same registry under another name; all four call sites in this tree are fluids, so the
		// fluid overload is the only one that has to be reachable.
		//
		// ⚠️ It moved house at 1.21.6, in the same sweep that replaced the chunk RenderTypes with the
		// ChunkSectionLayer enum: the whole `fabric-blockrenderlayer-v1` module is gone from
		// fabric-api 0.128.2+1.21.6 and the class lives in `fabric-rendering-v1` now, as
		// `api.client.rendering.v1.BlockRenderLayerMap` — and it is STATIC there, so the `INSTANCE`
		// singleton goes with the old package. Only the owner changes; the argument is already
		// version-correct on every loader via !mc2106-fluidlayer-*. Folded into this rule as a
		// version-dependent val rather than added as a second rule, because replacements never chain.
		val fabRenderLayerMap = if (eval(current.version, ">=1.21.6"))
			"net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap.putFluid("
		else "net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putFluid("
		string("!fab-setrenderlayer-fluid", true) {
			replace("ItemBlockRenderTypes.setRenderLayer(", fabRenderLayerMap)
		}

		// 1.21.9 moved the sprite-set registration interface out of ParticleEngine into the new
		// ParticleResources beside it, with a byte-identical shape (`ParticleProvider<T>
		// create(SpriteSet)`). It is package-private in both owners, so the access widener needs an
		// entry on each side of the gate; the Java side is this one rename, covering all three sites
		// (the sink interface, the event's forwarder and the client entrypoint's anonymous sink).
		//
		// Registered only from 1.21.9 rather than folded into a version-dependent val, because below
		// it the rule would be a no-op replacing a string with itself. The replacement is fully
		// qualified: the three files import ParticleEngine, not ParticleResources, and an import
		// cannot be added on one side of a version gate from a string rule.
		if (eval(current.version, ">=1.21.9")) {
			string("!fab-mc2119-particle-registration", true) {
				replace("ParticleEngine.SpriteParticleRegistration", "net.minecraft.client.particle.ParticleResources.SpriteParticleRegistration")
			}
		}

		// ── fabric-api's own 26 sweep ───────────────────────────────────────────────────────
		//
		// Seven renames in fabric-api itself, none of them a Minecraft change. Every one is a pure
		// rename — same package or a sibling one, same shape, same functional interface — which is
		// why they are rules here rather than arms in the two files that call them: an arm would
		// duplicate a method body to change one identifier in it.
		//
		// The one that is a rename only by luck is the model-layer registry: its second parameter
		// stopped being a `Supplier<LayerDefinition>` and became fabric's own
		// `TexturedLayerDefinitionProvider`, whose single method is `LayerDefinition
		// createLayerDefinition()`. The call site passes `definition::get`, and a method reference
		// binds to whichever functional interface is expected, so nothing at the call site has to
		// know. Verified by javap against fabric-api 0.145.1+26.1 rather than assumed.
		//
		// ⚠️ NOT in this list, because it is a shape change and not a rename: BlockRenderLayerMap is
		// gone from fabric-api entirely at 26 (see !fab-setrenderlayer-fluid above), and a fluid's
		// renderer is registered with a baked-model-shaped FluidRenderingRegistry.register(source,
		// flowing, FluidModel.Unbaked) instead. That one is a `fabric && >=26` arm in ClientProxy,
		// beside the two loaders' equivalents.
		if (eval(current.version, ">=26")) {
			// The whole `fabric-keybinding-api-v1` module was renamed `fabric-key-mapping-api-v1`,
			// taking its package and both halves of the class name with it. Two rules because the
			// import and the method reference share no substring worth keying on; they cannot
			// collide, since the qualified form never appears at the use site.
			string("!fab-mc26-keymapping-import", true) {
				replace("net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper",
					"net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper")
			}
			string("!fab-mc26-keymapping-use", true) {
				replace("KeyBindingHelper::registerKeyBinding", "KeyMappingHelper::registerKeyMapping")
			}
			// Both particle renames are inside `api.client.particle.v1`, so the bare token carries
			// the import and the use alike. FabricSpriteSet still extends vanilla's SpriteSet, which
			// is what lets the seventeen mod-side registrations stay written against the vanilla type.
			string("!fab-mc26-particle-registry", true) {
				replace("ParticleFactoryRegistry", "ParticleProviderRegistry")
			}
			string("!fab-mc26-particle-spriteset", true) {
				replace("FabricSpriteProvider", "FabricSpriteSet")
			}
			// 26 replaced a block's tint-index callback with a list of tint sources, so fabric-api's
			// two-registry ColorProviderRegistry became a block-only BlockColorRegistry with a static
			// register. The event stub already carries the matching >=26 arm; only the owner of the
			// method reference moves. The ITEM half is not renamed and does not need to be — it lives
			// under a <1.21.4 gate, four versions below the first node this rule sees.
			string("!fab-mc26-blockcolor-import", true) {
				replace("import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;",
					"import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;")
			}
			string("!fab-mc26-blockcolor-use", true) {
				replace("ColorProviderRegistry.BLOCK::register", "BlockColorRegistry::register")
			}
			// Same package in both cases; see the note above on why the layer registry's changed
			// parameter type costs the call site nothing.
			string("!fab-mc26-modellayer", true) {
				replace("EntityModelLayerRegistry", "ModelLayerRegistry")
			}
			string("!fab-mc26-tooltipcomponent", true) {
				replace("TooltipComponentCallback", "ClientTooltipComponentCallback")
			}
			// fabric-loot-api-v2 is gone from the bundle; v3's ALL_LOADED has the same
			// (ResourceManager, Registry<LootTable>) shape the >=1.20.5 arm of ACFabricLootModifiers
			// is already written against, so the version digit is the whole change.
			string("!fab-mc26-loot-v3", true) {
				replace("net.fabricmc.fabric.api.loot.v2", "net.fabricmc.fabric.api.loot.v3")
			}
			// fabric-networking-api-v1 6.3.0 renamed PayloadTypeRegistry's four accessors from the
			// C2S/S2C shorthand to vanilla's own serverbound/clientbound wording. Nothing else about
			// the interface moved — register() still takes (Type<T>, StreamCodec) and still hands back
			// a TypeAndCodec — so the two call sites in ACNetwork#registerServerReceiver are a rename.
			// The leading dot keeps each rule off the other's method name.
			string("!fab-mc26-payloadtype-c2s", true) {
				replace(".playC2S()", ".serverboundPlay()")
			}
			string("!fab-mc26-payloadtype-s2c", true) {
				replace(".playS2C()", ".clientboundPlay()")
			}
		}
	}

	// ── NeoForge's 1.20.5 common-tag sweep ──────────────────────────────────────
	// NeoForge re-cut its own `Tags` constants in 20.5 to line up with the vanilla/Fabric common
	// tag names. Forge kept the old spellings on every version, and NeoForge 20.4 still had them,
	// so the split is loader AND version — hence its own registration condition rather than a line
	// in either group above. Pure renames, same TagKey shape, so a rule beats a source gate.
	//
	// Only the constants this mod actually names are listed; every other Tags.Biomes member it
	// touches (IS_SNOWY, IS_DESERT, IS_MOUNTAIN, IS_SWAMP, IS_RARE, IS_MUSHROOM, IS_SPOOKY,
	// IS_PLAINS) survived the sweep unchanged — checked against the 20.6 sources jar.
	//
	// IS_CONIFEROUS cannot eat its own target: matching is identifier-boundary aware on the right
	// edge, so the `_TREE` suffix stops it. Tags.Items.SHEARS is spelled with its holder because a
	// bare `Items.SHEARS` is a vanilla item CandicornEntity legitimately names.
	if (current.project.endsWith("-neoforge") && eval(current.version, ">=1.20.5")) replacements {
		string("!nf205-tag-aquatic", true) { replace("Tags.Biomes.IS_WATER", "Tags.Biomes.IS_AQUATIC") }
	}

	// Forge eventually followed NeoForge onto the common names, but only partly and only from
	// 1.21.1 (52.x): IS_CONIFEROUS is *gone* there, while IS_WATER and Tags.Items.SHEARS survive as
	// deprecated aliases assigned from IS_AQUATIC / TOOLS_SHEAR in the same <clinit> (read out of
	// forge-universal 52.1.15), i.e. the identical TagKey. So only the coniferous rename has to
	// cross to Forge; aliasing the other two would be churn with a deprecation warning either way.
	if ((current.project.endsWith("-neoforge") && eval(current.version, ">=1.20.5"))
		|| (current.project.endsWith("-forge") && eval(current.version, ">=1.21.1"))) replacements {
		string("!tag-coniferous", true) { replace("Tags.Biomes.IS_CONIFEROUS", "Tags.Biomes.IS_CONIFEROUS_TREE") }
	}

	// The shears tag was renamed twice: SHEARS -> TOOLS_SHEARS in 1.20.5, then depluralised to
	// TOOLS_SHEAR in 1.21 when the whole `tools` family went singular. Two rules over disjoint
	// version ranges rather than one chained pair, because chaining would depend on rule order and
	// Stonecutter does not promise one. Both read from the same source spelling, which is the only
	// one root src/ ever contains, and neither target appears in this tree otherwise.
	if (current.project.endsWith("-neoforge")
		&& eval(current.version, ">=1.20.5") && !eval(current.version, ">=1.21")) replacements {
		string("!nf205-tag-shears", true) { replace("Tags.Items.SHEARS", "Tags.Items.TOOLS_SHEARS") }
	}

	// ── NeoForge 1.21 renames ───────────────────────────────────────────────────
	// Straight renames, each affecting the import as well as every use, so a rule beats a gate at
	// every one of the sites. The methods that take a tool action (Block#getToolModifiedState,
	// IItemExtension/ItemStack#canPerformAction) kept their own names, and the spawn-placement
	// event kept `register` and its nested `Operation`.
	//
	// LivingDamageEvent did NOT come through unchanged — it split into a non-cancellable Pre and a
	// Post, so CommonEvents#livingHurt is gated rather than renamed. Only LivingAttackEvent, whose
	// replacement is still one cancellable class, can be done here.
	//
	// Longest source first for the two that share a prefix, and both directions are safe: the
	// target spellings appear nowhere in this tree except where these rules put them.
	if (current.project.endsWith("-neoforge") && eval(current.version, ">=1.21")) replacements {
		string("!nf21-toolactions", true) { replace("ToolActions", "ItemAbilities") }
		string("!nf21-toolaction", true) { replace("ToolAction", "ItemAbility") }
		string("!nf21-spawnplacementevent", true) {
			replace("SpawnPlacementRegisterEvent", "RegisterSpawnPlacementsEvent")
		}
		string("!nf21-livingattack", true) { replace("LivingAttackEvent", "LivingIncomingDamageEvent") }

		// The `tools` tag family went singular with 1.21. See the 1.20.5 block above for why this
		// reads from the original SHEARS spelling rather than chaining off TOOLS_SHEARS.
		string("!nf21-tag-shears", true) { replace("Tags.Items.SHEARS", "Tags.Items.TOOLS_SHEAR") }

		// LivingChangeTargetEvent's getter grew the "AboutToBeSet" it always meant; Entity's
		// isAddedToWorld became isAddedToLevel in the same sweep that finished level/world.
		string("!nf21-newtarget", true) { replace(".getNewTarget()", ".getNewAboutToBeSetTarget()") }
		string("!nf21-addedtolevel", true) { replace(".isAddedToWorld()", ".isAddedToLevel()") }

		// IItemExtension's stack-aware attribute hook is getDefaultAttributeModifiers(ItemStack)
		// from 1.21 — the same method, renamed onto vanilla's spelling. Two narrow rules rather
		// than a bare `getAttributeModifiers` -> `getDefaultAttributeModifiers`, which would also
		// rewrite the <1.20.5 arms, ItemStack#getAttributeModifiers in ACCompat, and the javadoc
		// in all seven items that names both spellings to contrast them.
		string("!nf21-attrhook-decl", true) {
			replace("getAttributeModifiers(ItemStack stack) {", "getDefaultAttributeModifiers(ItemStack stack) {")
		}
		string("!nf21-attrhook-super", true) {
			replace("super.getAttributeModifiers(stack)", "super.getDefaultAttributeModifiers(stack)")
		}
	}

	// ── The swing hook gained the hand ──────────────────────────────────────────
	// NeoForge's IItemExtension#onEntitySwing takes the InteractionHand from 1.21.2; Forge's
	// IForgeItem still takes the stack and the entity alone, which is why this is a loader rule
	// and not a version one. The single override here (the primitive club's charge-up swing)
	// ignores the argument either way.
	if (current.project.endsWith("-neoforge") && eval(current.version, ">=1.21.2")) replacements {
		string("!nf2102-entity-swing-hand", true) {
			replace("onEntitySwing(ItemStack stack, LivingEntity entity)", "onEntitySwing(ItemStack stack, LivingEntity entity, net.minecraft.world.InteractionHand hand)")
		}
	}

	// ── ResourceLocation's factory methods ──────────────────────────────────────
	// fromNamespaceAndPath / parse / withDefaultNamespace are vanilla only from 1.21 on; below
	// that they are a FORGE PATCH, so ~500 call sites compile on every Forge node and on no other
	// loader. 1.21 then made the constructor private, so there is no one spelling that works
	// everywhere. The call sites keep the vanilla spelling and the nodes that lack the patch get
	// re-pointed at ACIdFactories — fully qualified, so no file needs an import added.
	//
	// Three rules, three DISTINCT targets: two rules sharing one target fail configuration with
	// "Ambiguous replacement", which is what mapping all three onto `new ResourceLocation(` would
	// do. In reverse these are no-ops — root src/ never spells the qualified helper name.
	if (!current.project.endsWith("-forge") && !eval(current.version, ">=1.21")) replacements {
		val helper = "com.github.alexmodguy.alexscaves.server.misc.ACIdFactories"
		string("!rl-of", true) { replace("ResourceLocation.fromNamespaceAndPath(", "$helper.of(") }
		string("!rl-parse", true) { replace("ResourceLocation.parse(", "$helper.parse(") }
		string("!rl-vanilla", true) { replace("ResourceLocation.withDefaultNamespace(", "$helper.vanilla(") }
	}

	// ── 1.20.2 renames ──────────────────────────────────────────────────────────
	// 1.20.2 renamed LevelRenderer#renderChunkLayer to #renderSectionLayer, same descriptor and
	// same position in renderLevel. Only one place in this tree names it: the Fabric render-stage
	// arm's @Inject selector in mixin/client/LevelRenderStageMixin. A //? gate cannot express it —
	// the selector sits INSIDE the `fabric && <1.21.6` arm and Stonecutter does not nest gates —
	// so it is a rule, anchored on the whole `method = "…"` fragment rather than on the bare name
	// so that the four places the prose above it spells renderChunkLayer are left alone.
	//
	// The rule is registered on every loader, not just Fabric: on Forge and NeoForge that arm is
	// commented out, and a replacement inside an inactive arm is harmless. Below 1.20.2 the group
	// does not exist, so 1.20.1-fabric keeps the old name.
	if (eval(current.version, ">=1.20.2")) replacements {
		string("!mc202-rendersectionlayer", true) {
			replace("method = \"renderChunkLayer\"", "method = \"renderSectionLayer\"")
		}
	}

	// ── 1.20.5 "component-ification" renames ────────────────────────────────────
	// 1.20.5 renamed a pile of symbols without changing their shape. Doing those here rather than
	// with //? if conditionals keeps ~250 call sites free of gates; only changes that alter a
	// *signature* or the *semantics* get a source-level conditional (or an ACCompat helper).
	//
	// The group is registered behind a Kotlin `if`, so on a node below 1.20.5 these rules do not
	// exist at all — which is also why the active node (always 1.20.1-forge) never sees them and
	// root src/ is never rewritten by them.
	//
	// Every target below was checked against the 1.20.6 bytecode with scripts/mcjavap.py rather
	// than taken from the sibling repo on faith — one of them (getFeetBlockState) does NOT map to
	// what a name-level guess suggests. See the note on !mc205-instate.
	if (eval(current.version, ">=1.20.5")) replacements {
		// Pathfinder: the enum and the static lookup were renamed, same package + shape.
		string("!mc205-pathtype-static", true) { replace("getBlockPathTypeStatic", "getPathTypeStatic") }
		string("!mc205-pathtype-enum", true) { replace("BlockPathTypes", "PathType") }

		// BlockBehaviour#isPathfindable lost the level and position it was handed — the twelve
		// blocks that override it all answer from the state and the computation type alone, so the
		// two dropped parameters are simply deleted here rather than gated twelve times over. That
		// only works because every declaration is spelled identically; ACC normalises the parameter
		// names (state/getter/pos/type) for exactly this reason, so do not rename them back.
		// BlockStateBase's caller-side overload lost the same pair, but it keeps a distinct arity
		// per version and so goes through ACCompat.isPathfindable instead.
		string("!mc205-pathfindable-sig", true) {
			replace(
				"isPathfindable(BlockState state, BlockGetter getter, BlockPos pos, PathComputationType type)",
				"isPathfindable(BlockState state, PathComputationType type)"
			)
		}
		string("!mc205-pathfindable-super", true) {
			replace("super.isPathfindable(state, getter, pos, type)", "super.isPathfindable(state, type)")
		}

		// AttributeModifier.Operation constants.
		string("!mc205-attr-add", true) { replace("Operation.ADDITION", "Operation.ADD_VALUE") }
		string("!mc205-attr-mulbase", true) { replace("Operation.MULTIPLY_BASE", "Operation.ADD_MULTIPLIED_BASE") }
		string("!mc205-attr-multotal", true) { replace("Operation.MULTIPLY_TOTAL", "Operation.ADD_MULTIPLIED_TOTAL") }

		// (FoodProperties.Builder had four rules here — saturationMod/alwaysEat and two shapes of
		// the deleted meat flag — plus a fifth further down for the effect supplier. 1.21.2 split
		// the class in two, which no string edit can express, so all five gave way to
		// ACFoodBuilder: it keeps upstream's vocabulary in ACFoods and resolves it per version.)

		// AbstractArrow's "what do I drop when picked up" hook. 1.20.5 keeps the old name for the
		// stored stack and renames the overridable factory, so every one of this mod's six arrows
		// and spears moves onto the new name — and so do the four bodies that call it, which want a
		// freshly built stack anyway. `getPickupItemStackOrigin` is safe from this: the match is
		// identifier-boundary aware on the right edge.
		string("!mc205-pickupitem", true) { replace("getPickupItem", "getDefaultPickupItem") }

		// Item#appendHoverText no longer receives the Level — a tooltip may be built with no level
		// loaded, so it gets an Item.TooltipContext (which carries the registries and the map data
		// the level used to be asked for). No body here reads the parameter; only the seventeen
		// declarations change, and the `super.appendHoverText(...)` pass-throughs go along for the
		// ride untouched. Two rules, because JellyBeanItem names its parameters differently; the
		// other sixteen were normalised onto one spelling, since two rules producing the *same*
		// target fail configuration with "Ambiguous replacement".
		string("!mc205-tooltipctx", true) {
			replace(
				"appendHoverText(ItemStack stack, @Nullable Level worldIn,",
				"appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext worldIn,"
			)
		}
		string("!mc205-tooltipctx-jelly", true) {
			replace(
				"appendHoverText(ItemStack itemStack, @Nullable Level level,",
				"appendHoverText(ItemStack itemStack, net.minecraft.world.item.Item.TooltipContext level,"
			)
		}

		// ── Renderer signatures that grew one argument ──────────────────────────
		// Three vanilla rendering methods gained a trailing parameter in 1.20.5 and are otherwise
		// unchanged, so the call sites are patched here rather than duplicated behind a gate. The
		// one place that *declares* one of them (LicowitchRenderer#setupRotations) is gated in
		// source instead — a declaration and its super call have to move together.
		//
		// setupRotations' new argument is the entity's render scale, which vanilla reads off the
		// entity immediately before the call; getScale() exists on every version this mod spans, so
		// both call sites can just ask for it. renderNameTag's is the partial tick. putBulkData's is
		// the quad's alpha, and 1.0F is what the pre-1.20.5 overload hardcoded.
		string("!mc205-setuprot-watcher", true) {
			replace(
				"this.setupRotations(entity, poseStack, f7, f, partialTicks)",
				"this.setupRotations(entity, poseStack, f7, f, partialTicks, entity.getScale())"
			)
		}
		string("!mc205-setuprot-teletor", true) {
			replace(
				"setupRotations(entityIn, poseStack, 0F, 180F, partialTicks)",
				"setupRotations(entityIn, poseStack, 0F, 180F, partialTicks, entityIn.getScale())"
			)
		}
		string("!mc205-nametag", true) {
			replace(
				"this.renderNameTag(entity, renderNameTagEvent.getContent(), poseStack, bufferSource, light)",
				"this.renderNameTag(entity, renderNameTagEvent.getContent(), poseStack, bufferSource, light, partialTicks)"
			)
		}
		// ItemFrameRendererMixin reaches renderNameTag through a @Shadow rather than inheritance, so
		// the compiler cannot tell it has gone stale — the mismatch would only surface as a mixin
		// apply failure at launch. Both halves move together.
		string("!mc205-nametag-shadow", true) {
			replace(
				"renderNameTag(ItemFrame entity, Component tag, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight)",
				"renderNameTag(ItemFrame entity, Component tag, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, float partialTick)"
			)
		}
		string("!mc205-nametag-itemframe", true) {
			replace(
				"renderNameTag(entity, renderNameTagEvent.getContent(), poseStack, bufferSource, packedLight)",
				"renderNameTag(entity, renderNameTagEvent.getContent(), poseStack, bufferSource, packedLight, partialTicks)"
			)
		}

		// The three renderers that genuinely override setupRotations. They carry @Override so that a
		// future signature change is a compile error rather than a silently dead method — which is
		// exactly what this one was until it was caught by hand. None of the bodies wants the scale.
		string("!mc205-setuprot-boundroid", true) {
			replace(
				"setupRotations(BoundroidWinchEntity entity, PoseStack poseStack, float bob, float yawIn, float partialTicks)",
				"setupRotations(BoundroidWinchEntity entity, PoseStack poseStack, float bob, float yawIn, float partialTicks, float scale)"
			)
		}
		string("!mc205-setuprot-gingerbread", true) {
			replace(
				"setupRotations(GingerbreadManEntity entity, PoseStack poseStack, float bob, float yawIn, float partialTicks)",
				"setupRotations(GingerbreadManEntity entity, PoseStack poseStack, float bob, float yawIn, float partialTicks, float scale)"
			)
		}
		string("!mc205-setuprot-gumworm", true) {
			replace(
				"setupRotations(GumWormEntity entity, PoseStack poseStack, float bob, float yawIn, float partialTicks)",
				"setupRotations(GumWormEntity entity, PoseStack poseStack, float bob, float yawIn, float partialTicks, float scale)"
			)
		}

		// …and Citadel's mixin into the vanilla one, which names the target by descriptor. Nothing
		// checks a mixin descriptor at compile time; getting this wrong is an InvalidInjectionException
		// at launch.
		string("!mc205-setuprot-mixin-desc", true) {
			replace(
				"setupRotations(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V",
				"setupRotations(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V"
			)
		}
		string("!mc205-setuprot-mixin-args", true) {
			replace(
				"float bodyYRot, float partialTick, CallbackInfo ci)",
				"float bodyYRot, float partialTick, float scale, CallbackInfo ci)"
			)
		}
		string("!mc205-bulkdata", true) {
			replace(
				"putBulkData(p_111059_, bakedquad, f, f1, f2, p_111065_, p_111066_)",
				"putBulkData(p_111059_, bakedquad, f, f1, f2, 1.0F, p_111065_, p_111066_)"
			)
		}
		// The same alpha argument, in ACClientCompat#renderSingleBlockTinted's pre-1.21.5 arm — a
		// second call site rather than a second spelling, so it needs its own rule (the two source
		// strings are disjoint, and their targets differ, so neither ambiguity nor overlap applies).
		string("!mc205-bulkdata-tinted", true) {
			replace(
				"putBulkData(poseStack.last(), quad, r, g, b, light, overlay)",
				"putBulkData(poseStack.last(), quad, r, g, b, 1.0F, light, overlay)"
			)
		}

		// SheetedDecalTextureGenerator takes the whole Pose now instead of its two matrices — the
		// normal matrix stopped being a separate thing to hand around.
		// (The `${'$'}` dance is Kotlin's, not Stonecutter's: the local is literally named
		// `posestack$pose1`, which a plain string template would read as an interpolation.)
		string("!mc205-decalgen", true) {
			replace(
				"posestack${'$'}pose1.pose(), posestack${'$'}pose1.normal(), 1.0F)",
				"posestack${'$'}pose1, 1.0F)"
			)
		}

		// ── Single call sites that changed shape ────────────────────────────────
		// Each of these is one occurrence in the whole mod, so a rule is cheaper than a gate.
		//
		// getBlockReach was Forge's; 1.20.5 gave vanilla two attributes (block and entity
		// interaction range) and the accessor that reads the first of them.
		string("!mc205-blockreach", true) { replace("player.getBlockReach()", "player.blockInteractionRange()") }
		// (disableShield used to be a rule here. It changed twice — 1.20.5 dropped the "did the
		// attacker swing an axe" flag, 1.21.2 asks for the blocked-with stack instead — and a rule
		// cannot describe a member that moves twice: two rules sharing a source are ambiguous, and
		// chaining one rule's target into another's source is the order dependence Stonecutter
		// rejects outright. It is ACCompat.disableShield now.)
		// setTame gained an "apply the tamed attribute changes too" flag; true is what the old
		// single-argument form did.
		string("!mc205-settame", true) { replace("dinosaur.setTame(true)", "dinosaur.setTame(true, true)") }
		// AttributeModifier.Operation is a StringRepresentable enum now, and its ordinal accessor
		// was renamed with it.
		string("!mc205-operation-id", true) {
			replace("ACCompat.operation(attributemodifier2).toValue()", "ACCompat.operation(attributemodifier2).id()")
		}
		// A chunk future resolves to a ChunkResult rather than an Either now — same two outcomes,
		// but the success side is the value itself instead of a left projection.
		string("!mc205-chunkresult-orelse", true) {
			replace(
				"getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK).left().orElse(null)",
				"getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK).orElse(null)"
			)
		}
		string("!mc205-chunkresult-present", true) {
			replace(
				"getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK).left().isPresent()",
				"getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK).isSuccess()"
			)
		}
		// BlockBehaviour#canSurvive stopped being widened to public by Forge's patches, so the one
		// place that asks a foreign block directly goes through its default state instead.
		string("!mc205-cansurvive", true) {
			replace("Blocks.LADDER.canSurvive(toPlace, world, pos)", "toPlace.canSurvive(world, pos)")
		}

		// Entity#getStepHeight is a FORGE PATCH on 1.20.1, which is what root src/ is written
		// against. Step height became a vanilla attribute in 1.20.5 and the patch went away — the
		// reader that survived is vanilla's own maxUpStep(), which exists on both versions, is
		// overridable on both, and means the same thing. This mod only ever reads or overrides,
		// never writes, so the whole family (17 overrides plus 7 call sites) is one rename. No
		// leading dot: it has to catch the declarations as well as the calls.
		string("!mc205-stepheight", true) { replace("getStepHeight()", "maxUpStep()") }

		// Gravity went the same way, except the accessor that stayed is final: an entity states its
		// pull by overriding getDefaultGravity(), and getGravity() reads that through the attribute.
		// It is a double now, which the float literals in both bodies widen to silently.
		string("!mc205-gravity-override", true) {
			replace("protected float getGravity() {", "protected double getDefaultGravity() {")
		}

		// (A frog's variant became a Holder here and a datapack registry in 1.21.5. That is three
		// shapes for one comparison, so it lives behind ACFrogRegistry.isPrimordial rather than a
		// rename rule — see that class.)

		// A tool's damage and attack speed moved out of the SwordItem constructor and into the
		// item's attribute modifiers, built by a static helper that takes the same two numbers.
		string("!mc205-swordctor", true) {
			replace(
				"super(Tiers.DIAMOND, 0, -2F, (new Item.Properties()).rarity(ACItemRegistry.RARITY_DEMONIC));",
				"super(Tiers.DIAMOND, (new Item.Properties()).rarity(ACItemRegistry.RARITY_DEMONIC).attributes(SwordItem.createAttributes(Tiers.DIAMOND, 0, -2F)));"
			)
		}

		// Enchanting a book is no longer a mutation of the stack's NBT — the component-carrying
		// stack is built from the enchantment instead, so the assignment moves to the left.
		string("!mc205-enchantedbook", true) {
			replace(
				"EnchantedBookItem.addEnchantment(itemStack, new EnchantmentInstance(enchantment, i));",
				"itemStack = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, i));"
			)
		}

		// The sky is drawn from a frustum matrix rather than a PoseStack from 1.20.5 (see
		// LevelRendererMixin, whose newer arm rebuilds the stack the parameter used to be) — the
		// dimension effects hook lost the same argument.
		string("!mc205-rendersky-effects", true) {
			replace(
				"renderSky(level, ticks, partialTick, poseStack, camera, matrix4f2, foggy, runnable)",
				"renderSky(level, ticks, partialTick, camera, matrix4f2, foggy, runnable)"
			)
		}

		// A villager trade states its price as an ItemCost — item plus count, matched against the
		// stack's components — rather than as a whole ItemStack.
		string("!mc205-merchantoffer", true) {
			replace(
				"new MerchantOffer(new ItemStack(Items.EMERALD, this.emeraldCost), new ItemStack(Items.COMPASS), itemstack,",
				"new MerchantOffer(new net.minecraft.world.item.trading.ItemCost(Items.EMERALD, this.emeraldCost), java.util.Optional.of(new net.minecraft.world.item.trading.ItemCost(Items.COMPASS)), itemstack,"
			)
		}

		// Loot tables became a datapack registry, so the built-in handles are ResourceKeys. This
		// mod's own tables are still addressed by ResourceLocation and ACPlatform converts, so only
		// the vanilla constant in the ternary needs unwrapping.
		// The accessor it unwraps with is version-dependent: 1.21.11 renamed ResourceKey#location
		// to #identifier. It cannot be a second rule in the 1.21.11 group — rules do not chain, so
		// one keyed on `SIMPLE_DUNGEON.location()` would never see what this rule produced. Same
		// shape as the glint owner and the entityInside tail.
		val keyAccessor = if (eval(current.version, ">=1.21.11")) "identifier" else "location"
		string("!mc205-builtinloot", true) {
			replace(
				"pickedBiome == null ? BuiltInLootTables.SIMPLE_DUNGEON :",
				"pickedBiome == null ? BuiltInLootTables.SIMPLE_DUNGEON.$keyAccessor() :"
			)
		}

		// Misc 1:1 renames.
		string("!mc205-ignite", true) { replace("setSecondsOnFire(", "igniteForSeconds(") }
		string("!mc205-samecomps", true) { replace("isSameItemSameTags", "isSameItemSameComponents") }
		string("!mc205-ench-fortune", true) { replace("Enchantments.BLOCK_FORTUNE", "Enchantments.FORTUNE") }

		// ⚠️ getFeetBlockState() became getInBlockState(), NOT getBlockStateOn(). Those are two
		// different methods that BOTH exist on 1.20.1: getFeetBlockState reads the state at
		// blockPosition(), getBlockStateOn the one at getOnPos() — i.e. the block below. Mapping
		// onto the wrong one compiles perfectly and silently moves every call site down a block.
		string("!mc205-instate", true) { replace("getFeetBlockState()", "getInBlockState()") }

		// ChunkStatus moved into its own package.
		string("!mc205-chunkstatus", true) {
			replace("world.level.chunk.ChunkStatus", "world.level.chunk.status.ChunkStatus")
		}

		// ── defineSynchedData ───────────────────────────────────────────────────
		// 1.20.5 moved entity data registration from "call this.entityData.define(...) inside a
		// no-arg defineSynchedData()" to "receive a SynchedEntityData.Builder and call
		// builder.define(...)". The bodies are otherwise unchanged, so three string rules retire
		// ~320 errors across 78 entity classes without a single //? gate.
		//
		// The parameter type is spelled fully qualified so no file needs an import added. All three
		// targets are absent from root src, so the reverse direction is a no-op on older nodes.
		//
		// ⚠️ The call rule must include the `this.`. Matching is identifier-boundary aware on the
		// RIGHT edge only, so a bare `entityData.define(` source fires inside `this.entityData.
		// define(` as well — and leaves `this.builder.define(`, i.e. a field access to something
		// that does not exist, on all 252 of them. The six genuinely bare call sites are all mixin
		// injections into defineSynchedData, which need their own gates anyway (the injector's
		// descriptor changes too), so they are not covered by a second rule: two rules sharing the
		// target `builder.define(` would fail configuration with "Ambiguous replacement".
		string("!mc205-define-decl", true) {
			replace(
				"void defineSynchedData() {",
				"void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {",
			)
		}
		string("!mc205-define-super", true) { replace("super.defineSynchedData()", "super.defineSynchedData(builder)") }
		string("!mc205-define-call", true) { replace("this.entityData.define(", "builder.define(") }

		// AbstractProjectileDispenseBehavior was deleted in favour of ProjectileDispenseBehavior,
		// which requires the *item* to implement ProjectileItem. ACItemRegistry registers six
		// anonymous subclasses over items that do not, so the old shape is vendored under the same
		// simple name and only the import line moves.
		string("!mc205-projdispense", true) {
			replace(
				"import net.minecraft.core.dispenser.AbstractProjectileDispenseBehavior;",
				"import com.github.alexmodguy.alexscaves.server.misc.AbstractProjectileDispenseBehavior;",
			)
		}

		// VertexConsumer#normal(Matrix3f, f, f, f) is gone in 1.20.5; it takes the whole
		// PoseStack.Pose now (the renderer needs the 4x4 to derive the normal matrix itself, since
		// PoseStack stopped keeping a separate Matrix3f). Its sibling vertex(Matrix4f, f, f, f)
		// survives unchanged, so only the normal half of every fluent chain is affected.
		//
		// There are 118 such calls and they all sit mid-chain
		// (`.vertex(m4,..).color(..).uv(..).overlayCoords(..).uv2(..).normal(m3,0,1,0).endVertex()`),
		// which makes wrapping the call itself impractical. Instead the *variable* changes type:
		// every one of them is fed from a `Matrix3f x = <pose>.normal();` local or a private helper
		// parameter that only ever forwards it to #normal. Retyping the declaration and dropping the
		// `.normal()` accessor makes all 118 call sites correct with no edit at their own line.
		//
		// Two halves, deliberately kept as separate rules so neither source overlaps the other:
		// the type half matches `Matrix3f <name>` (locals AND parameters — identical text), the
		// accessor half matches the initialiser `= <pose expr>.normal();`, anchored on the
		// semicolon so it can only ever hit a whole declaration and never a chained call.
		//
		// The names are enumerated rather than pattern-matched because `replacements` is a literal
		// string replace. `Matrix3f matrix3f` cannot damage `Matrix3f matrix3f1`: matching is
		// identifier-boundary aware on the right edge.
		//
		// ⚠️ Two locals are deliberately NOT covered — Citadel's AdvancedModelBox#doRender
		// (`lvt_10_1_`) and BasicModelPart#doRender (`normalMatrix`) do real `Vector3f.mul(m3)`
		// math instead of calling #normal, so they must stay a Matrix3f on every version. Both are
		// uniquely named so no rule below reaches them; keep it that way.
		// The SpawnPlacements.Type enum became the SpawnPlacementType interface, and its four
		// constants moved to a SpawnPlacementTypes holder interface. Only ON_GROUND and IN_WATER are
		// referenced here (40 times, all in ACEntityRegistry#registerSpawnPlacements); the two custom
		// placements are gated in that file instead, since they lose a factory call rather than a name.
		// ── Block-entity NBT ───────────────────────────────────────────────────────
		// 1.20.5 threaded a HolderLookup.Provider through the whole BlockEntity serialisation path
		// (item stacks inside a block entity now carry data components, and components hold registry
		// references that need a lookup to resolve). Four consequences, all textually uniform here:
		//
		//   load(CompoundTag)                 -> loadAdditional(CompoundTag, Provider)   [renamed too]
		//   saveAdditional(CompoundTag)       -> saveAdditional(CompoundTag, Provider)
		//   getUpdateTag()                    -> getUpdateTag(Provider)
		//   onDataPacket(Connection, packet)  -> onDataPacket(Connection, packet, Provider)  [Forge]
		//
		// …plus the calls those methods make onward — super.*, saveWithoutMetadata, ContainerHelper —
		// which all gain the same trailing argument. Rather than gate 15 block-entity classes twice
		// each, the signature grows a parameter named `acRegistries` and every onward call inside the
		// method body picks it up. The name is deliberately unmistakable: it appears nowhere else in
		// the tree, so no rule below can collide with real code.
		//
		// The parameter names are enumerated because `replacements` is a literal string replace, and
		// upstream's block entities use five different ones (`tag` dominates; `p_155055_`/`p_187459_`
		// are unmapped vanilla leftovers). Anchoring each rule on the full `(CompoundTag <name>) {`
		// means it can only ever match a declaration, never a call.
		val beProvider = ", net.minecraft.core.HolderLookup.Provider acRegistries"
		// ⚠️ Most of what follows describes a signature that exists only in the 1.20.5 → 1.21.5
		// window: 1.21.6 replaced the CompoundTag itself with the ValueInput/ValueOutput pair, and
		// the `>=1.21.6` block far below re-states each of these rules in its own terms. Hence the
		// inner `if`s — ordinary Kotlin, since `replacements { }` is a configuration lambda and a
		// rule is registered by calling `string(...)`.
		//
		// The two rules deliberately left OUT of them are getUpdateTag and saveWithoutMetadata:
		// both kept their HolderLookup.Provider overload through the 1.21.6 rewrite (checked
		// against 21.6.20-beta's BlockEntity), so they are still exactly right on the new nodes.
		// So is !mc205-be-stackregistries below — both eras' signature rules introduce
		// `acRegistries`, which is the whole point of giving it that one name.
		if (eval(current.version, "<1.21.6")) {
			for (name in listOf("tag", "compound", "compoundTag", "p_155055_")) {
				string("!mc205-be-loadsig-$name", true) {
					replace("void load(CompoundTag $name) {", "void loadAdditional(CompoundTag $name$beProvider) {")
				}
				// `super.load(resourceManager)` elsewhere in the tree is a different method on a
				// different class; none of these four names reach it.
				string("!mc205-be-loadsuper-$name", true) {
					replace("super.load($name);", "super.loadAdditional($name, acRegistries);")
				}
			}
			for (name in listOf("tag", "compound", "compoundTag", "p_187459_")) {
				string("!mc205-be-savesig-$name", true) {
					replace("void saveAdditional(CompoundTag $name) {", "void saveAdditional(CompoundTag $name$beProvider) {")
				}
				string("!mc205-be-savesuper-$name", true) {
					replace("super.saveAdditional($name);", "super.saveAdditional($name, acRegistries);")
				}
			}
		}
		string("!mc205-be-updatetag", true) {
			replace("public CompoundTag getUpdateTag() {", "public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider acRegistries) {")
		}
		string("!mc205-be-savewithoutmeta", true) {
			replace("return this.saveWithoutMetadata();", "return this.saveWithoutMetadata(acRegistries);")
		}
		if (eval(current.version, "<1.21.6")) {
			string("!mc205-be-datapacket", true) {
				replace(
					"public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {",
					"public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet$beProvider) {"
				)
			}
			// ContainerHelper's two bulk helpers gained the provider as a trailing argument. Each
			// distinct argument list is one rule; the `(p_155055_, this.items)` and
			// `(p_187459_, this.items)` forms each cover two call sites (the gingerbarrel and the
			// metal barrel share their code).
			for (call in listOf(
				"loadAllItems(p_155055_, this.items",
				"loadAllItems(compound, this.stacks",
				"loadAllItems(packet.getTag(), this.stacks",
				"loadAllItems(compoundTag, this.items",
				"saveAllItems(p_187459_, this.items",
				"saveAllItems(compound, this.stacks",
				"saveAllItems(compoundtag, this.stacks, true",
				"saveAllItems(compoundTag, this.items, true",
			)) {
				string("!mc205-be-containerhelper-${call.filter { it.isLetterOrDigit() }}", true) {
					replace("ContainerHelper.$call);", "ContainerHelper.$call, acRegistries);")
				}
			}
		}

		// The three block-entity fields that are a bare ItemStack rather than a container go through
		// ACCompat.loadStack/saveStack, which need the same provider. Their src spelling passes the
		// ACCompat.BE_REGISTRIES sentinel — see the note there — and this turns it into the parameter
		// the rules above introduced. Entities keep level().registryAccess() and never match.
		string("!mc205-be-stackregistries", true) {
			replace("ACCompat.BE_REGISTRIES", "acRegistries")
		}

		// SavedData#save gained the same provider, for the same reason. Two subclasses, ACWorldData
		// and the vendored CitadelServerData; neither body uses it.
		for (name in listOf("compound", "tag")) {
			string("!mc205-saveddata-$name", true) {
				replace("public CompoundTag save(CompoundTag $name) {", "public CompoundTag save(CompoundTag $name$beProvider) {")
			}
		}

		// EntityType#getAABB(x, y, z) -> #getSpawnAABB. Renamed, same signature and meaning; the only
		// two callers are the amber monolith and NaturalSpawnerMixin, both asking "does this mob fit
		// here" right next to the spawn-position check above.
		string("!mc205-spawnaabb", true) {
			replace(".getAABB(", ".getSpawnAABB(")
		}

		for (name in listOf("ON_GROUND", "IN_WATER", "IN_LAVA", "NO_RESTRICTIONS")) {
			string("!mc205-spawnplacement-$name", true) {
				replace("SpawnPlacements.Type.$name", "net.minecraft.world.entity.SpawnPlacementTypes.$name")
			}
		}

		// ── Attribute maps ─────────────────────────────────────────────────────────
		// 1.20.5 made attributes registry entries, so everything that used to key on `Attribute`
		// now keys on `Holder<Attribute>` — including the multimaps this mod's items build to
		// describe their modifiers. The constants those maps are keyed with (`Attributes.ATTACK_DAMAGE`
		// and friends) became holders at the same time, so retyping the map is the whole change: every
		// `put` and `get` around it then type-checks unchanged.
		//
		// The two rules cannot collide even though one source contains the other's shape: a
		// `Builder<Attribute, AttributeModifier>` occurrence has `.Builder` between the owner and the
		// `<`, so the bare-map rule never sees it. `ImmutableMultimap<Attribute, AttributeModifier>`
		// does end with the bare-map rule's source, and rewriting just that tail is the right answer.
		for (owner in listOf("Multimap", "Builder")) {
			string("!mc205-attrmap-$owner", true) {
				replace(
					"$owner<Attribute, AttributeModifier>",
					"$owner<net.minecraft.core.Holder<Attribute>, AttributeModifier>"
				)
			}
		}

		// ItemStack's tooltip number format moved onto the new component class along with everything
		// else about item attributes.
		string("!mc205-attrformat", true) {
			replace("ItemStack.ATTRIBUTE_MODIFIER_FORMAT", "net.minecraft.world.item.component.ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT")
		}

		for (name in listOf("matrix3f", "matrix3f1", "matrix3f2", "p_114092_", "p_229108_2_")) {
			string("!mc205-normal-type-$name", true) {
				replace("Matrix3f $name", "com.mojang.blaze3d.vertex.PoseStack.Pose $name")
			}
		}
		for (expr in listOf("posestack\$pose", "posestack\$pose1", "poseStack.last()", "matrixstack\$entry")) {
			string("!mc205-normal-init-${expr.filter { it.isLetterOrDigit() }}", true) {
				replace("= $expr.normal();", "= $expr;")
			}
		}
	}

	// ── 1.21's VertexConsumer builder DSL ───────────────────────────────────────
	// 1.21 renamed every method on VertexConsumer and deleted the terminator. The shapes are
	// unchanged, so this is six string rules over ~1900 call sites in 53 files rather than
	// anything structural:
	//
	//   vertex(..)          -> addVertex(..)        color(r,g,b,a) -> setColor(r,g,b,a)
	//   uv(u,v)             -> setUv(u,v)           uv2(packed)    -> setLight(packed)
	//   overlayCoords(pack) -> setOverlay(packed)   endVertex()    -> deleted
	//
	// Arity matters for two of them and this mod is uniform on both: every uv2 call passes one
	// packed int (the two-int form is setUv2) and so does every overlayCoords call (the two-int
	// form is setUv1). Checked by parenthesis-matching over the whole tree, not by eye.
	//
	// ⚠️ `.color(` cannot tell a vertex consumer from anything else with a colour method. The nine
	// FastColor.ARGB32/ABGR32 packing calls that used to collide with it were moved to ACColors —
	// if a future edit reintroduces one, this rule will silently rewrite it. Same reasoning applies
	// to any new `.uv(`/`.vertex(` receiver.
	//
	// endVertex is anchored on its semicolon so it can only ever delete a whole statement; the one
	// call that sat inside an expression lambda (Citadel's LightningRender) was given a block body
	// for exactly this reason.
	if (eval(current.version, ">=1.21")) replacements {
		// StructurePlaceSettings' boolean keepLiquids became a two-valued LiquidSettings enum, so the
		// data pack can express the same choice. All seven of this mod's uses turn it off.
		string("!mc21-liquidsettings", true) {
			replace(
				".setKeepLiquids(false)",
				".setLiquidSettings(net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings.IGNORE_WATERLOGGING)",
			)
		}

		// ── AttributeModifier lost its (UUID, name) identity ────────────────────
		// It is one ResourceLocation now, and Item's two vanilla constants renamed with it. Every
		// other construction in this mod goes through ACCompat#attributeModifier, which can carry
		// both spellings; these ten cannot, because the id they want is vanilla's own — a weapon has
		// to overwrite the base attack modifier rather than stack a second one beside it.
		//
		// The name argument disappears with the UUID, so the source has to swallow it: it is part of
		// the matched text. All ten sites spell it "Tool modifier", which is why two rules cover them.
		string("!mc21-attr-basedamage", true) {
			replace("AttributeModifier(BASE_ATTACK_DAMAGE_UUID, \"Tool modifier\", ", "AttributeModifier(BASE_ATTACK_DAMAGE_ID, ")
		}
		string("!mc21-attr-basespeed", true) {
			replace("AttributeModifier(BASE_ATTACK_SPEED_UUID, \"Tool modifier\", ", "AttributeModifier(BASE_ATTACK_SPEED_ID, ")
		}

		// ── 1.21 threaded a ServerLevel through the death-drop chain ────────────
		// dropAllDeathLoot and dropCustomDeathLoot take the level they are dropping into instead of
		// reading it off the entity, and both lost the looting multiplier — 1.21 derives looting from
		// the killer's weapon at the loot table instead of passing an int down. dropExperience gained
		// the killer for the same reason. Nine overrides across six entities, all spelled identically
		// because they were copied from vanilla, so rules beat nine pairs of duplicated bodies.
		//
		// The threaded-in parameter is named acServerLevel: no source in this tree uses that name, so
		// it cannot shadow anything at the sites it appears in.
		string("!mc21-dropall-decl", true) {
			replace(
				"protected void dropAllDeathLoot(DamageSource damageSource) {",
				"protected void dropAllDeathLoot(net.minecraft.server.level.ServerLevel acServerLevel, DamageSource damageSource) {",
			)
		}
		string("!mc21-dropall-super", true) {
			replace("super.dropAllDeathLoot(damageSource);", "super.dropAllDeathLoot(acServerLevel, damageSource);")
		}
		string("!mc21-dropcustom-decl", true) {
			replace(
				"protected void dropCustomDeathLoot(DamageSource damageSource, int experience, boolean idk) {",
				"protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel acServerLevel, DamageSource damageSource, boolean idk) {",
			)
		}
		string("!mc21-dropcustom-super", true) {
			replace(
				"super.dropCustomDeathLoot(damageSource, experience, idk);",
				"super.dropCustomDeathLoot(acServerLevel, damageSource, idk);",
			)
		}
		// GumWorm reimplements the whole chain, so it is the one place that calls the two of these
		// itself; its locals are what the targets name.
		string("!mc21-dropcustom-call", true) {
			replace("this.dropCustomDeathLoot(damageSource, i, flag);", "this.dropCustomDeathLoot(acServerLevel, damageSource, flag);")
		}
		// dropExperience gained the ServerLevel too, one release later, so its three rules live in
		// the two banded blocks below rather than here — a rule may not be chained onto another
		// rule's output, and two rules sharing a source would be an ambiguity error.
		// Forge's drop hook lost the looting int with them.
		string("!mc21-livingdrops", true) {
			replace(
				// The last argument is deliberately the `flag` local rather than the field test it
				// holds: a replacement's OUTPUT is not re-scanned, so spelling `lastHurtByPlayerTime`
				// inside this rule's target hid it from the 1.21.5 rename that renames that field.
				"onLivingDrops(this, damageSource, drops, i, flag)",
				"onLivingDrops(this, damageSource, drops, flag)",
			)
		}
		// getExperienceReward became final and asks getBaseExperienceReward for the number, so the
		// five entities that set their own XP move onto that. Both are protected on Mob, and none of
		// the five is called from anywhere in this mod, so the rename is the whole change.
		string("!mc21-xpreward", true) {
			replace("public int getExperienceReward() {", "protected int getBaseExperienceReward() {")
		}

		string("!mc21-vc-vertex", true) { replace(".vertex(", ".addVertex(") }
		string("!mc21-vc-color", true) { replace(".color(", ".setColor(") }
		string("!mc21-vc-uv", true) { replace(".uv(", ".setUv(") }
		string("!mc21-vc-light", true) { replace(".uv2(", ".setLight(") }
		string("!mc21-vc-overlay", true) { replace(".overlayCoords(", ".setOverlay(") }
		string("!mc21-vc-endvertex", true) { replace(".endVertex();", ";") }

		// normal() kept the Pose overload 1.20.5 gave it and only changed name, so this rides on the
		// !mc205-normal-type-* retyping above — same five variable names, for the same reason a bare
		// `.normal(` cannot be used: PoseStack.Pose#normal() is a no-arg accessor that must survive.
		for (name in listOf("matrix3f", "matrix3f1", "matrix3f2", "p_114092_", "p_229108_2_")) {
			string("!mc21-vc-normal-$name", true) {
				replace(".normal($name,", ".setNormal($name,")
			}
		}

		// ── 1.21's spawn packet takes the tracker entry ─────────────────────────
		// Entity#getAddEntityPacket() became getAddEntityPacket(ServerEntity) — the ServerEntity is
		// what actually knows the tracked position/velocity, which the packet used to re-read off the
		// entity. 38 overrides in this mod, all spelled identically, plus the ACPlatform helper they
		// funnel through; the parameter is threaded straight through under a name no source in this
		// tree uses, so it can never shadow anything.
		//
		// A rule rather than a gate for two reasons: 38 gated method headers would be 38 duplicated
		// bodies, and the difference is purely a signature. The helper's own header gets the same
		// treatment; its Forge arm ignores the new parameter (ForgeHooks.getEntitySpawnPacket is
		// still one-arg on 1.21, checked with javap), only the vanilla-constructor arm needs it.
		//
		// The declaration rule is anchored on the return type and the opening brace so it cannot
		// touch the two prose mentions of the method name in ACPlatform's javadoc — replacements
		// rewrite comments too.
		string("!mc21-addentitypacket-decl", true) {
			replace(
				"Packet<ClientGamePacketListener> getAddEntityPacket() {",
				"Packet<ClientGamePacketListener> getAddEntityPacket(net.minecraft.server.level.ServerEntity acServerEntity) {",
			)
		}
		string("!mc21-addentitypacket-helper", true) {
			replace(
				"getEntitySpawningPacket(Entity entity)",
				"getEntitySpawningPacket(Entity entity, net.minecraft.server.level.ServerEntity acServerEntity)",
			)
		}
		string("!mc21-addentitypacket-call", true) {
			replace("ACPlatform.getEntitySpawningPacket(this)", "ACPlatform.getEntitySpawningPacket(this, acServerEntity)")
		}
		string("!mc21-addentitypacket-ctor", true) {
			replace("ClientboundAddEntityPacket(entity)", "ClientboundAddEntityPacket(entity, acServerEntity)")
		}

		// ── 1.21 dropped the position/colour/uv vertex format ───────────────────
		// POSITION_COLOR_TEX and its core shader are gone; POSITION_TEX_COLOR, which existed
		// alongside it since 1.17 and differs only in the order the two trailing elements sit in the
		// buffer, is the survivor. Element order is not something this mod's call sites can see —
		// 1.21's BufferBuilder fills elements by name at their own offset, so the same
		// setColor-then-setUv sequence writes correctly into either — so the swap is a rename.
		//
		// The two irradiated shaders this mod registers against the format need a vertex program to
		// go with it, and the vanilla one was deleted with the format. They point at this mod's own
		// copy instead (assets/alexscaves/shaders/core/ac_position_color_tex.vsh), which serves both
		// element orders because attribute locations bind by name; that part is in the JSON, which no
		// rule or gate can reach.
		//
		// ⚠️ POSITION_COLOR_TEX_LIGHTMAP is a real format on every version — the cave map background
		// uses it — and a rule's source still matches when it is only a *prefix* of the identifier it
		// lands in, so a bare rename rewrites it to a name that does not exist. The delimiter after
		// the constant has to be part of the source; the two spellings in this tree are a comma (an
		// argument) and a close paren (the last argument), so it takes two rules.
		string("!mc21-vertexformat-postexcolor-arg", true) {
			replace("DefaultVertexFormat.POSITION_COLOR_TEX,", "DefaultVertexFormat.POSITION_TEX_COLOR,")
		}
		string("!mc21-vertexformat-postexcolor-last", true) {
			replace("DefaultVertexFormat.POSITION_COLOR_TEX)", "DefaultVertexFormat.POSITION_TEX_COLOR)")
		}
		string("!mc21-shader-postexcolor", true) {
			replace("getPositionColorTexShader", "getPositionTexColorShader")
		}

		// ── Item#getUseDuration was told who is holding the item ────────────────
		// 1.21: getUseDuration(ItemStack) -> getUseDuration(ItemStack, LivingEntity). Eleven
		// overrides in this mod and none of them cares who the user is, so the parameter is
		// accepted and ignored; the eleven internal calls pass null for the same reason. It is a
		// rule rather than eleven gates because gating a method header duplicates its whole body,
		// and because these overrides carry no @Override — on 1.21 a missed one would quietly stop
		// overriding rather than fail to compile, which is a behaviour bug, not a build error.
		//
		// The two spellings of the parameter name are both present in the tree, hence two decl
		// rules; the call rules key off the same two names. `getUseDuration(ItemStack stack)` is
		// not matched by the call rule's `getUseDuration(stack)` — the type sits between.
		//
		// acUser cannot shadow anything: no source in this tree uses that name.
		for (name in listOf("stack", "itemStack")) {
			string("!mc21-usedur-decl-$name", true) {
				replace(
					"public int getUseDuration(ItemStack $name) {",
					"public int getUseDuration(ItemStack $name, net.minecraft.world.entity.LivingEntity acUser) {",
				)
			}
			string("!mc21-usedur-call-$name", true) {
				replace("getUseDuration($name)", "getUseDuration($name, null)")
			}
		}

		// ── The loot context's "killer" is now the "attacker" ───────────────────
		// Renamed in 1.21 because the parameter was always filled from the damage source, whether
		// or not the hit was lethal. Two sites, both building a death-loot LootParams by hand.
		//
		// The `LootContextParams.` prefix is part of both sources on purpose: a rule matches at the
		// right identifier boundary only, so a bare KILLER_ENTITY would also rewrite the tail of
		// DIRECT_KILLER_ENTITY sitting next to it on the same line.
		string("!mc21-loot-killer", true) {
			replace("LootContextParams.KILLER_ENTITY", "LootContextParams.ATTACKING_ENTITY")
		}
		string("!mc21-loot-directkiller", true) {
			replace("LootContextParams.DIRECT_KILLER_ENTITY", "LootContextParams.DIRECT_ATTACKING_ENTITY")
		}
	}

	// ════════════════════════════════════════════════════════════════════════════
	//  1.21.2+
	// ════════════════════════════════════════════════════════════════════════════
	// The 1.21-only shape of dropExperience — see the note in the >=1.21 block. 1.21.2 threads a
	// ServerLevel in front of the killer, so that band gets its own three rules further down.
	if (eval(current.version, ">=1.21") && !eval(current.version, ">=1.21.2")) replacements {
		string("!mc21-dropexp-call", true) {
			replace("this.dropExperience();", "this.dropExperience(entity);")
		}
		string("!mc21-dropexp-decl", true) {
			replace("protected void dropExperience() {", "protected void dropExperience(net.minecraft.world.entity.Entity acKiller) {")
		}
		string("!mc21-dropexp-super", true) {
			replace("super.dropExperience();", "super.dropExperience(acKiller);")
		}
	}

	if (eval(current.version, ">=1.21.2")) replacements {

		// ── Straight renames ────────────────────────────────────────────────────
		// Both stayed in their old package and kept every constant this mod names, so a bare token
		// swap is the whole port. Verified against the 1.21.2 jar: net.minecraft.world.entity
		// .EntitySpawnReason keeps BUCKET/CHUNK_GENERATION/MOB_SUMMONED/NATURAL/SPAWNER/TRIGGERED,
		// and net.minecraft.world.item.ItemUseAnimation keeps NONE/EAT/DRINK/BLOCK/BOW/SPEAR.
		//
		// `UseAnim` may NOT be replaced as a bare token, whatever the boundary rules elsewhere in
		// this file suggest: the tree also holds 11 `getUseAnimation` overrides, and the bare rule
		// rewrites the `UseAnim` inside each of them into `getItemUseAnimationation`. So the three
		// characters that can follow the type name are spelled out instead — `;` for the import,
		// `.` for a constant, and a space for a declaration — which between them cover all 30
		// occurrences and none of the 11 method names. Distinct targets, so no ambiguity.
		string("!mc2102-spawnreason", true) {
			replace("MobSpawnType", "EntitySpawnReason")
		}
		string("!mc2102-useanim-import", true) { replace("UseAnim;", "ItemUseAnimation;") }
		string("!mc2102-useanim-const", true) { replace("UseAnim.", "ItemUseAnimation.") }
		string("!mc2102-useanim-decl", true) { replace("UseAnim ", "ItemUseAnimation ") }

		// ── BakedQuad gained a light emission ───────────────────────────────────
		// 1.21.2 gave a quad the block's baked-in light level. BakedModelShadeLayerFullbright
		// copies a quad, so it has to carry the field through or every fullbright quad silently
		// loses it. This is a rename rule rather than the single-line gate it used to be because
		// the whole class body is now gated `<1.21.4` — the emissive-model seam is gone from
		// there — and a gate inside a disabled arm would be a nested block comment.
		string("!mc2102-bakedquad-lightemission", true) {
			replace("quad.isShade());", "quad.isShade(), quad.getLightEmission());")
		}

		// BakedModel#getOverrides():ItemOverrides became a default overrides():BakedOverrides, in
		// the same package. Only the Fabric BakedModelWrapper stand-in names either — the mod never
		// calls them — so both rules are exact and cannot reach anything else in the tree. The two
		// keys cannot overlap: `getOverrides()` does not occur inside `ItemOverrides`. Which of
		// 1.21.2 / 1.21.3 made the change is an inference: there is no Forge build for 1.21.2 to
		// javap, 1.21.1 has the old shape and 1.21.3 the new one, and it arrived with the item-model
		// rework that 1.21.2 is otherwise full of.
		string("!mc2102-bakedoverrides-type", true) {
			replace("ItemOverrides", "BakedOverrides")
		}
		string("!mc2102-bakedoverrides-call", true) {
			replace("getOverrides()", "overrides()")
		}

		// ── BlockBehaviour#updateShape ──────────────────────────────────────────
		// Reordered and grown by two on 1.21.2:
		//
		//     (state, direction, neighbourState, LevelAccessor, pos, neighbourPos)
		//     (state, LevelReader, ScheduledTickAccess, pos, direction, neighbourPos,
		//      neighbourState, RandomSource)
		//
		// 64 of this mod's blocks override it. Rather than gate 64 headers, the parameter names
		// were first normalised in the source — upstream spelled them seven different ways — so
		// one rule now rewrites every header and a second rewrites the 53 super calls. Keeping the
		// names means not one method BODY changes, on any version.
		//
		// The other two rules are the fallout of LevelAccessor narrowing to LevelReader: the 62
		// scheduleTick calls move to the new tick access, and the single getRandom call takes the
		// random the new signature hands it. Both are safe as bare tokens because every OTHER
		// `levelAccessor` in the tree that schedules a tick or asks for a random lives in a method
		// that still takes a real LevelAccessor, and those five were renamed rather than gated.
		string("!mc2102-updateshape-decl", true) {
			replace(
				"updateShape(BlockState state, Direction direction, BlockState state1, LevelAccessor levelAccessor, BlockPos blockPos, BlockPos blockPos1)",
				"updateShape(BlockState state, net.minecraft.world.level.LevelReader levelAccessor, net.minecraft.world.level.ScheduledTickAccess scheduledTickAccess, BlockPos blockPos, Direction direction, BlockPos blockPos1, BlockState state1, net.minecraft.util.RandomSource randomSource)"
			)
		}
		string("!mc2102-updateshape-super", true) {
			replace(
				"super.updateShape(state, direction, state1, levelAccessor, blockPos, blockPos1)",
				"super.updateShape(state, levelAccessor, scheduledTickAccess, blockPos, direction, blockPos1, state1, randomSource)"
			)
		}
		string("!mc2102-updateshape-tick", true) {
			replace("levelAccessor.scheduleTick(", "scheduledTickAccess.scheduleTick(")
		}
		string("!mc2102-updateshape-random", true) {
			replace("levelAccessor.getRandom()", "randomSource")
		}

		// ── The armour split ────────────────────────────────────────────────────
		// 1.21.2 moved ArmorMaterial into net.minecraft.world.item.equipment and promoted the
		// nested ArmorItem.Type to a top-level ArmorType next to it. Neither changed the constants
		// this mod names (HELMET/CHESTPLATE/LEGGINGS/BOOTS, plus BODY which it does not), so both
		// are import/token swaps rather than gates — the shape changes that DID happen (durability
		// moving back onto the material, the TagKey repair ingredient, the modelId, the deleted
		// `type` field and getDefense()) are handled in ACArmorMaterial and the six armour items.
		//
		// ArmorItem.Type is spelled out at every one of its ~30 call sites — never the bare `Type`
		// an inner class lets a subclass write — precisely so this one rule reaches all of them.
		// The target is fully qualified because the seven files that name it do not all import
		// ArmorItem, and in reverse it is a no-op: root src/ never spells the equipment package.
		string("!mc2102-armortype", true) {
			replace("ArmorItem.Type", "net.minecraft.world.item.equipment.ArmorType")
		}
		string("!mc2102-armormaterial-import", true) {
			replace(
				"import net.minecraft.world.item.ArmorMaterial;",
				"import net.minecraft.world.item.equipment.ArmorMaterial;",
			)
		}

		// ── The enchanted book ──────────────────────────────────────────────────
		// EnchantedBookItem is gone: with enchantments living in a data component the class had no
		// behaviour left, so the book is a plain Item and its one static moved to
		// EnchantmentHelper#createBook(EnchantmentInstance) — same body, same return.
		//
		// The target is fully qualified deliberately. Swapping the IMPORT instead would put
		// `import ...EnchantmentHelper;` on the right-hand side, and the reverse direction of that
		// rule would then strip the import from the dozen unrelated files on older nodes that use
		// EnchantmentHelper for its own sake. Keyed through the open paren so it cannot half-match.
		//
		// This also lands on the output of !mc205-enchantedbook, which rewrites the loot mixin's
		// addEnchantment call into the same createForEnchantment form — correctly, since createBook
		// returns the stack exactly as createForEnchantment did.
		string("!mc2102-enchantedbook", true) {
			replace(
				"EnchantedBookItem.createForEnchantment(",
				"net.minecraft.world.item.enchantment.EnchantmentHelper.createBook(",
			)
		}

		// ── The entity render-state rewrite ─────────────────────────────────────
		// 1.21.2 split entity rendering into an "extract" pass and a "render" pass: every renderer,
		// layer and model gained a render-state type parameter and lost the live entity. That
		// touches 72 renderers, 41 layers and the whole model hierarchy in this tree.
		//
		// Rather than rewrite them, client/render/compat/ carries shim classes with the SAME SIMPLE
		// NAMES as the vanilla ones they stand in for, so a file only needs its import swapped and
		// keeps its old type parameters, its old overrides and its old bodies.
		//
		// That package uses the modern API directly and therefore carries no conditionals of its
		// own; ModPlatformPlugin.configureJava excludes it from the compile below 1.21.2.
		//
		// Keyed on the whole import statement including the trailing `;`, which is what makes the
		// rules safe: EntityRendererProvider / EntityRenderers / EntityRenderDispatcher share the
		// prefix but not the statement, and the compat classes themselves refer to their vanilla
		// counterparts fully-qualified rather than by import.
		//
		// ⚠️ A mixin whose @Mixin target is one of these five vanilla classes must spell the target
		// fully qualified and must never import it — the rule would silently retarget @Mixin at this
		// mod's own shim. It compiles clean and crashes at mixin-apply.
		string("!mc2102-render-import-entity", true) {
			replace(
				"import net.minecraft.client.renderer.entity.EntityRenderer;",
				"import com.github.alexmodguy.alexscaves.client.render.compat.EntityRenderer;",
			)
		}
		string("!mc2102-render-import-living", true) {
			replace(
				"import net.minecraft.client.renderer.entity.LivingEntityRenderer;",
				"import com.github.alexmodguy.alexscaves.client.render.compat.LivingEntityRenderer;",
			)
		}
		string("!mc2102-render-import-mob", true) {
			replace(
				"import net.minecraft.client.renderer.entity.MobRenderer;",
				"import com.github.alexmodguy.alexscaves.client.render.compat.MobRenderer;",
			)
		}
		string("!mc2102-render-import-arrow", true) {
			replace(
				"import net.minecraft.client.renderer.entity.ArrowRenderer;",
				"import com.github.alexmodguy.alexscaves.client.render.compat.ArrowRenderer;",
			)
		}
		string("!mc2102-render-import-layer", true) {
			replace(
				"import net.minecraft.client.renderer.entity.layers.RenderLayer;",
				"import com.github.alexmodguy.alexscaves.client.render.compat.RenderLayer;",
			)
		}
		// The two vanilla layer subclasses this mod extends. Both changed shape beyond what an
		// import swap on their base could carry — EnergySwirlLayer lost its PowerableMob bound and
		// ItemInHandLayer's renderArmWithItem gained a baked model and lost the entity — so each has
		// its own shim next to the base one.
		string("!mc2102-render-import-swirl", true) {
			replace(
				"import net.minecraft.client.renderer.entity.layers.EnergySwirlLayer;",
				"import com.github.alexmodguy.alexscaves.client.render.compat.EnergySwirlLayer;",
			)
		}
		string("!mc2102-render-import-iteminhand", true) {
			replace(
				"import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;",
				"import com.github.alexmodguy.alexscaves.client.render.compat.ItemInHandLayer;",
			)
		}
		// 1.21.2 deleted PowerableMob outright; the swirl layer above still bounds on it, and two of
		// this mod's entities implement it.
		string("!mc2102-render-import-powerable", true) {
			replace(
				"import net.minecraft.world.entity.PowerableMob;",
				"import com.github.alexmodguy.alexscaves.client.render.compat.PowerableMob;",
			)
		}
		// The files that name EntityModel<SomeEntity> as a renderer's or layer's model type. The
		// compat class is the base of this mod's whole model hierarchy on 1.21.2+ (the vendored
		// Citadel BasicEntityModel extends it), so those declarations stay valid with nothing but
		// this swap.
		string("!mc2102-render-import-model", true) {
			replace(
				"import net.minecraft.client.model.EntityModel;",
				"import com.github.alexmodguy.alexscaves.client.render.compat.EntityModel;",
			)
		}

		// ── The build-height accessors ──────────────────────────────────────────
		// LevelHeightAccessor#getMinBuildHeight/getMaxBuildHeight became getMinY/getMaxY, and the
		// two halves are NOT symmetric — read the 1.21.2 interface, not the names:
		//
		//     int getMinY()                                        // == old getMinBuildHeight()
		//     default int getMaxY() { minY + height - 1 }           // INCLUSIVE
		//
		// getMaxBuildHeight() was exclusive (minY + height), so every one of this mod's ~80 call
		// sites — nearly all of them `pos.getY() < level.getMaxBuildHeight()` bounds checks —
		// needs the +1 back or the top world layer silently stops being reachable.
		//
		// A textual `+ 1` inherits the surrounding precedence, so the one site where that would
		// have changed the value (a unary minus in ClientLevelDataMixin) carries explicit
		// parentheses in the source; it is a no-op on older nodes. Every other site is a
		// comparison, an argument or a `- k`, all of which are unaffected.
		//
		// The names appear in no mixin selector string in this tree, so there is nothing here that
		// a token swap could rewrite into a descriptor that no longer resolves.
		string("!mc2102-minbuildheight", true) {
			replace("getMinBuildHeight()", "getMinY()")
		}
		string("!mc2102-maxbuildheight", true) {
			replace("getMaxBuildHeight()", "getMaxY() + 1")
		}

		// ── 1.21.2 threaded a ServerLevel through the damage chain ──────────────
		// Damage stopped being something an entity could take on either side. Entity#hurt is now
		// `public final void` — it checks for a ServerLevel and forwards to the new
		//
		//     public abstract boolean hurtServer(ServerLevel, DamageSource, float)
		//
		// and the same parameter was threaded onto isInvulnerableTo, doHurtTarget, dropEquipment,
		// dropExperience, dropFromLootTable, customServerAiStep and spawnAtLocation, all of which
		// read the level off the entity before.
		//
		// Every one of those is overridden somewhere in this tree, so the level arrives as a
		// parameter and the bodies need no change — which is what makes rules the right tool here
		// rather than ~80 gated signatures. The parameter is named acServerLevel, matching the
		// !mc21-drop* rules that introduced the same name one release earlier; no source in this
		// tree declares that identifier, so it cannot shadow anything.
		//
		// The DECLARATION rules deliberately stop at the first parameter's TYPE rather than
		// matching the whole signature: the overrides spell their parameters five different ways
		// (`source`/`amount`, `damageSource`/`damageValue`, `damageSource`/`f`, …) and matching a
		// prefix means all 23 hurt overrides ride one rule with their names untouched. Each carries
		// its return type so that a call site can never match — `hurt(` on its own would.
		//
		// The CALL rules are bare `.name(` on purpose, because at every occurrence in this tree the
		// receiver is inside the matching override: `super.`/`this.` plus the four sites that ask a
		// sibling part or a parent entity (`body.`, `parent.`, `head.`). Call sites that are NOT in
		// such a scope have already been rewritten to go through ACCompat, which derives the level
		// from the entity — see the helpers there. Two things follow from that split, and both are
		// load-bearing: a new bare call to one of these outside an override will not compile, and
		// the ACCompat helper bodies must never spell the call in a form these rules can match.
		string("!mc2102-hurt-decl", true) {
			replace(
				"boolean hurt(DamageSource ",
				"boolean hurtServer(net.minecraft.server.level.ServerLevel acServerLevel, DamageSource ",
			)
		}
		string("!mc2102-hurt-super", true) {
			replace("super.hurt(", "super.hurtServer(acServerLevel, ")
		}
		// isInvulnerableTo is the one member of this family that did NOT move uniformly, and its
		// rules therefore cover only half its occurrences. LivingEntity kept an overridable
		// isInvulnerableTo and gained the level; Entity did not — its copy became
		//
		//     protected final boolean isInvulnerableToBase(DamageSource)
		//
		// so on an Entity or PartEntity subclass the mod's override is no longer an override at all
		// and its super call has nowhere to go. The two families are textually identical, so the
		// nine files in the Entity half carry hand-written //? arms and the rules below are written
		// to see only a comment there. Both halves are marked in place; grep isInvulnerableToBase.
		string("!mc2102-invulnerable-decl", true) {
			replace(
				"boolean isInvulnerableTo(DamageSource ",
				"boolean isInvulnerableTo(net.minecraft.server.level.ServerLevel acServerLevel, DamageSource ",
			)
		}
		string("!mc2102-invulnerable-super", true) {
			replace("super.isInvulnerableTo(", "super.isInvulnerableTo(acServerLevel, ")
		}
		string("!mc2102-dohurttarget-decl", true) {
			replace(
				"boolean doHurtTarget(Entity ",
				"boolean doHurtTarget(net.minecraft.server.level.ServerLevel acServerLevel, Entity ",
			)
		}
		string("!mc2102-dohurttarget-super", true) {
			replace("super.doHurtTarget(", "super.doHurtTarget(acServerLevel, ")
		}
		string("!mc2102-droploot-decl", true) {
			replace(
				"void dropFromLootTable(DamageSource ",
				"void dropFromLootTable(net.minecraft.server.level.ServerLevel acServerLevel, DamageSource ",
			)
		}
		string("!mc2102-droploot-super", true) {
			replace("super.dropFromLootTable(", "super.dropFromLootTable(acServerLevel, ")
		}
		string("!mc2102-droploot-call", true) {
			replace("this.dropFromLootTable(", "this.dropFromLootTable(acServerLevel, ")
		}
		string("!mc2102-dropequipment-decl", true) {
			replace(
				"void dropEquipment() {",
				"void dropEquipment(net.minecraft.server.level.ServerLevel acServerLevel) {",
			)
		}
		string("!mc2102-dropequipment-super", true) {
			replace("super.dropEquipment()", "super.dropEquipment(acServerLevel)")
		}
		string("!mc2102-dropequipment-call", true) {
			replace("this.dropEquipment()", "this.dropEquipment(acServerLevel)")
		}
		string("!mc2102-customai-decl", true) {
			replace(
				"void customServerAiStep() {",
				"void customServerAiStep(net.minecraft.server.level.ServerLevel acServerLevel) {",
			)
		}
		string("!mc2102-customai-super", true) {
			replace("super.customServerAiStep()", "super.customServerAiStep(acServerLevel)")
		}
		string("!mc2102-spawnatlocation-decl", true) {
			replace(
				"ItemEntity spawnAtLocation(ItemStack ",
				"ItemEntity spawnAtLocation(net.minecraft.server.level.ServerLevel acServerLevel, ItemStack ",
			)
		}
		// dropExperience gained the level in front of the killer that !mc21-dropexp-* introduced,
		// so this band restates all three rules against the original source rather than chaining.
		string("!mc2102-dropexp-call", true) {
			replace("this.dropExperience();", "this.dropExperience(acServerLevel, entity);")
		}
		string("!mc2102-dropexp-decl", true) {
			replace(
				"protected void dropExperience() {",
				"protected void dropExperience(net.minecraft.server.level.ServerLevel acServerLevel, net.minecraft.world.entity.Entity acKiller) {",
			)
		}
		string("!mc2102-dropexp-super", true) {
			replace("super.dropExperience();", "super.dropExperience(acServerLevel, acKiller);")
		}

		// ── The registry-accessor renames ───────────────────────────────────────
		// 1.21.2 shuffled four names across Registry and RegistryAccess so that "get" consistently
		// means "give me the Holder" and "getValue" means "give me the thing":
		//
		//     RegistryAccess#registry          -> #lookup            (Optional<Registry<E>>, same)
		//     RegistryAccess#registryOrThrow   -> #lookupOrThrow
		//     Registry#getHolder               -> #get               (Optional<Holder.Reference<T>>)
		//     Registry#getHolderOrThrow        -> #getOrThrow
		//     Registry#get(ResourceKey/-Location) -> #getValue       (the nullable T)
		//
		// The first four are pure renames with identical return types, so they are rules. The
		// fifth is not, because `get` is still a method — it just answers something else now — so
		// leaving a call site unrewritten is a silent type change rather than a compile error. It
		// is therefore keyed on the specific BuiltInRegistries constants this mod reads by id,
		// rather than on a bare `.get(`, and the handful of other receivers are gated by hand.
		string("!mc2102-registry-lookup", true) { replace(".registry(", ".lookup(") }
		string("!mc2102-registry-lookuporthrow", true) { replace(".registryOrThrow(", ".lookupOrThrow(") }
		string("!mc2102-registry-getholderorthrow", true) { replace(".getHolderOrThrow(", ".getOrThrow(") }
		string("!mc2102-registry-getholder", true) { replace(".getHolder(", ".get(") }
		string("!mc2102-registry-getvalue-entitytype", true) {
			replace("BuiltInRegistries.ENTITY_TYPE.get(", "BuiltInRegistries.ENTITY_TYPE.getValue(")
		}
		string("!mc2102-registry-getvalue-item", true) {
			replace("BuiltInRegistries.ITEM.get(", "BuiltInRegistries.ITEM.getValue(")
		}
		string("!mc2102-registry-getvalue-potion", true) {
			replace("BuiltInRegistries.POTION.get(", "BuiltInRegistries.POTION.getValue(")
		}
		string("!mc2102-registry-getvalue-mobeffect", true) {
			replace("BuiltInRegistries.MOB_EFFECT.get(", "BuiltInRegistries.MOB_EFFECT.getValue(")
		}

		// ── AbstractArrow's inGround field became a getter/setter pair ──────────
		// The eight reads are all spelled this.inGround in the source for the sake of this rule;
		// the two writes cannot be, so they are hand-gated (grep setInGround). The rule rewrites
		// the write sites' inactive arms too, into something that would not compile — which is
		// exactly why they are the arms that are commented out on these versions.
		string("!mc2102-arrow-inground", true) { replace("this.inGround", "this.isInGround()") }

		// ── LootContext parameters got their full name back ────────────────────
		// getParam/hasParam became getParameter/hasParameter when the key type moved out of the
		// loot package to the shared net.minecraft.util.context.ContextKey. The keys themselves
		// are still spelled LootContextParams.X, so the two method names are the whole change.
		string("!mc2102-loot-hasparam", true) { replace("hasParam(", "hasParameter(") }
		string("!mc2102-loot-getparam", true) { replace("getParam(", "getParameter(") }

		// ── Direction.getNearest split in two ──────────────────────────────────
		// The old "which axis does this vector mostly point along" became getApproximateNearest;
		// the name getNearest was reused for a new exact-axis lookup that takes a fallback
		// Direction for the zero vector. Citadel's two pathfinding callers want the old meaning,
		// and both hand it ints that widened to the float overload, so the rename is the whole fix.
		string("!mc2102-direction-getnearest", true) {
			replace("Direction.getNearest(", "Direction.getApproximateNearest(")
		}

		// ── neighborChanged now says which side the update came from ───────────
		// The `BlockPos fromPos` argument became a `net.minecraft.world.level.redstone.Orientation`
		// — the redstone-wire directional-update work — and none of this mod's nine overrides ever
		// read it, so only the type moves. Two of them spelled their parameters `blockPos1`/`idk`;
		// they were renamed to match the other seven so one rule covers all nine, and every one of
		// them gained the `@Override` it never had. That annotation is the point of doing this by
		// hand rather than leaving it: without it a signature change like this one turns nine
		// overrides into nine dead methods that still compile, and only two of the nine — the ones
		// that also call super — would have said a word about it.
		string("!mc2102-neighborchanged", true) {
			replace("BlockPos fromPos", "net.minecraft.world.level.redstone.Orientation fromPos")
		}

		// ── PARTICLE_SHEET_LIT was dropped ─────────────────────────────────────
		// It and PARTICLE_SHEET_OPAQUE had byte-identical begin() bodies bar the shader bind that
		// OPAQUE always did explicitly and LIT inherited from whatever ran before it; 1.21.2 keeps
		// the one that names its shader. Both are the opaque (pre-translucent) pass, same atlas,
		// same blend state, so this is the substitution vanilla itself made.
		string("!mc2102-particle-sheet-lit", true) {
			replace("ParticleRenderType.PARTICLE_SHEET_LIT", "ParticleRenderType.PARTICLE_SHEET_OPAQUE")
		}

		// ── BlockEntityType.Builder was dropped for a plain constructor ────────
		// Builder.of(supplier, blocks...).build(null) is now a constructor call — the DataFixer
		// argument the builder took was already ignored. Two rules because the call is split
		// around its argument list; nothing else in the tree spells ").build(null)".
		//
		// It goes through ACCompat#blockEntityType rather than naming the constructor, because
		// vanilla's takes a Set<Block> and these 21 call sites are variadic. NeoForge patches a
		// varargs overload back in and would not need the helper; Forge does not, and neither
		// will Fabric.
		string("!mc2102-blockentitytype-builder", true) {
			replace("BlockEntityType.Builder.of(", "com.github.alexmodguy.alexscaves.server.misc.ACCompat.blockEntityType(")
		}
		string("!mc2102-blockentitytype-build", true) { replace(").build(null)", ")") }

		// ── getProfiler() moved to a thread-local accessor ─────────────────────
		// Level and Minecraft both lost it; the active ProfilerFiller is Profiler.get() now. All
		// six of this mod's Level-receiver uses are inside the three explosion classes and spell
		// it identically. LightTextureMixin's Minecraft-receiver pair is gated by hand, since a
		// second rule cannot share this one's target.
		string("!mc2102-profiler", true) {
			replace("this.level.getProfiler()", "net.minecraft.util.profiling.Profiler.get()")
		}

		// ── WalkAnimationState#update gained a position scale ───────────────────
		// The third argument scales the walk position, and vanilla's own LivingEntity#
		// calculateEntityAnimation passes `this.isBaby() ? 3.0F : 1.0F`; all 25 of this mod's call
		// sites are inside LivingEntity subclasses (or a LivingEntity mixin, which shadows isBaby
		// for this), so they can pass the same thing. Two rules because upstream spells the speed
		// argument two ways — distinct targets, so no ambiguity.
		string("!mc2102-walkanim", true) {
			replace("walkAnimation.update(f2, 0.4F)", "walkAnimation.update(f2, 0.4F, this.isBaby() ? 3.0F : 1.0F)")
		}
		// ── propagatesSkylightDown lost its getter and position ─────────────────
		// It only ever read the state, and 1.21.2 says so in the signature. Eight of this mod's
		// blocks override it and not one body touched the other two arguments, so the parameter
		// names were normalised in the source (upstream spelled them three ways) and one rule
		// rewrites every header — bodies untouched, on every version. Widening `protected` to
		// `public` is legal, so the modifier stays put. The single *call* site is hand-gated in
		// AmbersolLightBlock, since a second rule would have to share this one's target.
		string("!mc2102-skylight", true) {
			replace(
				"public boolean propagatesSkylightDown(BlockState state, BlockGetter getter, BlockPos pos)",
				"public boolean propagatesSkylightDown(BlockState state)"
			)
		}

		// ── TextureStateShard's blur flag became a TriState ─────────────────────
		// net.minecraft.util.TriState (vanilla's own, not NeoForge's) — DEFAULT means "whatever the
		// texture was registered with". Every one of this mod's 18 shards states the flag outright,
		// so each maps to TRUE or FALSE and nothing changes. Four rules because upstream spells the
		// first argument four ways; they have distinct targets, so no ambiguity.
		string("!mc2102-blur-locationin", true) {
			replace("TextureStateShard(locationIn, false,", "TextureStateShard(locationIn, net.minecraft.util.TriState.FALSE,")
		}
		string("!mc2102-blur-texture", true) {
			replace("TextureStateShard(texture, false,", "TextureStateShard(texture, net.minecraft.util.TriState.FALSE,")
		}
		string("!mc2102-blur-resloc-false", true) {
			replace("TextureStateShard(resourceLocation, false,", "TextureStateShard(resourceLocation, net.minecraft.util.TriState.FALSE,")
		}
		string("!mc2102-blur-resloc-true", true) {
			replace("TextureStateShard(resourceLocation, true,", "TextureStateShard(resourceLocation, net.minecraft.util.TriState.TRUE,")
		}

		// ── Item#releaseUsing returns a boolean ─────────────────────────────────
		// "Did the item do something", which ItemStack#releaseUsing uses to decide whether to apply
		// the after-use component side effects (use_remainder and friends). None of this mod's
		// eleven overrides carries those components, so every one of them returns `false` — exactly
		// what the old `void` shape did — through a one-line gate at the end of each body. Only the
		// header is a rule; a rule cannot add the return statement.
		string("!mc2102-releaseusing", true) {
			replace("public void releaseUsing(", "public boolean releaseUsing(")
		}

		string("!mc2102-walkanim-const", true) {
			replace("walkAnimation.update(0.5F, 0.4F)", "walkAnimation.update(0.5F, 0.4F, this.isBaby() ? 3.0F : 1.0F)")
		}

		// ── The crafting remainder ──────────────────────────────────────────────
		// 1.21.2 renamed the loaders' stack-aware container-item hook: what was
		// ItemStack#getCraftingRemainingItem() (a Forge/NeoForge extension for as long as this
		// mod's range goes back) is now ItemStack#getCraftingRemainder(), matching the
		// minecraft:use_remainder component that vanilla grew alongside it. Same answer, same
		// no-argument shape; four call sites, all feeding a dinosaur's eaten stack back.
		string("!mc2102-crafting-remainder", true) {
			replace("getCraftingRemainingItem()", "getCraftingRemainder()")
		}

		// ── Core shader programs ────────────────────────────────────────────────
		// 1.21.2 took the compiled-shader getters off GameRenderer and made the core programs
		// declarations in their own right: net.minecraft.client.renderer.CoreShaders holds one
		// ShaderProgram constant per built-in, and RenderSystem.setShader gained an overload that
		// takes it and does the compilation lookup itself. So the old supplier method reference
		// becomes a plain constant — 16 call sites, all of them "bind a vanilla core shader before
		// a hand-rolled buffer draw". Fully qualified so no call site needs a new import.
		string("!mc2102-coreshader-position-tex-color", true) {
			replace("GameRenderer::getPositionTexColorShader", "net.minecraft.client.renderer.CoreShaders.POSITION_TEX_COLOR")
		}
		string("!mc2102-coreshader-position-tex", true) {
			replace("GameRenderer::getPositionTexShader", "net.minecraft.client.renderer.CoreShaders.POSITION_TEX")
		}
		string("!mc2102-coreshader-position-color", true) {
			replace("GameRenderer::getPositionColorShader", "net.minecraft.client.renderer.CoreShaders.POSITION_COLOR")
		}

		// ── Occlusion shape ────────────────────────────────────────────────────
		// 1.21.2 narrowed BlockBehaviour#getOcclusionShape to the state alone — the light engine
		// now caches one shape per state rather than asking per position. The one override in this
		// mod (ice cream's dripping/above shape) already answered from the state only.
		string("!mc2102-occlusion-shape", true) {
			replace("getOcclusionShape(BlockState state, BlockGetter getter, BlockPos blockPos)", "getOcclusionShape(BlockState state)")
		}

		// ── Straight renames ───────────────────────────────────────────────────
		// Each of these is a member that kept its shape and changed only its name in 1.21.2;
		// they are grouped because there is nothing to say about any of them individually
		// beyond "javap says the old spelling is gone and the new one has the same descriptor".
		//
		//   Minecraft#getTimer                 -> getDeltaTracker   (2 sites, both in ACClientCompat's
		//                                                            own >=1.21 arm)
		//   RenderType#entityGlintDirect       -> entityGlint       (vanilla folded the "direct"
		//                                                            variant away; 2 sites, the
		//                                                            custom armour foil pass)
		//   Direction#getNormal                -> getUnitVec3i      (2 sites, barrel open/close
		//                                                            sound position)
		//   Entity#checkInsideBlocks           -> applyEffectsFromBlocks
		//   ChunkAccess#setUnsaved(true)       -> markUnsaved()     (the setter lost its argument;
		//                                                            isUnsaved() still reads it)
		//   Block#updateEntityAfterFallOn      -> updateEntityMovementAfterFallOn  (also rewrites
		//                                                            the @At descriptor in
		//                                                            EntityMixin, which is right)
		string("!mc2102-delta-tracker", true) {
			replace("getTimer()", "getDeltaTracker()")
		}
		// 1.21.11 moved every static factory off RenderType onto a sibling RenderTypes class, so the
		// owner this rule emits is version-dependent. It CANNOT be a second rule in the 1.21.11 group:
		// replacement rules do not chain — every rule matches the ORIGINAL file text, so one keyed on
		// `RenderType.entityGlint()` would never see what this rule produced. Same shape as the
		// entityInside tail the 1.21.10 wave had to grow.
		val glintOwner = if (eval(current.version, ">=1.21.11")) "RenderTypes" else "RenderType"
		string("!mc2102-entity-glint-direct", true) {
			replace("RenderType.entityGlintDirect()", "$glintOwner.entityGlint()")
		}
		string("!mc2102-direction-unit-vec", true) {
			replace("getNormal()", "getUnitVec3i()")
		}
		string("!mc2102-effects-from-blocks", true) {
			replace("checkInsideBlocks()", "applyEffectsFromBlocks()")
		}
		string("!mc2102-mark-unsaved", true) {
			replace("setUnsaved(true)", "markUnsaved()")
		}
		string("!mc2102-fall-on-movement", true) {
			replace("updateEntityAfterFallOn", "updateEntityMovementAfterFallOn")
		}

		// ── The dispenser's equip behaviour is its own class now ───────────────
		// 1.21.2 stripped ArmorItem down to a constructor — everything else about wearing an item
		// moved to the equippable component — and the dispenser behaviour that used to hang off it
		// as ArmorItem.DISPENSE_ITEM_BEHAVIOR is now EquipmentDispenseItemBehavior.INSTANCE. One
		// site, the galena gauntlet.
		string("!mc2102-equipment-dispense", true) {
			replace("ArmorItem.DISPENSE_ITEM_BEHAVIOR", "net.minecraft.core.dispenser.EquipmentDispenseItemBehavior.INSTANCE")
		}

		// ── HoneyBottleItem is gone ────────────────────────────────────────────
		// From 1.21.2 a honey bottle is a plain Item whose behaviour is entirely components, so the
		// class it used to be an instance of no longer exists. The one test for it — the sack of
		// sating handing the empty bottle back — becomes an identity check against the item itself,
		// which is what the instanceof meant in the first place (nothing else ever subclassed it).
		string("!mc2102-honey-bottle-item", true) {
			replace("instanceof HoneyBottleItem", "== net.minecraft.world.item.Items.HONEY_BOTTLE")
		}

		// ── The debug line box moved off LevelRenderer ─────────────────────────
		// 1.21.2 split the shape/outline drawing helpers out of LevelRenderer into
		// net.minecraft.client.renderer.ShapeRenderer, unchanged. Two sites, both the magnet
		// block's range box. Fully qualified so the file needs no new import — and note that
		// Citadel's own WorldRenderMacros#renderLineBox is called unqualified, so the
		// LevelRenderer prefix in the source is what keeps these rules apart.
		//
		// 1.21.11 then deleted every renderLineBox overload — a line's width became a per-vertex
		// format element, leaving only renderShape(…, int color, float lineWidth) — so from there the
		// call goes to ACClientCompat#renderLineBox, which makes exactly that call. It has to be this
		// rule's target rather than a second rule in the >=1.21.11 group: rules do not chain, every
		// one of them matches the ORIGINAL file text, so a rule keyed on the ShapeRenderer spelling
		// this one emits could never fire.
		val lineBoxOwner = if (eval(current.version, ">=1.21.11")) {
			"com.github.alexmodguy.alexscaves.client.ACClientCompat"
		} else {
			"net.minecraft.client.renderer.ShapeRenderer"
		}
		string("!mc2102-shape-renderer-line-box", true) {
			replace("LevelRenderer.renderLineBox(", "$lineBoxOwner.renderLineBox(")
		}

		// ── The model id helper lost its vanilla() shorthand ───────────────────
		// ModelResourceLocation is a record from 1.21.2 with only inventory()/standalone()
		// factories left; vanilla(name, variant) was exactly the constructor with a
		// default-namespaced id, so that is what it becomes. Two sites, the item-frame map model.
		string("!mc2102-mrl-vanilla-map-frame", true) {
			replace("ModelResourceLocation.vanilla(\"item_frame\", \"map=true\")", "new ModelResourceLocation(net.minecraft.resources.ResourceLocation.withDefaultNamespace(\"item_frame\"), \"map=true\")")
		}
		string("!mc2102-mrl-vanilla-glow-map-frame", true) {
			replace("ModelResourceLocation.vanilla(\"glow_item_frame\", \"map=true\")", "new ModelResourceLocation(net.minecraft.resources.ResourceLocation.withDefaultNamespace(\"glow_item_frame\"), \"map=true\")")
		}
	}

	// ── 1.21.4: the armour layer enum moved client-side ─────────────────────────
	// The type that says "which of an equipment asset's layers is being drawn" was a server-side
	// nested enum for exactly two versions; 1.21.4 moved the whole class into the client resource
	// package as EquipmentClientInfo, with the same constants. One site — the NeoForge arm of
	// ACArmorRenderProperties' loader hook — and the rule is fully qualified so it cannot fire on
	// the comment above that arm, which names the old type without its package.
	if (eval(current.version, ">=1.21.4")) replacements {
		string("!mc2104-equip-layertype", true) {
			replace(
				"net.minecraft.world.item.equipment.EquipmentModel.LayerType",
				"net.minecraft.client.resources.model.EquipmentClientInfo.LayerType",
			)
		}
	}

	// ── 1.21.5: package moves and the weighted-list rebuild ─────────────────────
	if (eval(current.version, ">=1.21.5")) replacements {
		// Two mobs got their own package alongside the variant registries that arrived with them.
		// Fully qualified, so neither can fire on a bare type name anywhere else.
		string("!mc2105-wolf-package", true) {
			replace("net.minecraft.world.entity.animal.Wolf", "net.minecraft.world.entity.animal.wolf.Wolf")
		}
		string("!mc2105-frogvariant-package", true) {
			replace("net.minecraft.world.entity.animal.FrogVariant", "net.minecraft.world.entity.animal.frog.FrogVariant")
		}

		// A splash potion and a lingering potion are separate entities now, both under
		// AbstractThrownPotion. The (Level, LivingEntity, ItemStack) constructor this mod uses
		// survives verbatim on the splash one, so the throw site is a pure rename.
		string("!mc2105-thrownpotion", true) {
			replace("ThrownPotion", "ThrownSplashPotion")
		}

		// WeightedRandomList became WeightedList in the same package, and the weight moved out of
		// the element and into a Weighted<E> wrapper. isEmpty/getRandom are unchanged, so only
		// unwrap() and create() have to be gated (see AmberMonolithBlockEntity); the type name and
		// SpawnerData's now-record accessors are renames. Each rule names its variable, because a
		// bare ".type" would fire on half the codebase.
		string("!mc2105-weightedlist", true) {
			replace("WeightedRandomList", "WeightedList")
		}
		// SimpleWeightedRandomList went the same way and did not survive as an alias, so a biome's
		// music is an Optional<WeightedList<Music>> from 1.21.5. Its picker is spelled getRandom
		// rather than getRandomValue — same Optional<E> return, so only the name moves. One call
		// site (MinecraftMixin's 1.21.4 arm); the receiver is carried along so the rule cannot fire
		// on any other getRandom* in the tree.
		string("!mc2105-weightedlist-pick", true) {
			replace(
				"list.getRandomValue(this.player.level().getRandom())",
				"list.getRandom(this.player.level().getRandom())"
			)
		}
		string("!mc2105-spawnerdata-settings-type", true) {
			replace("settings.type", "settings.type()")
		}
		string("!mc2105-spawnerdata-type", true) {
			replace("spawnerData.type", "spawnerData.type()")
		}
		string("!mc2105-spawnerdata-mincount", true) {
			replace("spawnerData.minCount", "spawnerData.minCount()")
		}
		string("!mc2105-spawnerdata-maxcount", true) {
			replace("spawnerData.maxCount", "spawnerData.maxCount()")
		}
		string("!mc2105-mixin-spawnerdata-type", true) {
			replace("mobspawnsettings\$spawnerdata.type", "mobspawnsettings\$spawnerdata.type()")
		}
		string("!mc2105-mixin-spawnerdata-mincount", true) {
			replace("mobspawnsettings\$spawnerdata.minCount", "mobspawnsettings\$spawnerdata.minCount()")
		}
		string("!mc2105-mixin-spawnerdata-maxcount", true) {
			replace("mobspawnsettings\$spawnerdata.maxCount", "mobspawnsettings\$spawnerdata.maxCount()")
		}

		// ── ArmorItem is gone ──────────────────────────────────────────────
		// 1.21.5 deleted net.minecraft.world.item.ArmorItem and net.minecraft.world.item.SwordItem;
		// an armour piece or a sword is a plain Item whose Properties carry humanoidArmor(material,
		// type) / sword(material, damage, speed). The class headers and the two constructors are
		// gated at the source (six armour classes plus DesolateDaggerItem) because the shape
		// changes; the two things below are pure token substitutions.
		//
		// ArmorItem.Type moved to net.minecraft.world.item.equipment.ArmorType and is written out in
		// full so no import has to move with it — the six armour classes and ACItemRegistry reach it
		// through `import net.minecraft.world.item.ArmorItem` / `import ...item.*`, neither of which
		// covers the equipment package. The `import net.minecraft.world.item.ArmorItem;` lines that
		// are left behind are gated `<1.21.5` in each file rather than renamed here: renaming an
		// import and renaming the type it names are not order-independent, which is a configuration
		// error rather than a bug you find later.
		string("!mc2105-armortype", true) {
			replace("ArmorItem.Type", "net.minecraft.world.item.equipment.ArmorType")
		}
		// All six sets construct identically, so one rule restates the super() call. What used to be
		// ArmorItem's three-argument constructor is now Item's one-argument one, with the material
		// and the slot applied inside ACArmorMaterial#properties via Item.Properties#humanoidArmor.
		string("!mc2105-armor-super", true) {
			replace(
				"super(armorMaterial.vanilla(), slot, armorMaterial.properties(",
				"super(armorMaterial.properties("
			)
		}

		// ── MobEffects finally uses the names the players see ──────────────
		// Six of the twenty constants this mod names were still spelled the way they were in beta;
		// 1.21.5 renamed them to match their translation keys. Same effects, same ids, so each is a
		// token substitution — qualified, so the reverse direction can only fire on the constant.
		string("!mc2105-effect-haste", true) { replace("MobEffects.DIG_SPEED", "MobEffects.HASTE") }
		string("!mc2105-effect-fatigue", true) { replace("MobEffects.DIG_SLOWDOWN", "MobEffects.MINING_FATIGUE") }
		string("!mc2105-effect-nausea", true) { replace("MobEffects.CONFUSION", "MobEffects.NAUSEA") }
		string("!mc2105-effect-strength", true) { replace("MobEffects.DAMAGE_BOOST", "MobEffects.STRENGTH") }
		string("!mc2105-effect-resistance", true) { replace("MobEffects.DAMAGE_RESISTANCE", "MobEffects.RESISTANCE") }
		string("!mc2105-effect-speed", true) { replace("MobEffects.MOVEMENT_SPEED", "MobEffects.SPEED") }

		// ── the tooltip hook grew a TooltipDisplay and hands out a Consumer ─
		// 1.21.5 gave Item#appendHoverText the stack's TooltipDisplay component (which says what
		// vanilla itself has been told to hide) and replaced the mutable List the lines were added
		// to with a Consumer. No implementor here reads the new parameter and none of them ever did
		// anything with the list but append, so all seventeen overrides are token substitutions:
		// the tail of the declaration, the appender, and the twelve pass-throughs to super. The
		// three odd spellings among the seventeen were normalised onto one first, because two rules
		// producing the same target fail configuration with "Ambiguous replacement".
		string("!mc2105-tooltip-display", true) {
			replace(
				"List<Component> tooltip, TooltipFlag flagIn) {",
				"net.minecraft.world.item.component.TooltipDisplay acDisplay, java.util.function.Consumer<Component> tooltip, TooltipFlag flagIn) {"
			)
		}
		string("!mc2105-tooltip-accept", true) {
			replace("tooltip.add(", "tooltip.accept(")
		}
		string("!mc2105-tooltip-super", true) {
			replace(
				"super.appendHoverText(stack, worldIn, tooltip, flagIn);",
				"super.appendHoverText(stack, worldIn, acDisplay, tooltip, flagIn);"
			)
		}

		// ── the plant base class split in two ──────────────────────────────
		// What this mod's six plants extended is now called VegetationBlock, in the same package and
		// with the same members (mayPlaceOn, the canSurvive/updateShape pair); the old name survives
		// as a *concrete* subclass of it whose codec() is declared MapCodec<BushBlock>, i.e. no longer
		// overridable. So this is a rename rather than a shape change — but it has to be spelled two
		// ways instead of one, because a bare token would also rewrite SweetBerryBushBlock (matching
		// is identifier-boundary aware only on the right edge).
		string("!mc2105-vegetationblock-import", true) {
			replace(
				"net.minecraft.world.level.block.BushBlock",
				"net.minecraft.world.level.block.VegetationBlock"
			)
		}
		string("!mc2105-vegetationblock-extends", true) {
			replace("extends BushBlock", "extends VegetationBlock")
		}

		// ── a bucket knows who swung it ────────────────────────────────────
		// BucketPickup#pickupBlock and LiquidBlockContainer#canPlaceLiquid widened the actor they
		// gained in 1.20.2 from Player to LivingEntity, for the same game-event attribution. Nothing
		// here reads it. Each rule carries its method name so the four unrelated helpers that declare
		// a `Player player` first parameter are out of reach in both directions.
		string("!mc2105-pickupblock-actor", true) {
			replace(
				"pickupBlock(net.minecraft.world.entity.player.Player player,",
				"pickupBlock(net.minecraft.world.entity.LivingEntity player,"
			)
		}
		string("!mc2105-placeliquid-actor", true) {
			replace(
				"canPlaceLiquid(net.minecraft.world.entity.player.Player player,",
				"canPlaceLiquid(net.minecraft.world.entity.LivingEntity player,"
			)
		}

		// ── being inside a block is applied, not done ──────────────────────
		// entityInside gained an InsideBlockEffectApplier at 1.21.5, so vanilla can defer its own
		// effects to the end of the movement step, and a trailing boolean at 1.21.10 — true when
		// the entity's bounding box really intersects this block, false when it merely swept
		// through it during the step (Entity#checkInsideBlocks computes it as
		// `flag || aabb.intersects(pos)`). None of the nine blocks here wants either: they all act
		// immediately, on any pass. So both parameters are added and ignored.
		//
		// The 1.21.10 half is a VARYING TARGET on the same two rules rather than a second rule in
		// a `>=1.21.10` group, because replacement groups do NOT chain: every rule matches against
		// the ORIGINAL file text, so a later rule keyed on what an earlier one produced silently
		// never fires. (Only PurpleSodaBlock carries an `@Override` and would have reported it —
		// the other eight would have compiled into methods that override nothing.)
		//
		// Two spellings because the upstream files disagree on the parameter names, and each rule
		// names the method so nothing else with a trailing `Entity entity` is in reach.
		val acEntityInsideTail = if (eval(current.version, ">=1.21.10"))
			", net.minecraft.world.entity.InsideBlockEffectApplier acApplier, boolean acReallyInside) {"
		else
			", net.minecraft.world.entity.InsideBlockEffectApplier acApplier) {"
		string("!mc2105-entityinside-a", true) {
			replace(
				"entityInside(BlockState blockState, Level level, BlockPos pos, Entity entity) {",
				"entityInside(BlockState blockState, Level level, BlockPos pos, Entity entity$acEntityInsideTail"
			)
		}
		string("!mc2105-entityinside-b", true) {
			replace(
				"entityInside(BlockState state, Level level, BlockPos blockPos, Entity entity) {",
				"entityInside(BlockState state, Level level, BlockPos blockPos, Entity entity$acEntityInsideTail"
			)
		}

		// ── who is authoritative over this entity ──────────────────────────
		// Renamed, same meaning and no arguments: "does this side own the movement of this entity".
		// Three call sites, one of which is an override, so a rule beats three gates.
		string("!mc2105-local-authority", true) {
			replace("isControlledByLocalInstance()", "isLocalInstanceAuthoritative()")
		}

		// ── the last player to hurt me became a reference ──────────────────
		// LivingEntity#lastHurtByPlayer is an EntityReference<Player> now — it survives the player
		// logging out — so reading it goes through the public resolver instead. The companion
		// counter was renamed in the same pass. The right edge of a rule's token is a boundary,
		// so the shorter rule cannot fire inside the longer name.
		string("!mc2105-lasthurtbyplayer", true) {
			replace("this.lastHurtByPlayer", "this.getLastHurtByPlayer()")
		}
		string("!mc2105-lasthurtbyplayertime", true) {
			replace("lastHurtByPlayerTime", "lastHurtByPlayerMemoryTime")
		}

		// ── daylight is a property of the sky, not the clock ───────────────
		// Level#isDay/#isNight are gone; what they asked — "is the sky bright where the sun is not
		// fixed" — is now spelled directly. Both bodies are the old ones verbatim.
		string("!mc2105-isday", true) {
			replace(".isDay()", ".isBrightOutside()")
		}
		string("!mc2105-isnight", true) {
			replace(".isNight()", ".isDarkOutside()")
		}

		// ── one of the three wetness tests survived ────────────────────────
		// isInWaterRainOrBubble is gone; isInWaterOrRain is what is left, and a bubble column is a
		// water block, so the only case this loses is standing in the column's air pocket.
		string("!mc2105-inwaterorrain", true) {
			replace("isInWaterRainOrBubble()", "isInWaterOrRain()")
		}

		// ── a model's particle sprite lost its get- ────────────────────────
		string("!mc2105-particleicon", true) {
			replace(".getParticleIcon()", ".particleIcon()")
		}

		// ── the anvil event's two costs got told apart ─────────────────────
		// NeoForge renamed setCost to setXpCost next to the material cost it was always paired with.
		// Forge still spells it setCost (and takes a long), so this is a loader rule. The three
		// occurrences are the three version arms of the same handler; the vendored pathfinder's
		// MNode.setCost is spelled `node.setCost(` and is out of reach.
		if (current.project.endsWith("-neoforge")) {
			string("!mc2105-anvil-xpcost", true) {
				replace("event.setCost(", "event.setXpCost(")
			}
		}

		// ── the weighted list's random pick lost its -Value ────────────────
		// SimpleWeightedRandomList#getRandomValue -> WeightedList#getRandom; same Optional return.
		// Spelled out in full rather than as a bare rename: `getRandom(` already appears on two
		// other weighted lists here, and this rule's reverse direction would rewrite those.
		string("!mc2105-weighted-getrandom", true) {
			replace(
				"list.getRandomValue(this.player.level().random)",
				"list.getRandom(this.player.level().random)"
			)
		}

		// ── a top-level item resolve no longer asks about handedness ───────
		// ItemModelResolver#updateForTopItem dropped its left-handed flag (the transform carries it
		// now). Only ACItemRenderCompat calls it, and it always passed false.
		string("!mc2105-updatefortopitem", true) {
			replace(".updateForTopItem(state, stack, ctx, false, level, null, 0)", ".updateForTopItem(state, stack, ctx, level, null, 0)")
		}

		// ── "draws as a block" is now asked as "lights as a block" ─────────
		// ItemStackRenderState#isGui3d was renamed usesBlockLight; vanilla's own GUI code asks the
		// new name at exactly the site that asked the old one.
		string("!mc2105-usesblocklight", true) {
			replace(".isGui3d()", ".usesBlockLight()")
		}

		// ── a fall distance is a double now ────────────────────────────────
		// Entity#causeFallDamage and LivingEntity#calculateFallDamage widened their first
		// parameter from float to double. Only the declarations carry a type, so matching on
		// "(float" rewrites every override and touches no call site (a float widens on its own).
		// This one is worth a rule rather than gates because getting it wrong is SILENT: an
		// un-widened override compiles as a new overload and the entity simply stops being
		// fall-damage-immune. Two of the four sites had no @Override to catch it.
		string("!mc2105-causefalldamage", true) {
			replace("causeFallDamage(float", "causeFallDamage(double")
		}
		string("!mc2105-calculatefalldamage", true) {
			replace("calculateFallDamage(float", "calculateFallDamage(double")
		}
	}

	// ── 1.21.6: mechanical vanilla renames ──────────────────────────────────────
	if (eval(current.version, ">=1.21.6")) replacements {

		// ── a mob's "restriction" is now its "home" ────────────────────────
		// Mob#restrictTo/getRestrictCenter/hasRestriction/clearRestriction were renamed wholesale
		// to setHomeTo/getHomePosition/hasHome/clearHome. Same semantics, same signatures; the
		// radius accessor AC never used came along as getHomeRadius.
		string("!mc2106-home-set", true) { replace("restrictTo(", "setHomeTo(") }
		string("!mc2106-home-center", true) { replace("getRestrictCenter()", "getHomePosition()") }
		string("!mc2106-home-has", true) { replace("hasRestriction()", "hasHome()") }
		string("!mc2106-home-clear", true) { replace("clearRestriction()", "clearHome()") }

		// ── collision is asked on behalf of somebody ───────────────────────
		// Entity#canBeCollidedWith() gained the colliding entity (@Nullable). None of AC's nine
		// overrides cares who is asking, so the parameter is only ever forwarded — which is why
		// the multipart bodies that delegate to their parent need the second rule. All fifteen
		// sites are textually uniform, so two rules beat nine gates.
		string("!mc2106-collide-decl", true) {
			replace(
				"public boolean canBeCollidedWith() {",
				"public boolean canBeCollidedWith(net.minecraft.world.entity.Entity acCollider) {"
			)
		}
		string("!mc2106-collide-call", true) {
			replace("parent.canBeCollidedWith()", "parent.canBeCollidedWith(acCollider)")
		}

		// ── ServerPlayer#level() is covariant now ──────────────────────────
		// It returns ServerLevel directly, so the narrowing accessor is gone. Spelled out with
		// its receiver because a bare rename's reverse direction would rewrite every level() call.
		string("!mc2106-serverlevel", true) {
			replace("serverPlayer.serverLevel()", "serverPlayer.level()")
		}

		// ── the cloud's particle became explicitly custom ──────────────────
		// AreaEffectCloud#setParticle -> setCustomParticle(@Nullable ParticleOptions); getParticle
		// kept its name. All four call sites in this tree are on an AreaEffectCloud.
		string("!mc2106-cloud-particle", true) { replace(".setParticle(", ".setCustomParticle(") }

		// ── the narrator says which channel it is speaking on ──────────────
		// GameNarrator#sayNow -> saySystemNow; the family also gained sayChatQueued /
		// saySystemChatQueued / saySystemQueued. Matching on the open paren rewrites the three
		// calls AND the @At target descriptor in ClientPacketListenerMixin in one rule.
		string("!mc2106-narrator-saynow", true) { replace("sayNow(", "saySystemNow(") }

		// ── FlyingMob was deleted ──────────────────────────────────────────
		// Ghast and Phantom extend Mob directly now, and they were the only two subclasses, so
		// naming both is exactly what the instanceof used to mean. One site.
		string("!mc2106-flyingmob", true) {
			replace(
				"controlledEntity instanceof FlyingMob;",
				"controlledEntity instanceof net.minecraft.world.entity.monster.Ghast || controlledEntity instanceof net.minecraft.world.entity.monster.Phantom;"
			)
		}

		// ── a fluid's chunk layer is its own enum ──────────────────────────
		// ItemBlockRenderTypes.setRenderLayer takes a ChunkSectionLayer rather than a RenderType.
		// Matched on the argument tail: those four fluid registrations are the only
		// `.get(), RenderType.x()` in the tree, and a bare RenderType rename would hit the
		// dozens of unrelated cutoutMipped()/translucent() uses.
		//
		// 1.21.11 then collapsed the two cutout layers into one: the enum has only SOLID, CUTOUT,
		// TRANSLUCENT and TRIPWIRE, and vanilla's own ItemBlockRenderTypes gives CUTOUT to everything
		// that used to be either — so the mipmapped variant is what CUTOUT now is. Folded into this
		// rule's target as a version-dependent val rather than added as a second rule, because
		// replacement rules do not chain: every rule matches the ORIGINAL file text, so one keyed on
		// `ChunkSectionLayer.CUTOUT_MIPPED` would never fire.
		val fluidCutoutLayer = if (eval(current.version, ">=1.21.11")) "CUTOUT" else "CUTOUT_MIPPED"
		string("!mc2106-fluidlayer-cutout", true) {
			replace(
				".get(), RenderType.cutoutMipped());",
				".get(), net.minecraft.client.renderer.chunk.ChunkSectionLayer.$fluidCutoutLayer);"
			)
		}
		string("!mc2106-fluidlayer-translucent", true) {
			replace(
				".get(), RenderType.translucent());",
				".get(), net.minecraft.client.renderer.chunk.ChunkSectionLayer.TRANSLUCENT);"
			)
		}

		// ── ValueInput / ValueOutput ───────────────────────────────────────
		// 1.21.6 replaced the CompoundTag on every save and load signature with the
		// ValueInput/ValueOutput pair — an abstraction over "a keyed tree of values" that also
		// carries a ProblemReporter, so a malformed field is reported instead of silently
		// defaulting. Roughly 150 overrides in this tree are affected.
		//
		// Every one of their BODIES already reads through the ACCompat.getX helpers, so porting
		// them individually would be ~150 identical mechanical rewrites for no behavioural gain.
		// Only the HEADER is rewritten here: the parameter becomes the new type under a bridge
		// name, and the ORIGINAL name is re-bound on the same line to the CompoundTag behind it
		// (ACCompat.tagOf). Bodies are untouched on all 58 nodes. What makes that legal is that
		// both directions are zero-copy on the one implementation of each interface vanilla
		// declares — see the long note in ACCompat, which is where the casts live.
		//
		// The bridge name carries the original parameter name as a suffix — acIn_tag,
		// acOut_compound — for one specific reason: every rule's TARGET has to be unique. Two
		// rules that produced the same text (`super.addAdditionalSaveData(acOutput);` from both
		// the `compound` and the `tag` form) fail configuration as an ambiguous replacement,
		// because a reversible rule set must also be able to run backwards.
		val acIn = "net.minecraft.world.level.storage.ValueInput"
		val acOut = "net.minecraft.world.level.storage.ValueOutput"
		val acCompat = "com.github.alexmodguy.alexscaves.server.misc.ACCompat"
		val acProv = "net.minecraft.core.HolderLookup.Provider"

		// Entities. Anchoring on the full `(CompoundTag <name>) {` means a rule can only ever
		// match a declaration, never a call; the modifier in front is left alone, which is why
		// the source starts at `void`. `nbt` has no super-calling override — its two rules find
		// nothing and are registered only so the four names stay one list.
		for (name in listOf("compound", "compoundTag", "tag", "nbt")) {
			string("!mc216-entity-savesig-$name", true) {
				replace(
					"void addAdditionalSaveData(CompoundTag $name) {",
					"void addAdditionalSaveData($acOut acOut_$name) { CompoundTag $name = $acCompat.tagOf(acOut_$name);"
				)
			}
			string("!mc216-entity-readsig-$name", true) {
				replace(
					"void readAdditionalSaveData(CompoundTag $name) {",
					"void readAdditionalSaveData($acIn acIn_$name) { CompoundTag $name = $acCompat.tagOf(acIn_$name);"
				)
			}
			string("!mc216-entity-savesuper-$name", true) {
				replace("super.addAdditionalSaveData($name);", "super.addAdditionalSaveData(acOut_$name);")
			}
			string("!mc216-entity-readsuper-$name", true) {
				replace("super.readAdditionalSaveData($name);", "super.readAdditionalSaveData(acIn_$name);")
			}
		}

		// The five multipart entities refuse to be saved at all — every body is `return false;`,
		// so nothing is re-bound. Entity#save is still the boolean-returning override it was.
		string("!mc216-multipart-save", true) {
			replace("public boolean save(CompoundTag tag) {", "public boolean save($acOut acOut_save) {")
		}

		// Block entities. These also introduce `acRegistries`, which the 1.20.5 block's
		// !mc205-be-stackregistries rule still rewrites ACCompat.BE_REGISTRIES into: a block entity
		// has no level to ask for a lookup while it loads, so it takes the one ValueInput publishes
		// as lookup(). ValueOutput publishes none and its implementation offers no way back to one
		// (see the note in ACCompat) — but a block entity being SAVED is in a level, so the save
		// rule asks the block entity itself. Most bodies never touch the variable, which Java is
		// content with.
		for (name in listOf("tag", "compound", "compoundTag", "p_155055_")) {
			string("!mc216-be-loadsig-$name", true) {
				replace(
					"void load(CompoundTag $name) {",
					"void loadAdditional($acIn acIn_$name) { CompoundTag $name = $acCompat.tagOf(acIn_$name); $acProv acRegistries = acIn_$name.lookup();"
				)
			}
			string("!mc216-be-loadsuper-$name", true) {
				replace("super.load($name);", "super.loadAdditional(acIn_$name);")
			}
		}
		for (name in listOf("tag", "compound", "compoundTag", "p_187459_")) {
			string("!mc216-be-savesig-$name", true) {
				replace(
					"void saveAdditional(CompoundTag $name) {",
					"void saveAdditional($acOut acOut_$name) { CompoundTag $name = $acCompat.tagOf(acOut_$name); $acProv acRegistries = $acCompat.registriesOf(this);"
				)
			}
			string("!mc216-be-savesuper-$name", true) {
				replace("super.saveAdditional($name);", "super.saveAdditional(acOut_$name);")
			}
		}

		// ContainerHelper's two bulk helpers now take the ValueInput/ValueOutput itself, so unlike
		// the 1.20.5 pass (which appended an argument) each call site swaps its FIRST argument for
		// the bridge the enclosing signature introduced. The odd one out is the abyssal altar's
		// getUpdateTag, which builds a fresh local tag rather than being handed one — that keeps
		// its Provider overload on 1.21.6, so the tag is wrapped on the spot instead.
		//
		// ⚠️ The packet one is loader-dependent, and it is the only member of this list that is.
		// Fabric never patched onDataPacket, so its declaration keeps the two-argument vanilla shape
		// and the two `!mc216-be-datapacket-*` rules that introduce `acIn_packet` are scoped to the
		// loaders that did — but the METHOD BODY is shared, and this rule is what rewrites it. On
		// Fabric the tag is therefore wrapped where it stands instead. Written as one rule with a
		// loader-dependent replacement rather than as a second rule, because replacements do not
		// chain and two rules may not share a source string.
		val altarPacketRead = if (current.project.endsWith("-fabric"))
			"loadAllItems($acCompat.asInput(packet.getTag(), $acCompat.registriesOf(this)), this.stacks)"
		else "loadAllItems(acIn_packet, this.stacks)"
		for ((call, replacementCall) in listOf(
			"loadAllItems(p_155055_, this.items)" to "loadAllItems(acIn_p_155055_, this.items)",
			"loadAllItems(compound, this.stacks)" to "loadAllItems(acIn_compound, this.stacks)",
			"loadAllItems(packet.getTag(), this.stacks)" to altarPacketRead,
			"loadAllItems(compoundTag, this.items)" to "loadAllItems(acIn_compoundTag, this.items)",
			"saveAllItems(p_187459_, this.items)" to "saveAllItems(acOut_p_187459_, this.items)",
			"saveAllItems(compound, this.stacks)" to "saveAllItems(acOut_compound, this.stacks)",
			"saveAllItems(compoundTag, this.items, true)" to "saveAllItems(acOut_compoundTag, this.items, true)",
			"saveAllItems(compoundtag, this.stacks, true)"
				to "saveAllItems($acCompat.asOutput(compoundtag, acRegistries), this.stacks, true)",
		)) {
			string("!mc216-be-containerhelper-${call.filter { it.isLetterOrDigit() }}", true) {
				replace("ContainerHelper.$call;", "ContainerHelper.$replacementCall;")
			}
		}

		// RandomizableContainerBlockEntity's two loot-table hooks took the same turn. Both barrels
		// share the same generated parameter names, so this is two rules for four call sites.
		string("!mc216-be-trysaveloot", true) {
			replace("this.trySaveLootTable(p_187459_)", "this.trySaveLootTable(acOut_p_187459_)")
		}
		string("!mc216-be-tryloadloot", true) {
			replace("this.tryLoadLootTable(p_155055_)", "this.tryLoadLootTable(acIn_p_155055_)")
		}

		// ProjectileUtil#getEntityHitResult's Level-taking overload narrowed its second parameter
		// from Entity to Projectile, gaining a computeMargin(projectile) call in the process — and
		// the raygun's x-ray sweep passes the *player*, which is not one. The seven-argument form it
		// used to delegate to is still there and still public, so the fix is to spell out the margin
		// the six-argument form used to hardcode: 1.21.5 passed 0.3F, so this is byte-for-byte the
		// same trace, not a re-tuning.
		string("!mc216-projectileutil-margin", true) {
			replace("maxAABB, Entity::canBeHitByProjectile);", "maxAABB, Entity::canBeHitByProjectile, 0.3F);")
		}

		// ── every shader uniform is a std140 block now ─────────────────────
		// 1.21.6 deleted UniformType.FLOAT/INT/VEC3/MATRIX4X4 — the only members left are
		// UNIFORM_BUFFER and TEXEL_BUFFER — and folded the old scalars into four vanilla blocks
		// declared by the GLSL includes: DynamicTransforms (ModelViewMat, ColorModulator,
		// ModelOffset, TextureMat, LineWidth), Projection (ProjMat), Fog and Globals (ScreenSize,
		// GlintAlpha, GameTime, MenuBlurRadius). The snippets that used to carry the matrices and
		// the colour modulator were renamed to match, since "COLOR" is no longer a separate thing
		// they contribute. Both targets are absent from root src/, so the reverse direction is a
		// no-op on the older nodes.
		string("!mc216-snippet-matrices-fog", true) {
			replace("RenderPipelines.MATRICES_COLOR_FOG_SNIPPET", "RenderPipelines.MATRICES_FOG_SNIPPET")
		}
		// ⚠️ 26.2 deleted MATRICES_PROJECTION_SNIPPET along with every other snippet whose name
		// carried a uniform set rather than a draw shape, so this rule's *target* moves again at
		// that version. Rules do not chain — every one of them matches the ORIGINAL file text — and
		// two rules sharing a source string fail configuration as an ambiguous replacement, so the
		// second band has to go INSIDE this rule as a version-dependent val rather than beside it.
		// The replacement is this mod's own snippet (ACPipelineState), fully qualified because
		// WorldRenderMacros names the source string too and imports nothing from this package.
		val acMatricesSnippet =
			if (eval(current.version, ">=26.2"))
				"com.github.alexmodguy.alexscaves.client.render.ACPipelineState.MATRICES_PROJECTION_SNIPPET"
			else "RenderPipelines.MATRICES_PROJECTION_SNIPPET"
		string("!mc216-snippet-matrices", true) {
			replace("RenderPipelines.MATRICES_COLOR_SNIPPET", acMatricesSnippet)
		}
		// GameTime is a field of the Globals block, so a pipeline that animates asks for the whole
		// block instead. GLOBALS_SNIPPET is exactly this one call, and RenderSystem#bindDefaultUniforms
		// binds Globals for every draw, so nothing has to be pushed per frame — the six shaders that
		// read GameTime keep reading it, through `#moj_import <minecraft:globals.glsl>` which
		// DataPackMigration.migrateCoreShadersTo1216 splices in.
		//
		// ⚠️ From 26.2 the call is DELETED rather than translated, because withUniform is gone from
		// the builder entirely — a uniform reaches a pipeline as part of a BindGroupLayout now — and
		// every chain that carries one of these already reaches BindGroupLayouts.GLOBALS through its
		// root snippet: the two entity-derived ones through ENTITY_SNIPPET, the rest through the
		// mod-owned MATRICES_PROJECTION_SNIPPET above, which is built on RenderPipelines
		// .GLOBALS_SNIPPET for exactly this reason. Declaring GLOBALS a second time here would be a
		// duplicate binding, so removing the call is the correct translation, not a capitulation.
		//
		// It is removed by replacing it with a COMMENT rather than with nothing: Stonecutter rejects
		// an empty replacement outright ("Substituting an empty string is an irreversible operation"),
		// and a comment is the one non-empty span that is inert everywhere the call appears. All six
		// sites are either a whole line or the head of a `), arg)` continuation, and a comment is
		// legal between tokens in both shapes.
		val acGameTimeUniform =
			if (eval(current.version, ">=26.2")) "/*globals*/"
			else ".withUniform(\"Globals\", UniformType.UNIFORM_BUFFER)"
		string("!mc216-uniform-gametime", true) {
			replace(".withUniform(\"GameTime\", UniformType.FLOAT)", acGameTimeUniform)
		}
	}

	// ── 1.21.6: BlockEntity#onDataPacket, which is a loader extension on both sides ──
	// Neither loader kept the old (Connection, ClientboundBlockEntityDataPacket) shape, and they
	// did not agree on the new one: NeoForge's IBlockEntityExtension takes (Connection, ValueInput)
	// and expects the lookup to be read off the input, while Forge's IForgeBlockEntity kept the
	// trailing HolderLookup.Provider it added in 1.20.5 and takes (Connection, ValueInput,
	// Provider). Hence one rule per loader, on disjoint predicates — which is also why they may
	// share a source string without tripping the ambiguity check.
	//
	// All nine bodies dereference `packet.getTag()`, so the packet name is re-bound to
	// ACCompat.PacketData: a one-field record whose getTag() hands back the very tag the
	// ValueInput is reading, leaving every body byte-identical.
	if (current.project.endsWith("-neoforge") && eval(current.version, ">=1.21.6")) replacements {
		string("!mc216-be-datapacket-nf", true) {
			replace(
				"public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {",
				"public void onDataPacket(Connection net, net.minecraft.world.level.storage.ValueInput acIn_packet) {" +
					" com.github.alexmodguy.alexscaves.server.misc.ACCompat.PacketData packet =" +
					" com.github.alexmodguy.alexscaves.server.misc.ACCompat.packetData(acIn_packet);" +
					" net.minecraft.core.HolderLookup.Provider acRegistries = acIn_packet.lookup();"
			)
		}
	}
	if (current.project.endsWith("-forge") && eval(current.version, ">=1.21.6")) replacements {
		string("!mc216-be-datapacket-fg", true) {
			replace(
				"public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {",
				"public void onDataPacket(Connection net, net.minecraft.world.level.storage.ValueInput acIn_packet," +
					" net.minecraft.core.HolderLookup.Provider acRegistries) {" +
					" com.github.alexmodguy.alexscaves.server.misc.ACCompat.PacketData packet =" +
					" com.github.alexmodguy.alexscaves.server.misc.ACCompat.packetData(acIn_packet);"
			)
		}
	}

	// ── 1.21.6 on FORGE ONLY: EventBus 7 ────────────────────────────────────────
	// Forge 56.0.0 is the first Forge build on eventbus 7, the same generation NeoForge moved to
	// years earlier — except Forge did not copy NeoForge's API. What lands here is the mechanical
	// half: three packages moved and two types were renamed. The parts that cannot be expressed as
	// a string swap — the per-event static BUS replacing MinecraftForge.EVENT_BUS.post, BusGroup
	// replacing the mod bus object, and cancellation moving from event.setCanceled(true) to a
	// boolean return — are `//? if forge && >=1.21.6` gates in the source.
	//
	// This block is registered only on Forge nodes, so the NeoForge `!nf-eventbus` rule (which
	// rewrites the same net.minecraftforge.eventbus.api. prefix to net.neoforged.bus.api.) can
	// never see it and the two cannot collide.
	if (current.project.endsWith("-forge") && eval(current.version, ">=1.21.6")) replacements {

		// @SubscribeEvent moved into the .listener subpackage. It only ever appears as an import
		// here, so one fully-qualified rule covers every use.
		string("!fg216-subscribeevent", true) {
			replace(
				"net.minecraftforge.eventbus.api.SubscribeEvent",
				"net.minecraftforge.eventbus.api.listener.SubscribeEvent"
			)
		}

		// EventPriority became `listener.Priority`, and its constants went from enum values to
		// byte constants — same names, so only the type has to move. Two disjoint rules rather
		// than one on the bare token: the import ends in `;` and every usage ends in `.`, so
		// neither rule can match where the other does and their order cannot matter.
		string("!fg216-priority-import", true) {
			replace(
				"net.minecraftforge.eventbus.api.EventPriority;",
				"net.minecraftforge.eventbus.api.listener.Priority;"
			)
		}
		string("!fg216-priority-use", true) { replace("EventPriority.", "Priority.") }

		// The mod bus is no longer an object with addListener — it is a BusGroup that each event
		// type resolves its own bus from (see the gated block in AlexsCaves). The variable still
		// travels through Citadel.registerModBus and DeferredRegister.register unchanged, since
		// register() takes the BusGroup directly.
		//
		// One rule per spelling rather than one on the bare token, because ACPlatform names the
		// type fully qualified (it has no top-level loader imports) and a bare-token rule would
		// rewrite that to net.minecraftforge.eventbus.api.BusGroup — the wrong package. No source
		// below contains another, so the rules are order-independent.
		string("!fg216-buscls-import", true) {
			replace(
				"net.minecraftforge.eventbus.api.IEventBus;",
				"net.minecraftforge.eventbus.api.bus.BusGroup;"
			)
		}
		string("!fg216-buscls-fqn", true) {
			replace(
				"net.minecraftforge.eventbus.api.IEventBus modEventBus",
				"net.minecraftforge.eventbus.api.bus.BusGroup modEventBus"
			)
		}
		string("!fg216-buscls-param", true) { replace("(IEventBus modEventBus", "(BusGroup modEventBus") }
		string("!fg216-buscls-local", true) { replace("IEventBus modEventBus =", "BusGroup modEventBus =") }
		string("!fg216-buscls-localbus", true) { replace("IEventBus bus =", "BusGroup bus =") }
		string("!fg216-modbus-getter", true) { replace("getModEventBus()", "getModBusGroup()") }

		// Event.Result is gone with the Event base class; the tri-state moved to
		// net.minecraftforge.common.util.Result, handed out by the HasResult interface. getResult()
		// and the three constant names are unchanged, so only the type has to be re-pointed.
		string("!fg216-event-result", true) {
			replace("net.minecraftforge.eventbus.api.Event.Result.", "net.minecraftforge.common.util.Result.")
		}
	}

	// ── 1.21.7 on NEOFORGE ONLY: the serverbound half of PacketDistributor moved ────────────────
	// 21.7 split the one send-anywhere facade in two: PacketDistributor keeps the seven clientbound
	// methods and lost sendToServer, which now lives alone on
	// net.neoforged.neoforge.client.network.ClientPacketDistributor. Despite the package it carries
	// no @OnlyIn, so naming it from ACNetwork — a common class — is safe; the method is reached only
	// from AlexsCaves.sendMSGToServer, which by definition runs client-side.
	//
	// A rule rather than a `//?` gate because the difference is one qualified name inside the
	// `neoforge && >=1.20.5` arm, and Stonecutter cannot nest a second condition inside it. The
	// source string carries `.sendToServer` so it cannot touch the sendToPlayer line two below,
	// which keeps the old owner on every version.
	if (current.project.endsWith("-neoforge") && eval(current.version, ">=1.21.7")) replacements {
		string("!mc217-sendtoserver-nf", true) {
			replace(
				"net.neoforged.neoforge.network.PacketDistributor.sendToServer",
				"net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer"
			)
		}

		// ⚠️ NeoForge 21.7 gave `playBidirectional` a SECOND handler — serverbound first,
		// clientbound second — and turned the old three-argument form into a convenience that
		// passes `null` for the clientbound one. Same name, same arity, opposite meaning: the
		// three-argument call registers both directions below 1.21.7 and only the serverbound
		// direction from it, so the source compiles everywhere and the CLIENT dies at load with
		// "Some clientbound payloads are missing client-side handlers: [alexscaves:main_channel]".
		// A dedicated server never runs that check, which is why every NeoForge node booted green.
		// The handler is hoisted into a local in ACNetwork purely so this rule can name the call's
		// tail as one token.
		string("!mc217-bidirectional-nf", true) {
			replace("ACPayload.CODEC, handler)", "ACPayload.CODEC, handler, handler)")
		}
	}

	// ── 1.21.9 ──────────────────────────────────────────────────────────────────────────────────
	if (eval(current.version, ">=1.21.9")) replacements {

		// A block that does not collide finally lost its second `s`. Pure typo fix in vanilla;
		// nothing else in the tree spells the misspelling, and the correct name has never existed
		// on an older version, so this cannot fire in either direction anywhere but the 44 call
		// sites in ACBlockRegistry.
		string("!mc219-nocollision-typo", true) {
			replace("noCollission()", "noCollision()")
		}

		// ParticleProvider#createParticle gained a trailing RandomSource — the engine now hands the
		// provider the shared per-engine source instead of every particle pulling one out of its
		// level. Not one of this mod's ~90 factories wants it (they all read `random` off the
		// particle they construct), so the parameter is added and ignored.
		//
		// Enumerated as four whole signatures rather than a tail match: the upstream parameter names
		// are inconsistent (`zSpeed` / `zd` / `zMotion`) and `double zd) {` on its own also ends a
		// dozen particle *constructors*, which must not gain the argument.
		string("!mc219-createparticle-random", true) {
			replace(
				"public Particle createParticle(BlockParticleOption typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {",
				"public Particle createParticle(BlockParticleOption typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, net.minecraft.util.RandomSource acRandom) {"
			)
			replace(
				"public Particle createParticle(ItemParticleOption itemParticleOption, ClientLevel clientLevel, double x, double y, double z, double xd, double yd, double zd) {",
				"public Particle createParticle(ItemParticleOption itemParticleOption, ClientLevel clientLevel, double x, double y, double z, double xd, double yd, double zd, net.minecraft.util.RandomSource acRandom) {"
			)
			replace(
				"public Particle createParticle(SimpleParticleType simpleParticleType, ClientLevel clientLevel, double x, double y, double z, double xMotion, double yMotion, double zMotion) {",
				"public Particle createParticle(SimpleParticleType simpleParticleType, ClientLevel clientLevel, double x, double y, double z, double xMotion, double yMotion, double zMotion, net.minecraft.util.RandomSource acRandom) {"
			)
			replace(
				"public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {",
				"public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, net.minecraft.util.RandomSource acRandom) {"
			)
		}

		// net.minecraft.core.particles.ParticleGroup — the per-type spawn cap — was renamed
		// ParticleLimit, and Particle#getParticleGroup with it. The old name now belongs to an
		// unrelated ParticleEngine inner class, so leaving it would compile into the wrong type on a
		// wildcard import; only RainbowParticle uses it.
		string("!mc219-particlegroup-limit", true) {
			replace("net.minecraft.core.particles.ParticleGroup", "net.minecraft.core.particles.ParticleLimit")
			replace("Optional<ParticleGroup> getParticleGroup()", "Optional<ParticleLimit> getParticleLimit()")
			replace("ParticleGroup PARTICLE_GROUP = new ParticleGroup(", "ParticleLimit PARTICLE_GROUP = new ParticleLimit(")
		}

		// ItemModelResolver#updateForTopItem dropped its leftHand flag — handedness is read off the
		// display context now. The one call site lives inside ACItemRenderCompat's `>=1.21.4` arm, and
		// Stonecutter cannot nest a second condition inside an arm, so this is a rule rather than a
		// gate. Matched on the whole argument list, which appears exactly once in the tree.
		string("!mc219-updatefortopitem-lefthand", true) {
			replace(
				".updateForTopItem(state, stack, ctx, false, level, null, 0)",
				".updateForTopItem(state, stack, ctx, level, null, 0)"
			)
		}

		// The server's chunk-progress callback was rebuilt: ChunkProgressListenerFactory is gone and
		// MinecraftServer's constructor takes a LevelLoadListener instead. Only Citadel's
		// MinecraftServerMixin names it — in its import, in the constructor's descriptor string and
		// in the handler's parameter list — and matching the bare type name covers all three, since
		// the slashed descriptor spelling ends at the same boundary.
		string("!mc219-chunkprogress-listener", true) {
			replace("ChunkProgressListenerFactory", "LevelLoadListener")
		}

		// PlayerSkin moved from net.minecraft.client.resources to net.minecraft.world.entity.player.
		// Matched fully qualified in both spellings, never as a bare token: `DefaultPlayerSkin` ends
		// on the same identifier boundary, and a bare rule would rewrite the middle of that name.
		string("!mc219-playerskin-pkg", true) {
			replace("net.minecraft.client.resources.PlayerSkin", "net.minecraft.world.entity.player.PlayerSkin")
		}
		string("!mc219-playerskin-pkg-desc", true) {
			replace("net/minecraft/client/resources/PlayerSkin", "net/minecraft/world/entity/player/PlayerSkin")
		}

		// PlayerRenderer became AvatarRenderer (with PlayerRenderState → AvatarRenderState behind it),
		// part of 1.21.9 splitting "the local avatar" out of "a player entity". Two spellings: the
		// import/parameter FQN, and the local-variable declaration CaveMapRenderHelper makes twice.
		// Never matched bare — see the PlayerSkin note above for why.
		string("!mc219-playerrenderer-avatar", true) {
			replace(
				"net.minecraft.client.renderer.entity.player.PlayerRenderer",
				"net.minecraft.client.renderer.entity.player.AvatarRenderer"
			)
			replace(
				"PlayerRenderer playerrenderer = (PlayerRenderer)",
				"AvatarRenderer playerrenderer = (AvatarRenderer)"
			)
		}

		// BlockEntityRenderer<T> gained a render-state type parameter and swapped render(...) for
		// submit(state, pose, collector, camera) — the same extract/submit split entity renderers got
		// in 1.21.2, and absorbed the same way: point the thirteen tile renderers' import at the
		// compat interface of the same simple name, which reconstructs the old call from the state.
		// None of the thirteen overrides shouldRenderOffScreen, which lost its argument in the same
		// rewrite, so unlike the sibling AlexsMobsContinued tree this needs no companion rule.
		string("!mc219-tile-import", true) {
			replace(
				"import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;",
				"import com.github.alexmodguy.alexscaves.client.render.compat.BlockEntityRenderer;"
			)
		}

		// ArmedModel became generic — ArmedModel<T extends EntityRenderState>, with
		// translateToHand(T, HumanoidArm, PoseStack). All six of this mod's models implement it
		// *raw*, so the erasure they have to declare is the EntityRenderState one; the state itself
		// is unused by every one of the six bodies (they position an arm from their own cubes), it
		// only has to be there for the override to match.
		//
		// Three declaration spellings, because upstream names the parameters inconsistently. The
		// mod's own unrelated VallumraptorModel#translateToHand(PoseStack, boolean) is deliberately
		// not matched — it takes no HumanoidArm, so none of the three sources can touch it.
		string("!mc219-translatetohand-decl", true) {
			replace(
				"public void translateToHand(HumanoidArm arm, PoseStack poseStack) {",
				"public void translateToHand(net.minecraft.client.renderer.entity.state.EntityRenderState acState, HumanoidArm arm, PoseStack poseStack) {"
			)
			replace(
				"public void translateToHand(HumanoidArm humanoidArm, PoseStack poseStack) {",
				"public void translateToHand(net.minecraft.client.renderer.entity.state.EntityRenderState acState, HumanoidArm humanoidArm, PoseStack poseStack) {"
			)
			replace(
				"public void translateToHand(HumanoidArm humanoidArm, PoseStack matrixStackIn) {",
				"public void translateToHand(net.minecraft.client.renderer.entity.state.EntityRenderState acState, HumanoidArm humanoidArm, PoseStack matrixStackIn) {"
			)
		}

		// …and the four call sites gain the state. Every one of them sits in a compat RenderLayer
		// subclass, so `this.renderingState` is the state of the render in flight. The two
		// `(humanoidArm, poseStack)` sites are byte-identical (GingerbreadManRenderer and
		// LicowitchRenderer both spell it that way), so one rule covers both.
		string("!mc219-translatetohand-call", true) {
			replace(
				".translateToHand(arm, poseStack)",
				".translateToHand(this.renderingState, arm, poseStack)"
			)
			replace(
				".translateToHand(humanoidArm, poseStack)",
				".translateToHand(this.renderingState, humanoidArm, poseStack)"
			)
			replace(
				".translateToHand(HumanoidArm.RIGHT, matrixStackIn)",
				".translateToHand(this.renderingState, HumanoidArm.RIGHT, matrixStackIn)"
			)
		}

		// shouldDropLoot() takes the ServerLevel it is dropping into, the last of LivingEntity's death
		// path to be converted (dropAllDeathLoot, dropCustomDeathLoot and dropFromLootTable all took
		// theirs in 1.21.2). The parameter is named acServerLevel to match those rules, so the five
		// super-calls inside the six overrides simply forward it.
		//
		// The two plain call sites can't: neither sits in a method that was handed a level. GumWorm's
		// is inside dropAllDeathLoot, which by then *has* acServerLevel, so it forwards; the
		// GingerbreadMan one is in onLoseArm, already behind `if(!level().isClientSide())`, so casting
		// level() there is safe. Both sources carry enough neighbouring text that neither overlaps the
		// `super.` rule.
		string("!mc219-shoulddroploot-level", true) {
			replace(
				"protected boolean shouldDropLoot() {",
				"protected boolean shouldDropLoot(net.minecraft.server.level.ServerLevel acServerLevel) {"
			)
			replace("super.shouldDropLoot()", "super.shouldDropLoot(acServerLevel)")
			replace(
				"&& shouldDropLoot() && random.nextInt(2) == 0",
				"&& shouldDropLoot((net.minecraft.server.level.ServerLevel) this.level()) && random.nextInt(2) == 0"
			)
			replace(
				"if (this.shouldDropLoot() && ACCompat.gameRule(",
				"if (this.shouldDropLoot(acServerLevel) && ACCompat.gameRule("
			)
		}

		// Entity#lerpMotion(double, double, double) became lerpMotion(Vec3). Sixteen entities override
		// it, every one with the identical signature and none of them calling super, so rather than
		// rewrite sixteen bodies the declaration grows a bridge in front of it: the @Override above now
		// lands on the Vec3 method, which unpacks the vector into the old three-double overload. That
		// overload stops overriding anything and becomes an ordinary public method — which is fine,
		// because every in-tree caller passes three doubles anyway.
		string("!mc219-lerpmotion-vec3", true) {
			replace(
				"public void lerpMotion(double lerpX, double lerpY, double lerpZ) {",
				"public void lerpMotion(net.minecraft.world.phys.Vec3 acLerp) { this.lerpMotion(acLerp.x, acLerp.y, acLerp.z); } public void lerpMotion(double lerpX, double lerpY, double lerpZ) {"
			)
		}

		// startRiding(Entity, boolean) gained a third flag. Read out of the bytecode rather than
		// guessed: the final one-argument startRiding delegates as (vehicle, false, true), and the flag
		// gates the GameEvent.ENTITY_MOUNT emission at the end of the method — so `true` is what the
		// old two-argument form did, on both the override and the one explicit call site.
		string("!mc219-startriding-gameevent", true) {
			replace(
				"public boolean startRiding(Entity entity, boolean force) {",
				"public boolean startRiding(Entity entity, boolean force, boolean acEmitGameEvent) {"
			)
			replace("super.startRiding(entity, force)", "super.startRiding(entity, force, acEmitGameEvent)")
			replace(
				"pickupMonster.startRiding(GammaroachEntity.this, true)",
				"pickupMonster.startRiding(GammaroachEntity.this, true, true)"
			)
		}

		// spawnAtLocation lost the (ItemLike, int yOffset) overload — the surviving offset form is
		// (ItemStack, float). The call is inside ACCompat's own `>=1.21.2` arm, which cannot be nested,
		// hence a rule; ACCompat is the only place in the tree that spells it.
		string("!mc219-spawnatlocation-offset", true) {
			replace(
				"entity.spawnAtLocation(serverLevel, item, yOffset)",
				"entity.spawnAtLocation(serverLevel, new net.minecraft.world.item.ItemStack(item), (float) yOffset)"
			)
		}

		// Container#startOpen/stopOpen take a ContainerUser instead of a Player, and
		// ContainerOpenersCounter#incrementOpeners a trailing interaction range (vanilla passes the
		// user's getContainerInteractionRange()); isOwnContainer became public in the same pass.
		// Losing this silently would be the bad outcome — the two barrels' overrides would simply stop
		// overriding, and their lids would never animate or play a sound.
		//
		// The two barrels are byte-identical here down to the upstream parameter names, so one rule
		// covers both; AbyssalAltar's pair of empty overrides spell theirs `player` and are matched
		// separately.
		string("!mc219-containeruser", true) {
			replace(
				"public void startOpen(Player p_58616_) {",
				"public void startOpen(net.minecraft.world.entity.ContainerUser p_58616_) {"
			)
			replace("!p_58616_.isSpectator()", "!p_58616_.getLivingEntity().isSpectator()")
			replace(
				"incrementOpeners(p_58616_, this.getLevel(), this.getBlockPos(), this.getBlockState())",
				"incrementOpeners(p_58616_.getLivingEntity(), this.getLevel(), this.getBlockPos(), this.getBlockState(), p_58616_.getContainerInteractionRange())"
			)
			replace(
				"public void stopOpen(Player p_58614_) {",
				"public void stopOpen(net.minecraft.world.entity.ContainerUser p_58614_) {"
			)
			replace("!p_58614_.isSpectator()", "!p_58614_.getLivingEntity().isSpectator()")
			replace(
				"decrementOpeners(p_58614_, this.getLevel()",
				"decrementOpeners(p_58614_.getLivingEntity(), this.getLevel()"
			)
			replace(
				"protected boolean isOwnContainer(Player p_155060_) {",
				"public boolean isOwnContainer(Player p_155060_) {"
			)
			replace(
				"public void startOpen(Player player) {",
				"public void startOpen(net.minecraft.world.entity.ContainerUser player) {"
			)
			replace(
				"public void stopOpen(Player player) {",
				"public void stopOpen(net.minecraft.world.entity.ContainerUser player) {"
			)
		}

		// RangeSelectItemModelProperty#get's third parameter widened from LivingEntity to ItemOwner —
		// a level + position + facing, so an item frame or a display entity can drive a range-select
		// too. All nine predicates behind ACItemModelShims.Legacy want a LivingEntity, so the
		// parameter is unwrapped straight back to one. Two rules rather than one because the class
		// sits inside a `>=1.21.4` Stonecutter arm, and arms do not nest.
		string("!mc219-rangeselect-itemowner", true) {
			replace(
				"net.minecraft.world.entity.LivingEntity acHolder, int seed) {",
				"net.minecraft.world.entity.ItemOwner acHolder, int seed) {"
			)
			replace(
				"net.minecraft.world.entity.LivingEntity holder = acHolder;",
				"net.minecraft.world.entity.LivingEntity holder = acHolder == null ? null : acHolder.asLivingEntity();"
			)
		}

		// The three things ACItemRenderCompat needs from the 1.21.9 item pipeline. All of them live
		// inside its single `>=1.21.4` arm, which is why they are replacement rules and not gates.
		//
		//  * updateForTopItem lost its `left-handed` flag — the whole left/right split moved into the
		//    ItemDisplayContext values themselves.
		//  * ItemStackRenderState#isGui3d is gone; usesBlockLight is the surviving flag that says the
		//    same thing (the layer resolved to a block model rather than a flat sprite), and it is what
		//    the pedestal fan-out was really asking.
		//  * render(PoseStack, lookup, light, overlay) became submit(PoseStack, SubmitNodeCollector,
		//    light, overlay, outlineColor). ACDrawCollector puts the caller's lookup back in the middle
		//    and draws each submitted node immediately; the trailing 0 is "no outline", which is what
		//    the old render produced.
		string("!mc219-itemstate-submit", true) {
			replace(
				"net.minecraft.world.item.ItemDisplayContext.FIXED).isGui3d()",
				"net.minecraft.world.item.ItemDisplayContext.FIXED).usesBlockLight()"
			)
			replace(
				"state.render(poseStack, lookup::apply, light, overlay);",
				"state.submit(poseStack, new com.github.alexmodguy.alexscaves.client.render.compat.ACDrawCollector(lookup), light, overlay, 0);"
			)
		}

		// The copper age gave chains a metal in their name; the plain `chain` block id is gone.
		// Spelled with the trailing call so the reverse direction cannot turn some future
		// `Blocks.IRON_CHAIN` prefix into `Blocks.CHAIN` — the forlorn bridge is the only site.
		string("!mc219-iron-chain", true) {
			replace("Blocks.CHAIN.defaultBlockState()", "Blocks.IRON_CHAIN.defaultBlockState()")
		}

		// A keybind's category stopped being a free translation key and became a registered
		// KeyMapping.Category record. `key.categories.misc` is exactly Category.MISC.
		string("!mc219-keybind-category", true) {
			replace(
				"InputConstants.KEY_G, \"key.categories.misc\")",
				"InputConstants.KEY_G, KeyMapping.Category.MISC)"
			)
		}

		// TicketType became a record of (timeout, flags) — the boolean `persist` and the TicketUse
		// enum both folded into the flag word. LOADING_AND_SIMULATION is FLAG_LOADING | FLAG_SIMULATION;
		// LOADING is FLAG_LOADING alone. Neither of this mod's two types persists, so the dropped
		// `false` needs no flag of its own. Both spellings live inside ACPlatform's single
		// `forge && >=1.21.5` arm, which is why they are rules rather than gates.
		string("!mc219-tickettype-flags", true) {
			replace(
				"net.minecraft.server.level.TicketType.NO_TIMEOUT, false, net.minecraft.server.level.TicketType.TicketUse.LOADING_AND_SIMULATION",
				"net.minecraft.server.level.TicketType.NO_TIMEOUT, net.minecraft.server.level.TicketType.FLAG_LOADING | net.minecraft.server.level.TicketType.FLAG_SIMULATION"
			)
			replace(
				"net.minecraft.server.level.TicketType.NO_TIMEOUT, false, net.minecraft.server.level.TicketType.TicketUse.LOADING",
				"net.minecraft.server.level.TicketType.NO_TIMEOUT, net.minecraft.server.level.TicketType.FLAG_LOADING"
			)
		}

		// The debug shape helpers take the current Pose rather than the whole stack now. Only the
		// magnet's range box calls one; the vendored Citadel pathfinding renderer has its own
		// same-named overloads that take a VertexConsumer first and are untouched, which is why the
		// argument that follows is spelled out here.
		//
		// The match deliberately starts at the argument and NOT at the method name: the >=1.21.5
		// block already rewrites `LevelRenderer.renderLineBox(` to the ShapeRenderer spelling, and
		// two rules whose matches overlap do not both apply — the earlier-starting one consumes the
		// text and this one then has nowhere to begin. That is why the original wording of this rule
		// silently did nothing on both 1.21.9 nodes.
		//
		// Its emitted `RenderType.lines()` is version-dependent for the same reason the glint rule's
		// is: this match starts earlier than a bare `RenderType.lines` one, so it consumes the text
		// and the 1.21.11 owner rename never gets a chance to fire here.
		//
		// The pose argument is version-dependent for a third reason: on >=1.21.11 the call has been
		// retargeted (by !mc2102-shape-renderer-line-box) at ACClientCompat#renderLineBox, which wraps
		// renderShape and so takes the whole PoseStack back again.
		//
		// And the buffer argument is version-dependent for a fourth: 26.2 deleted ShapeRenderer
		// entirely, so ACClientCompat#renderLineBox submits a shape outline to the frame's collector
		// there — which it recovers from the MultiBufferSource itself, not from a VertexConsumer
		// pulled out of it. Same reasoning as the other three: this match starts earliest, so it is
		// the only rule that can spell this span.
		val linesOwner = if (eval(current.version, ">=1.21.11")) "RenderTypes" else "RenderType"
		val lineBoxPose = if (eval(current.version, ">=1.21.11")) "stack" else "stack.last()"
		val lineBoxBuffer = if (eval(current.version, ">=26.2")) "bufferIn" else "bufferIn.getBuffer($linesOwner.lines())"
		string("!mc219-shape-pose", true) {
			replace("stack, bufferIn.getBuffer(RenderType.lines())", "$lineBoxPose, $lineBoxBuffer")
		}

		// A dimension's "always fully lit" flag was renamed when 1.21.9 turned it into an ambient
		// colour rather than a boolean branch in the lightmap shader. Safe as a rule in both
		// directions: the new spelling appears nowhere else in this tree.
		string("!mc219-constant-ambient-light", true) {
			replace("forceBrightLightmap()", "constantAmbientLight()")
		}

		// The full-screen blit's *vertex* stage was replaced by one that needs no geometry at all
		// (see ACInternalShaders). Only the vertex spelling moves — the fragment stage of the same
		// name still exists, which is why the withVertexShader call is part of the match.
		string("!mc219-screenquad", true) {
			replace(".withVertexShader(\"core/blit_screen\")", ".withVertexShader(\"core/screenquad\")")
		}
	}

	// FMLEnvironment's public `dist` field became a getter when fancymodloader 10 gave the class a
	// private constructor (9.0.x has the field, 10.0.36 and every 11.x build have getDist()), which
	// on this tree's pin table lands exactly at NeoForge 21.9. Forge keeps the field, so this is
	// NeoForge-only.
	//
	// A rule rather than a gate because the two call sites — the AlexsCaves and Citadel proxy fields
	// — are already inside `neoforge && >=1.21` arms that nothing can nest a second condition into.
	// It cannot overlap the `!nf-fml` package rename: that match ends at `net.neoforged.fml.`, three
	// segments before this one begins. The reverse direction finds no getDist() spelling anywhere.
	if (current.project.endsWith("-neoforge") && eval(current.version, ">=1.21.9")) replacements {
		string("!nf219-fmlenvironment-getdist", true) {
			replace("FMLEnvironment.dist.isClient()", "FMLEnvironment.getDist().isClient()")
		}
	}

	// ── 1.21.11 ───────────────────────────────────────────────────────────────────────────────
	// The largest *mechanical* wave of the walk and the smallest logical one: 1.21.11 renamed
	// ResourceLocation and shuffled ~37 classes into deeper packages without changing a single
	// signature this mod calls. Almost all of it is expressible as string rules; what is not —
	// the RenderType/RenderStateShard split, and three client classes that lost a member — is
	// gated in source instead and called out below.
	if (eval(current.version, ">=1.21.11")) replacements {

		// ── ResourceLocation -> Identifier ──────────────────────────────────
		// A pure rename inside the same package, so ONE bare-token rule covers all 1543 sites:
		// the import line, every declaration, and — because the package part is unchanged — the
		// slash-form descriptors inside mixin `method =` / `@At(target =)` string literals, which
		// a dotted rule could never see (`Lnet/minecraft/resources/ResourceLocation;`).
		//
		// ⚠️ A blanket rule on a bare type name matches inside longer identifiers on BOTH edges —
		// neither is boundary-checked, whatever an older note in the workspace notes claimed.
		// This rule proved it twice:
		//   * `ModelResourceLocation` is rewritten (left edge). Harmless: that class exists on no
		//     node >=1.21.2, so every use of it here is already inside a dead arm, and the reverse
		//     direction maps `ModelIdentifier` back.
		//   * this mod's own `ACResourceLocations` shim was rewritten too (right edge, despite the
		//     trailing `s`) — which renames the class but not the file, so javac stops with "is
		//     public, should be declared in a file named ACIdentifiers.java". It is called
		//     `ACIdFactories` now, carrying none of the token. Do not rename it back.
		// The reverse direction is a no-op on the other 27 nodes: `src/` contains zero `Identifier`
		// tokens. Keeping it that way is why CitadelCapes.Cape#getIdentifier was renamed getCapeId.
		string("!mc2111-resourcelocation", true) {
			replace("ResourceLocation", "Identifier")
		}

		// ── package moves ───────────────────────────────────────────────────
		// 1.21.11 pushed a long tail of classes down one package level. Every one below was
		// verified present at its new path in the node's own patched jar, not guessed from the
		// name. They are all dotted-form rules, so they rewrite the import line and any
		// fully-qualified use in one go; the three that also appear in a mixin descriptor string
		// get a slash-form twin further down.
		string("!mc2111-pkg-util", true) {
			replace("net.minecraft.Util", "net.minecraft.util.Util")
		}
		string("!mc2111-pkg-critereon", true) {
			replace("net.minecraft.advancements.critereon", "net.minecraft.advancements.criterion")
		}
		string("!mc2111-pkg-gamerules", true) {
			replace("net.minecraft.world.level.GameRules", "net.minecraft.world.level.gamerules.GameRules")
		}
		string("!mc2111-pkg-creepermodel", true) {
			replace("net.minecraft.client.model.CreeperModel", "net.minecraft.client.model.monster.creeper.CreeperModel")
		}
		string("!mc2111-pkg-playermodel", true) {
			replace("net.minecraft.client.model.PlayerModel", "net.minecraft.client.model.player.PlayerModel")
		}
		string("!mc2111-pkg-abstractgolem", true) {
			replace("net.minecraft.world.entity.animal.AbstractGolem", "net.minecraft.world.entity.animal.golem.AbstractGolem")
		}
		string("!mc2111-pkg-irongolem", true) {
			replace("net.minecraft.world.entity.animal.IronGolem", "net.minecraft.world.entity.animal.golem.IronGolem")
		}
		string("!mc2111-pkg-cat", true) {
			replace("net.minecraft.world.entity.animal.Cat", "net.minecraft.world.entity.animal.feline.Cat")
		}
		string("!mc2111-pkg-fox", true) {
			replace("net.minecraft.world.entity.animal.Fox", "net.minecraft.world.entity.animal.fox.Fox")
		}
		string("!mc2111-pkg-polarbear", true) {
			replace("net.minecraft.world.entity.animal.PolarBear", "net.minecraft.world.entity.animal.polarbear.PolarBear")
		}
		string("!mc2111-pkg-wateranimal", true) {
			replace("net.minecraft.world.entity.animal.WaterAnimal", "net.minecraft.world.entity.animal.fish.WaterAnimal")
		}
		string("!mc2111-pkg-zombie", true) {
			replace("net.minecraft.world.entity.monster.Zombie", "net.minecraft.world.entity.monster.zombie.Zombie")
		}
		string("!mc2111-pkg-husk", true) {
			replace("net.minecraft.world.entity.monster.Husk", "net.minecraft.world.entity.monster.zombie.Husk")
		}
		string("!mc2111-pkg-drowned", true) {
			replace("net.minecraft.world.entity.monster.Drowned", "net.minecraft.world.entity.monster.zombie.Drowned")
		}
		string("!mc2111-pkg-abstractillager", true) {
			replace("net.minecraft.world.entity.monster.AbstractIllager", "net.minecraft.world.entity.monster.illager.AbstractIllager")
		}
		string("!mc2111-pkg-evoker", true) {
			replace("net.minecraft.world.entity.monster.Evoker", "net.minecraft.world.entity.monster.illager.Evoker")
		}
		string("!mc2111-pkg-illusioner", true) {
			replace("net.minecraft.world.entity.monster.Illusioner", "net.minecraft.world.entity.monster.illager.Illusioner")
		}
		string("!mc2111-pkg-pillager", true) {
			replace("net.minecraft.world.entity.monster.Pillager", "net.minecraft.world.entity.monster.illager.Pillager")
		}
		string("!mc2111-pkg-vindicator", true) {
			replace("net.minecraft.world.entity.monster.Vindicator", "net.minecraft.world.entity.monster.illager.Vindicator")
		}
		string("!mc2111-pkg-abstractvillager", true) {
			replace("net.minecraft.world.entity.npc.AbstractVillager", "net.minecraft.world.entity.npc.villager.AbstractVillager")
		}
		string("!mc2111-pkg-villagerprofession", true) {
			replace("net.minecraft.world.entity.npc.VillagerProfession", "net.minecraft.world.entity.npc.villager.VillagerProfession")
		}
		string("!mc2111-pkg-villagertrades", true) {
			replace("net.minecraft.world.entity.npc.VillagerTrades", "net.minecraft.world.entity.npc.villager.VillagerTrades")
		}
		string("!mc2111-pkg-abstractarrow", true) {
			replace("net.minecraft.world.entity.projectile.AbstractArrow", "net.minecraft.world.entity.projectile.arrow.AbstractArrow")
		}
		string("!mc2111-pkg-arrow", true) {
			replace("net.minecraft.world.entity.projectile.Arrow", "net.minecraft.world.entity.projectile.arrow.Arrow")
		}
		string("!mc2111-pkg-throwntrident", true) {
			replace("net.minecraft.world.entity.projectile.ThrownTrident", "net.minecraft.world.entity.projectile.arrow.ThrownTrident")
		}
		string("!mc2111-pkg-dragonfireball", true) {
			replace("net.minecraft.world.entity.projectile.DragonFireball", "net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball")
		}
		string("!mc2111-pkg-throwableitemprojectile", true) {
			replace("net.minecraft.world.entity.projectile.ThrowableItemProjectile", "net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile")
		}
		// The one package move that has to be keyed on the OLD class name. `!mc2105-thrownpotion`
		// already rewrites the bare token `ThrownPotion` -> `ThrownSplashPotion` from 1.21.5, and
		// rules do not chain, so a rule keyed on the post-1.21.5 spelling of the import would never
		// fire. Keyed on the original text it starts at `net.` — earlier than the bare rule's match
		// at `ThrownPotion` — and an earlier-starting match consumes the text, so this one wins on
		// the import line while the bare rule still handles the two constructor call sites.
		string("!mc2111-pkg-thrownsplashpotion", true) {
			replace("net.minecraft.world.entity.projectile.ThrownPotion", "net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion")
		}
		string("!mc2111-pkg-abstractminecart", true) {
			replace("net.minecraft.world.entity.vehicle.AbstractMinecart", "net.minecraft.world.entity.vehicle.minecart.AbstractMinecart")
		}
		string("!mc2111-pkg-minecartbehavior", true) {
			replace("net.minecraft.world.entity.vehicle.MinecartBehavior", "net.minecraft.world.entity.vehicle.minecart.MinecartBehavior")
		}
		string("!mc2111-pkg-oldminecartbehavior", true) {
			replace("net.minecraft.world.entity.vehicle.OldMinecartBehavior", "net.minecraft.world.entity.vehicle.minecart.OldMinecartBehavior")
		}
		// Named only from ACCompat#defaultRailMaxSpeed, which inlines the rail-speed default that
		// NeoForge 26.1.2 deleted — the furnace cart is the one case that default treats specially.
		string("!mc2111-pkg-minecartfurnace", true) {
			replace("net.minecraft.world.entity.vehicle.MinecartFurnace", "net.minecraft.world.entity.vehicle.minecart.MinecartFurnace")
		}
		string("!mc2111-pkg-boat", true) {
			replace("net.minecraft.world.entity.vehicle.Boat", "net.minecraft.world.entity.vehicle.boat.Boat")
		}
		string("!mc2111-pkg-chestboat", true) {
			replace("net.minecraft.world.entity.vehicle.ChestBoat", "net.minecraft.world.entity.vehicle.boat.ChestBoat")
		}

		// ── package moves, slash form ───────────────────────────────────────
		// A mixin's `method =` / `@Mixin(targets =)` string is a *descriptor*, not code, so the
		// dotted rules above cannot see it. Only three of the moved classes are named that way in
		// this tree; `scripts/slash.py`-style scanning of the generated tree is what proves the
		// list is complete, since nothing else reads those literals. (ResourceLocation needs no
		// twin: its package did not move, so the bare-token rule already rewrote it.)
		string("!mc2111-pkgslash-abstractarrow", true) {
			replace("net/minecraft/world/entity/projectile/AbstractArrow", "net/minecraft/world/entity/projectile/arrow/AbstractArrow")
		}
		string("!mc2111-pkgslash-abstractminecart", true) {
			replace("net/minecraft/world/entity/vehicle/AbstractMinecart", "net/minecraft/world/entity/vehicle/minecart/AbstractMinecart")
		}
		string("!mc2111-pkgslash-oldminecartbehavior", true) {
			replace("net/minecraft/world/entity/vehicle/OldMinecartBehavior", "net/minecraft/world/entity/vehicle/minecart/OldMinecartBehavior")
		}

		// ── RenderType split into RenderType + RenderTypes ──────────────────
		// The wave's one real API change. `net.minecraft.client.renderer.RenderType` moved to
		// `…renderer.rendertype.RenderType` and kept ONLY the instance surface plus create(); all
		// 16 static factories this mod calls moved to a sibling `RenderTypes`. RenderStateShard
		// dissolved into RenderSetup/LayeringTransform/OutputTarget/TextureTransform, and
		// CompositeState became RenderSetup.Builder — those two are NOT rules; they are gated arms
		// in the only two files that build render types by hand (ACRenderTypes, WorldRenderMacros).
		//
		// The import becomes an on-demand import so that one rule serves both class names. That is
		// safe here even in the ten affected files that already carry a wildcard import: none of
		// their packages declares any of this package's six types, and a single-type import would
		// win over an on-demand one regardless.
		string("!mc2111-rendertype-import", true) {
			replace("import net.minecraft.client.renderer.RenderType;", "import net.minecraft.client.renderer.rendertype.*;")
		}
		// The dozen fully-qualified *type* uses, matched with their trailing delimiter so the rule
		// cannot also eat the import line or the `RenderType$CompositeRenderType` mixin target.
		string("!mc2111-rendertype-fqn-space", true) {
			replace("net.minecraft.client.renderer.RenderType ", "net.minecraft.client.renderer.rendertype.RenderType ")
		}
		string("!mc2111-rendertype-fqn-comma", true) {
			replace("net.minecraft.client.renderer.RenderType,", "net.minecraft.client.renderer.rendertype.RenderType,")
		}
		string("!mc2111-rendertype-fqn-generic", true) {
			replace("net.minecraft.client.renderer.RenderType>", "net.minecraft.client.renderer.rendertype.RenderType>")
		}
		// …and the one fully-qualified *static* use, which changes owner as well as package.
		string("!mc2111-rendertype-fqn-zoffset", true) {
			replace(
				"net.minecraft.client.renderer.RenderType.entitySolidZOffsetForward",
				"net.minecraft.client.renderer.rendertype.RenderTypes.entitySolidZOffsetForward"
			)
		}
		// The static factories, one rule per member. `entityCutout` DOES match inside
		// `entityCutoutNoCull` — neither edge is identifier-boundary-checked, whatever the older
		// notes in this file used to claim — but it is harmless here, in both directions, because
		// the prefix rule rewrites only the prefix and leaves the tail: `RenderType.entityCutout`
		// + `NoCull` reassembles as `RenderTypes.entityCutoutNoCull`, which is exactly what the
		// longer rule would have produced. Same for entityTranslucent/entityTranslucentEmissive.
		// Do NOT read that as a general licence: a pair whose replacements disagree on the shared
		// prefix has to be spelled so one cannot start at the same offset as the other.
		// Deliberately absent from this list:
		//   * cutoutMipped / translucent — `!mc2106-fluidlayer-*` already consumes both call sites
		//     whole, replacing them with a ChunkSectionLayer constant, so nothing is left to rename.
		//   * entityGlintDirect / entityGlint and lines-inside-renderLineBox — owned by the two
		//     rules above that grew a version-dependent target, for the no-chaining reason.
		//   * create — it stays on RenderType but lost every parameter except the name and the
		//     RenderSetup, so its call sites are re-pointed at the shim below instead.
		//   * the entity-cutout family and itemEntityTranslucentCull — they change NAME as well as
		//     owner from 26, so they get rules of their own directly below.
		listOf(
			"armorCutoutNoCull", "energySwirl",
			"entityTranslucent", "entityTranslucentEmissive", "eyes",
			"leash", "lightning", "lines", "outline", "text", "waterMask"
		).forEach { member ->
			string("!mc2111-rendertypes-$member", true) {
				replace("RenderType.$member", "RenderTypes.$member")
			}
		}
		// ⚠️ 26 renamed the entity-cutout family: culling stopped being the default spelling, so
		// `entityCutoutNoCull` took the plain `entityCutout` name and the old `entityCutout` became
		// `entityCutoutCull`. `itemEntityTranslucentCull` became `entityTranslucentCullItemTarget`
		// in the same sweep. Two consequences:
		//   * The two cutout names swap INTO each other, so the harmless-shared-prefix reasoning
		//     above stops holding — `entityCutout` and `entityCutoutNoCull` would start at the same
		//     offset with replacements that disagree. Both rules therefore carry the call site's
		//     opening parenthesis, which makes the shorter one FAIL on a `NoCull` site rather than
		//     race the longer one for it. Every call site in the tree is the `(` form (checked:
		//     51 NoCull, 1 plain, 12 itemEntity, plus three `::` references handled below).
		//   * The plain `entityCutout` site is the dangerous one: it COMPILES on 26 untouched and
		//     silently means the no-cull type. Nothing but this rule catches that.
		// A version-dependent target on the existing >=1.21.11 rules, not a second group under
		// >=26, because replacement rules never chain.
		val cutoutCull = if (eval(current.version, ">=26")) "entityCutoutCull" else "entityCutout"
		val cutoutNoCull = if (eval(current.version, ">=26")) "entityCutout" else "entityCutoutNoCull"
		val itemTranslucentCull =
			if (eval(current.version, ">=26")) "entityTranslucentCullItemTarget" else "itemEntityTranslucentCull"
		string("!mc2111-rendertypes-entityCutout", true) {
			replace("RenderType.entityCutout(", "RenderTypes.$cutoutCull(")
		}
		string("!mc2111-rendertypes-entityCutoutNoCull", true) {
			replace("RenderType.entityCutoutNoCull(", "RenderTypes.$cutoutNoCull(")
		}
		string("!mc2111-rendertypes-itemEntityTranslucentCull", true) {
			replace("RenderType.itemEntityTranslucentCull", "RenderTypes.$itemTranslucentCull")
		}
		// …and the method-reference spelling of the one factory this mod names that way (three sites,
		// all the `this(RenderType::entityCutoutNoCull)` default in a model base class). A separate
		// rule because the list above matches the `.` form only. The two `RenderType::guiTextured`
		// references in ACClientCompat are deliberately left alone: they sit in a <1.21.6 arm, which
		// is dead here, and 1.21.11 has no guiTextured on either class to point them at.
		string("!mc2111-rendertypes-ref-entitycutoutnocull", true) {
			replace("RenderType::entityCutoutNoCull", "RenderTypes::$cutoutNoCull")
		}

		// ── RenderStateShard / CompositeState -> RenderSetup ────────────────
		// The other half of the split. `RenderStateShard` dissolved, but three of its constant
		// families simply moved to classes of their own keeping the SAME constant names
		// (LayeringTransform, OutputTarget, TextureTransform — and OutputStateShard's
		// `(String, Supplier<RenderTarget>)` shape survives verbatim as OutputTarget), so those
		// are plain renames. What did NOT survive is the builder: CompositeState became
		// RenderSetup with a different shape, and the texture/lightmap/overlay shards stopped
		// being values at all — they are builder calls now.
		//
		// This tree builds 27 render types by hand (26 in ACRenderTypes, one shared factory in
		// Citadel's WorldRenderMacros), so a gated arm per factory would be 27 duplicated bodies
		// to keep in step forever. `client/render/ACRenderSetup` re-implements the old builder on
		// top of RenderSetup instead and these rules re-point the call sites at it, keeping one
		// code path from 1.21.5 up. Read that class's javadoc before touching any of this.
		//
		// ⚠️ The create rule matches the OPENING QUOTE of the render type's name, which every real
		// call site passes as a literal. That is the only thing keeping it off the shim's own
		// `RenderType.create(name, …)` line — without it the shim would call itself forever.
		val acRenderSetup = "com.github.alexmodguy.alexscaves.client.render.ACRenderSetup"
		val rendertypePkg = "net.minecraft.client.renderer.rendertype"
		mapOf(
			"OutputStateShard" to "$rendertypePkg.OutputTarget",
			"MAIN_TARGET" to "$rendertypePkg.OutputTarget.MAIN_TARGET",
			"ITEM_ENTITY_TARGET" to "$rendertypePkg.OutputTarget.ITEM_ENTITY_TARGET",
			"NO_LAYERING" to "$rendertypePkg.LayeringTransform.NO_LAYERING",
			"VIEW_OFFSET_Z_LAYERING" to "$rendertypePkg.LayeringTransform.VIEW_OFFSET_Z_LAYERING",
			"DEFAULT_TEXTURING" to "$rendertypePkg.TextureTransform.DEFAULT_TEXTURING",
			"EmptyTextureStateShard" to "$acRenderSetup.Texture",
			"NO_TEXTURE" to "$acRenderSetup.NO_TEXTURE",
			"LIGHTMAP" to "$acRenderSetup.LIGHTMAP",
			"NO_LIGHTMAP" to "$acRenderSetup.NO_LIGHTMAP",
			"OVERLAY" to "$acRenderSetup.OVERLAY",
			"NO_OVERLAY" to "$acRenderSetup.NO_OVERLAY"
		).forEach { (member, target) ->
			string("!mc2111-shard-${member.lowercase()}", true) {
				replace("RenderStateShard.$member", target)
			}
		}
		string("!mc2111-compositestate-builder", true) {
			replace("RenderType.CompositeState.builder()", "$acRenderSetup.builder()")
		}
		// The type spelling, kept apart from the builder rule by the trailing space — the two can
		// never start at the same offset. Only Citadel's `compositeState()` return type is live at
		// >=1.21.5; the other hit is in a dead <1.21.5 arm, which the rule rewrites harmlessly.
		// It has to be a rule rather than a gate because that method sits INSIDE the file's
		// `//? if >=1.21.5 {` arm and Stonecutter does not nest gates: a `//? if >=1.21.11` line
		// placed in there leaves BOTH arms commented out and the method loses its signature.
		string("!mc2111-compositestate-type", true) {
			replace("RenderType.CompositeState ", "$acRenderSetup.Composite ")
		}

		// ── OrderedSubmitNodeCollector#submitHitbox deleted ─────────────────
		// Hitboxes are an EntityHitboxDebugRenderer's business from 1.21.11 and HitboxesRenderState
		// is gone with the interface method. ACDrawCollector's no-op override therefore has to
		// disappear — but the whole class body is one `//? if >=1.21.9 {` arm and gates do not
		// nest, so it is demoted to a dead private method by rule. See that file's own note.
		string("!mc2111-drawcollector-hitbox", true) {
			replace(
				"@Override public void submitHitbox(PoseStack poseStack, net.minecraft.client.renderer.entity.state.EntityRenderState state, net.minecraft.client.renderer.entity.state.HitboxesRenderState hitboxes) {",
				"private void ac_submitHitboxGoneIn1_21_11(PoseStack poseStack, net.minecraft.client.renderer.entity.state.EntityRenderState state, PoseStack hitboxes) {"
			)
		}
		string("!mc2111-rendertype-create", true) {
			replace("RenderType.create(\"", "$acRenderSetup.create(\"")
		}

		// ── ResourceKey#location -> #identifier ─────────────────────────────
		// The rename swept the accessors too. TagKey#location did NOT move, so this cannot be a
		// bare-token rule on principle — it is safe here only because every one of this tree's 27
		// `.location()` call sites is on a ResourceKey (checked one by one). Re-check that before
		// adding a TagKey#location call. The reverse direction finds no `.identifier()` in src/.
		// The `::location` method reference in ACCompat needs its own spelling.
		string("!mc2111-key-identifier", true) {
			replace(".location()", ".identifier()")
		}
		string("!mc2111-key-identifier-ref", true) {
			replace("ResourceKey::location", "ResourceKey::identifier")
		}

		// ── game rules ─────────────────────────────────────────────────────
		// GameRules stopped being a bag of nested Key/BooleanValue/IntegerValue types: a rule is a
		// `GameRule<T>` now, read with one generic `get(GameRule<T>)`, and the constants dropped
		// their RULE_ prefix and switched to the underscored spelling of the *command* name. Only
		// the constants are rules; the two accessor signatures are a gated arm in ACCompat, which
		// every call site already goes through.
		mapOf(
			"RULE_DOENTITYDROPS" to "ENTITY_DROPS",
			"RULE_DOMOBLOOT" to "MOB_DROPS",
			"RULE_MOBGRIEFING" to "MOB_GRIEFING",
			"RULE_MAX_ENTITY_CRAMMING" to "MAX_ENTITY_CRAMMING",
			"RULE_MOB_EXPLOSION_DROP_DECAY" to "MOB_EXPLOSION_DROP_DECAY"
		).forEach { (old, new) ->
			string("!mc2111-gamerule-${new.lowercase()}", true) {
				replace("GameRules.$old", "GameRules.$new")
			}
		}

		// ── Entity#hasImpulse -> #needsSync ────────────────────────────────
		// Same public boolean in the same place (set beside setDeltaMovement in every jump path,
		// confirmed in Entity/LivingEntity bytecode); it just says what it is for now.
		string("!mc2111-hasimpulse", true) {
			replace("hasImpulse", "needsSync")
		}

		// ── Camera's getters lost their get ────────────────────────────────
		// getPosition/getBlockPosition/getXRot/getYRot/getEntity -> position/blockPosition/xRot/
		// yRot/entity. These MUST stay receiver-qualified: `getPosition`, `getXRot`, `getYRot` and
		// `getEntity` are all live vanilla methods on other types this mod calls (Entity#getXRot
		// alone has 114 sites, and every Forge event has getEntity), and the post-image spellings
		// `position()`/`xRot()` are ordinary Entity/Vec3 methods used 424 times in src/ — so a bare
		// rule would be wrong in the forward direction and catastrophic in the reverse one.
		listOf(
			"camera.getPosition()" to "camera.position()",
			"getMainCamera().getPosition()" to "getMainCamera().position()",
			"live.getPosition()" to "live.position()",
			"live.getBlockPosition()" to "live.blockPosition()",
			"live.getEntity()" to "live.entity()",
			"getCamera().getBlockPosition()" to "getCamera().blockPosition()",
			"getCamera().getEntity()" to "getCamera().entity()",
			"camera.getXRot()" to "camera.xRot()",
			"camera.getYRot()" to "camera.yRot()"
		).forEachIndexed { i, (old, new) ->
			string("!mc2111-camera-$i", true) { replace(old, new) }
		}
	}

	// ── 26.1 ──────────────────────────────────────────────────────────────────────────────────
	// The first UNOBFUSCATED MC line, and — like 1.21.11 — mostly a rename wave: 23 distinct types
	// this mod names either moved package or were renamed outright. What is NOT expressible as a
	// string rule is gated in source instead: the render-pipeline builder was restructured, the
	// lightmap became a render state, villager trades went data-driven, and LootItemFunctionType
	// was deleted. Those four are called out in the repo's DEVELOPMENT.md, not here.
	//
	// Every target below was verified present at its new path in the node's own patched jar.
	if (eval(current.version, ">=26")) replacements {

		// ── GuiGraphics -> GuiGraphicsExtractor (same package) ──────────────
		// ⚠️ The guard MUST come first. BOTH loaders still spell the overlay-event accessor
		// `getGuiGraphics()` on 26.1 — Forge 62.0.9 and NeoForge 26.1.0.19 each declare
		// `public GuiGraphicsExtractor getGuiGraphics()` — so the bare rule below would rewrite
		// the method name at its 12 call sites and nothing would resolve. Widening the span to
		// include `get` makes this rule START EARLIER there, and the earlier-starting rule
		// consumes the span, so the bare one never sees those offsets. The inserted space is
		// legal Java either side of the parenthesis. This is CleanHUD's trap in mirror image:
		// there the same collision arrived through the reverse pass, which a Kotlin-`if`-guarded
		// group like this one never gets.
		string("!mc261-guigraphics-accessor-guard", true) {
			replace("getGuiGraphics(", "getGuiGraphics (")
		}
		// The package is unchanged, so one bare-token rule also covers the 13 slash-form
		// descriptors inside mixin `method =` / `@At(target =)` literals.
		string("!mc261-guigraphics", true) {
			replace("GuiGraphics", "GuiGraphicsExtractor")
		}

		// ── the GUI draw pipeline: render*/draw* -> extract* ─────────────────
		// 26 finished what 1.21.6 and 1.21.9 started: a screen no longer draws, it *extracts* render
		// state, so every hook in the chain was renamed. The ones below are pure renames — same
		// parameter list, same position, same semantics — so a string rule is exactly right. The four
		// that are NOT pure renames are gated in source instead and deliberately have no rule here:
		// AbstractContainerScreen#renderBg (its arguments were reordered AND it became Screen's own
		// extractBackground, so a rename would compile into a method that overrides nothing),
		// Screen#renderBackground/#render on CaveBookScreen, and the tooltip call
		// AbstractContainerScreen#extractRenderState now makes for itself.
		//
		// ⚠️ Every source pattern is spelled in the ORIGINAL pre-26 form and every target in the 26
		// form, because this whole group is behind a Kotlin `if (eval(...))` and so never gets the
		// reverse pass a condition-guarded rule would.
		string("!mc261-drawstring", true) {
			replace(".drawString(", ".text(")
		}
		// The only AbstractWidget subclass in the tree is SpelunkeryTableWordButton, and the method is
		// abstract on 26, so this is the difference between compiling and not.
		string("!mc261-renderwidget", true) {
			replace("renderWidget", "extractWidgetRenderState")
		}
		string("!mc261-drawconnectivity", true) {
			replace("drawConnectivity", "extractConnectivity")
		}
		// AdvancementWidget#draw is too short a token to rewrite bare. `root.draw(` is unambiguous:
		// the paren keeps it off `root.drawConnectivity(` two lines above it, which the rule before
		// this one handles.
		string("!mc261-advancementwidget-draw", true) {
			replace("root.draw(", "root.extractRenderState(")
		}
		// Scoped by its first parameter type, because CaveMapRenderer has its own unrelated
		// renderLabels(PoseStack, MultiBufferSource, int) and a bare token would rename that too —
		// its declaration and its two call sites, silently and pointlessly. Scoping this way makes the
		// rule START EARLIER than the bare GuiGraphics rule's match inside it, so it consumes that
		// span and has to carry the renamed type itself.
		string("!mc261-renderlabels", true) {
			replace("void renderLabels(GuiGraphics", "void extractLabels(GuiGraphicsExtractor")
		}
		// ClientTooltipComponent#renderImage. Scoped to the declaration form so the two prose
		// mentions of the old name in ClientSackOfSatingTooltip's comments are left alone.
		string("!mc261-renderimage", true) {
			replace("void renderImage(", "void extractImage(")
		}
		// AdvancementTabMixin's two `method =` selectors. Slash-form descriptors, so they are scoped
		// by the owner rather than by a parameter type; the GuiGraphics inside each descriptor sits
		// past the end of this span and is rewritten by the bare rule as usual.
		string("!mc261-advancementtab-drawcontents", true) {
			replace("AdvancementTab;drawContents(", "AdvancementTab;extractContents(")
		}
		string("!mc261-advancementtab-drawtooltips", true) {
			replace("AdvancementTab;drawTooltips(", "AdvancementTab;extractTooltips(")
		}
		// SplashRenderer#render, the last of the draw-chain renames and the only one this tree names
		// only from a mixin selector. Scoped by the owner so the token `render(` — which appears in
		// dozens of unrelated descriptors — cannot be reached; the GuiGraphics inside the same
		// descriptor sits past this span and the bare rule above rewrites it as usual. Nothing else
		// about the method moved: same four parameters, same Matrix3x2f#rotate and
		// ActiveTextCollector#accept anchors 1.21.11 introduced, so that arm serves 26 unchanged.
		string("!mc261-splashrenderer-render", true) {
			replace("SplashRenderer;render(", "SplashRenderer;extractRenderState(")
		}
		// GuiRenderState's whole submit* family is add* on 26. Only the picture-in-picture one is
		// reached from this tree (the cave book's deferred draw).
		string("!mc261-submitpip", true) {
			replace(".submitPicturesInPictureState(", ".addPicturesInPictureState(")
		}
		// Screen#renderBackground. Unlike the container screens' renderBg this IS a pure rename — the
		// argument order was already (mouseX, mouseY, partialTick) — so CaveBookScreen's override and
		// its one self-call are rules. Scoped the same way as renderLabels above; the self-call form
		// also matches the inactive pre-26 arms of the two container screens, which is a no-op.
		string("!mc261-renderbackground-decl", true) {
			replace("void renderBackground(GuiGraphics", "void extractBackground(GuiGraphicsExtractor")
		}
		string("!mc261-renderbackground-call", true) {
			replace("this.renderBackground(", "this.extractBackground(")
		}

		// The addSkyPass lambda, which has no name of its own and so is whatever the loader's
		// mappings invent. Forge numbered it `method_62215` from 1.21.2; from 26 it ships official
		// names rather than remapping, so the synthetic is javac's own `lambda$addSkyPass$0` — the
		// same spelling and, on the loom-mapped loaders, the same capture the 1.21.11 arm of
		// LevelRenderStageMixin already describes, which is why that arm needs no upper bound.
		// The token appears in exactly two files (that mixin and SkyTimeOfDayMixin) and only ever
		// inside a `method =` selector, so a bare rule is safe; SkyTimeOfDayMixin is excluded from
		// the source set above 1.21.10 anyway, so its occurrence is rewritten in a file no 26 node
		// compiles.
		string("!mc261-skypass-lambda", true) {
			replace("method_62215", "lambda\$addSkyPass\$0")
		}

		// GameRenderer's boss-fog dimming field got the name it always meant — the amount by which a
		// boss bar darkens the world, not a generic "darken". Both loaders agree, and 26 also grew a
		// previous-frame sibling and a lerping getter that this tree does not need: GameRendererMixin
		// writes the field at the TAIL of tick(), after vanilla has copied it into the O field, which
		// is exactly where the pre-26 spelling wrote it too. The token appears in one file and
		// nowhere else in the tree, so the rule is bare; it cannot reach the O sibling because the
		// source never spells one.
		string("!mc261-bossoverlaydarkening", true) {
			replace("darkenWorldAmount", "bossOverlayWorldDarkening")
		}
		// renderItemInHand takes the frame's CameraRenderState again — 1.21.6 had dropped the Camera
		// for the game renderer's own, and the deferred-render rewrite gives it back as a state — and
		// its matrix is the read-only Matrix4fc interface now. Two identical `@At` targets in
		// GameRendererMixin, so a rule rather than a fourth arm in each; the whole descriptor is the
		// source string, which cannot collide with anything else in the tree.
		string("!mc261-renderiteminhand", true) {
			replace(
				"renderItemInHand(FZLorg/joml/Matrix4f;)V",
				"renderItemInHand(Lnet/minecraft/client/renderer/state/level/CameraRenderState;FLorg/joml/Matrix4fc;)V"
			)
		}

		// ── package moves ───────────────────────────────────────────────────
		string("!mc261-pkg-bakedquad", true) {
			replace("net.minecraft.client.renderer.block.model.BakedQuad", "net.minecraft.client.resources.model.geometry.BakedQuad")
		}
		string("!mc261-pkg-blockstatemodel", true) {
			replace("net.minecraft.client.renderer.block.model.BlockStateModel", "net.minecraft.client.renderer.block.dispatch.BlockStateModel")
		}
		// BlockAndTintGetter left net.minecraft.world.level for the client entirely. It needs a
		// slash-form twin as well: citadel/LevelRendererMixin names it inside a `method =`
		// descriptor, which no dotted rule can see.
		string("!mc261-pkg-blockandtintgetter", true) {
			replace("net.minecraft.world.level.BlockAndTintGetter", "net.minecraft.client.renderer.block.BlockAndTintGetter")
		}
		string("!mc261-pkg-blockandtintgetter-desc", true) {
			replace("net/minecraft/world/level/BlockAndTintGetter", "net/minecraft/client/renderer/block/BlockAndTintGetter")
		}

		// ── the level render states moved down one package ──────────────────
		// ⚠️ Per CLASS, never as a package-prefix rule: net.minecraft.client.renderer.state still
		// exists and still holds MapRenderState, LightmapRenderState, GameRenderState,
		// OptionsRenderState and WindowRenderState. Only the level-facing six moved into
		// `.state.level`, and MapRendererMapInstanceMixin names one of the stayers.
		listOf(
			"CameraRenderState",
			"LevelRenderState",
			"ParticleGroupRenderState",
			"ParticlesRenderState",
			"QuadParticleRenderState",
			"SkyRenderState"
		).forEach { type ->
			string("!mc261-pkg-${type.lowercase()}", true) {
				replace("net.minecraft.client.renderer.state.$type", "net.minecraft.client.renderer.state.level.$type")
			}
		}
		// …and the three of those six that also appear in a mixin descriptor string.
		listOf("CameraRenderState", "LevelRenderState", "SkyRenderState").forEach { type ->
			string("!mc261-pkg-${type.lowercase()}-desc", true) {
				replace("net/minecraft/client/renderer/state/$type", "net/minecraft/client/renderer/state/level/$type")
			}
		}

		// ── the GUI render states moved the OTHER way, out of client.gui ────
		// net.minecraft.client.gui.render.state is gone entirely; net.minecraft.client.gui.render
		// itself is not (GuiRenderer and the pip package are still there), so this is two
		// per-class rules rather than one prefix rule.
		string("!mc261-pkg-guirenderstate", true) {
			replace("net.minecraft.client.gui.render.state.GuiRenderState", "net.minecraft.client.renderer.state.gui.GuiRenderState")
		}
		string("!mc261-pkg-pipstate", true) {
			replace("net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState", "net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState")
		}

		// ── LightTexture was split in two ───────────────────────────────────
		// The instance half (the texture, its lifecycle and the per-frame upload) kept the class and
		// was renamed `Lightmap`; the static bit-packing helpers moved out to `net.minecraft.util
		// .LightCoordsUtil`, which is not even a client class any more. So the two static call sites
		// this mod has go to DIFFERENT destinations and a bare-token rule cannot express it — nor
		// could it be written at all, since the token also spells `LightTextureMixin`'s own class
		// name (which Stonecutter can never rewrite, the filename being fixed) and the `updateLight
		// Texture` field. Both call sites are therefore fully qualified in source and matched whole.
		string("!mc261-lightcoords-pack", true) {
			replace("net.minecraft.client.renderer.LightTexture.pack", "net.minecraft.util.LightCoordsUtil.pack")
		}
		string("!mc261-lightmap-getbrightness", true) {
			replace("net.minecraft.client.renderer.LightTexture.getBrightness", "net.minecraft.client.renderer.Lightmap.getBrightness")
		}

		// ── ItemRenderer -> feature.ItemFeatureRenderer ─────────────────────
		// 26.1 moved the item draw into the deferred feature-render pass and renamed the class with
		// it. Only ONE of its statics survives the move — getFoilBuffer, with the same four
		// arguments — and that is the only one live source still calls; renderItem became a private
		// instance method reading a SubmitNodeStorage$ItemSubmit, which is why ACDrawCollector gets
		// a >=26 arm rather than a rule. Matched on the qualified name plus the member, so the two
		// getArmorFoilBuffer arms (dead above 1.21.9) and every `getItemRenderer()` are untouched.
		string("!mc261-itemfeaturerenderer-foil", true) {
			replace(
				"net.minecraft.client.renderer.entity.ItemRenderer.getFoilBuffer",
				"net.minecraft.client.renderer.feature.ItemFeatureRenderer.getFoilBuffer"
			)
		}

		// ── getLightColor -> getLightCoords ─────────────────────────────────
		// 26 renamed both halves of the same idea in one sweep: Particle#getLightColor(float) and
		// LevelRenderer's two getLightColor statics are all getLightCoords now. This mod has 26
		// particles that OVERRIDE the first one and twelve call sites, and the overrides are the
		// dangerous half — without an @Override (upstream wrote none) a stale name compiles clean and
		// simply stops being called, so every particle that forces full-bright would silently go dark.
		// A bare token is safe here: `getLightColor` appears nowhere in src/ except those sites, the
		// LevelRendererMixin selector, and that mixin's own `ac_getLightColor` handler, which the rule
		// renames consistently on both its declaration and its call. The mixin's live >=26 arm is
		// written natively (its parameter type moved too), so the rule never has to be right about it.
		string("!mc261-getlightcoords", true) {
			replace("getLightColor", "getLightCoords")
		}

		// ── DimensionDataStorage -> SavedDataStorage ────────────────────────
		// Same package, same API surface for the three call sites here (computeIfAbsent/get/set).
		// Safe as a bare token: neither spelling appears inside any longer identifier in src/.
		string("!mc261-saveddatastorage", true) {
			replace("DimensionDataStorage", "SavedDataStorage")
		}

		// ── Entity#interact gained the hit location ─────────────────────────
		// 26 folded interactAt into interact, so the method carries the hit Vec3 now. All eight
		// overrides in this tree are spelled identically and none of them reads the new argument, so
		// the declaration is a rule; the parameter name it introduces is what the forwarding rule
		// below passes on. The mixed call in MultipartEntityMessage has no hit location to forward
		// and is gated in source instead.
		string("!mc261-entity-interact-decl", true) {
			replace("InteractionResult interact(Player player, InteractionHand hand)",
					"InteractionResult interact(Player player, InteractionHand hand, net.minecraft.world.phys.Vec3 vec3)")
		}
		string("!mc261-entity-interact-forward", true) {
			replace("parent.interact(player, hand)", "parent.interact(player, hand, vec3)")
		}

		// ── Forge 62 moved the client registration events off the mod bus ──────────────────
		// Same shape as the Forge 59 move of EntityAttributeCreationEvent: on eventbus 7 a
		// game-bus event carries a `public static final EventBus<X> BUS` and a mod-bus event
		// carries only `getBus(BusGroup)`. In 62.0.9 every event this mod listens to from
		// ClientProxy — RegisterParticleProviders, RegisterKeyMappings,
		// RegisterColorHandlersEvent.Block, RegisterClientTooltipComponentFactories and
		// EntityRenderersEvent.AddLayers/.RegisterLayerDefinitions — has the field and no
		// longer implements IModBusEvent, while the FML lifecycle and config events keep
		// getBus. So this cannot be one blanket rule: the ClientProxy sites are matched
		// through their bus variable (`bus`, used nowhere else in this tree) and the single
		// AlexsCaves site by name, leaving its five lifecycle neighbours on getBus.
		string("!mc261-clientevent-bus", true) {
			replace(".getBus(bus)", ".BUS")
		}
		string("!mc261-layerdefinitions-bus", true) {
			replace("RegisterLayerDefinitions.getBus(modEventBus)", "RegisterLayerDefinitions.BUS")
		}

		// ── 26 turned "does this block need post-processing" into "where" ─────────────────────
		// BlockBehaviour.Properties#hasPostProcess(StatePredicate) is now #postProcess(PostProcess),
		// whose getPostProcessPos returns the BlockPos to re-process or null for none — the default
		// is a lambda returning null, and vanilla's Blocks#postProcessSelf, the direct successor of
		// the old `-> true`, returns the pos it was handed (read out of the Blocks bootstrap-method
		// table, since the lambda has no javap-visible name). So `-> true` becomes `-> pos`, which
		// is a rename AND a body change in one contiguous span, hence one rule rather than two.
		// All three call sites in this mod (both primal magmas and the volcanic core) are written
		// byte-identically for exactly that reason.
		string("!mc26-haspostprocess", true) {
			replace("hasPostProcess((state, getter, pos) -> true)", "postProcess((state, getter, pos) -> pos)")
		}

		// ── PathType's danger/damage constants were renamed for what they mean ────────────────
		// The enum is otherwise untouched — same package, same order, same malus values — so the
		// mapping was read positionally out of the two bytecodes rather than guessed from the
		// names: 1.21.11 and 26.1 declare 26 and 27 constants whose first 25 line up one for one,
		// and DANGER_* turns out to mean "a neighbour is dangerous", DAMAGE_* "this block is".
		// (26.1 also adds BIG_MOBS_CLOSE_TO_DANGER at the end; nothing here wants it.)
		//
		// Keyed on the CONSTANT ALONE, not on `PathType.CONST`. The class itself is renamed from
		// BlockPathTypes by !mc205-pathtype-enum, and rules do not chain — they all match the
		// original text — so a rule spelling the qualified name would start at the same offset as
		// that one and the two would fight over the span. Splitting them at the dot leaves two
		// adjacent, non-overlapping matches instead. Safe because each of these six tokens occurs
		// in this tree only as a PathType constant (checked across src/main).
		string("!mc26-pathtype-danger-fire", true) { replace("DANGER_FIRE", "FIRE_IN_NEIGHBOR") }
		string("!mc26-pathtype-damage-fire", true) { replace("DAMAGE_FIRE", "FIRE") }
		string("!mc26-pathtype-danger-other", true) { replace("DANGER_OTHER", "DAMAGING_IN_NEIGHBOR") }
		string("!mc26-pathtype-damage-other", true) { replace("DAMAGE_OTHER", "DAMAGING") }
		string("!mc26-pathtype-danger-snow", true) { replace("DANGER_POWDER_SNOW", "ON_TOP_OF_POWDER_SNOW") }

		// ── The per-tick fluid step was renamed with the EntityFluidInteraction split ─────────
		// 26 pulled an entity's fluid state out into an EntityFluidInteraction object, and the
		// tick method that fills it is `updateFluidInteraction()` — same `protected boolean ()`
		// on both sides, so this is a pure rename. Both call sites (the nuclear bomb and the
		// minecart mixin, which extends Entity and therefore calls it directly rather than
		// shadowing it) are inherited calls, so nothing else has to move with it.
		string("!mc26-fluid-interaction", true) {
			replace("updateInWaterStateAndDoFluidPushing", "updateFluidInteraction")
		}

		// ── A container component holds templates now, so spilling one has to instantiate ─────
		// ItemContainerContents stores List<Optional<ItemStackTemplate>> from 26, and its
		// nonEmptyItems() yields those templates rather than stacks. ItemUtils#onContainerDestroyed
		// moved the same way it did between 1.20.1 and 1.20.5, back from Iterable to
		// Stream<ItemStack>, and the member that hands it one is nonEmptyItemCopyStream() — the
		// copy is what the old Iterable already gave, since the dropped stacks must not alias the
		// component. One call site (the shulker-box arm of BlockItemWithSupplier).
		string("!mc26-container-spill", true) {
			replace("nonEmptyItems()", "nonEmptyItemCopyStream()")
		}

		// ── DefaultVertexFormat.NEW_ENTITY -> ENTITY ──────────────────────────────────────────
		// The "new" entity format has been the only entity format since 1.15; 26 finally dropped
		// the adjective. Same elements in the same order (position, colour, uv0, uv1 overlay, uv2
		// light, normal), so it is a pure rename. Qualified with the class because `NEW_ENTITY` on
		// its own is also the name RenderStateShard-era code gives its shader constants, and this
		// rule must not reach those. Roughly twenty sites across ACRenderTypes, ACInternalShaders
		// and ClientProxy, most of them inside arms that are gated out on 26 — harmless, since a
		// rewritten token inside a commented-out branch is never compiled.
		string("!mc26-vertexformat-entity", true) {
			replace("DefaultVertexFormat.NEW_ENTITY", "DefaultVertexFormat.ENTITY")
		}

		// ── EntityRenderer#submitNameTag -> submitNameDisplay ─────────────────────────────────
		// A pure rename: same protected access, same four-argument descriptor (S, PoseStack,
		// SubmitNodeCollector, CameraRenderState), same position in the class. Only the two legacy
		// name-tag bridges in client/render/compat call it, and both spell the call identically, so
		// the rule matches the whole `super.<name>(this.renderingState` prefix rather than the bare
		// method name — this mod's own ACDrawCollector declares a submitNameTag of its own (a
		// different, five-argument thing that draws into a collector) and a token-level rule would
		// rename that too, along with its callers and its javadoc.
		string("!mc26-nametag-submit", true) {
			replace("super.submitNameTag(this.renderingState", "super.submitNameDisplay(this.renderingState")
		}
	}

	// ══ 26.2 ═════════════════════════════════════════════════════════════════════════════════
	if (eval(current.version, ">=26.2")) replacements {
		// ── MultiBufferSource is gone ─────────────────────────────────────────────────────────
		// 26.2 deleted immediate-mode rendering outright: no MultiBufferSource, no
		// VertexMultiConsumer, no Minecraft#renderBuffers(). Roughly 150 files in this tree name
		// the interface, almost all of them as a parameter type on a render body that has kept its
		// pre-1.21.2 shape all the way down the walk — and rewriting those bodies is NOT the port,
		// because ACSubmitBuffers has translated "draw into a MultiBufferSource" into "record per
		// RenderType, then submitCustomGeometry" since 1.21.9 and that translation is unchanged
		// here. The only thing missing on 26.2 is the interface itself, so the tree vendors it in
		// client/render/compat and this one rule re-points every reference at the copy.
		//
		// ONE rule covers all 144 import lines AND the ~15 fully-qualified uses, because the FQN is
		// a substring of each import. The ~22 remaining occurrences are slash-separated mixin
		// selector strings, which this rule cannot reach by construction — and must not, since
		// those describe vanilla bytecode on the versions where they are live.
		//
		// ⚠️ Rewriting an import in ACSubmitBuffers/MultiBufferSource's own package leaves a
		// same-package import, which javac allows. And the prose in those two files deliberately
		// never spells the qualified name, since a rule reaches comments as readily as code.
		string("!mc262-multibuffersource", true) {
			replace("net.minecraft.client.renderer.MultiBufferSource",
					"com.github.alexmodguy.alexscaves.client.render.compat.MultiBufferSource")
		}

		// ── the HUD moved out of Gui ──────────────────────────────────────────────────────────
		// 26.2 split the in-game HUD off Gui into its own net.minecraft.client.gui.Hud, reachable
		// as the public final Gui#hud. NeoForge's two status-bar counters — the only members of
		// either class this tree touches — went with it. A rule rather than a gate because both
		// reads sit inside ClientEvents#hudStackHeight's existing `neoforge && >=1.20.5` arm and
		// Stonecutter cannot nest a second condition in it; one rule rather than two because the
		// two reads are one expression, so the span is unique either way.
		string("!mc262-hud-heights", true) {
			replace("Minecraft.getInstance().gui.leftHeight, Minecraft.getInstance().gui.rightHeight",
					"Minecraft.getInstance().gui.hud.leftHeight, Minecraft.getInstance().gui.hud.rightHeight")
		}

		// ── …and so is the global that handed one out ─────────────────────────────────────────
		// Eleven sites ask the game for a buffer source mid-frame. On 26.2 there is no global to
		// ask, so they go to ACRenderContext, which holds the collector LevelRenderStageMixin
		// pushes for the length of the submission phase.
		//
		// Both rules carry the whole receiver chain from `Minecraft.getInstance()` — a rule on the
		// tail alone would leave a dangling `Minecraft.getInstance().` in front of a static call.
		// That is also why UiRenderMacros' `mc.renderBuffers()` was normalised to this spelling in
		// source rather than given a rule of its own.
		//
		// The two sources cannot overlap (after `renderBuffers().` one reads `bufferSource` and the
		// other `crumblingBufferSource`, so neither is a substring of the other) and their targets
		// differ, so this is not an ambiguous replacement.
		//
		// Two sites reach a RenderBuffers that is NOT the game instance's — GameRendererMixin off
		// its @Shadow field, PathfindingDebugRenderer off a `new RenderBuffers(1)` of its own — so
		// they spell it `renderBuffers.bufferSource()`, with no parens after the receiver, and the
		// chain rule above cannot see them. Both go to the same place, because on 26.2 there is no
		// such thing as a private immediate buffer either: RenderBuffers kept only its section
		// pack, its pool and the staged vertex buffer. That makes this two sources on ONE rule
		// (the allowed many-to-one shape) rather than a second rule sharing a target, which would
		// fail configuration as an ambiguous replacement.
		//
		// Neither site needs the receiver to survive: GameRenderer still declares the field, so the
		// @Shadow keeps resolving and is merely unused, and PathfindingDebugRenderer's own
		// `new RenderBuffers(1)` still constructs. And ACRenderContext.bufferSource() is a
		// singleton getter, so the debug renderer's static-initialiser call is safe at class-load.
		string("!mc262-buffersource", true) {
			replace("Minecraft.getInstance().renderBuffers().bufferSource()",
					"com.github.alexmodguy.alexscaves.client.render.compat.ACRenderContext.bufferSource()")
			replace("renderBuffers.bufferSource()",
					"com.github.alexmodguy.alexscaves.client.render.compat.ACRenderContext.bufferSource()")
		}
		string("!mc262-crumblingbuffersource", true) {
			replace("Minecraft.getInstance().renderBuffers().crumblingBufferSource()",
					"com.github.alexmodguy.alexscaves.client.render.compat.ACRenderContext.crumblingBufferSource()")
		}

		// VertexMultiConsumer went with it — the glint pass is a property of a submitted node now,
		// not a second consumer. Two sites (the armour foil branches); the copy fans out the same
		// way the vanilla class did.
		string("!mc262-vertexmulticonsumer", true) {
			replace("com.mojang.blaze3d.vertex.VertexMultiConsumer",
					"com.github.alexmodguy.alexscaves.client.render.compat.VertexMultiConsumer")
		}

		// ── The GUI split in two ──────────────────────────────────────────────────────────────
		// `net.minecraft.client.gui.Gui` still exists on 26.2 and Minecraft#gui is still typed as
		// it, so MinecraftMixin's @Shadow needs nothing — but the class behind the name is a
		// different one. Gui is the screen/overlay manager now (it took Minecraft#screen and
		// #setScreen with it) and the in-game HUD moved wholesale to a new sibling,
		// net.minecraft.client.gui.Hud, reachable as the public final `Gui#hud`. So every call
		// this tree makes THROUGH the field grows one hop; the field's own type does not move.
		//
		// Each rule carries the `gui.` receiver so it cannot fire on some unrelated
		// same-named method, and the two screen rules carry `Minecraft.getInstance()` for the same
		// reason the buffer-source rules above do.
		string("!mc262-hud-overlaymessage", true) {
			replace("gui.setOverlayMessage(", "gui.hud.setOverlayMessage(")
		}
		string("!mc262-hud-bossoverlay", true) {
			replace("gui.getBossOverlay()", "gui.hud.getBossOverlay()")
		}
		string("!mc262-setscreen", true) {
			replace("Minecraft.getInstance().setScreen(", "Minecraft.getInstance().gui.setScreen(")
		}

		// ── Package moves, no behaviour attached ──────────────────────────────────────────────
		// Bucketable left world.entity.animal for world.entity (the package was reorganised into
		// per-species subpackages and the shared interfaces moved up a level).
		string("!mc262-bucketable", true) {
			replace("net.minecraft.world.entity.animal.Bucketable", "net.minecraft.world.entity.Bucketable")
		}

		// The advancement criteria system was re-filed: `advancements.critereon` is gone entirely,
		// split into `advancements.triggers` (the triggers, Criterion, CriteriaTriggers) and
		// `advancements.predicates` (+ a `.entity` sub-package). Only the six names this tree
		// spells are rewritten; ACAdvancementTrigger's pre-1.20.2 arm names three more, but it is
		// commented out from 1.20.2 up so nothing there has to resolve.
		//
		// ⚠️ `net.minecraft.advancements.CriterionTrigger` is a prefix of the untouched
		// `…advancements.CriterionTriggerInstance`. This tree never names the latter (checked), so
		// the rule is safe as written — but it would silently corrupt it if one ever appeared.
		string("!mc262-criteriatriggers", true) {
			replace("net.minecraft.advancements.CriteriaTriggers", "net.minecraft.advancements.triggers.CriteriaTriggers")
		}
		string("!mc262-criteriontrigger", true) {
			replace("net.minecraft.advancements.CriterionTrigger", "net.minecraft.advancements.triggers.CriterionTrigger")
		}
		string("!mc262-simplecriteriontrigger", true) {
			replace("net.minecraft.advancements.critereon.SimpleCriterionTrigger",
					"net.minecraft.advancements.triggers.SimpleCriterionTrigger")
		}
		string("!mc262-contextawarepredicate", true) {
			replace("net.minecraft.advancements.critereon.ContextAwarePredicate",
					"net.minecraft.advancements.predicates.ContextAwarePredicate")
		}
		string("!mc262-entitypredicate", true) {
			replace("net.minecraft.advancements.critereon.EntityPredicate",
					"net.minecraft.advancements.predicates.entity.EntityPredicate")
		}

		// ── The 2d/3d noise condition split ───────────────────────────────────────────────────
		//
		// 26.2 replaced SurfaceRules#noiseCondition(key, D, D) with noiseCondition2d and
		// noiseCondition3d; both build the same NoiseThresholdConditionSource and differ only in a
		// trailing boolean, which the old method passed as `false` — so the direct successor of
		// every existing call is the 2d one. Both of this mod's sites (GRAVEL and ICE, in
		// ACSurfaceRules) are plain surface noise, so neither wants the 3d sampler.
		string("!mc262-noisecondition", true) {
			replace("SurfaceRules.noiseCondition(", "SurfaceRules.noiseCondition2d(")
		}

		// ── MobCategory gained a debug abbreviation ───────────────────────────────────────────
		//
		// 26.2 added a third constructor argument to MobCategory: a short code shown in the F3
		// mob-count readout (MONSTER "MO", CREATURE "C", UNDERGROUND_WATER_CREATURE "UWC", …),
		// inserted after the serialized name. It reaches ACMobCategories twice — once in the
		// Forge-and-below MobCategory.create arm, once in the NeoForge EnumProxy arm — and both
		// spell the same argument list, so one rule per category covers the pair rather than the
		// four-arm gate chain a nested condition would need. "CC" and "DSC" collide with none of
		// vanilla's eight.
		string("!mc262-mobcategory-cave", true) {
			replace("\"alexscaves:cave_creature\", 10,", "\"alexscaves:cave_creature\", \"CC\", 10,")
		}
		string("!mc262-mobcategory-deepsea", true) {
			replace("\"alexscaves:deep_sea_creature\", 20,", "\"alexscaves:deep_sea_creature\", \"DSC\", 20,")
		}

		// ── Three vanilla spelling fixes ──────────────────────────────────────────────────────
		//
		// "Instantenous" was misspelled on MobEffect#isInstantenous and #applyInstantenousEffect
		// since forever and is corrected in 26.2; one rule covers both, since the misspelling is
		// the shared part. markPosForPostprocessing likewise gained its capital P. Neither is an
		// API change — the descriptors are identical either side.
		string("!mc262-instantaneous", true) {
			replace("Instantenous", "Instantaneous")
		}
		string("!mc262-postprocessing", true) {
			replace("markPosForPostprocessing", "markPosForPostProcessing")
		}

		// ── Two more vanilla classes deleted outright, both vendored ──────────────────────────
		// Tuple had no successor and no behaviour; FlyingAnimal had no successor at all. See each
		// vendored file for what the copy does and does not recover — the FlyingAnimal one loses
		// the ability to recognise a VANILLA flier, which is why the three `instanceof` sites go
		// through ACFlyingAnimals instead of testing the interface directly.
		string("!mc262-tuple", true) {
			replace("net.minecraft.util.Tuple", "com.github.alexmodguy.alexscaves.server.compat.Tuple")
		}
		string("!mc262-flyinganimal", true) {
			replace("net.minecraft.world.entity.animal.FlyingAnimal",
					"com.github.alexmodguy.alexscaves.server.compat.FlyingAnimal")
		}

		// ── Every EntityType constant moved to a sibling holder ──
		//
		// 26.2 emptied net.minecraft.world.entity.EntityType of its ~150 `public static final
		// EntityType<X>` constants and re-declared them all on a new class beside it,
		// net.minecraft.world.entity.EntityTypes. The type itself is still spelled EntityType
		// everywhere and keeps its own statics (CODEC, STREAM_CODEC, getKey, the Builder DSL) — only
		// the constants moved.
		//
		// The workspace notes say a rename rule "cannot express this", because EntityType is a
		// prefix of EntityTypes and a bidirectional rule would rewrite the type token too, then
		// rewrite it back on the other nodes. Neither half applies here: each rule below carries the
		// `.CONSTANT` suffix in its source, so the bare type token and `EntityType.Builder` /
		// `EntityType.CODEC` are untouched, and this whole group sits behind a Kotlin
		// `if (eval(...))` so it is never registered on another node and gets no reverse pass. One
		// rule per constant rather than one per site; the replacement is fully qualified so no file
		// needs an import it would not otherwise have.
		//
		// ⚠️ Deliberately NOT covering EntityType.SIGN / EntityType.HANGING_SIGN: those two strings
		// occur in this tree only as the tail of BlockEntityType.SIGN / .HANGING_SIGN, which is an
		// unrelated class, and a rule matches with no left boundary. Those two are handled in source.
		string("!mc262-et-lightning-bolt", true) {
			replace("EntityType.LIGHTNING_BOLT", "net.minecraft.world.entity.EntityTypes.LIGHTNING_BOLT")
		}
		string("!mc262-et-enderman", true) {
			replace("EntityType.ENDERMAN", "net.minecraft.world.entity.EntityTypes.ENDERMAN")
		}
		string("!mc262-et-player", true) {
			replace("EntityType.PLAYER", "net.minecraft.world.entity.EntityTypes.PLAYER")
		}
		string("!mc262-et-witch", true) {
			replace("EntityType.WITCH", "net.minecraft.world.entity.EntityTypes.WITCH")
		}
		string("!mc262-et-pig", true) {
			replace("EntityType.PIG", "net.minecraft.world.entity.EntityTypes.PIG")
		}
		string("!mc262-et-magma-cube", true) {
			replace("EntityType.MAGMA_CUBE", "net.minecraft.world.entity.EntityTypes.MAGMA_CUBE")
		}
		string("!mc262-et-glow-item-frame", true) {
			replace("EntityType.GLOW_ITEM_FRAME", "net.minecraft.world.entity.EntityTypes.GLOW_ITEM_FRAME")
		}
		string("!mc262-et-falling-block", true) {
			replace("EntityType.FALLING_BLOCK", "net.minecraft.world.entity.EntityTypes.FALLING_BLOCK")
		}
		string("!mc262-et-ender-dragon", true) {
			replace("EntityType.ENDER_DRAGON", "net.minecraft.world.entity.EntityTypes.ENDER_DRAGON")
		}
		string("!mc262-et-creeper", true) {
			replace("EntityType.CREEPER", "net.minecraft.world.entity.EntityTypes.CREEPER")
		}
		string("!mc262-et-arrow", true) {
			replace("EntityType.ARROW", "net.minecraft.world.entity.EntityTypes.ARROW")
		}

		// The other half of the GUI split: the HUD tick counter went with the HUD.
		string("!mc262-hud-guiticks", true) {
			replace("gui.getGuiTicks()", "gui.hud.getGuiTicks()")
		}

		// ── emissiveRendering stopped being a StatePredicate ──
		//
		// Every other BlockBehaviour.Properties predicate (isRedstoneConductor, isSuffocating,
		// isViewBlocking, hasPostProcess) is still `StatePredicate` — (state, level, pos) -> boolean
		// — but 26.2 narrowed emissiveRendering alone to a plain Predicate<BlockState>: whether a
		// state glows is a property of the state, and nothing in vanilla ever consulted the two
		// dropped arguments. Neither does this mod: all eighteen call sites are a constant, so the
		// translation loses nothing.
		//
		// A gate would mean eighteen arms inside eighteen long constructor chains; the difference is
		// one argument expression, which is exactly what a rule is for. The three targets differ only
		// in the lambda's parameter name because two rules that shared a target would fail
		// configuration with "Ambiguous replacement".
		string("!mc262-emissive-level-true", true) {
			replace("emissiveRendering((state, level, pos) -> true)", "emissiveRendering((state) -> true)")
		}
		string("!mc262-emissive-getter-true", true) {
			replace("emissiveRendering((state, getter, pos) -> true)", "emissiveRendering((blockState) -> true)")
		}
		string("!mc262-emissive-world-false", true) {
			replace("emissiveRendering((state, world, pos) -> false)", "emissiveRendering((state) -> false)")
		}
		// DepthGlassBlock::yes is a StatePredicate that the class also uses nowhere else, so it stays
		// three-argument for its own sake and only this reference is rewritten.
		string("!mc262-emissive-depthglass", true) {
			replace("emissiveRendering(DepthGlassBlock::yes)", "emissiveRendering((s) -> true)")
		}

		// ── VertexFormat.Mode became a top-level PrimitiveTopology ──
		//
		// 26.2 lifted the nested enum out of com.mojang.blaze3d.vertex.VertexFormat and re-declared
		// it as com.mojang.blaze3d.PrimitiveTopology, with the same eight constants under the same
		// names — a topology is a property of the draw, not of the vertex layout, and the two are
		// supplied separately now (RenderPipeline.Builder#withVertexBinding + #withPrimitiveTopology
		// where there was one #withVertexFormat).
		//
		// The replacement is fully qualified so none of the eight files needs an import; the ones
		// that keep using VertexFormat itself keep theirs. Source must therefore spell the nested
		// type SHORT everywhere — a fully-qualified `com.mojang.blaze3d.vertex.VertexFormat.Mode`
		// would be rewritten in place and yield a doubled package prefix.
		string("!mc262-vertexformat-mode", true) {
			replace("VertexFormat.Mode", "com.mojang.blaze3d.PrimitiveTopology")
		}

		// ── samplers and uniforms became bind group layouts ──
		//
		// 26.2 deleted RenderPipeline.Builder#withSampler and #withUniform outright. A pipeline no
		// longer names its samplers and its uniform blocks one at a time; it declares the
		// BindGroupLayouts it draws with, and each layout carries a fixed set of both. The layouts
		// vanilla ships are exactly the combinations its own pipelines use, and every combination
		// this mod asks for is among them — read out of RenderPipelines.<clinit> in the 26.2 jar,
		// where ENTITY_TRANSLUCENT is ENTITY_SNIPPET plus BindGroupLayouts.SAMPLER1, which is
		// precisely what .withSampler("Sampler1") on top of ENTITY_SNIPPET means here, and LIGHTMAP
		// is GLOBALS_SNIPPET plus BindGroupLayouts.LIGHTMAP_INFO.
		//
		// There is deliberately no Sampler2 rule: its one site is inside ACInternalShaders#text(),
		// which is a gated arm from 26.2 because WORLD_TEXT_SNIPPET already carries SAMPLER2 — so
		// that line does not survive into the 26.2 tree to be rewritten.
		//
		// The replacement is fully qualified so no file needs a new import.
		string("!mc262-sampler0", true) {
			replace(".withSampler(\"Sampler0\")", ".withBindGroupLayout(net.minecraft.client.renderer.BindGroupLayouts.SAMPLER0)")
		}
		string("!mc262-sampler1", true) {
			replace(".withSampler(\"Sampler1\")", ".withBindGroupLayout(net.minecraft.client.renderer.BindGroupLayouts.SAMPLER1)")
		}
		string("!mc262-sampler-in", true) {
			replace(".withSampler(\"InSampler\")", ".withBindGroupLayout(net.minecraft.client.renderer.BindGroupLayouts.IN_SAMPLER)")
		}
		string("!mc262-uniform-lightmapinfo", true) {
			replace(".withUniform(\"LightmapInfo\", UniformType.UNIFORM_BUFFER)", ".withBindGroupLayout(net.minecraft.client.renderer.BindGroupLayouts.LIGHTMAP_INFO)")
		}

		// The two pipelines built from a bare builder — the lightmap and the post-effect blit — are
		// rooted at GLOBALS_SNIPPET, which is what vanilla's own LIGHTMAP and ENTITY_OUTLINE_BLIT do
		// on 26.2. A declared-but-unread bind group is harmless (vanilla's BEACON_BEAM_SNIPPET
		// declares GLOBALS for a shader that never reads it); a *missing* one the shader does read
		// is fatal, so this is the safe direction.
		string("!mc262-builder-globals", true) {
			replace("RenderPipeline.builder()", "RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)")
		}

		// ── withVertexFormat split into withVertexBinding + withPrimitiveTopology ──
		//
		// The vertex layout and the primitive topology are independent now, so one call became two.
		// A rule per format rather than one generic rule on ", VertexFormat.Mode." because
		// ACRenderTypes spells that same pair inside RenderType.create(...) calls, where there is no
		// builder to chain onto.
		//
		// Each source carries the trailing comma, and that is what keeps the six mutually exclusive:
		// `POSITION,` cannot match a POSITION_COLOR site and `POSITION_TEX,` cannot match a
		// POSITION_TEX_COLOR one, so no two of them can ever start at the same offset. The mode
		// token itself is left to !mc262-vertexformat-mode above, whose match starts after this
		// one's ends — which also covers WorldRenderMacros#pipeline, whose mode is a *parameter* and
		// so could never be part of a whole-expression rule.
		//
		// ⚠️ Replacements do NOT chain — every rule matches the ORIGINAL file text — and where two
		// matches overlap the earlier-STARTING rule consumes the span. These start at
		// `.withVertexFormat(`, i.e. before !mc26-vertexformat-entity's `DefaultVertexFormat
		// .NEW_ENTITY` and before !mc21-vertexformat-postexcolor-arg's `POSITION_COLOR_TEX,`, so
		// neither of those rules ever sees these sites and the replacements below have to spell the
		// final 26.2 names themselves: ENTITY, and POSITION_TEX_COLOR.
		string("!mc262-vertexbinding-entity", true) {
			replace(".withVertexFormat(DefaultVertexFormat.NEW_ENTITY,", ".withVertexBinding(0, DefaultVertexFormat.ENTITY).withPrimitiveTopology(")
		}
		string("!mc262-vertexbinding-position", true) {
			replace(".withVertexFormat(DefaultVertexFormat.POSITION,", ".withVertexBinding(0, DefaultVertexFormat.POSITION).withPrimitiveTopology(")
		}
		string("!mc262-vertexbinding-position-color", true) {
			replace(".withVertexFormat(DefaultVertexFormat.POSITION_COLOR,", ".withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR).withPrimitiveTopology(")
		}
		string("!mc262-vertexbinding-position-color-tex", true) {
			replace(".withVertexFormat(DefaultVertexFormat.POSITION_COLOR_TEX,", ".withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR).withPrimitiveTopology(")
		}
		string("!mc262-vertexbinding-position-tex", true) {
			replace(".withVertexFormat(DefaultVertexFormat.POSITION_TEX,", ".withVertexBinding(0, DefaultVertexFormat.POSITION_TEX).withPrimitiveTopology(")
		}
		string("!mc262-vertexbinding-particle", true) {
			replace(".withVertexFormat(DefaultVertexFormat.PARTICLE,", ".withVertexBinding(0, DefaultVertexFormat.PARTICLE).withPrimitiveTopology(")
		}

		// The single POSITION_TEX_COLOR site cannot take the short form above: its replacement would
		// be byte-identical to the POSITION_COLOR_TEX rule's (both formats end up spelled
		// POSITION_TEX_COLOR on 26.2), and two rules sharing a target fail configuration as an
		// ambiguous replacement. Spelling the whole expression, mode included, makes the target
		// distinct — and the mode rule above never fires inside it, since this match starts earlier.
		string("!mc262-vertexbinding-position-tex-color-quads", true) {
			replace(".withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)", ".withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR).withPrimitiveTopology(com.mojang.blaze3d.PrimitiveTopology.QUADS)")
		}

		// DefaultVertexFormat.EMPTY is gone with the call that consumed it: a screenquad pass binds
		// no vertex buffer at all now, it just declares a topology and draws three vertices out of
		// gl_VertexID. Vanilla's own LIGHTMAP and ENTITY_OUTLINE_BLIT are exactly this shape on
		// 26.2 — a topology and no withVertexBinding — so the binding half of the call is dropped
		// rather than translated. Both sites (the lightmap and the post-effect blit) are the same
		// whole expression, which is what lets one rule serve them.
		string("!mc262-vertexbinding-empty", true) {
			replace(".withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)", ".withPrimitiveTopology(com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)")
		}

		// ── ColorTargetState gained the target's format ──
		//
		// 26.2 put the render target's GpuFormat into the state, between the blend function and the
		// write mask. The one-argument convenience constructor fills in RGBA8_UNORM (read from its
		// bytecode), so ACPipelineState#blend is unaffected; the two call sites that spell the long
		// constructor — because they need a non-default write mask — have to name it. A rule per
		// site rather than one on the type name, so the inserted argument lands in the right place.
		string("!mc262-colortarget-blend-no-alpha", true) {
			replace("ColorTargetState(java.util.Optional.of(blend), com.mojang.blaze3d.pipeline.ColorTargetState.WRITE_COLOR)", "ColorTargetState(java.util.Optional.of(blend), com.mojang.blaze3d.GpuFormat.RGBA8_UNORM, com.mojang.blaze3d.pipeline.ColorTargetState.WRITE_COLOR)")
		}
		string("!mc262-colortarget-no-color", true) {
			replace("ColorTargetState(java.util.Optional.empty(), com.mojang.blaze3d.pipeline.ColorTargetState.WRITE_NONE)", "ColorTargetState(java.util.Optional.empty(), com.mojang.blaze3d.GpuFormat.RGBA8_UNORM, com.mojang.blaze3d.pipeline.ColorTargetState.WRITE_NONE)")
		}

		// ── StructureProcessor became an interface ──
		//
		// 26.2 turned the abstract class into an interface whose only abstract member is
		// `codec()` — the processor names its own MapCodec instead of a StructureProcessorType that
		// wraps it, and Registries.STRUCTURE_PROCESSOR is a Registry<MapCodec<? extends
		// StructureProcessor>> now (javap'd: the whole StructureProcessorType type survives only as
		// a holder for four static Codec constants and takes no type parameter at all).
		//
		// The `extends` -> `implements` half is a rule; the getType()/codec() half is a gate in each
		// of the four processors, because the bodies differ.
		string("!mc262-structureprocessor-iface", true) {
			replace("extends StructureProcessor {", "implements StructureProcessor {")
		}

		// processBlock's fourth parameter went from the relative StructureBlockInfo to just its
		// position — the only part of it anyone read. Confirmed at the call site rather than
		// guessed: StructureTemplate.processBlockInfos passes `aload 11; getfield
		// StructureBlockInfo.pos`, i.e. exactly the old relativeInfo.pos(). So the two rules below
		// are a faithful translation and neither processor loses information.
		string("!mc262-processblock-relative", true) {
			replace(
				"StructureTemplate.StructureBlockInfo relativeInfo, StructureTemplate.StructureBlockInfo info",
				"BlockPos relativePos, StructureTemplate.StructureBlockInfo info"
			)
		}
		string("!mc262-processblock-relativepos", true) {
			replace("relativeInfo.pos()", "relativePos")
		}

		// 26.2 gave the slime family a package of its own. CaramelCubeEntity extends it, so the
		// only occurrence is the import.
		string("!mc262-slime-package", true) {
			replace("net.minecraft.world.entity.monster.Slime", "net.minecraft.world.entity.monster.cubemob.Slime")
		}

		// The source and destination blend-factor enums merged into one BlendFactor. Only the
		// argument list is a rule — see the gated import in ACInternalShaders for why the two
		// spellings cannot each be renamed on their own.
		string("!mc262-blendfactor", true) {
			replace(
				"SourceFactor.SRC_ALPHA, DestFactor.ONE, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA",
				"BlendFactor.SRC_ALPHA, BlendFactor.ONE, BlendFactor.ONE, BlendFactor.ONE_MINUS_SRC_ALPHA"
			)
		}

		// ── the entity-outline batch became a prepared-frame stage ──
		//
		// 26.2 deleted OutlineBufferSource: the outline draws are one of the FeatureRenderDispatcher
		// frame's execute* stages now. Read out of lambda$addMainPass$0 rather than inferred —
		// executeOutline() sits in exactly the slot endOutlineBatch() used to, between
		// executeTranslucent() and the tripwire renderGroup — so Citadel's post-effect pass still
		// runs at the same point in the frame.
		//
		// Three rules rather than one gate because the @Redirect this belongs to already carries a
		// six-arm `method` chain and Stonecutter cannot nest a second condition inside it.
		string("!mc262-outline-at", true) {
			replace(
				"Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V",
				"Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher\$PreparedFrame;executeOutline()V"
			)
		}
		string("!mc262-outline-param", true) {
			replace(
				"net.minecraft.client.renderer.OutlineBufferSource outlineBufferSource",
				"net.minecraft.client.renderer.feature.FeatureRenderDispatcher.PreparedFrame outlineBufferSource"
			)
		}
		string("!mc262-outline-call", true) {
			replace("outlineBufferSource.endOutlineBatch();", "outlineBufferSource.executeOutline();")
		}

		// ── the three GameRenderer getters lost their get- ────────────────────────────────────
		// 26.2 de-`get`-ed GameRenderer the way 1.21.11 did Camera: mainCamera(), mainRenderTarget()
		// and lighting() are all plain accessors now, over fields of the same names. Three rules
		// rather than one because the mainRenderTarget one is not a rename — see below.
		string("!mc262-maincamera", true) {
			replace(".getMainCamera()", ".mainCamera()")
		}
		string("!mc262-lighting", true) {
			replace("gameRenderer.getLighting()", "gameRenderer.lighting()")
		}

		// Minecraft#getMainRenderTarget() did not move house, it was DELETED: the target belongs to
		// the GameRenderer and only it hands one out. So the replacement grows a hop rather than
		// dropping a prefix, and it is keyed on the bare `.getMainRenderTarget()` precisely so it
		// serves all three receiver spellings in this tree at once — `Minecraft.getInstance()`,
		// `this.minecraft` and a bare `minecraft` local — each of which already has a public
		// `gameRenderer` field in front of the new call.
		//
		// The four `bindWrite` sites next to it need nothing: RenderTarget lost that method too, but
		// every one of them lives in a `<1.21.5` arm and is commented out here.
		string("!mc262-mainrendertarget", true) {
			replace(".getMainRenderTarget()", ".gameRenderer.mainRenderTarget()")
		}

		// ── Minecraft#screen moved onto the Gui ───────────────────────────────────────────────
		// The open screen is the Gui's business on 26.2 — `Minecraft.screen` is gone and
		// `Gui#screen()` replaces it, over the Gui's own private field. One site (the advancements
		// tab hook); keyed through `instanceof` so a bare `.screen` elsewhere can never match.
		string("!mc262-minecraft-screen", true) {
			replace("Minecraft.getInstance().screen instanceof", "Minecraft.getInstance().gui.screen() instanceof")
		}

		// ── Sheets' block sheet is an ITEM sheet now ──────────────────────────────────────────
		// 26.2 pared Sheets down to the five render types the item renderer actually asks for, and
		// renamed them to say so: cutoutBlockSheet() is cutoutBlockItemSheet(). translucentItemSheet()
		// beside it in the same method is unchanged, which is why only the one call is rewritten.
		string("!mc262-cutoutblocksheet", true) {
			replace("Sheets.cutoutBlockSheet()", "Sheets.cutoutBlockItemSheet()")
		}

		// ── the packed-light statics left LevelRenderer ───────────────────────────────────────
		// Both getLightCoords overloads and the BrightnessGetter interface moved to
		// net.minecraft.util.LightCoordsUtil (mixin/client/BlockLightCoordsMixin gates its @Mixin
		// target on exactly that). One ordinary call site is left, in SubmarineRenderer.
		//
		// ⚠️ This rule deliberately OVERLAPS `!mc261-getlightcoords`, which rewrites the bare token
		// `getLightColor` everywhere. Rules do not chain — every rule matches the ORIGINAL file text
		// — so this one has to spell the pre-26 name, and where two matches overlap the
		// earlier-STARTING rule consumes the span. `LevelRenderer.getLightColor` starts at the owner
		// and the bare token starts eight characters later, so this one wins and the bare rule never
		// sees the tail. Nothing else in src/ spells the owner in front of the name.
		string("!mc262-lightcoordsutil", true) {
			replace("LevelRenderer.getLightColor", "net.minecraft.util.LightCoordsUtil.getLightCoords")
		}

		// ── a render setup no longer declares a buffer size ───────────────────────────────────
		// 26.2 dropped RenderSetupBuilder#bufferSize (and RenderType#bufferSize() with it): nothing
		// accumulates into a per-type immediate buffer any more, so the number has nowhere to go.
		// ACRenderSetup keeps taking one, because it re-implements the 1.21.5-era CompositeState
		// builder whose ~90 call sites all pass one — it is simply not forwarded from here.
		//
		// A rule rather than an arm because the call sits inside ACRenderSetup's own >=1.21.11 arm,
		// which Stonecutter cannot nest a second condition into. The whole receiver chain is carried
		// so the rule cannot touch the `int bufferSize` parameters two lines above it.
		string("!mc262-buffersize", true) {
			replace("RenderSetup.builder(pipeline).bufferSize(bufferSize)", "RenderSetup.builder(pipeline)")
		}

		// ── the mob-effect sprite lookup is the Hud's ─────────────────────────────────────────
		// 26.2 split the in-game overlay out of Gui into net.minecraft.client.gui.Hud and took the
		// static getMobEffectSprite(Holder<MobEffect>) with it. Gui itself still exists (Minecraft#gui
		// is still typed as it, which !mc262-minecraft-screen relies on) — it simply no longer
		// declares this one. The name is unique enough to key on with its owner attached, and the
		// two matches sit in the two <1.21.9 / else arms of one method in CitadelItemstackRenderer:
		// rewriting both is correct, since only ever one of them is live.
		string("!mc262-mobeffectsprite", true) {
			replace("net.minecraft.client.gui.Gui.getMobEffectSprite",
					"net.minecraft.client.gui.Hud.getMobEffectSprite")
		}

		// ── ParticleRenderType gained a shorthand ─────────────────────────────────────────────
		// 26.2 made it a two-component record (name, shorthand) — vanilla's four read SINGLE_QUADS/SQ,
		// ITEM_PICKUP/IP, ELDER_GUARDIANS/EG, NO_RENDER/NR — so the one-argument constructor is gone.
		// The value is only ever shown in debug output; identity is what the engine's TreeMap orders
		// on (see ACParticleBuffers' javadoc), so the shorthand is free-form.
		string("!mc262-particlerendertype", true) {
			replace("ParticleRenderType(\"alexscaves:custom\")", "ParticleRenderType(\"alexscaves:custom\", \"ACC\")")
		}

		// ── ParticleGroup#getAll() is gone ────────────────────────────────────────────────────
		// …but the `protected final Queue<P> particles` it wrapped is not, so a subclass iterating
		// its own live particles reads the field directly. The receiver is carried in the rule
		// because the tree's only other getAll() is ACPlayerCapes' static one, which is never
		// spelled with a `this.` in front of it.
		string("!mc262-particlegroup-getall", true) {
			replace("this.getAll()", "this.particles")
		}
	}

	// ── 26.1, NeoForge only ───────────────────────────────────────────────────────────────────
	// Both loaders converged on `register(List<BlockTintSource>, Block...)` for block colours, so
	// ClientProxy#onBlockColors has ONE >=26 arm — but they disagree on which nested event class
	// carries it. Forge 62.0.9 kept RegisterColorHandlersEvent.Block; NeoForge 26.1 deleted that
	// class and put the identical method on RegisterColorHandlersEvent.BlockTintSources (beside
	// its new ItemTintSources / ColorResolvers siblings). One token, inside an arm that already
	// carries a version condition Stonecutter cannot nest a loader condition into — so a rule.
	if (current.project.endsWith("-neoforge") && eval(current.version, ">=26")) replacements {
		string("!mc261-colorhandlers-block-nf", true) {
			replace("RegisterColorHandlersEvent.Block", "RegisterColorHandlersEvent.BlockTintSources")
		}

		// 26.1 introduced net.minecraft.world.item.ItemInstance, the read-only view of "an item with
		// components" that ItemStack now implements, and NeoForge widened IItemExtension#canPerformAction
		// from ItemStack to it. Only the OVERRIDE's parameter has to move — widening is safe for the body
		// (super.canPerformAction takes the same interface) and for every caller, since an ItemStack is
		// still an ItemInstance. Forge 62.0.9 kept ItemStack, so this cannot be a shared >=26 spelling.
		//
		// CandyCaneHookItem carries the tree's only override; the six other canPerformAction mentions are
		// one-argument ItemStack#canPerformAction(ability) CALL sites, which this rule cannot reach because
		// it carries the `boolean ` return type and the `ItemStack stack,` parameter with it.
		string("!mc261-canperformaction-nf", true) {
			replace("boolean canPerformAction(ItemStack stack,",
					"boolean canPerformAction(net.minecraft.world.item.ItemInstance stack,")
		}
	}

	// ── 26.1, Forge only ──────────────────────────────────────────────────────────────────────
	// 26.1 merged Entity#interact and #interactAt into one Entity#interact(Player, InteractionHand,
	// Vec3), so Player#interactOn has a single post site left. Forge 62.0.9 followed it and DELETED
	// PlayerInteractEvent$EntityInteract, keeping only EntityInteractSpecific — which now fires for
	// every entity interaction, not just the aimed-at-a-body-part kind. NeoForge 26.1.0.19-beta
	// kept both names, so this cannot be a shared >=26 spelling.
	//
	// Everything CommonEvents#acPlayerEntityInteract reads survives the swap: getTarget() is
	// declared on EntityInteractSpecific itself and getItemStack()/getHand()/getEntity()/
	// setCancellationResult() come from the shared abstract parent.
	//
	// A rule rather than an arm because the two @SubscribeEvent methods are already split by the
	// forge && >=1.21.6 boolean-return gate, which Stonecutter cannot nest a version condition
	// into. Safe as a substring: the mod's source never spells EntityInteractSpecific, so the
	// reverse direction (which this Kotlin-if-guarded group does not get anyway) has nothing to
	// match, and `onEntityInteract` cannot be hit because the rule carries the outer class name.
	if (current.project.endsWith("-forge") && eval(current.version, ">=26")) replacements {
		string("!mc261-entityinteract-forge", true) {
			replace("PlayerInteractEvent.EntityInteract", "PlayerInteractEvent.EntityInteractSpecific")
		}

		// Forge 62 renamed the sapling event for what it has actually described since 1.19: a block
		// growing into a configured feature, saplings being only one caller. Same package, same
		// constructor, same getFeature()/setFeature()/getResult() — the ForgeEventFactory hook keeps
		// its blockGrowFeature name too — so only the type moves. The tri-state it returns was
		// already re-pointed at common.util.Result by !fg216-event-result.
		string("!mc261-saplinggrow-forge", true) {
			replace("net.minecraftforge.event.level.SaplingGrowTreeEvent",
					"net.minecraftforge.event.level.BlockFeatureGrowEvent")
		}

		// Forge 62 finally dropped the two deprecated tag aliases it had been carrying since 52.x —
		// IS_WATER (= IS_AQUATIC) and SHEARS (= TOOLS_SHEAR) — so Forge arrives at the spellings
		// NeoForge moved to in 1.20.5 and 1.21. These are the same two renames the NeoForge groups
		// above apply, repeated here because a node is either Forge or NeoForge and the two groups
		// are therefore never registered together; no rule can be shared between them. Every other
		// Tags.Biomes constant this mod names (IS_DESERT, IS_MOUNTAIN, IS_MUSHROOM, IS_PLAINS,
		// IS_RARE, IS_SNOWY, IS_SPOOKY, IS_SWAMP) is present unchanged in forge-universal 62.0.9,
		// and IS_CONIFEROUS is already handled by the shared !tag-coniferous rule.
		string("!mc26-tag-aquatic-forge", true) { replace("Tags.Biomes.IS_WATER", "Tags.Biomes.IS_AQUATIC") }
		string("!mc26-tag-shears-forge", true) { replace("Tags.Items.SHEARS", "Tags.Items.TOOLS_SHEAR") }

		// …and the same for IForgeEntity#isAddedToWorld, renamed isAddedToLevel to finish the
		// world -> level sweep NeoForge did in 1.21. Two call sites (CommonEvents, Citadel's
		// LocalEntityTickRateModifier); the leading dot keeps it off any declaration.
		string("!mc26-addedtolevel-forge", true) { replace(".isAddedToWorld()", ".isAddedToLevel()") }
	}

	// Forge 65.1.0 stopped supporting `@OnlyIn` in MOD code, and it is a hard failure rather than a
	// deprecation warning. `RuntimeDistCleaner.processClassWithFlags` throws
	// `UnsupportedOperationException("… is annotated with @OnlyIn, this is no longer supported as it
	// slowed down startup times")` for any non-Minecraft class carrying one, and 61.1.0 (1.21.11)
	// already had the same three throws behind a `!production && "21.6".equals(mcVersion)` guard
	// that could never fire — a later build simply deleted the guard. ⚠ That build is **62.0.9, the
	// first 26.1 build**, not 65.1.0 as this note first claimed: all three 26.1.x Forge nodes throw
	// exactly the same 13 times, while 1.21.11-forge (61.1.0) boots clean. So this is a Forge-BUILD
	// change that no earlier node can warn you about, and it bites in two different ways:
	//
	//   • a CLASS-level `@OnlyIn` throws unconditionally, on either dist — so the ~60 client-only
	//     particle factories and the vendored Citadel model classes kill a dev CLIENT;
	//   • a METHOD- or FIELD-level one throws only when the annotation's side is the wrong one, i.e.
	//     `Dist.CLIENT` on a dedicated server — so the nine methods on classes the server does load
	//     (seven `getRenderBoundingBox`, `SodaBottleRocketEntity#handleEntityEvent`/`#getItem`) kill
	//     a dev SERVER, which is where this was found: the first one aborted `RegisterEvent`
	//     dispatch and the log then filled with `Registry Object not present` cascades.
	//
	// Neutralising the annotation is behaviour-neutral. All 71 sites are `Dist.CLIENT`, the nine
	// server-loaded bodies are dist-neutral (AABB/BlockPos/ItemStack arithmetic and `Level#addParticle`,
	// which exists on both sides), and nothing else on the server references the client classes — the
	// stripping was an optimisation and a guard rail, never load-bearing. Fabric is untouched: its
	// own `!fab-onlyin-class` rule turns the same token into `@Environment(EnvType.CLIENT)`, and the
	// two groups are never registered together.
	//
	// ⚠ NeoForge needs the same neutralisation, for a *different* symptom, and this note asserted the
	// opposite until a 1.21.11-neoforge dev CLIENT was actually booted. NeoForge does not throw — it
	// reports the finding through `net.neoforged.neoforge.common.OnlyInWarningsHandler` — but that
	// handler raises a **blocking modal**: the client stops on "Warning while loading mods / 1 warning
	// has occurred during loading", naming this mod, and waits for a click on "Proceed to main menu"
	// before it will reach the title screen. Every launch, for every player. The boundary is a
	// NeoForge-BUILD one exactly like Forge's: `OnlyInWarningsHandler` is absent from every cached
	// universal jar through **21.6.20-beta (1.21.6)** and present from **21.7.25-beta (1.21.7)** up,
	// so nine nodes are affected (1.21.7 → 26.2). Stripping is already gone on those builds, so
	// neutralising costs nothing there either — same argument as above, same 71 dist-neutral bodies.
	//
	// The two imports are left dangling on purpose: an unused import is legal Java, and
	// `net.minecraftforge.api.distmarker.OnlyIn` still exists on 65.1.0 (the cleaner reads its own
	// descriptor), so no second rule is needed. The replacement is a comment rather than an empty
	// string so the annotation's intent survives in the generated tree, and it deliberately does not
	// contain the source token — a rule whose target embeds its source is not safely idempotent.
	if (current.project.endsWith("-forge") && eval(current.version, ">=26.1")) replacements {
		string("!mc261-onlyin-forge", true) {
			replace("@OnlyIn(Dist.CLIENT)", "/* client-only */")
		}
	}

	// The NeoForge half of the note above. A separate group with its own rule name, because a node is
	// either Forge or NeoForge and the two are therefore never registered together — sharing one rule
	// name across two groups would be fine, but sharing a *target* inside one group is what fails
	// configuration with "Ambiguous replacement", and keeping them apart makes the two boundaries
	// (Forge >=26.1, NeoForge >=1.21.7) independently editable.
	if (current.project.endsWith("-neoforge") && eval(current.version, ">=1.21.7")) replacements {
		string("!mc2117-onlyin-neoforge", true) {
			replace("@OnlyIn(Dist.CLIENT)", "/* client-only */")
		}
	}
}
