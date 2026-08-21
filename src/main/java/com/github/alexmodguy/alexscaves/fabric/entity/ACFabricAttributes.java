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

    /**
     * Puts the pair into the game registry, which is what lets an attribute instance be written to
     * a client — {@code ClientboundUpdateAttributesPacket} sends the registry id, so an unregistered
     * attribute would throw the first time a modified entity came into view.
     *
     * <p><b>⚠️ This is a class initialiser, and it has to be.</b> It used to be the body of
     * {@link #register()}, called from the Fabric entrypoint — and that is one class load too late.
     * From 1.20.5 an {@code AttributeSupplier} is a {@code Map} keyed by <b>{@code Holder}</b>, and
     * {@code ACCompat#attribute} answers with {@code BuiltInRegistries.ATTRIBUTE.wrapAsHolder(…)},
     * which returns the bound {@code Holder.Reference} for a registered attribute and a fresh
     * {@code Holder.direct(…)} for an unregistered one. A Direct and a Reference can never be equal,
     * so if the supplier is built <i>before</i> registration the map is keyed by a Direct and every
     * later lookup — which by then gets the Reference — misses:
     * {@code IllegalArgumentException: Can't find attribute alexscaves:swim_speed}. Note the message
     * names the attribute, because it is the <i>lookup</i> holder that is printed
     * ({@code Holder#getRegisteredName}, which a Direct answers {@code [unregistered]}); a message
     * that reads correctly is therefore <b>not</b> evidence that registration was missed.
     *
     * <p>And the supplier really is built first. A dev run calls {@code Bootstrap.validate()} →
     * {@code DefaultAttributes.validate()}, which forces {@code DefaultAttributes}' own initialiser
     * and so runs {@code createLivingAttributes} for every vanilla entity — in this tree's
     * {@code 26.1.2-fabric} log that whole entity class-load cascade is stamped four seconds before
     * {@code Alex's Caves Continued: Fabric common init}. Touching {@code SWIM_SPEED} from the
     * mixin is what initialises this class, so putting the registration here makes it happen at
     * exactly the moment the first holder is wrapped, whichever of the two comes first.
     *
     * <p>The symptom is narrow enough to have survived the whole version walk: the three call sites
     * only run when a living entity is <i>in a fluid</i>, so a boot, a summon and a loot roll all
     * pass. It took a vanilla bat swimming.
     */
    static {
        Registry.register(BuiltInRegistries.ATTRIBUTE, ACIdFactories.of(AlexsCaves.MODID, "swim_speed"), SWIM_SPEED);
        //? if <1.20.5
        Registry.register(BuiltInRegistries.ATTRIBUTE, ACIdFactories.of(AlexsCaves.MODID, "entity_gravity"), ENTITY_GRAVITY);
    }

    private ACFabricAttributes() {
    }

    /**
     * Forces this class's initialiser, and so the registration above, at a known point.
     *
     * <p>Kept as an explicit call from the Fabric entrypoint rather than left to whoever touches
     * {@link #SWIM_SPEED} first: mod init is where the registry is unambiguously open, so on a
     * production run — where {@code Bootstrap.validate()} does not run and nothing loads
     * {@code DefaultAttributes} this early — the pair still lands at the same point in the boot it
     * always did.
     */
    public static void register() {
    }
}
