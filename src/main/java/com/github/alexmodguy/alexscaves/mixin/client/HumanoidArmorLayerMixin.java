package com.github.alexmodguy.alexscaves.mixin.client;


import com.github.alexmodguy.alexscaves.client.render.item.ACArmorRenderProperties;
import com.github.alexmodguy.alexscaves.server.item.CustomArmorPostRender;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
//? if <1.21.5
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.Map;

@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin extends net.minecraft.client.renderer.entity.layers.RenderLayer {

    private static final Map<String, ResourceLocation> AC_ARMOR_LOCATION_CACHE = Maps.newHashMap();
    private ItemStack lastArmorItemStackRendered = ItemStack.EMPTY;

    // 1.21.9 deleted setPartVisibility outright: an ArmorModelSet now bakes one model per slot, so
    // vanilla never toggles parts at runtime any more. A @Shadow resolves against the target class
    // alone, so leaving it declared is a hard load failure there — the >=1.21.9 arm below carries its
    // own copy of what the method used to do, since this mod hands the helper its own armour model
    // rather than one out of the set.
    //? if <1.21.9 {
    @Shadow
    protected abstract void setPartVisibility(HumanoidModel humanoidModel, EquipmentSlot equipmentSlot);
    //?}

    public HumanoidArmorLayerMixin(RenderLayerParent renderLayerParent) {
        super(renderLayerParent);
    }

    // ── 1.21.2 and up ─────────────────────────────────────────────────────────────────────────────
    // renderArmorPiece takes the ItemStack straight off the render state now, and the LivingEntity
    // is gone from the whole layer. Two of the three things this needs it for — the texture and the
    // animated model variant — still want an entity, so it has to be recovered from the state, and
    // the two loaders hand that state over differently. Forge patched renderArmorPiece to carry the
    // HumanoidRenderState as a trailing argument, so its arm reads the entity off the parameter.
    // NeoForge left the private method exactly as vanilla wrote it, so its arm captures the state at
    // the top of render() instead; renderArmorPiece is private and called only from there, on the
    // render thread, so the field is read within the same call it was written in.
    //
    // The third use, `armorItem.getEquipmentSlot() == equipmentSlot`, is answered by the EQUIPPABLE
    // component instead: 1.21.2 moved the slot an armour piece belongs in out of the item and into
    // that component, which ACArmorMaterial#vanilla puts on all six sets.
    //
    // The drawing itself is identical on both loaders, so it lives in one @Unique helper that each
    // arm calls with whichever entity it managed to recover.
    //? if >=1.21.2 && <1.21.9 {
    /*@org.spongepowered.asm.mixin.Unique
    private void ac_renderCustomArmor(PoseStack poseStack, MultiBufferSource multiBufferSource, ItemStack itemstack, EquipmentSlot equipmentSlot, int light, HumanoidModel humanoidModel, @Nullable LivingEntity livingEntity) {
        Item item = itemstack.getItem();
        net.minecraft.world.item.equipment.Equippable equippable = itemstack.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
        // No `item instanceof ArmorItem` here: the caller already gated on CustomArmorPostRender,
        // which only AC's six sets implement, and 1.21.5 deleted the class. The EQUIPPABLE
        // component answers the slot question on its own.
        if (equippable != null && equippable.slot() == equipmentSlot) {
            boolean legs = equipmentSlot == EquipmentSlot.LEGS;
            HumanoidModel model = this.getParentModel() instanceof HumanoidModel humanoidModel1 ? humanoidModel1 : humanoidModel;
            // Straight to this mod's own resolver rather than through the loader's armour-model hook:
            // NeoForge's lost its entity parameter in 1.21.2 and would hand back the un-animated
            // model. This branch only ever runs for AC's own CustomArmorPostRender items, whose
            // IClientItemExtensions is ACArmorRenderProperties either way.
            Model armorModel = ACArmorRenderProperties.getACArmorModel(livingEntity, itemstack, model);
            setPartVisibility((HumanoidModel) armorModel, equipmentSlot);
            ResourceLocation texture = getACArmorResource(livingEntity, itemstack, equipmentSlot, null);
            ACArmorRenderProperties.renderCustomArmor(poseStack, multiBufferSource, light, itemstack, item, armorModel, legs, texture);
        }
    }
    *///?}

    // ── 1.21.9 and up ─────────────────────────────────────────────────────────────────────────────
    // The deferred-submit rewrite reaches this layer: renderArmorPiece takes a SubmitNodeCollector
    // instead of a MultiBufferSource, and the HumanoidModel parameter is gone (an ArmorModelSet bakes
    // one model per slot, which is also why setPartVisibility went with it). Forge's trailing-state
    // patch is folded into vanilla, so for the first time since 1.21.2 both loaders share one
    // descriptor and one arm.
    //
    // Without the model parameter there is no fallback for a non-humanoid parent model, so the draw
    // is simply skipped in that case — every wearer of this mod's six sets is a humanoid. The legacy
    // draw body still speaks MultiBufferSource, so it goes through ACSubmitBuffers like the other
    // 1.21.9 render sites.
    //
    // HumanoidModel#setAllVisible is gone in 26 as well, and this arm cannot nest a version gate, so
    // the seven parts it assigned are spelled out instead — read out of 1.21.11's bytecode, so this
    // is the same set on every node the arm covers.
    //? if >=1.21.9 {
    /*@org.spongepowered.asm.mixin.Unique
    private void ac_setPartVisibility(HumanoidModel model, EquipmentSlot slot) {
        model.head.visible = false;
        model.hat.visible = false;
        model.body.visible = false;
        model.rightArm.visible = false;
        model.leftArm.visible = false;
        model.rightLeg.visible = false;
        model.leftLeg.visible = false;
        switch (slot) {
            case HEAD -> {
                model.head.visible = true;
                model.hat.visible = true;
            }
            case CHEST -> {
                model.body.visible = true;
                model.rightArm.visible = true;
                model.leftArm.visible = true;
            }
            case LEGS -> {
                model.body.visible = true;
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
            }
            case FEET -> {
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
            }
            default -> {
            }
        }
    }

    @org.spongepowered.asm.mixin.Unique
    private void ac_renderCustomArmor(PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector, ItemStack itemstack, EquipmentSlot equipmentSlot, int light, net.minecraft.client.renderer.entity.state.HumanoidRenderState state) {
        Item item = itemstack.getItem();
        net.minecraft.world.item.equipment.Equippable equippable = itemstack.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
        if (equippable != null && equippable.slot() == equipmentSlot && this.getParentModel() instanceof HumanoidModel parentModel) {
            LivingEntity livingEntity = com.github.alexmodguy.alexscaves.client.render.compat.ACStateAccess.entity(state) instanceof LivingEntity living ? living : null;
            Model armorModel = ACArmorRenderProperties.getACArmorModel(livingEntity, itemstack, parentModel);
            ac_setPartVisibility((HumanoidModel) armorModel, equipmentSlot);
            ResourceLocation texture = getACArmorResource(livingEntity, itemstack, equipmentSlot, null);
            com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers buffers =
                    new com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers(collector);
            ACArmorRenderProperties.renderCustomArmor(poseStack, buffers, light, itemstack, item, armorModel, equipmentSlot == EquipmentSlot.LEGS, texture);
            buffers.flush();
        }
    }

    @Inject(
            method = {"Lnet/minecraft/client/renderer/entity/layers/HumanoidArmorLayer;renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V"},
            at = @At(value = "HEAD"),
            remap = true,
            cancellable = true
    )
    private void ac_renderArmorPiece(PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector, ItemStack itemstack, EquipmentSlot equipmentSlot, int light, net.minecraft.client.renderer.entity.state.HumanoidRenderState state, CallbackInfo ci) {
        if (itemstack.getItem() instanceof CustomArmorPostRender) {
            ci.cancel();
            ac_renderCustomArmor(poseStack, collector, itemstack, equipmentSlot, light, state);
        }
    }
    *///?} elif forge && >=1.21.2 {
    /*@Inject(
            method = {"Lnet/minecraft/client/renderer/entity/layers/HumanoidArmorLayer;renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V"},
            at = @At(value = "HEAD"),
            remap = true,
            cancellable = true
    )
    private void ac_renderArmorPiece(PoseStack poseStack, MultiBufferSource multiBufferSource, ItemStack itemstack, EquipmentSlot equipmentSlot, int light, HumanoidModel humanoidModel, net.minecraft.client.renderer.entity.state.HumanoidRenderState state, CallbackInfo ci) {
        if (itemstack.getItem() instanceof CustomArmorPostRender) {
            ci.cancel();
            ac_renderCustomArmor(poseStack, multiBufferSource, itemstack, equipmentSlot, light, humanoidModel,
                    com.github.alexmodguy.alexscaves.client.render.compat.ACStateAccess.entity(state) instanceof LivingEntity living ? living : null);
        }
    }
    *///?} elif >=1.21.2 {
    /*@org.spongepowered.asm.mixin.Unique
    private net.minecraft.client.renderer.entity.state.HumanoidRenderState ac_renderState;

    @Inject(
            method = {"Lnet/minecraft/client/renderer/entity/layers/HumanoidArmorLayer;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V"},
            at = @At(value = "HEAD"),
            remap = true
    )
    private void ac_captureRenderState(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, net.minecraft.client.renderer.entity.state.HumanoidRenderState state, float yRot, float xRot, CallbackInfo ci) {
        this.ac_renderState = state;
    }

    @Inject(
            method = {"Lnet/minecraft/client/renderer/entity/layers/HumanoidArmorLayer;renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;)V"},
            at = @At(value = "HEAD"),
            remap = true,
            cancellable = true
    )
    private void ac_renderArmorPiece(PoseStack poseStack, MultiBufferSource multiBufferSource, ItemStack itemstack, EquipmentSlot equipmentSlot, int light, HumanoidModel humanoidModel, CallbackInfo ci) {
        if (itemstack.getItem() instanceof CustomArmorPostRender) {
            ci.cancel();
            ac_renderCustomArmor(poseStack, multiBufferSource, itemstack, equipmentSlot, light, humanoidModel,
                    this.ac_renderState != null
                            && com.github.alexmodguy.alexscaves.client.render.compat.ACStateAccess.entity(this.ac_renderState) instanceof LivingEntity living ? living : null);
        }
    }
    *///?} else {

    @Inject(
            method = {"Lnet/minecraft/client/renderer/entity/layers/HumanoidArmorLayer;renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;)V"},
            at = @At(value = "HEAD"),
            remap = true,
            cancellable = true
    )
    private void ac_renderArmorPiece(PoseStack poseStack, MultiBufferSource multiBufferSource, LivingEntity livingEntity, EquipmentSlot equipmentSlot, int light, HumanoidModel humanoidModel, CallbackInfo ci) {
        ItemStack itemstack = livingEntity.getItemBySlot(equipmentSlot);
        if (itemstack.getItem() instanceof CustomArmorPostRender) {
            ci.cancel();
            lastArmorItemStackRendered = livingEntity.getItemBySlot(equipmentSlot);
            Item item = itemstack.getItem();
            if (item instanceof ArmorItem armorItem) {
                if (armorItem.getEquipmentSlot() == equipmentSlot) {
                    boolean legs = equipmentSlot == EquipmentSlot.LEGS;
                    HumanoidModel model = this.getParentModel() instanceof HumanoidModel humanoidModel1 ? humanoidModel1 : humanoidModel;
                    Model armorModel = ForgeHooksClient.getArmorModel(livingEntity, itemstack, equipmentSlot, model);
                    setPartVisibility((HumanoidModel) armorModel, equipmentSlot);
                    ResourceLocation texture = getACArmorResource(livingEntity, itemstack, equipmentSlot, null);
                    ACArmorRenderProperties.renderCustomArmor(poseStack, multiBufferSource, light, lastArmorItemStackRendered, armorItem, armorModel, legs, texture);
                }
            }
        }
    }
    //?}


    /**
     * This used to be a copy of Forge's own resolver: build vanilla's
     * {@code textures/models/armor/<material>_layer_N.png} path from the armour material's name,
     * then let {@code ForgeHooksClient.getArmorTexture} hand it to the item to override.
     *
     * <p>Both halves of that are gone from 1.20.5 — the material is a record with no name, and the
     * hook is keyed on an {@code ArmorMaterial.Layer}. Neither loss matters, because this method
     * only ever runs for a {@link CustomArmorPostRender} and every one of those overrode the path
     * unconditionally. Asking the item directly is what the code always did in effect, and it is
     * the same on every version and every loader.
     */
    private ResourceLocation getACArmorResource(LivingEntity entity, ItemStack stack, EquipmentSlot slot, @Nullable String type) {
        String s1 = ((CustomArmorPostRender) stack.getItem()).getArmorTexture(stack, entity, slot, type);
        ResourceLocation resourcelocation = AC_ARMOR_LOCATION_CACHE.get(s1);

        if (resourcelocation == null) {
            resourcelocation = ResourceLocation.parse(s1);
            AC_ARMOR_LOCATION_CACHE.put(s1, resourcelocation);
        }

        return resourcelocation;
    }
}
