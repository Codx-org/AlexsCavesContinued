package com.github.alexmodguy.alexscaves.server.block;

import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

/**
 * Constructors for the vanilla "wood set" blocks, which 1.20.3 reshuffled wholesale: every one of
 * them moved {@code BlockBehaviour.Properties} from the <i>first</i> parameter to the <i>last</i>,
 * and three of them changed what the other parameter is. Rather than gate sixteen registry lines —
 * each a 300-character one-liner that would have to be duplicated verbatim — the registry calls
 * these, and the version difference lives in one place per block type.
 *
 * <p>Every factory keeps upstream's argument order, so the registry lines read the same as they did
 * on 1.20.1.
 *
 * @see com.github.alexmodguy.alexscaves.server.misc.ACPlatform for the same idea applied to
 *      non-block APIs.
 */
public class ACBlockFactory {

    public static StandingSignBlock standingSign(BlockBehaviour.Properties properties, WoodType woodType) {
        //? if >=1.20.3
        /*return new StandingSignBlock(woodType, properties);*/
        //? if <1.20.3
        return new StandingSignBlock(properties, woodType);
    }

    public static WallSignBlock wallSign(BlockBehaviour.Properties properties, WoodType woodType) {
        //? if >=1.20.3
        /*return new WallSignBlock(woodType, properties);*/
        //? if <1.20.3
        return new WallSignBlock(properties, woodType);
    }

    public static CeilingHangingSignBlock hangingSign(BlockBehaviour.Properties properties, WoodType woodType) {
        //? if >=1.20.3
        /*return new CeilingHangingSignBlock(woodType, properties);*/
        //? if <1.20.3
        return new CeilingHangingSignBlock(properties, woodType);
    }

    public static WallHangingSignBlock wallHangingSign(BlockBehaviour.Properties properties, WoodType woodType) {
        //? if >=1.20.3
        /*return new WallHangingSignBlock(woodType, properties);*/
        //? if <1.20.3
        return new WallHangingSignBlock(properties, woodType);
    }

    /**
     * 1.20.3 also dropped the {@code Sensitivity} parameter: what a plate reacts to now comes from
     * its {@code BlockSetType}. Both of Alex's Caves' plates asked for {@code EVERYTHING} and both
     * use a wooden set, whose {@code BlockSetType} says exactly that, so nothing changes.
     */
    public static PressurePlateBlock pressurePlate(BlockBehaviour.Properties properties, BlockSetType blockSetType) {
        //? if >=1.20.3
        /*return new PressurePlateBlock(blockSetType, properties);*/
        //? if <1.20.3
        return new PressurePlateBlock(PressurePlateBlock.Sensitivity.EVERYTHING, properties, blockSetType);
    }

    public static TrapDoorBlock trapDoor(BlockBehaviour.Properties properties, BlockSetType blockSetType) {
        //? if >=1.20.3
        /*return new TrapDoorBlock(blockSetType, properties);*/
        //? if <1.20.3
        return new TrapDoorBlock(properties, blockSetType);
    }

    public static DoorBlock door(BlockBehaviour.Properties properties, BlockSetType blockSetType) {
        //? if >=1.20.3
        /*return new DoorBlock(blockSetType, properties);*/
        //? if <1.20.3
        return new DoorBlock(properties, blockSetType);
    }

    /**
     * The {@code arrowsCanPress} flag went away in 1.20.3 — a button is arrow-pressable iff its
     * {@code BlockSetType} is wooden, which is true of both callers, so passing {@code true} and
     * dropping the argument agree.
     */
    public static ButtonBlock button(BlockBehaviour.Properties properties, BlockSetType blockSetType, int ticksToStayPressed, boolean arrowsCanPress) {
        //? if >=1.20.3
        /*return new ButtonBlock(blockSetType, ticksToStayPressed, properties);*/
        //? if <1.20.3
        return new ButtonBlock(properties, blockSetType, ticksToStayPressed, arrowsCanPress);
    }

    /**
     * A fence gate's open/close sounds became part of its {@code WoodType} in 1.20.3 rather than
     * being passed in. The pewen gate therefore clicks like its own wood type (which is built on
     * {@code BlockSetType.OAK}) instead of like cherry from 1.20.3 up; the thornwood gate already
     * asked for the oak sounds, so it is unchanged.
     */
    public static FenceGateBlock fenceGate(BlockBehaviour.Properties properties, WoodType woodType,
                                           net.minecraft.sounds.SoundEvent closeSound, net.minecraft.sounds.SoundEvent openSound) {
        //? if >=1.20.3
        /*return new FenceGateBlock(woodType, properties);*/
        //? if fabric && <1.20.3
        /*return new FenceGateBlock(properties, woodType);*/
        //? if !fabric && <1.20.3
        return new FenceGateBlock(properties, closeSound, openSound);
    }

    /**
     * A potted plant. The version axis here is not Minecraft's — vanilla's two-argument constructor
     * is unchanged from 1.20.1 to 26.2 — it is the loader's.
     *
     * <p>Vanilla's constructor takes the plant itself and files the pot under it in a static map
     * keyed by content block. Both Forge and NeoForge replace that with a deferred pair, because a
     * mod builds its pots while the block registry is still filling and cannot hand over a block
     * that does not exist yet; the map moves onto the empty pot as {@code addPlant}, which is why
     * {@code ACBlockRegistry.setup} exists at all.
     *
     * <p>Fabric has neither patch, so it uses the vanilla constructor and simply resolves the plant
     * now. That is safe for exactly the reason the deferred form exists to avoid: every one of this
     * mod's ten pots is declared <em>after</em> its plant in the same registry, so the plant is
     * always bound by the time the pot is built. It also makes the registration automatic — the
     * vanilla constructor does the filing itself, which is why {@code setup} has nothing to do
     * there.
     */
    public static net.minecraft.world.level.block.FlowerPotBlock flowerPot(java.util.function.Supplier<? extends net.minecraft.world.level.block.Block> content, BlockBehaviour.Properties properties) {
        //? if fabric
        /*return new net.minecraft.world.level.block.FlowerPotBlock(content.get(), properties);*/
        //? if !fabric
        return new net.minecraft.world.level.block.FlowerPotBlock(() -> (net.minecraft.world.level.block.FlowerPotBlock) net.minecraft.world.level.block.Blocks.FLOWER_POT, content, properties);
    }
}
