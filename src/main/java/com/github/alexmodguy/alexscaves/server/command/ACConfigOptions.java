package com.github.alexmodguy.alexscaves.server.command;

import codx.codxlib.api.ui.menu.CodxMenuButton;
import codx.codxlib.api.ui.menu.CodxMenuLayout;
import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.config.ACServerConfig;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The single description of every {@link ACServerConfig} option that {@code /acc} exposes -- one
 * entry per option, carrying the page it belongs on, its label, its icon, its lore, how to read and
 * write it, and how to print it.
 *
 * <p>It exists so the chest menu ({@link ACAdminMenu}) and the plain-text listing
 * ({@code /acc config}) are two renderings of ONE table rather than two hand-maintained copies of
 * 39 entries that can drift apart. A new option is one line here and appears in both.</p>
 *
 * <h2>Why doubles are surfaced as scaled ints</h2>
 * The CodxLib pinned by this tree (1.3.6, {@code deps.codxlib}) ships {@code adjustInt} and
 * {@code slider} and no double-valued widget at all, so the seven-plus fractional options are
 * presented in whole display units and divided back down on write: a 0..1 chance becomes 0..100
 * with a {@code %} suffix, and a multiplier becomes tenths with a {@code x} suffix. The rounding
 * is exact for every default in {@link ACServerConfig} (0.45, 0.15, 1.75, 3.0 ... all land on a
 * whole scaled unit), so opening the menu and closing it again cannot perturb a value.
 *
 * <h2>Why this file is authored in the Forge spelling</h2>
 * {@code ForgeConfigSpec} is rewritten to NeoForge's {@code ModConfigSpec} by the
 * {@code !nf-cls-configspec} replacement rule and to this tree's own stand-in package by
 * {@code !fab-forgeconfigspec}. Both rules only ever rewrite the old spelling upwards, so the
 * source must be written in it -- exactly as {@link ACServerConfig} itself is.
 *
 * <h2>Persistence</h2>
 * {@code AlexsCaves.COMMON_CONFIG_SPEC} is private, so nothing outside {@link AlexsCaves} can ask
 * the spec to save. {@link #save()} therefore goes through {@code ConfigValue#save()} on one
 * option, which writes the whole file on every loader -- verified by bytecode sweep across every
 * cached Forge build from 47.4.21 to 65.1.0 and every NeoForge build from 20.4.251 to
 * 26.2.0.37-beta, and implemented to match in this tree's Fabric stand-in.
 */
public final class ACConfigOptions {

    private ACConfigOptions() {
    }

    /** A page of the admin menu. Page 0 is the read-only status page and holds no options. */
    public static final class Page {
        public final String title;
        public final Item icon;
        public final String blurb;

        private Page(String title, Item icon, String blurb) {
            this.title = title;
            this.icon = icon;
            this.blurb = blurb;
        }
    }

    public static final Page[] PAGES = {
            new Page("Status", Items.COMMAND_BLOCK, "This server's Alex's Caves at a glance"),
            new Page("World Generation", Items.GRASS_BLOCK, "Size, spacing and shape of the cave biomes"),
            new Page("Mobs", Items.SPAWNER, "How the cave mobs spawn and how dangerous they are"),
            new Page("Blocks", Items.BRICKS, "Behaviour of the mod's interactive blocks"),
            new Page("Items & Potions", Items.CRAFTING_TABLE, "Tools, nukes, the cloak and the sugar rush"),
            new Page("Cave Tablet Loot", Items.BOOKSHELF, "How often each biome's tablet turns up in loot"),
            new Page("Vanilla Changes", Items.BELL, "What the mod adds to vanilla content"),
    };

    /** Base of the three option shapes. Every option knows its own page, label, icon and lore. */
    public abstract static class Option {
        public final int page;
        /** The key as it is spelled in {@code alexscaves-general.toml}, for the text listing. */
        public final String key;
        public final String label;
        public final Item icon;
        public final String[] lore;

        Option(int page, String key, String label, Item icon, String... lore) {
            this.page = page;
            this.key = key;
            this.label = label;
            this.icon = icon;
            this.lore = lore;
        }

        /** The current value, formatted the same way the menu widget formats it. */
        public abstract String display();

        /** The value this option was declared with, formatted the same way. */
        public abstract String displayDefault();

        /** True when the current value differs from the declared default. */
        public abstract boolean isModified();

        /** Puts this option back to its declared default. Does not save. */
        public abstract void reset();

        /** Places this option's widget in a menu page. */
        public abstract void layout(CodxMenuLayout layout, int slot);

        /** Writes the whole config file. Called once per change, not once per option. */
        public abstract void save();

        /** The lore the widget shows: the authored lines plus the default, so a reset is legible. */
        String[] menuLore() {
            String[] out = new String[lore.length + 1];
            System.arraycopy(lore, 0, out, 0, lore.length);
            out[lore.length] = "§8Default: " + displayDefault();
            return out;
        }
    }

    /** A plain on/off option. */
    public static final class BoolOption extends Option {
        private final ForgeConfigSpec.BooleanValue value;

        BoolOption(int page, String key, String label, Item icon, ForgeConfigSpec.BooleanValue value, String... lore) {
            super(page, key, label, icon, lore);
            this.value = value;
        }

        public boolean get() {
            return value.get();
        }

        @Override
        public String display() {
            return value.get() ? "§aon" : "§coff";
        }

        @Override
        public String displayDefault() {
            return value.getDefault() ? "on" : "off";
        }

        @Override
        public boolean isModified() {
            return value.get().booleanValue() != value.getDefault().booleanValue();
        }

        @Override
        public void reset() {
            value.set(value.getDefault());
        }

        @Override
        public void layout(CodxMenuLayout layout, int slot) {
            layout.toggle(slot, icon, label, value::get, value::set, menuLore());
        }

        @Override
        public void save() {
            value.save();
        }
    }

    /** A whole-number option, adjusted in its own units. */
    public static final class IntOption extends Option {
        private final ForgeConfigSpec.IntValue value;
        private final CodxMenuButton.Step step;
        private final Format format;

        IntOption(int page, String key, String label, Item icon, ForgeConfigSpec.IntValue value,
                  int step, int shiftStep, int min, int max, Format format, String... lore) {
            super(page, key, label, icon, lore);
            this.value = value;
            this.format = format;
            this.step = CodxMenuButton.Step.of(step, shiftStep).range(min, max).format(format::apply);
        }

        @Override
        public String display() {
            return format.apply(value.get());
        }

        @Override
        public String displayDefault() {
            return format.apply(value.getDefault());
        }

        @Override
        public boolean isModified() {
            return value.get().intValue() != value.getDefault().intValue();
        }

        @Override
        public void reset() {
            value.set(value.getDefault());
        }

        @Override
        public void layout(CodxMenuLayout layout, int slot) {
            layout.adjustInt(slot, icon, label, value::get, value::set, step, menuLore());
        }

        @Override
        public void save() {
            value.save();
        }
    }

    /**
     * A fractional option shown in whole display units. {@code scale} is how many display units
     * make up 1.0 -- 100 for a percentage, 10 for a multiplier shown to one decimal.
     */
    public static final class DoubleOption extends Option {
        private final ForgeConfigSpec.DoubleValue value;
        private final int scale;
        private final CodxMenuButton.Step step;
        private final Format format;
        private final boolean slider;

        DoubleOption(int page, String key, String label, Item icon, ForgeConfigSpec.DoubleValue value,
                     int scale, int step, int shiftStep, int min, int max, boolean slider,
                     Format format, String... lore) {
            super(page, key, label, icon, lore);
            this.value = value;
            this.scale = scale;
            this.format = format;
            this.slider = slider;
            this.step = CodxMenuButton.Step.of(step, shiftStep).range(min, max).format(format::apply);
        }

        private int scaled() {
            return (int) Math.round(value.get() * scale);
        }

        private void setScaled(int scaledValue) {
            value.set(scaledValue / (double) scale);
        }

        @Override
        public String display() {
            return format.apply(scaled());
        }

        @Override
        public String displayDefault() {
            return format.apply((int) Math.round(value.getDefault() * scale));
        }

        @Override
        public boolean isModified() {
            return scaled() != (int) Math.round(value.getDefault() * scale);
        }

        @Override
        public void reset() {
            value.set(value.getDefault());
        }

        @Override
        public void layout(CodxMenuLayout layout, int slot) {
            if (slider) {
                layout.slider(slot, icon, label, this::scaled, this::setScaled, step, menuLore());
            } else {
                layout.adjustInt(slot, icon, label, this::scaled, this::setScaled, step, menuLore());
            }
        }

        @Override
        public void save() {
            value.save();
        }
    }

    /**
     * How a numeric value is spelled. Its own interface rather than {@code IntFunction<String>} so
     * the same instance can serve both the widget's {@code Step#format} and the text listing.
     */
    public interface Format {
        String apply(int value);
    }

    private static final Format PLAIN = v -> Integer.toString(v);
    private static final Format PERCENT = v -> v + "%";
    /** Tenths, e.g. 18 -> "1.8x". */
    private static final Format TENTHS = v -> String.format(Locale.ROOT, "%.1fx", v / 10.0D);
    /** Game ticks, with the wall-clock equivalent, e.g. 300 -> "300t (15.0s)". */
    private static final Format TICKS = v -> v + "t (" + String.format(Locale.ROOT, "%.1fs", v / 20.0D) + ")";
    /** Blocks. */
    private static final Format BLOCKS = v -> v + " blocks";

    private static final List<Option> ALL = new ArrayList<>();

    private static <T extends Option> T add(T option) {
        ALL.add(option);
        return option;
    }

    static {
        final ACServerConfig c = AlexsCaves.COMMON_CONFIG;

        // --- Page 1: World Generation -------------------------------------------------------
        add(new DoubleOption(1, "cave_biome_mean_width", "Cave Biome Size", Items.STONE,
                c.caveBiomeMeanWidth, 1, 10, 50, 10, 4000, false, BLOCKS,
                "§7Average radius of an Alex's Caves cave biome.",
                "§7Larger biomes are easier to find but rarer per world."));
        add(new IntOption(1, "cave_biome_mean_separation", "Cave Biome Separation", Items.COMPASS,
                c.caveBiomeMeanSeparation, 25, 100, 50, 8000, BLOCKS,
                "§7Average distance between two cave biomes.",
                "§7Lower means the cave biomes turn up more often."));
        add(new DoubleOption(1, "cave_biome_width_randomness", "Shape Randomness", Items.CLAY_BALL,
                c.caveBiomeWidthRandomness, 100, 5, 25, 0, 100, true, PERCENT,
                "§70% = every biome is nearly circular.",
                "§7100% = biomes are completely squiggly."));
        add(new DoubleOption(1, "cave_biome_spacing_randomness", "Spacing Randomness", Items.STRING,
                c.caveBiomeSpacingRandomness, 100, 5, 25, 0, 100, true, PERCENT,
                "§70% = biomes sit on an even grid.",
                "§7100% = biomes scatter, sometimes side by side."));
        add(new BoolOption(1, "warn_generation_incompatibility", "Warn On Incompatible Worldgen", Items.PAPER,
                c.warnGenerationIncompatibility,
                "§7Log a warning at startup when another",
                "§7terrain mod is detected that may hide the caves."));

        // --- Page 2: Mobs -------------------------------------------------------------------
        add(new DoubleOption(2, "cave_creature_spawn_count_modifier", "Cave Creature Spawn Cap", Items.BONE,
                c.caveCreatureSpawnCountModifier, 10, 1, 5, 0, 100, false, TENTHS,
                "§7Multiplier on the vanilla animal mob cap for",
                "§7dinosaurs, raycats and the rest. 0.0x disables them."));
        add(new DoubleOption(2, "drowned_diving_gear_spawn_chance", "Diving Gear Drowned", Items.TRIDENT,
                c.drownedDivingGearSpawnChance, 100, 5, 25, 0, 100, true, PERCENT,
                "§7Chance a drowned in the Abyssal Chasm",
                "§7spawns wearing diving gear."));
        add(new IntOption(2, "pathfinding_threads", "Pathfinding Threads", Items.REDSTONE,
                c.pathfindingThreads, 1, 5, 1, 100, PLAIN,
                "§7CPU threads the large mobs use to path.",
                "§7Higher costs more CPU but less TPS impact."));
        add(new DoubleOption(2, "luxtructosaurus_block_drop_chance", "Luxtructosaurus Drops", Items.DIAMOND_PICKAXE,
                c.luxtructosaurusBlockDropChance, 100, 5, 25, 0, 100, true, PERCENT,
                "§7Chance blocks it smashes drop themselves,",
                "§7if mob griefing is on."));
        add(new IntOption(2, "atlatitan_max_block_explosion_resistance", "Atlatitan Stomp Power", Items.OBSIDIAN,
                c.atlatitanMaxExplosionResistance, 1, 10, 0, 1000, PLAIN,
                "§7Highest blast resistance an atlatitan stomp breaks.",
                "§7Set to 0 to stop it breaking blocks entirely."));
        add(new IntOption(2, "nucleeper_fuse_time", "Nucleeper Fuse", Items.TNT,
                c.nucleeperFuseTime, 20, 100, 20, 6000, TICKS,
                "§7How long a nucleeper takes to detonate."));
        add(new BoolOption(2, "devastating_tremorzilla_beam", "Devastating Tremorzilla Beam", Items.FIRE_CHARGE,
                c.devastatingTremorzillaBeam,
                "§7Off makes the beam far gentler on terrain."));
        add(new BoolOption(2, "watcher_possession", "Watcher Possession", Items.ENDER_EYE,
                c.watcherPossession,
                "§7Lets the Watcher take over a player's camera.",
                "§7Turn off if it upsets your players."));
        add(new IntOption(2, "watcher_possession_cooldown", "Watcher Possession Cooldown", Items.CLOCK,
                c.watcherPossessionCooldown, 20, 200, 20, 24000, TICKS,
                "§7Wait between two possession attempts."));

        // --- Page 3: Blocks -----------------------------------------------------------------
        add(new BoolOption(3, "walking_on_magnets", "Walk On Magnets", Items.IRON_BOOTS,
                c.walkingOnMagnets,
                "§7Players in boots can walk on any",
                "§7scarlet neodymium surface."));
        add(new IntOption(3, "amber_monolith_mean_time", "Amber Monolith Interval", Items.HONEY_BLOCK,
                c.amberMonolithMeanTime, 1000, 5000, 1000, 240000, TICKS,
                "§7Average wait before an amber monolith",
                "§7spawns another animal."));
        add(new BoolOption(3, "nuclear_furnace_blasting_only", "Nuclear Furnace: Blasting Only", Items.BLAST_FURNACE,
                c.nuclearFurnaceBlastingOnly,
                "§7On: blasting recipes only.",
                "§7Off: every smelting recipe works."));
        add(new BoolOption(3, "nuclear_furnace_custom_type", "Nuclear Furnace: Custom Type", Items.FURNACE,
                c.nuclearFurnaceCustomType,
                "§7On: only alexscaves:nuclear_furnace recipes.",
                "§7For pack authors who want their own list."));

        // --- Page 4: Items & Potions --------------------------------------------------------
        add(new BoolOption(4, "only_one_research_needed", "One Codex Unlocks All", Items.BOOK,
                c.onlyOneResearchNeeded,
                "§7On: one Cave Codex unlocks every",
                "§7Cave Compendium entry at once."));
        add(new IntOption(4, "cave_map_search_attempts", "Cave Map Search Attempts", Items.FILLED_MAP,
                c.caveMapSearchAttempts, 1000, 16000, 64, 512000, PLAIN,
                "§7Higher searches further for a biome.",
                "§7Lower answers faster."));
        add(new IntOption(4, "cave_map_search_width", "Cave Map Search Step", Items.MAP,
                c.caveMapSearchWidth, 4, 16, 4, 256, BLOCKS,
                "§7Width of each search sample. Higher is faster",
                "§7but can step over a small biome."));
        add(new IntOption(4, "nuke_max_block_explosion_resistance", "Nuke Power", Items.NETHERITE_SCRAP,
                c.nukeMaxBlockExplosionResistance, 50, 500, 0, 100000, PLAIN,
                "§7Highest blast resistance a nuke breaks.",
                "§7Set to 0 to stop nukes breaking blocks."));
        add(new BoolOption(4, "nuke_spawn_item_drops", "Nukes Drop Items", Items.HOPPER,
                c.nukesSpawnItemDrops,
                "§7Off makes nuclear explosions drop nothing."));
        add(new DoubleOption(4, "nuclear_explosion_size_modifier", "Nuke Blast Size", Items.TNT,
                c.nukeExplosionSizeModifier, 10, 1, 10, 0, 200, false, TENTHS,
                "§7Multiply by 16 for the blast radius in blocks.",
                "§7Default 3.0x is roughly 48 blocks."));
        add(new BoolOption(4, "totem_of_possession_works_on_players", "Possess Players", Items.TOTEM_OF_UNDYING,
                c.totemOfPossessionPlayers,
                "§7Whether the Totem of Possession works",
                "§7on other players, not just mobs."));
        add(new IntOption(4, "darkness_cloak_charge_time", "Cloak Charge Time", Items.ECHO_SHARD,
                c.darknessCloakChargeTime, 20, 200, 20, 24000, TICKS,
                "§7How long the Cloak of Darkness takes",
                "§7to charge its ability."));
        add(new IntOption(4, "darkness_cloak_fly_time", "Cloak Flight Time", Items.FEATHER,
                c.darknessCloakFlightTime, 20, 100, 20, 24000, TICKS,
                "§7How long a player can fly with the",
                "§7Cloak of Darkness."));
        add(new BoolOption(4, "sugar_rush_slows_time", "Sugar Rush Slows Time", Items.SUGAR,
                c.sugarRushSlowsTime,
                "§7On: Sugar Rush changes the tick rate",
                "§7around affected players. Off is cheaper on TPS."));

        // --- Page 5: Cave Tablet Loot -------------------------------------------------------
        add(new DoubleOption(5, "magnetic_tablet_loot_chance", "Magnetic Tablet", Items.IRON_INGOT,
                c.magneticTabletLootChance, 100, 5, 25, 0, 100, true, PERCENT,
                "§7Chance a bastion chest holds the",
                "§7Magnetic Caves tablet."));
        add(new DoubleOption(5, "primordial_tablet_loot_chance", "Primordial Tablet", Items.BONE_BLOCK,
                c.primordialTabletLootChance, 100, 5, 25, 0, 100, true, PERCENT,
                "§7Chance suspicious sand holds the",
                "§7Primordial Caves tablet."));
        add(new DoubleOption(5, "toxic_tablet_loot_chance", "Toxic Tablet", Items.SLIME_BALL,
                c.toxicTabletLootChance, 100, 5, 25, 0, 100, true, PERCENT,
                "§7Chance a jungle temple holds the",
                "§7Toxic Caves tablet."));
        add(new DoubleOption(5, "abyssal_tablet_loot_chance", "Abyssal Tablet", Items.PRISMARINE_SHARD,
                c.abyssalTabletLootChance, 100, 5, 25, 0, 100, true, PERCENT,
                "§7Chance underwater ruins hold the",
                "§7Abyssal Chasm tablet."));
        add(new DoubleOption(5, "forlorn_tablet_loot_chance", "Forlorn Tablet", Items.SKELETON_SKULL,
                c.forlornTabletLootChance, 100, 5, 25, 0, 100, true, PERCENT,
                "§7Chance a woodland mansion holds the",
                "§7Forlorn Hollows tablet."));
        add(new DoubleOption(5, "candy_cavity_loot_chance", "Candy Tablet", Items.CAKE,
                c.candyTabletLootChance, 100, 5, 25, 0, 100, true, PERCENT,
                "§7Chance a witch hut chest holds the",
                "§7Candy Cavity tablet."));
        add(new DoubleOption(5, "cabin_map_loot_chance", "Underground Cabin Map", Items.PAPER,
                c.cabinMapLootChance, 100, 5, 25, 0, 100, true, PERCENT,
                "§7Chance a mineshaft chest holds a map",
                "§7to a nearby Underground Cabin."));

        // --- Page 6: Vanilla Changes --------------------------------------------------------
        add(new BoolOption(6, "cartographers_sell_cabin_maps", "Cartographers Sell Cabin Maps", Items.EMERALD,
                c.cartographersSellCabinMaps,
                "§7Adds an Underground Cabin map to the",
                "§7cartographer's trades."));
        add(new BoolOption(6, "wandering_traders_sell_cabin_maps", "Traders Sell Cabin Maps", Items.LEAD,
                c.wanderingTradersSellCabinMaps,
                "§7Adds an Underground Cabin map to the",
                "§7wandering trader's stock."));
        add(new BoolOption(6, "loot_chest_in_witch_huts", "Loot Chest In Witch Huts", Items.CHEST,
                c.lootChestInWitchHuts,
                "§7Adds a chest to vanilla witch huts --",
                "§7another place to find the Candy tablet."));
        add(new BoolOption(6, "enchantments_in_loot", "Mod Enchantments In Loot", Items.ENCHANTED_BOOK,
                c.enchantmentsInLoot,
                "§7Lets the mod's enchantments turn up",
                "§7in vanilla loot tables."));
    }

    /** Every option, in menu order. */
    public static List<Option> all() {
        return ALL;
    }

    /** The options on one page, in menu order. Page 0 is the status page and has none. */
    public static List<Option> onPage(int page) {
        List<Option> out = new ArrayList<>();
        for (Option option : ALL) {
            if (option.page == page) {
                out.add(option);
            }
        }
        return out;
    }

    /** How many options currently differ from their declared default. */
    public static int modifiedCount() {
        int count = 0;
        for (Option option : ALL) {
            if (option.isModified()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Writes the config file. One {@code ConfigValue#save()} saves the whole spec, so this is
     * called once after a change rather than once per option.
     */
    public static void save() {
        if (!ALL.isEmpty()) {
            ALL.get(0).save();
        }
    }
}
