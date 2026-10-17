package com.github.alexmodguy.alexscaves.mixin.fabric;

import com.github.alexmodguy.alexscaves.server.item.ACSwingControlItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric's dispatcher for the second half of {@link ACSwingControlItem} — the veto an item holds over
 * its own arm animation.
 *
 * <p>{@code Item#onEntitySwing} is a <b>loader patch</b> with no vanilla counterpart. Unlike its
 * sibling {@code onLeftClickEntity}, this one Forge does splice into the vanilla method itself: the
 * 1.20.1 disassembly of {@code LivingEntity#swing(InteractionHand, boolean)} opens with
 *
 * <pre>
 * ItemStack stack = this.getItemInHand(hand);
 * if (!stack.isEmpty() &amp;&amp; stack.onEntitySwing(this)) return;
 * </pre>
 *
 * ahead of vanilla's own {@code this.swinging} test, and is byte-identical from there on — so a
 * cancellable {@code @At("HEAD")} inject is the whole patch. {@code ItemStack#onEntitySwing} is
 * itself only a delegate to {@code getItem().onEntitySwing(stack, entity)}.
 *
 * <p>⚠️ The hook is called for its <em>side effects</em> as much as for its verdict — the primitive
 * club resets {@code swingTime} on the branch where it returns {@code false} — so it must be invoked
 * whenever the item implements the interface, and only the {@code true} answer cancels. Do not
 * "optimise" this into a check that skips the call.
 *
 * <p>⚠️ Written against 1.20.1; re-derive the anchor per new Fabric band. 26.3 is the first band
 * that moved it: swinging became a {@code SwingAnimation} item component, so {@code swing} gained a
 * third parameter <em>and</em> changed its return type from {@code void} to {@code boolean}, which is
 * why that arm needs a {@code CallbackInfoReturnable} and cannot share the handler below. The value
 * to return is not a guess — the 26.3 disassembly of the loader patch is
 * {@code if (!stack.isEmpty() && stack.onEntitySwing(this, hand)) return true;}, so the veto answers
 * {@code true} there, where below it the method simply returned.
 *
 * <p>The verdict lives in one {@code @Unique} helper rather than in each arm, because gates do not
 * nest: the selector and the handler signature both differ per band, so the two arms have to be
 * whole sibling members, and duplicating the hook call into both is exactly how the side-effect rule
 * above gets broken later.
 */
@Mixin(LivingEntity.class)
public class LivingEntitySwingMixin {

    //? if >=26.3 {
    /*@Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/component/SwingAnimation;Z)Z", at = @At("HEAD"), cancellable = true)
    private void ac_onEntitySwing(InteractionHand hand, net.minecraft.world.item.component.SwingAnimation animation, boolean updateSelf, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (ac_swingVetoed(hand)) {
            cir.setReturnValue(true);
        }
    }
    *///?} else {
    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @At("HEAD"), cancellable = true)
    private void ac_onEntitySwing(InteractionHand hand, boolean updateSelf, CallbackInfo ci) {
        if (ac_swingVetoed(hand)) {
            ci.cancel();
        }
    }
    //?}

    @Unique
    private boolean ac_swingVetoed(InteractionHand hand) {
        LivingEntity self = (LivingEntity) (Object) this;
        ItemStack stack = self.getItemInHand(hand);
        return !stack.isEmpty() && stack.getItem() instanceof ACSwingControlItem typed
                && typed.onEntitySwing(stack, self);
    }
}
