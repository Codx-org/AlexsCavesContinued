package com.github.alexmodguy.alexscaves.server.level.carver;

/**
 * The carver registry, which registers exactly one carver that nothing uses.
 *
 * <p>26.3 made {@code WorldCarver} a non-generic interface and moved the registry of carver
 * implementations to {@code Registries.CARVER_TYPE}, which holds {@code MapCodec}s — reusing the name
 * {@code CARVER} for what used to be {@code CONFIGURED_CARVER}, the same swap {@code FEATURE} and
 * {@code FEATURE_TYPE} underwent. So the declared type of {@code DEF_REG} cannot be spelled the same
 * way on both sides, and {@code DeferredRegister<WorldCarver<?>>} does not compile at 26.3 at all.
 *
 * <p>The 26.3 arm therefore declares the register and no entries. That is not a behaviour change:
 * {@code WaterBubbleCarver} is marked unused upstream, no datapack JSON in this mod references it,
 * and 26.3 turned {@code CaveWorldCarver} into a final record, so it could not be subclassed even if
 * something did. Keeping {@code DEF_REG} present on both arms is what lets the single call site in
 * {@code AlexsCaves} stay ungated.
 */
//? if >=26.3 {
/*import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraftforge.registries.DeferredRegister;

public class ACCarverRegistry {

    public static final DeferredRegister<MapCodec<? extends WorldCarver>> DEF_REG = DeferredRegister.create(Registries.CARVER_TYPE, AlexsCaves.MODID);

}
*///?} else {
import com.github.alexmodguy.alexscaves.AlexsCaves;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.carver.CaveCarverConfiguration;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraftforge.registries.DeferredRegister;
import java.util.function.Supplier;

public class ACCarverRegistry {

    public static final DeferredRegister<WorldCarver<?>> DEF_REG = DeferredRegister.create(Registries.CARVER, AlexsCaves.MODID);

    //Unused for now.
    public static Supplier<WaterBubbleCarver> WATER_BUBBLE_CARVER = DEF_REG.register("water_bubble_carver", () -> new WaterBubbleCarver(CaveCarverConfiguration.CODEC));

}
//?}
