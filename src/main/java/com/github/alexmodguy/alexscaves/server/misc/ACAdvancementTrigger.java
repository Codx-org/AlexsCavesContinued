package com.github.alexmodguy.alexscaves.server.misc;

import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * A criterion that fires on nothing but "the mod said so" — every one of Alex's Caves' 23
 * advancement triggers is a bare flag with no conditions to test.
 *
 * <p>The criteria system was rewritten in <b>two</b> steps, not one, and this tree could not see the
 * first of them until Fabric reached 1.20.2 — neither Forge nor NeoForge publishes a 1.20.2 or a
 * 1.20.3 build, so the walk jumped straight from 1.20.1 to 1.20.4 and the middle era was invisible.
 * All three shapes were read out of the vanilla jars with javap:
 *
 * <ul>
 *   <li><b>&lt;1.20.2</b> — the instance bound is {@code AbstractCriterionTriggerInstance}, which
 *       carries the criterion's own id; the trigger answers {@code getId()} and deserialises by hand
 *       from a {@code JsonObject} with a bare {@code ContextAwarePredicate} player.</li>
 *   <li><b>1.20.2</b> — the bound becomes the new {@code SimpleCriterionTrigger.SimpleInstance}
 *       interface and the id leaves the instance (so {@code getId()} is gone from
 *       {@code CriterionTrigger} and registration takes a string), but deserialisation is
 *       <i>still</i> the hand-written {@code createInstance} — only its player argument gained an
 *       {@code Optional}. {@code AbstractCriterionTriggerInstance} survives here as a convenience
 *       base that implements {@code SimpleInstance}.</li>
 *   <li><b>&gt;=1.20.3</b> — {@code createInstance} is replaced by a {@code Codec} the trigger hands
 *       back from {@code codec()}, and {@code SimpleInstance} renames its accessor
 *       {@code playerPredicate()} to {@code player()}.</li>
 * </ul>
 *
 * <p>All three are whole-class arms because the supertype, the abstract members and the instance's
 * shape all change together — there is no line-level gate that spans them.
 */
//? if >=1.20.3 {
/*public class ACAdvancementTrigger extends SimpleCriterionTrigger<ACAdvancementTrigger.Instance> {

    public final ResourceLocation resourceLocation;

    public ACAdvancementTrigger(ResourceLocation resourceLocation) {
        this.resourceLocation = resourceLocation;
    }

    @Override
    public com.mojang.serialization.Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer serverPlayer) {
        this.trigger(serverPlayer, instance -> true);
    }

    public void triggerForEntity(Entity entity) {
        if (entity instanceof ServerPlayer serverPlayer) {
            trigger(serverPlayer);
        }
    }

    // The optional player predicate is the whole payload: an advancement JSON may narrow the
    // criterion to a player state, and nothing else about these triggers is configurable.
    public record Instance(java.util.Optional<net.minecraft.advancements.critereon.ContextAwarePredicate> player)
            implements SimpleCriterionTrigger.SimpleInstance {

        public static final com.mojang.serialization.Codec<Instance> CODEC =
                com.mojang.serialization.codecs.RecordCodecBuilder.create(instance -> instance.group(
                        net.minecraft.advancements.critereon.EntityPredicate.ADVANCEMENT_CODEC
                                .optionalFieldOf("player").forGetter(Instance::player)
                ).apply(instance, Instance::new));
    }
}
*///?} elif >=1.20.2 {
/*public class ACAdvancementTrigger extends SimpleCriterionTrigger<ACAdvancementTrigger.Instance> {

    public final ResourceLocation resourceLocation;

    public ACAdvancementTrigger(ResourceLocation resourceLocation) {
        this.resourceLocation = resourceLocation;
    }

    @Override
    protected Instance createInstance(com.google.gson.JsonObject json,
                                      java.util.Optional<net.minecraft.advancements.critereon.ContextAwarePredicate> player,
                                      net.minecraft.advancements.critereon.DeserializationContext context) {
        return new Instance(player);
    }

    public void trigger(ServerPlayer serverPlayer) {
        this.trigger(serverPlayer, instance -> true);
    }

    public void triggerForEntity(Entity entity) {
        if (entity instanceof ServerPlayer serverPlayer) {
            trigger(serverPlayer);
        }
    }

    // AbstractCriterionTriggerInstance is still here on this one version, and it already implements
    // SimpleInstance and serialises the optional player — so the whole instance is its constructor.
    public static class Instance extends net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance {

        public Instance(java.util.Optional<net.minecraft.advancements.critereon.ContextAwarePredicate> player) {
            super(player);
        }
    }
}
*///?} else {
public class ACAdvancementTrigger extends SimpleCriterionTrigger<ACAdvancementTrigger.Instance> {

    public final ResourceLocation resourceLocation;

    public ACAdvancementTrigger(ResourceLocation resourceLocation) {
        this.resourceLocation = resourceLocation;
    }

    public Instance createInstance(com.google.gson.JsonObject json,
                                   net.minecraft.advancements.critereon.ContextAwarePredicate player,
                                   net.minecraft.advancements.critereon.DeserializationContext context) {
        return new Instance(player, resourceLocation);
    }

    public void trigger(ServerPlayer serverPlayer) {
        this.trigger(serverPlayer, instance -> true);
    }

    @Override
    public ResourceLocation getId() {
        return resourceLocation;
    }

    public void triggerForEntity(Entity entity) {
        if (entity instanceof ServerPlayer serverPlayer) {
            trigger(serverPlayer);
        }
    }

    public static class Instance extends net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance {

        public Instance(net.minecraft.advancements.critereon.ContextAwarePredicate player, ResourceLocation res) {
            super(res, player);
        }

        public com.google.gson.JsonObject serializeToJson(net.minecraft.advancements.critereon.SerializationContext context) {
            return super.serializeToJson(context);
        }
    }
}
//?}
