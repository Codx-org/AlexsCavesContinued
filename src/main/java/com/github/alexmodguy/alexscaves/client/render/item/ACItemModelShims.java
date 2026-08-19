package com.github.alexmodguy.alexscaves.client.render.item;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import net.minecraft.resources.ResourceLocation;

/**
 * The mod's four entries in the 1.21.4 item-model pipeline.
 *
 * <p>1.21.4 moved everything dynamic about an item's appearance out of code and into an
 * <em>item model definition</em> ({@code assets/<ns>/items/<id>.json}). Three of the things Alex's
 * Caves used to do from Java are declarations there now, each resolved through a vanilla
 * {@code LateBoundIdMapper} keyed by a namespaced id — and mods are meant to add their own:
 *
 * <ul>
 * <li>{@code alexscaves:item_renderer} and {@code alexscaves:icon} — the two {@code minecraft:special}
 *     renderers that replace the deleted {@code BlockEntityWithoutLevelRenderer}. See
 *     {@link ACItemSpecialRenderer}.</li>
 * <li>{@code alexscaves:tint} — the five dynamic item tints that used to be registered on
 *     {@code RegisterColorHandlersEvent.Item}, which 1.21.4 deleted. A definition names one by
 *     {@code source}; the layer it applies to is its position in the model's {@code tints} list,
 *     which is what the old {@code tintIndex} meant.</li>
 * <li>{@code alexscaves:legacy} — a {@code minecraft:range_dispatch} property that answers the same
 *     eleven values {@code ItemProperties} used to, by {@code name}. The thresholds live in the
 *     definition, exactly as they used to live in the model's {@code overrides} list.</li>
 * </ul>
 *
 * <p>The values themselves are {@link ACItemPredicates}, shared with the pre-1.21.4 path.
 *
 * <p><b>Why reflection.</b> All three id-mappers are {@code private static final}. NeoForge exposes
 * events for two of them and none for the third; Forge exposes none at all and its member names are
 * SRG, so a name-based lookup is fragile. Locating the field <i>by type</i> is unique in each holder
 * class, identical on every loader — the Fabric milestone included — and immune to mappings. It
 * fails loudly, because the alternative symptom is silently missing-model items.
 *
 * <p>Below 1.21.4 the whole body is gated out and nothing calls {@code register()}. Nothing inside
 * that gate may carry a block comment, since the inactive arm is itself one.
 */
public final class ACItemModelShims {

    private ACItemModelShims() {
    }

