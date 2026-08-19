package com.github.alexmodguy.alexscaves.fabric.entity;

import com.github.alexmodguy.alexscaves.fabric.forge.event.entity.EntityAttributeCreationEvent;
import com.github.alexmodguy.alexscaves.fabric.forge.event.entity.SpawnPlacementRegisterEvent;
import com.github.alexmodguy.alexscaves.server.entity.ACEntityRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Delivers the two entity mod-bus events {@code ACEntityRegistry} declares handlers for — the 45
 * attribute suppliers and the 43 spawn placements.
 *
 * <p><b>Called, not posted.</b> Both handlers are {@code @SubscribeEvent} statics on a class marked
 * {@code @Mod.EventBusSubscriber}, and that annotation is inert on this loader (see the Fabric
 * {@code Mod} stand-in) — nothing scans for it, so nothing would ever deliver a posted event. There
 * is no mod bus here either, so the two methods are simply called with a freshly built event. The
 * 88 registration lines themselves stay byte-identical on all three loaders; only the delivery
 * differs.
 *
 * <p><b>Attributes</b> go into Fabric API's {@code FabricDefaultAttributeRegistry}, which is a
 * plain static map put — the same table vanilla's {@code DefaultAttributes} reads when an entity is
 * constructed. A type with no supplier does not fail at registration, it fails the first time one
 * spawns, with a null attribute map, so this has to run after the entity types are registered and
 * before any world loads.
 *
 * <p><b>Spawn placements</b> go into vanilla's own {@code SpawnPlacements.DATA_BY_TYPE}, through
 * the private static {@code register} an access-widener line opens. Forge reaches the same map from
 * the other side: {@code fireSpawnPlacementEvent} copies every existing entry into the event, fires
 * it, and writes the result back — read out of its patched {@code SpawnPlacements} rather than
 * remembered.
 *
 * <p>Which is what makes {@link SpawnPlacementRegisterEvent.Operation} a non-question here. All 43
 * calls pass {@code AND}, and all 43 name one of this mod's own entity types, so every key is
 * <b>absent</b> from that map when this runs — and against an absent key Forge's own code ignores
 * the operation entirely and puts the registration in as-is. AND, OR and REPLACE only differ once
 * something is already there. Nothing composes, so nothing here composes.
 *
 * <p>⚠️ The path that would compose is deliberately not reproduced, and the failure mode is the
 * point: a second registration for one entity type walks into vanilla's own duplicate check and
 * throws {@code IllegalStateException} naming the type — the same exception class Forge throws for
 * a re-register that is neither {@code REPLACE} nor a (null, null) pair. So the day a call site
 * really does need to merge with an existing rule, this stops loudly instead of silently keeping
 * one side of an {@code AND}. Reaching the existing rule would mean widening a package-private
 * class and three of its fields for a branch no caller takes.
 *
 * <p>The placement parameter is gated in step with the event it feeds: 1.20.5 replaced the
 * {@code SpawnPlacements.Type} enum with the {@code SpawnPlacementType} interface. That moves the
 * widened method's <i>descriptor</i> too, so the access widener carries one entry per era — and an
 * access-widener entry that matches nothing is a hard build failure, not a no-op, which is why
 * neither of the two may be left unconditional.
 */
public final class ACFabricEntityRegistration {

    private ACFabricEntityRegistration() {
    }

    public static void register() {
        // Two overloads exist — the other takes an AttributeSupplier.Builder — and the event's sink
        // type picks this one, so the reference is unambiguous.
        ACEntityRegistry.initializeAttributes(
                new EntityAttributeCreationEvent(FabricDefaultAttributeRegistry::register));

        // An anonymous class rather than a lambda: the method is generic, which no functional
        // interface conversion can express.
        ACEntityRegistry.spawnPlacements(new SpawnPlacementRegisterEvent(new SpawnPlacementRegisterEvent.Registrar() {
            //? if >=1.20.5 {
            /*@Override
            public <T extends Mob> void register(EntityType<T> entityType,
                                                 net.minecraft.world.entity.SpawnPlacementType placement,
                                                 Heightmap.Types heightmap,
                                                 SpawnPlacements.SpawnPredicate<T> predicate,
                                                 SpawnPlacementRegisterEvent.Operation operation) {
                SpawnPlacements.register(entityType, placement, heightmap, predicate);
            }
            *///?} else {
            @Override
            public <T extends Mob> void register(EntityType<T> entityType,
                                                 SpawnPlacements.Type placement,
                                                 Heightmap.Types heightmap,
                                                 SpawnPlacements.SpawnPredicate<T> predicate,
                                                 SpawnPlacementRegisterEvent.Operation operation) {
                SpawnPlacements.register(entityType, placement, heightmap, predicate);
            }
            //?}
        }));
    }
}
