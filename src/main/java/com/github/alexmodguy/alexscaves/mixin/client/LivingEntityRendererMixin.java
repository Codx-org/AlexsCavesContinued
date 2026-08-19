package com.github.alexmodguy.alexscaves.mixin.client;

import com.github.alexmodguy.alexscaves.client.render.entity.LivingEntityRendererAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(net.minecraft.client.renderer.entity.LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin extends net.minecraft.client.renderer.entity.EntityRenderer implements LivingEntityRendererAccessor {

    //? if <1.21.2 {
    @Shadow protected abstract void scale(LivingEntity living, PoseStack poseStack, float f);
    //?}

    protected LivingEntityRendererMixin(EntityRendererProvider.Context context) {
        super(context);
    }

    //? if >=1.21.2 {
    /*// 1.21.2 respells this as scale(S state, PoseStack), and a render state is not something this
    // hook can be handed. Alex's Caves' own renderers override scaleForHologram in
    // client.render.compat.LivingEntityRenderer, which still has the entity; a vanilla mob just
    // renders at its base scale inside a notor's hologram.
    public void scaleForHologram(LivingEntity entity, PoseStack poseStack, float partialTicks) {
    }
    *///?} else {
    public void scaleForHologram(LivingEntity entity, PoseStack poseStack, float partialTicks) {
        this.scale(entity, poseStack, partialTicks);
    }
    //?}
}
