package com.github.alexmodguy.alexscaves.citadel.client.event;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.github.alexmodguy.alexscaves.citadel.CitadelEvent;

@OnlyIn(Dist.CLIENT)
public class EventPosePlayerHand extends CitadelEvent {
    private final LivingEntity entityIn;
    private final HumanoidModel model;
    private final boolean left;

    public EventPosePlayerHand(LivingEntity entityIn, HumanoidModel model, boolean left) {
        this.entityIn = entityIn;
        this.model = model;
        this.left = left;
    }

    public Entity getEntityIn() {
        return entityIn;
    }

    public HumanoidModel getModel() {
        return model;
    }

    public boolean isLeftHand() {
        return left;
    }

    //? if forge && >=1.21.6
    /*public static final net.minecraftforge.eventbus.api.bus.EventBus<EventPosePlayerHand> BUS = net.minecraftforge.eventbus.api.bus.EventBus.create(EventPosePlayerHand.class);*/

    /**
     * Posts this event. EventBus 7 (Forge 56, 1.21.6) has no bus-wide {@code post} — an event type
     * owns its bus — so every call site asks the event rather than the bus, on every loader and
     * every version.
     */
    public static void post(EventPosePlayerHand event) {
        //? if forge && >=1.21.6
        /*BUS.post(event);*/
        //? if !forge || <1.21.6
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
    }
}
