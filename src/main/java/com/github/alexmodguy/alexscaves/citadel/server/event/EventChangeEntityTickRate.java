package com.github.alexmodguy.alexscaves.citadel.server.event;

import net.minecraft.world.entity.Entity;
//? if (forge && <1.21.6) || fabric
import net.minecraftforge.eventbus.api.Cancelable;
//? if !forge || <1.21.6
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired before an entity is skipped or re-ticked at a modified rate. Cancel it to let the entity
 * tick normally regardless of the active {@code TickRateModifier}s.
 *
 * <p>Cancellability is declared three different ways across the matrix: an annotation on Forge's
 * event bus 6, the {@code ICancellableEvent} interface on NeoForge's bus 7, and the
 * {@code Cancellable} characteristic on Forge 56's own (differently shaped) bus 7. On both bus-7
 * flavours the caller learns the verdict from {@code post} rather than from the event, which is why
 * {@link #post} answers a boolean everywhere and no call site reads {@code isCanceled()}.
 */
//? if (forge && <1.21.6) || fabric
@Cancelable
//? if neoforge
/*public class EventChangeEntityTickRate extends Event implements net.neoforged.bus.api.ICancellableEvent {*/
//? if forge && >=1.21.6
/*public class EventChangeEntityTickRate extends net.minecraftforge.eventbus.api.event.MutableEvent implements net.minecraftforge.eventbus.api.event.characteristic.Cancellable {*/
//? if (forge && <1.21.6) || fabric
public class EventChangeEntityTickRate extends Event {

    private final Entity entity;
    private final float targetTickRate;

    public EventChangeEntityTickRate(Entity entity, float targetTickRate) {
        this.entity = entity;
        this.targetTickRate = targetTickRate;
    }

    public Entity getEntity() {
        return entity;
    }

    public float getTargetTickRate() {
        return targetTickRate;
    }

    //? if forge && >=1.21.6
    /*public static final net.minecraftforge.eventbus.api.bus.CancellableEventBus<EventChangeEntityTickRate> BUS = net.minecraftforge.eventbus.api.bus.CancellableEventBus.create(EventChangeEntityTickRate.class);*/

    /**
     * Posts this event and answers whether a listener cancelled it.
     *
     * <p>The NeoForge arm reads {@code isCanceled()} straight off the returned event rather than
     * through an {@code instanceof} pattern: the bus hands the event back, so the pattern
     * would be provably true, which javac rejects under {@code --release 17} (1.20.4) and only
     * tolerates from 21. The class already implements {@code ICancellableEvent} here.
     */
    public static boolean post(EventChangeEntityTickRate event) {
        //? if forge && >=1.21.6
        /*return BUS.post(event);*/
        //? if (forge && <1.21.6) || fabric
        return net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
        //? if neoforge
        /*return net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event).isCanceled();*/
    }
}
