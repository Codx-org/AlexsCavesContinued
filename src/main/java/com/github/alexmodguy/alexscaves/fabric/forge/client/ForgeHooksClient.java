package com.github.alexmodguy.alexscaves.fabric.forge.client;

//? if <1.21.2 {
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
//?}

/**
 * Fabric stand-in for the loader's client hook bag. Exactly one of its ~forty methods is reached
 * from this tree — {@link #getArmorModel} — and only below 1.21.2, from
 * {@code mixin.client.HumanoidArmorLayerMixin}'s oldest arm.
 *
 * <p>The loader's implementation is a one-line delegate: it asks the stack's client item extension
 * for {@code getGenericArmorModel(entity, stack, slot, _default)}, whose own default answers
 * {@code _default} after copying the humanoid pose onto it. No item in this mod overrides that hook
 * — the six armour sets go through {@code CustomArmorPostRender} and
 * {@code ACArmorRenderProperties} instead, which is the path the mixin cancels into two lines
 * later. So handing back the model the caller already had is not an approximation of the loader's
 * behaviour here; it is the same behaviour.
 *
 * <p>The parameters are kept rather than trimmed to the one that is used, because the point of a
 * stand-in is that the call site is spelled identically on every loader, and this one is a mixin
 * arm shared with Forge and NeoForge.
 *
 * <p>Above 1.21.2 the whole call site is gone (the armour layer's render signature changed and the
 * newer arms build the model themselves), so the class is an empty shell there — same shape as
 * {@code BakedModelWrapper} above 1.21.4. It is left importable on every node so the mixin's import
 * needs no gate of its own.
 */
public final class ForgeHooksClient {

    private ForgeHooksClient() {
    }

    //? if <1.21.2 {
    public static Model getArmorModel(LivingEntity entityLiving, ItemStack itemStack, EquipmentSlot slot, HumanoidModel<?> _default) {
        return _default;
    }
    //?}
}
