package com.github.alexmodguy.alexscaves.server.misc;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.DeferredRegister;

public class ACAdvancementTriggerRegistry {

    public static final ACAdvancementTrigger KILL_MOB_WITH_GALENA_GAUNTLET = new ACAdvancementTrigger(ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "kill_mob_with_galena_gauntlet"));
    public static final ACAdvancementTrigger FINISHED_QUARRY = new ACAdvancementTrigger(ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "finished_quarry"));
    public static final ACAdvancementTrigger DINOSAURS_MINECART = new ACAdvancementTrigger(ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "dinosaurs_minecart"));
    public static final ACAdvancementTrigger CAVE_PAINTING = new ACAdvancementTrigger(ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "cave_painting"));
    public static final ACAdvancementTrigger MYSTERY_CAVE_PAINTING = new ACAdvancementTrigger(ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "mystery_cave_painting"));
    public static final ACAdvancementTrigger SUMMON_LUXTRUCTOSAURUS = new ACAdvancementTrigger(ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "summon_luxtructosaurus"));
    public static final ACAdvancementTrigger ATLATITAN_STOMP = new ACAdvancementTrigger(ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "atlatitan_stomp"));
    public static final ACAdvancementTrigger ENTER_ACID_WITH_ARMOR = new ACAdvancementTrigger(ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "enter_acid_with_armor"));
    public static final ACAdvancementTrigger ACID_CREATE_RUST = new ACAdvancementTrigger(ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "acid_create_rust"));
    public static final ACAdvancementTrigger REMOTE_DETONATION = new ACAdvancementTrigger(ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "remote_detonation"));
    public static final ACAdvancementTrigger STOP_NUCLEAR_FURNACE_MELTDOWN = new ACAdvancementTrigger(ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "stop_nuclear_furnace_meltdown"));
    public static final ACAdvancementTrigger HATCH_TREMORZILLA_EGG = new ACAdvancementTrigger(ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "hatch_tremorzilla_egg"));
    public static final ACAdvancementTrigger TREMORZILLA_KILL_BEAM = new ACAdvancementTrigger(ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "tremorzilla_kill_beam"));
    public static final ACAdvancementTrigger STALKED_BY_DEEP_ONE = new ACAdvancementTrigger(ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "stalked_by_deep_one"));
    public static final ACAdvancementTrigger DEEP_ONE_TRADE = new ACAdvancementTrigger(ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "deep_one_trade"));
    public static final ACAdvancementTrigger DEEP_ONE_NEUTRAL = new ACAdvancementTrigger(ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "deep_one_neutral"));
    public static final ACAdvancementTrigger DEEP_ONE_HELPFUL = new ACAdvancementTrigger(ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "deep_one_helpful"));
    public static final ACAdvancementTrigger UNDERZEALOT_SACRIFICE = new ACAdvancementTrigger(ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "underzealot_sacrifice"));
    public static final ACAdvancementTrigger BEHOLDER_FAR_AWAY = new ACAdvancementTrigger(ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "beholder_far_away"));
    public static final ACAdvancementTrigger EAT_DARKENED_APPLE = new ACAdvancementTrigger(ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "eat_darkened_apple"));
    public static final ACAdvancementTrigger FROSTMINT_EXPLOSION = new ACAdvancementTrigger(ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "frostmint_explosion"));
    public static final ACAdvancementTrigger CONVERT_BIOME = new ACAdvancementTrigger(ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "convert_biome"));
    public static final ACAdvancementTrigger CONVERT_NETHER_BIOME = new ACAdvancementTrigger(ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "convert_nether_biome"));

    private static final ACAdvancementTrigger[] ALL = {
            KILL_MOB_WITH_GALENA_GAUNTLET,
            DINOSAURS_MINECART,
            CAVE_PAINTING,
            MYSTERY_CAVE_PAINTING,
            SUMMON_LUXTRUCTOSAURUS,
            ATLATITAN_STOMP,
            FINISHED_QUARRY,
            ENTER_ACID_WITH_ARMOR,
            ACID_CREATE_RUST,
            REMOTE_DETONATION,
            STOP_NUCLEAR_FURNACE_MELTDOWN,
            HATCH_TREMORZILLA_EGG,
            TREMORZILLA_KILL_BEAM,
            STALKED_BY_DEEP_ONE,
            DEEP_ONE_TRADE,
            DEEP_ONE_NEUTRAL,
            DEEP_ONE_HELPFUL,
            UNDERZEALOT_SACRIFICE,
            BEHOLDER_FAR_AWAY,
            EAT_DARKENED_APPLE,
            FROSTMINT_EXPLOSION,
            CONVERT_BIOME,
            CONVERT_NETHER_BIOME
    };

    // 1.20.3 turned the criteria list into a real frozen registry (`minecraft:trigger_type`), so
    // CriteriaTriggers.register can no longer be called from common setup — it throws "Registry is
    // already frozen". A DeferredRegister registers the very same trigger instances during the
    // registry event instead, which leaves every call site holding a constant unchanged.
    //
    // 1.20.3, not 1.20.2: javap on the vanilla 1.20.2 jar shows Registries.TRIGGER_TYPE does not
    // exist yet there and CriteriaTriggers.CRITERIA is still a plain BiMap that stays open past mod
    // setup. Neither loader publishes a 1.20.2 build, so nothing before 1.20.2-fabric could say so.
    //? if >=1.20.3 {
    /*public static final DeferredRegister<CriterionTrigger<?>> DEF_REG =
            DeferredRegister.create(Registries.TRIGGER_TYPE, AlexsCaves.MODID);

    static {
        for (ACAdvancementTrigger trigger : ALL) {
            DEF_REG.register(trigger.resourceLocation.getPath(), () -> trigger);
        }
    }
    *///?}

    public static void setup() {
        // Registration is three-way, because 1.20.2 is a middle era of its own. Below it a trigger
        // carried its own getId() and registered itself; on 1.20.2 the id moved out of the instance
        // and register() takes it as a string, but the map is still an ordinary open BiMap; from
        // 1.20.3 it is a frozen registry and the DEF_REG above does the work instead, leaving this
        // method empty. The string form parses namespace:path, so the mod's namespace survives.
        //? if >=1.20.2 && <1.20.3 {
        /*for (ACAdvancementTrigger trigger : ALL) {
            CriteriaTriggers.register(trigger.resourceLocation.toString(), trigger);
        }
        *///?}
        //? if <1.20.2 {
        for (ACAdvancementTrigger trigger : ALL) {
            CriteriaTriggers.register(trigger);
        }
        //?}
    }
}
