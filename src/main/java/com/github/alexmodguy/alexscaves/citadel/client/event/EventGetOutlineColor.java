package com.github.alexmodguy.alexscaves.citadel.client.event;

import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.github.alexmodguy.alexscaves.citadel.CitadelEvent;

@OnlyIn(Dist.CLIENT)
public class EventGetOutlineColor extends CitadelEvent {
    private Entity entityIn;
    private int color;

    public EventGetOutlineColor(Entity entityIn, int color) {
        this.entityIn = entityIn;
        this.color = color;
    }

    public Entity getEntityIn() {
        return entityIn;
    }

    public void setEntityIn(Entity entityIn) {
        this.entityIn = entityIn;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    //? if forge && >=1.21.6
    /*public static final net.minecraftforge.eventbus.api.bus.EventBus<EventGetOutlineColor> BUS = net.minecraftforge.eventbus.api.bus.EventBus.create(EventGetOutlineColor.class);*/

    /** @see EventPosePlayerHand#post */
    public static void post(EventGetOutlineColor event) {
        //? if forge && >=1.21.6
        /*BUS.post(event);*/
        //? if !forge || <1.21.6
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
    }
}
