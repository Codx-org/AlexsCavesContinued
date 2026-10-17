package com.github.alexmodguy.alexscaves.server.item;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;
import com.github.alexmodguy.alexscaves.server.entity.item.AlexsCavesBoatEntity;
import com.github.alexmodguy.alexscaves.server.entity.item.AlexsCavesChestBoatEntity;
import com.github.alexmodguy.alexscaves.server.entity.util.AlexsCavesBoat;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
//? if <1.21.2
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Predicate;

public class CaveBoatItem extends Item {
    private static final Predicate<Entity> ENTITY_PREDICATE = EntitySelector.NO_SPECTATORS.and(Entity::isPickable);
    private final AlexsCavesBoat.Type type;
    private final boolean hasChest;

    public CaveBoatItem(boolean hasChest, AlexsCavesBoat.Type type, Item.Properties properties) {
        super(properties);
        this.type = type;
        this.hasChest = hasChest;
    }

    @Override
    // 1.21.2 merged the result-plus-stack pair back into a plain InteractionResult.
    //? if >=1.21.2
    /*public net.minecraft.world.InteractionResult use(Level level, Player player, InteractionHand hand) {*/
    //? if <1.21.2
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        HitResult hitresult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hitresult.getType() == HitResult.Type.MISS) {
            return ACCompat.usePass(itemstack);
        } else {
            Vec3 vec3 = player.getViewVector(1.0F);
            double d0 = 5.0D;
            List<Entity> list = level.getEntities(player, player.getBoundingBox().expandTowards(vec3.scale(5.0D)).inflate(1.0D), ENTITY_PREDICATE);
            if (!list.isEmpty()) {
                Vec3 vec31 = player.getEyePosition();

                for (Entity entity : list) {
                    AABB aabb = entity.getBoundingBox().inflate((double) entity.getPickRadius());
                    if (aabb.contains(vec31)) {
                        return ACCompat.usePass(itemstack);
                    }
                }
            }

            if (hitresult.getType() == HitResult.Type.BLOCK) {
                // Declared as the Entity it is placed as, not as a Boat: 1.21.2 split the chest
                // boats onto their own branch under the new AbstractBoat, so the two sides of this
                // conditional stopped sharing Boat as a supertype. Nothing below asks for more than
                // an entity — rotate it, check it fits, add it — so the wider declaration is the
                // same code on every version rather than a gated pair.
                Entity boat = this.hasChest ? new AlexsCavesChestBoatEntity(level, hitresult.getLocation(), type) : new AlexsCavesBoatEntity(level, hitresult.getLocation(), type);
                boat.setYRot(player.getYRot());

                if (!level.noCollision(boat, boat.getBoundingBox())) {
                    return ACCompat.useFail(itemstack);
                } else {
                    if (!level.isClientSide()) {
                        level.addFreshEntity(boat);
                        level.gameEvent(player, GameEvent.ENTITY_PLACE, hitresult.getLocation());
                        if (!player.getAbilities().instabuild) {
                            itemstack.shrink(1);
                        }
                    }

                    player.awardStat(Stats.ITEM_USED.get(this));
                    return ACCompat.useSidedSuccess(itemstack, level.isClientSide());
                }
            } else {
                return ACCompat.usePass(itemstack);
            }
        }
    }
}
