package com.github.alexmodguy.alexscaves.client.render.item;

import com.github.alexmodguy.alexscaves.server.item.CandyCaneHookItem;
import com.github.alexmodguy.alexscaves.server.item.HolocoderItem;
import com.github.alexmodguy.alexscaves.server.item.RemoteDetonatorItem;
import com.github.alexmodguy.alexscaves.server.item.SackOfSatingItem;
import com.github.alexmodguy.alexscaves.server.item.TotemOfPossessionItem;
import com.github.alexmodguy.alexscaves.server.misc.ACCompat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The eleven dynamic item-model values Alex's Caves draws with, in one version-neutral place.
 *
 * <p>Below 1.21.4 these are the bodies of the {@code ItemProperties.register} lambdas in
 * {@code ClientProxy#clientInit}, matched against the thresholds in each item model's
 * {@code overrides} list. From 1.21.4 both of those mechanisms are gone: the value is fetched
 * through a {@code minecraft:range_dispatch} entry in the item's model definition, resolved by the
 * mod's own {@code alexscaves:legacy} range property (see {@code ACItemModelShims}), against the
 * very same thresholds — {@code DataPackMigration} rewrites the override lists into dispatch
 * entries verbatim.
 *
 * <p>Both eras call in here so the two cannot drift apart. Nine methods for eleven registrations:
 * the three spears share {@link #throwing} and so the signature is uniform —
 * {@code (stack, level, holder)}, the three arguments both APIs supply — even where a body ignores
 * some of them.
 */
public final class ACItemPredicates {

    private ACItemPredicates() {
    }

    public static float bound(ItemStack stack, ClientLevel level, LivingEntity holder) {
        return HolocoderItem.isBound(stack) ? 1.0F : 0.0F;
    }

    public static float nugget(ItemStack stack, ClientLevel level, LivingEntity holder) {
        return (stack.getCount() % 4) / 4F;
    }

    public static float throwing(ItemStack stack, ClientLevel level, LivingEntity holder) {
        return holder != null && holder.isUsingItem() && holder.getUseItem() == stack ? 1.0F : 0.0F;
    }

    public static float active(ItemStack stack, ClientLevel level, LivingEntity holder) {
        return RemoteDetonatorItem.isActive(stack) ? 1.0F : 0.0F;
    }

    public static float tooting(ItemStack stack, ClientLevel level, LivingEntity holder) {
        return holder != null && holder.isUsingItem() && holder.getUseItem() == stack ? 1.0F : 0.0F;
    }

    public static float charging(ItemStack stack, ClientLevel level, LivingEntity holder) {
        return holder != null && holder.isUsingItem() && holder.getUseItem() == stack ? 1.0F : 0.0F;
    }

    public static float totem(ItemStack stack, ClientLevel level, LivingEntity holder) {
        return TotemOfPossessionItem.isBound(stack) ? holder != null && holder.isUsingItem() && holder.getUseItem() == stack ? 1.0F : 0.5F : 0.0F;
    }

    public static float cast(ItemStack stack, ClientLevel level, LivingEntity holder) {
        return holder != null && CandyCaneHookItem.isActive(stack) ? 1.0F : 0.0F;
    }

    public static float open(ItemStack stack, ClientLevel level, LivingEntity holder) {
        return level != null && SackOfSatingItem.isChewing(stack, level.getGameTime()) ? 1.0F : ACCompat.getTag(stack) == null || holder instanceof Player player && player.containerMenu != null && SackOfSatingItem.calculateWholeStackHungerValue(player.containerMenu.getCarried(), player) > 0 ? 0.5F : 0.0F;
    }
}
