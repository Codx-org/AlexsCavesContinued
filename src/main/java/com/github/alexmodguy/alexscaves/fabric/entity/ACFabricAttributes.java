package com.github.alexmodguy.alexscaves.fabric.entity;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.misc.ACIdFactories;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

/**
 * Fabric stand-in for the two attributes Forge adds to every living entity.
 *
 * <p><b>What is missing on this loader.</b> Forge's {@code ForgeMod} declares
 * {@code SWIM_SPEED} and {@code ENTITY_GRAVITY}, adds both to {@code createLivingAttributes} and
 * reads them from three places in {@code LivingEntity} — the gravity term at the top of
 * {@code travel}, the swim-speed multiplier in {@code travel}'s water branch, and the upward nudge
 * in {@code jumpInLiquid}. NeoForge kept the same pair until 1.20.5, when vanilla adopted gravity
 * as {@code Attributes.GRAVITY} and both loaders dropped their own; swim speed has no vanilla
 * counterpart on any version in this matrix. So Fabric is short <b>both</b> below 1.20.5 and short
 * <b>swim speed alone</b> from 1.20.5 up. {@code ACPlatform#entityGravityAttribute} and
 * {@code #swimSpeedAttribute} are the accessors that pick between the three answers; this class is
 * what they return on Fabric.
 *
 * <p><b>These are not cosmetic.</b> Two call sites in this mod dereference the attribute instance
 * with no null check — {@code AbstractMovingBlockEntity} reads the gravity of a living rider and
 * {@code AcidFluidType} reads a swimmer's swim speed — so an attribute that is declared but never
 * added to the supplier is a crash, not a missing feature. That is what makes the
 * {@code createLivingAttributes} injection in {@code mixin.fabric.LivingEntityAttributesMixin}
 * mandatory rather than polish, and it is why the mod registers these itself rather than shrugging
 * at the gap.
 *
 * <p><b>Values are Forge's, read out of its bytecode rather than remembered</b>: swim speed is a
 * {@code RangedAttribute} of default 1.0 over [0, 1024], gravity 0.08 over [-8, 8], both syncable.
 * A default of 1.0 and a multiplicative use make an unmodified entity behave exactly as vanilla
 * does, and 0.08 is the constant vanilla's {@code travel} hard-codes, so an entity with no modifier
 * falls at precisely the vanilla rate. That is the whole reason the mixin can add these
 * unconditionally to every living entity without changing how any of them move.
 *
 * <p><b>Registered under {@code alexscaves:}, deliberately not {@code forge:}.</b> Squatting the
 * other loader's namespace would collide the day a Fabric port of Forge's own compatibility layer
 * appears, and the only thing it could buy — an entity's modifiers surviving a world moved between
 * loaders — is worth nothing here: the one mod-owned modifier is re-applied from an item's
 * attribute map on every equip, and a mob whose stored id fails to resolve simply falls back to its
 * supplier default. The description ids follow vanilla's convention
 * ({@code attribute.name.<namespace>.<name>}, the full translation key — Forge passes a bare
 * {@code forge.swim_speed} instead), so both have real entries in {@code en_us.json}; the swim-speed
 * one is user-visible, in the diving armour's tooltip.
 */
public final class ACFabricAttributes {

    /** Forge's {@code ForgeMod.SWIM_SPEED}, on every node — vanilla has never had an equivalent. */
    public static final Attribute SWIM_SPEED =
            new RangedAttribute("attribute.name.alexscaves.swim_speed", 1.0D, 0.0D, 1024.0D).setSyncable(true);

    /** Forge's {@code ForgeMod.ENTITY_GRAVITY}. Superseded by {@code Attributes.GRAVITY} from 1.20.5. */
    public static final Attribute ENTITY_GRAVITY =
            new RangedAttribute("attribute.name.alexscaves.entity_gravity", 0.08D, -8.0D, 8.0D).setSyncable(true);

    private ACFabricAttributes() {
    }

    /**
     * Puts the pair into the game registry, which is what lets an attribute instance be written to
     * a client — {@code ClientboundUpdateAttributesPacket} sends the registry id, so an unregistered
     * attribute would throw the first time a modified entity came into view.
     *
     * <p>Called from the Fabric entrypoint rather than from a register in {@code AlexsCaves}'s
     * constructor: these two are not the mod's content, they are a loader gap being filled, and
     * nothing in the mod's own flush order depends on them.
     */
    public static void register() {
        Registry.register(BuiltInRegistries.ATTRIBUTE, ACIdFactories.of(AlexsCaves.MODID, "swim_speed"), SWIM_SPEED);
        //? if <1.20.5
        Registry.register(BuiltInRegistries.ATTRIBUTE, ACIdFactories.of(AlexsCaves.MODID, "entity_gravity"), ENTITY_GRAVITY);
    }
}
