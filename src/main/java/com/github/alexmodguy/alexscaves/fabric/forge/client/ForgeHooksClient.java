package com.github.alexmodguy.alexscaves.fabric.forge.client;

//? if <1.21.2 {
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import com.github.alexmodguy.alexscaves.server.item.ACClientExtensionItem;
//?}

/**
 * Fabric stand-in for the loader's client hook bag. Exactly one of its ~forty methods is reached
 * from this tree — {@link #getArmorModel} — and only below 1.21.2, from
 * {@code mixin.client.HumanoidArmorLayerMixin}'s oldest arm.
 *
 * <p>The loader's implementation is a two-step delegate, read out of Forge 47.1.65's bytecode: it
 * asks the stack's client item extension for {@code getGenericArmorModel(entity, stack, slot,
 * _default)}, whose default asks the same object for {@code getHumanoidArmorModel} and, when that
 * hands back a different model, copies the humanoid pose onto it. This mod's six armour sets DO
 * override {@code getHumanoidArmorModel} — {@code ACArmorRenderProperties} answers it with the set's
 * own baked model — and the mixin arm draws whatever this method returns. So this has to perform the
 * same lookup. It used to return {@code _default} on the theory that nothing overrides the hook,
 * which made every Fabric node below 1.21.2 draw the WEARER's own model (the player model, or the
 * armour stand's body with its base plate and sticks) under the 128x128 armour texture: report 13's
 * "gingerbread armour model is screwed up", true of all six sets on those eight nodes.
 *
 * <p>The pose copy is left to the caller, which does it unconditionally right after this returns, as
 * it must on the other two loaders too.
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
        if (!(itemStack.getItem() instanceof ACClientExtensionItem extensionItem)) {
            return _default;
        }
        // initializeClient is Consumer-shaped (the loaders' own signature), so the answer comes back
        // through a one-slot holder. An item that accepts no extension object, or a null one (the
        // armour sets hand over AlexsCaves.PROXY.getArmorProperties(), null on CommonProxy), leaves
        // _default.
        Model[] armorModel = {_default};
        extensionItem.initializeClient(extensions -> {
            if (extensions != null) {
                armorModel[0] = extensions.getHumanoidArmorModel(entityLiving, itemStack, slot, _default);
            }
        });
        return armorModel[0] != null ? armorModel[0] : _default;
    }
    //?}
}