    /** The type id of the mod's dynamic tint source, as written into the item model definitions. */
    public static ResourceLocation tintId() {
        return ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "tint");
    }

    /** The type id of the mod's range-select property, as written into the item model definitions. */
    public static ResourceLocation legacyId() {
        return ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "legacy");
    }

    //? if >=1.21.4 {
    /*// Puts all four of the mod's type ids into their vanilla id-mappers. Called from
    // ClientProxy#commonInit, i.e. at CONSTRUCT, so the first resource reload already
    // resolves them.
    public static void register() {
        net.minecraft.util.ExtraCodecs.LateBoundIdMapper specials =
                mapperOf(net.minecraft.client.renderer.special.SpecialModelRenderers.class);
        specials.put(ACItemSpecialRenderer.itemRendererId(), ACItemSpecialRenderer.Unbaked.MAP_CODEC);
        specials.put(ACItemSpecialRenderer.iconId(), ACItemSpecialRenderer.Icon.Unbaked.MAP_CODEC);

        mapperOf(net.minecraft.client.color.item.ItemTintSources.class).put(tintId(), Tint.MAP_CODEC);
        mapperOf(net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties.class)
                .put(legacyId(), Legacy.MAP_CODEC);

        AlexsCaves.LOGGER.info("registered item model definition types");
    }

    // Finds a holder class's sole LateBoundIdMapper field and reads it.
    @SuppressWarnings("rawtypes")
    private static net.minecraft.util.ExtraCodecs.LateBoundIdMapper mapperOf(Class<?> holder) {
        for (java.lang.reflect.Field field : holder.getDeclaredFields()) {
            if (field.getType() == net.minecraft.util.ExtraCodecs.LateBoundIdMapper.class) {
                try {
                    field.setAccessible(true);
                    return (net.minecraft.util.ExtraCodecs.LateBoundIdMapper) field.get(null);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Could not read the id mapper out of " + holder.getName(), e);
                }
            }
        }
        throw new IllegalStateException("No LateBoundIdMapper field in " + holder.getName());
    }

    // The five dynamic tints, by name. Each returns an opaque colour: vanilla's own
    // minecraft:constant source hands its value straight through, so a source that computes an RGB
    // triple has to supply the alpha itself or the layer draws fully transparent.
    public record Tint(String source) implements net.minecraft.client.color.item.ItemTintSource {

        public static final com.mojang.serialization.MapCodec<Tint> MAP_CODEC =
                com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(instance -> instance.group(
                        com.mojang.serialization.Codec.STRING.fieldOf("source").forGetter(Tint::source)
                ).apply(instance, Tint::new));

        @Override
        public int calculate(net.minecraft.world.item.ItemStack stack,
                             net.minecraft.client.multiplayer.ClientLevel level,
                             net.minecraft.world.entity.LivingEntity holder) {
            net.minecraft.client.multiplayer.ClientLevel world =
                    level != null ? level : net.minecraft.client.Minecraft.getInstance().level;
            return net.minecraft.util.ARGB.opaque(switch (source) {
                case "biome" -> com.github.alexmodguy.alexscaves.server.item.CaveInfoItem.getBiomeColorOf(world, stack, false);
                case "pearl" -> com.github.alexmodguy.alexscaves.server.item.GazingPearlItem.getPearlColor(stack);
                case "jelly_bean" -> com.github.alexmodguy.alexscaves.server.item.JellyBeanItem.getBeanColor(stack);
                case "biome_treat" -> com.github.alexmodguy.alexscaves.server.item.BiomeTreatItem.getBiomeTreatColorOf(world, stack);
                default -> throw new IllegalStateException("Unknown alexscaves:tint source '" + source + "'");
            });
        }

        @Override
        public com.mojang.serialization.MapCodec<Tint> type() {
            return MAP_CODEC;
        }
    }

    // The eleven ItemProperties values, by name — the thresholds that select on them stay in the
    // item model definition, so this is a straight lookup.
    public record Legacy(String name) implements net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty {

        public static final com.mojang.serialization.MapCodec<Legacy> MAP_CODEC =
                com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(instance -> instance.group(
                        com.mojang.serialization.Codec.STRING.fieldOf("name").forGetter(Legacy::name)
                ).apply(instance, Legacy::new));

        // 1.21.9 widened the holder to ItemOwner (a level + position + facing, so an item frame or a
        // display entity can drive a range-select too). The nine predicates behind this all want a
        // LivingEntity, so the parameter is unwrapped back to one on the first line — written as a
        // single-line signature plus a separate local because both halves are rewritten by the
        // `!mc219-rangeselect-itemowner` replacement rule, and this class sits inside a `>=1.21.4`
        // arm that a nested gate cannot live in.
        @Override
        public float get(net.minecraft.world.item.ItemStack stack, net.minecraft.client.multiplayer.ClientLevel level, net.minecraft.world.entity.LivingEntity acHolder, int seed) {
            net.minecraft.world.entity.LivingEntity holder = acHolder;
            return switch (name) {
                case "bound" -> ACItemPredicates.bound(stack, level, holder);
                case "nugget" -> ACItemPredicates.nugget(stack, level, holder);
                case "throwing" -> ACItemPredicates.throwing(stack, level, holder);
                case "active" -> ACItemPredicates.active(stack, level, holder);
                case "tooting" -> ACItemPredicates.tooting(stack, level, holder);
                case "charging" -> ACItemPredicates.charging(stack, level, holder);
                case "totem" -> ACItemPredicates.totem(stack, level, holder);
                case "cast" -> ACItemPredicates.cast(stack, level, holder);
                case "open" -> ACItemPredicates.open(stack, level, holder);
                default -> throw new IllegalStateException("Unknown alexscaves:legacy property '" + name + "'");
            };
        }

        @Override
        public com.mojang.serialization.MapCodec<Legacy> type() {
            return MAP_CODEC;
        }
    }
    *///?}

    // The NeoForge path. Same four ids, same four codecs, registered through the three mod-bus
    // events NeoForge fires for exactly this — never through mapperOf(), because on this loader
    // ClientBootstrap.bootstrap() runs from Minecraft.<init>, AFTER mod loading, and reading
    // SpecialModelRenderers' mapper at CONSTRUCT would force Sheets.<clinit> too early (see the
    // long comment at the call site in ClientProxy#commonInit).
    //
    // The two special-renderer codecs go through raw locals on purpose: 26.1 made
    // SpecialModelRenderer.Unbaked generic, so the event's bound is `? extends Unbaked<?>` there
    // and `? extends Unbaked` on 1.21.4-1.21.11, and this class's own Unbaked implements the raw
    // type. A raw MapCodec is accepted against both bounds; a parameterised one is not.
    //? if neoforge && >=1.21.4 {
    /*@SuppressWarnings({"rawtypes", "unchecked"})
    public static void registerNeoForge(net.neoforged.bus.api.IEventBus bus) {
        bus.addListener((net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent event) -> {
            com.mojang.serialization.MapCodec itemRenderer = ACItemSpecialRenderer.Unbaked.MAP_CODEC;
            com.mojang.serialization.MapCodec icon = ACItemSpecialRenderer.Icon.Unbaked.MAP_CODEC;
            event.register(ACItemSpecialRenderer.itemRendererId(), itemRenderer);
            event.register(ACItemSpecialRenderer.iconId(), icon);
        });
        bus.addListener((net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.ItemTintSources event) ->
                event.register(tintId(), Tint.MAP_CODEC));
        bus.addListener((net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent event) ->
                event.register(legacyId(), Legacy.MAP_CODEC));
        AlexsCaves.LOGGER.info("registered item model definition types");
    }
    *///?}
}
