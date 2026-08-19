package com.github.alexmodguy.alexscaves.client.render.item;

import com.github.alexmodguy.alexscaves.client.ACClientCompat;
import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.client.model.layered.*;
import com.github.alexmodguy.alexscaves.client.render.ACRenderTypes;
import com.github.alexmodguy.alexscaves.server.item.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public class ACArmorRenderProperties implements IClientItemExtensions {

    private static final ResourceLocation DARKNESS_ARMOR_GLOW = ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "textures/armor/darkness_armor_glow.png");
    private static boolean init;
    public static PrimordialArmorModel PRIMORDIAL_ARMOR_MODEL;
    public static HazmatArmorModel HAZMAT_ARMOR_MODEL;
    public static DivingArmorModel DIVING_ARMOR_MODEL;
    public static DarknessArmorModel DARKNESS_ARMOR_MODEL;
    public static RainbounceArmorModel RAINBOUNCE_ARMOR_MODEL;
    public static GingerbreadArmorModel GINGERBREAD_ARMOR_MODEL;


    public static void initializeModels() {
        init = true;
        PRIMORDIAL_ARMOR_MODEL = new PrimordialArmorModel(Minecraft.getInstance().getEntityModels().bakeLayer(ACModelLayers.PRIMORDIAL_ARMOR));
        HAZMAT_ARMOR_MODEL = new HazmatArmorModel(Minecraft.getInstance().getEntityModels().bakeLayer(ACModelLayers.HAZMAT_ARMOR));
        DIVING_ARMOR_MODEL = new DivingArmorModel(Minecraft.getInstance().getEntityModels().bakeLayer(ACModelLayers.DIVING_ARMOR));
        DARKNESS_ARMOR_MODEL = new DarknessArmorModel(Minecraft.getInstance().getEntityModels().bakeLayer(ACModelLayers.DARKNESS_ARMOR));
        RAINBOUNCE_ARMOR_MODEL = new RainbounceArmorModel(Minecraft.getInstance().getEntityModels().bakeLayer(ACModelLayers.RAINBOUNCE_ARMOR));
        GINGERBREAD_ARMOR_MODEL = new GingerbreadArmorModel(Minecraft.getInstance().getEntityModels().bakeLayer(ACModelLayers.GINGERBREAD_ARMOR));
    }

    // ── The loader hook ───────────────────────────────────────────────────────────────────────────
    // 1.21.2 rewrote it around the render state, and the two loaders rewrote it differently.
    //
    // NeoForge went furthest: the LivingEntity is gone from the signature, the slot became an
    // EquipmentModel.LayerType, and the return type widened to Model. The entity is what picks the
    // animated variants below, and that API can no longer supply one — so the arm passes null and
    // the three animated sets fall back to their static model. The mixin that actually draws this
    // mod's armour does have an entity (off the render-state duck) and calls getACArmorModel
    // directly, so nothing this mod renders loses its animation; only a third party reaching for
    // AC's armour model through the loader hook would see the static pose.
    //
    // Forge only swapped the entity for the state it was extracted from and left the rest alone, and
    // the same duck hands the entity straight back, so its arm keeps the animation.
    //? if forge && >=1.21.2 {
    /*@Override
    public HumanoidModel<?> getHumanoidArmorModel(net.minecraft.client.renderer.entity.state.LivingEntityRenderState renderState, ItemStack itemStack, EquipmentSlot armorSlot, HumanoidModel<?> _default) {
        net.minecraft.world.entity.Entity entity = com.github.alexmodguy.alexscaves.client.render.compat.ACStateAccess.entity(renderState);
        return (HumanoidModel<?>) getACArmorModel(entity instanceof LivingEntity living ? living : null, itemStack, _default);
    }
    *///?} elif >=1.21.2 {
    /*@Override
    public Model getHumanoidArmorModel(ItemStack itemStack, net.minecraft.world.item.equipment.EquipmentModel.LayerType layerType, Model _default) {
        return getACArmorModel(null, itemStack, _default);
    }
    *///?} else {
    @Override
    public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entityLiving, ItemStack itemStack, EquipmentSlot armorSlot, HumanoidModel<?> _default) {
        return (HumanoidModel<?>) getACArmorModel(entityLiving, itemStack, _default);
    }
    //?}

    /**
     * The armour model for {@code itemStack}, animated for {@code entityLiving} where the set has an
     * animated variant. {@code null} is a legal entity and means "no animation" — the pre-1.21.2
     * hook was already documented as nullable, and the 1.21.2 hook can never supply one.
     *
     * <p>The slot is not a parameter because it never was one in effect: every set's model covers
     * all four pieces and {@code HumanoidArmorLayer} hides the parts it does not want.
     */
    public static Model getACArmorModel(LivingEntity entityLiving, ItemStack itemStack, Model _default) {
        if (!init) {
            initializeModels();
        }
        if (itemStack.getItem() instanceof PrimordialArmorItem) {
            return entityLiving == null ? PRIMORDIAL_ARMOR_MODEL : PRIMORDIAL_ARMOR_MODEL.withAnimations(entityLiving);
        }
        if (itemStack.getItem() instanceof HazmatArmorItem) {
            return entityLiving == null ? HAZMAT_ARMOR_MODEL : HAZMAT_ARMOR_MODEL.withAnimations(entityLiving);
        }
        if (itemStack.getItem() instanceof DivingArmorItem) {
            return DIVING_ARMOR_MODEL;
        }
        if (itemStack.getItem() instanceof DarknessArmorItem) {
            return entityLiving == null ? DARKNESS_ARMOR_MODEL : DARKNESS_ARMOR_MODEL.withAnimations(entityLiving);
        }
        if (itemStack.getItem() instanceof RainbounceBootsItem) {
            return RAINBOUNCE_ARMOR_MODEL;
        }
        if (itemStack.getItem() instanceof GingerbreadArmorItem) {
            return GINGERBREAD_ARMOR_MODEL;
        }
        return _default;
    }

    // Takes a plain Item, not an ArmorItem: the parameter is only ever instanceof-tested against
    // this mod's own classes, and 1.21.5 deleted ArmorItem.
    public static void renderCustomArmor(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, ItemStack itemStack, Item armorItem, Model armorModel, boolean legs, ResourceLocation texture) {
        // Keyed on the item class rather than on the material. From 1.20.5 getMaterial() returns a
        // Holder around a freshly built record, so an identity check against ACItemRegistry's
        // ACArmorMaterial is not a compile error — Holder is an interface — but it can never be
        // true, and the two custom sets would have silently stopped drawing.
        if(armorItem instanceof DarknessArmorItem){
            VertexConsumer vertexconsumer1 = itemStack.hasFoil() ? VertexMultiConsumer.create(multiBufferSource.getBuffer(RenderType.entityGlintDirect()), multiBufferSource.getBuffer(RenderType.entityTranslucent(texture))) : multiBufferSource.getBuffer(RenderType.entityTranslucent(texture));
            ACClientCompat.renderToBuffer(armorModel, poseStack, vertexconsumer1, light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            VertexConsumer vertexconsumer2 = multiBufferSource.getBuffer(ACRenderTypes.getEyesAlphaEnabled(DARKNESS_ARMOR_GLOW));
            ACClientCompat.renderToBuffer(armorModel, poseStack, vertexconsumer2, 240, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        }else if(armorItem instanceof RainbounceBootsItem){
            VertexConsumer vertexconsumer1 = itemStack.hasFoil() ? VertexMultiConsumer.create(multiBufferSource.getBuffer(RenderType.entityGlintDirect()), multiBufferSource.getBuffer(ACRenderTypes.getTeslaBulb(texture))) : multiBufferSource.getBuffer(ACRenderTypes.getTeslaBulb(texture));
            ACClientCompat.renderToBuffer(armorModel, poseStack, vertexconsumer1, 240, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        }else{
            // Plain armour, drawn the way HumanoidArmorLayer#renderModel would have. The other four
            // sets (primordial, hazmat, diving, gingerbread) used to reach that vanilla path and
            // override only the texture, through Forge's IForgeItem#getArmorTexture. From 1.20.5
            // there is no such hook and ACArmorMaterial.vanilla() hands the layer an empty
            // `layers()` list, so vanilla drew *nothing* and all four were invisible. They are
            // CustomArmorPostRender now, which routes them here instead; none of them is dyeable or
            // trimmable, so a single armorCutoutNoCull draw is the whole of what vanilla did.
            VertexConsumer vertexconsumer1 = ACClientCompat.armorFoilBuffer(multiBufferSource, RenderType.armorCutoutNoCull(texture), itemStack.hasFoil());
            ACClientCompat.renderToBuffer(armorModel, poseStack, vertexconsumer1, light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
