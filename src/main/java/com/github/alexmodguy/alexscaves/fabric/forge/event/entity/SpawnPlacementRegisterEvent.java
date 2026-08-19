package com.github.alexmodguy.alexscaves.fabric.forge.event.entity;

import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Event;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Fabric stand-in for "declare where your mobs may spawn" — the 43 calls in
 * {@code ACEntityRegistry.spawnPlacements}.
 *
 * <p>A mod-bus event on the other two loaders, so as with {@link EntityAttributeCreationEvent} the
 * Fabric dispatcher constructs one and calls the registrar directly rather than posting it.
 *
 * <p>{@link Operation} is the part worth reading, and it turns out to be moot. It means "keep
 * whatever rule this entity type already has and combine it with mine" — every call here passes
 * {@code AND} — but the composing branch is unreachable from this mod: Forge seeds its event map
 * from vanilla's {@code SpawnPlacements.DATA_BY_TYPE} before firing, and all 43 targets are this
 * mod's own entity types, which have no entry there. Against an absent key Forge ignores the
 * operation and simply puts the registration in, so AND, OR and REPLACE are the same thing here.
 * {@code fabric.entity.ACFabricEntityRegistration} therefore puts as well, and says at length why —
 * including what happens on the day a call site really does have something to compose with.
 *
 * <p>The placement parameter is gated, because 1.20.5 turned the {@code SpawnPlacements.Type} enum
 * into the {@code SpawnPlacementType} interface beside it. That cannot ride on the {@code
 * !mc205-spawnplacement-*} replacement rules: those rewrite the four vanilla <i>constants</i>
 * ({@code SpawnPlacements.Type.ON_GROUND} and friends), and a rule on the bare type name instead
 * would reach every place the prose here and in {@code ACEntityRegistry} spells it. Two arms per
 * member rather than a gate on the parameter line alone, which is this tree's house shape and keeps
 * each signature readable as a whole.
 */
public class SpawnPlacementRegisterEvent extends Event {

    /** How a registered rule combines with the one the entity type already carries. */
    public enum Operation {
        /** Both must pass. The only one this mod uses. */
        AND,
        /** Either may pass. */
        OR,
        /** Discard the existing rule. */
        REPLACE,
    }

    /**
     * Where a registration ends up. Not a lambda target — the method is generic, so the dispatcher
     * implements this as a class — which is deliberate: it also has to reach vanilla's placement
     * table, which is not public on this loader.
     */
    public interface Registrar {
        //? if >=1.20.5 {
        /*<T extends Mob> void register(EntityType<T> entityType,
                                      net.minecraft.world.entity.SpawnPlacementType placement,
                                      Heightmap.Types heightmap,
                                      SpawnPlacements.SpawnPredicate<T> predicate,
                                      Operation operation);
        *///?} else {
        <T extends Mob> void register(EntityType<T> entityType,
                                      SpawnPlacements.Type placement,
                                      Heightmap.Types heightmap,
                                      SpawnPlacements.SpawnPredicate<T> predicate,
                                      Operation operation);
        //?}
    }

    private final Registrar registrar;

    public SpawnPlacementRegisterEvent(Registrar registrar) {
        this.registrar = registrar;
    }

    //? if >=1.20.5 {
    /*public <T extends Mob> void register(EntityType<T> entityType,
                                         net.minecraft.world.entity.SpawnPlacementType placement,
                                         Heightmap.Types heightmap,
                                         SpawnPlacements.SpawnPredicate<T> predicate,
                                         Operation operation) {
        registrar.register(entityType, placement, heightmap, predicate, operation);
    }
    *///?} else {
    public <T extends Mob> void register(EntityType<T> entityType,
                                         SpawnPlacements.Type placement,
                                         Heightmap.Types heightmap,
                                         SpawnPlacements.SpawnPredicate<T> predicate,
                                         Operation operation) {
        registrar.register(entityType, placement, heightmap, predicate, operation);
    }
    //?}
}
