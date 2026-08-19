package com.github.alexmodguy.alexscaves.server.entity.item;

import com.github.alexmodguy.alexscaves.server.entity.ACEntityRegistry;
import com.github.alexmodguy.alexscaves.server.item.ACItemRegistry;
import com.github.alexmodguy.alexscaves.server.misc.ACPlatform;
import com.github.alexmodguy.alexscaves.server.misc.ACSoundRegistry;
import com.github.alexmodguy.alexscaves.server.misc.ACTagRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class BurrowingArrowEntity extends AbstractArrow {
    private static final EntityDataAccessor<Integer> DUG_BLOCK_COUNT = SynchedEntityData.defineId(BurrowingArrowEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DIGGING = SynchedEntityData.defineId(BurrowingArrowEntity.class, EntityDataSerializers.BOOLEAN);

    private float prevDiggingProgress;
    private float diggingProgress;

    private int miningTime = 0;
    private int lastMineBlockBreakProgress = -1;

    private int soundTime = 0;
    private BlockPos hitPos;

    public BurrowingArrowEntity(EntityType entityType, Level level) {
        // 1.20.3 gave AbstractArrow the stack it drops when picked up, stored and saved on the
        // entity. This mod's arrows answer that with their own getPickupItem() override on every
        // version, so the stack passed here only has to match it.
        //
        // 1.21 then took it off the two-argument form again and appended a nullable weapon stack to
        // the other two, the item the shot was fired from, which vanilla reads for the piercing and
        // punch enchantments. These arrows are not fired from anything the game should enchant off,
        // so they pass null and keep deciding their own pierce level.
        //? if >=1.21
        /*super(entityType, level);*/
        //? if >=1.20.3 && <1.21
        /*super(entityType, level, new ItemStack(ACItemRegistry.BURROWING_ARROW.get()));*/
        //? if <1.20.3
        super(entityType, level);
        this.setBaseDamage(3.5D);
        // setPierceLevel went private in 1.21 — see AbstractArrowAccessor.
        ((com.github.alexmodguy.alexscaves.mixin.AbstractArrowAccessor) this).ac$setPierceLevel((byte)(this.getPierceLevel() + 1));
    }

    public BurrowingArrowEntity(Level level, LivingEntity shooter) {
        //? if >=1.21
        /*super(ACEntityRegistry.BURROWING_ARROW.get(), shooter, level, new ItemStack(ACItemRegistry.BURROWING_ARROW.get()), null);*/
        //? if >=1.20.3 && <1.21
        /*super(ACEntityRegistry.BURROWING_ARROW.get(), shooter, level, new ItemStack(ACItemRegistry.BURROWING_ARROW.get()));*/
        //? if <1.20.3
        super(ACEntityRegistry.BURROWING_ARROW.get(), shooter, level);
        this.setBaseDamage(3.5D);
        // setPierceLevel went private in 1.21 — see AbstractArrowAccessor.
        ((com.github.alexmodguy.alexscaves.mixin.AbstractArrowAccessor) this).ac$setPierceLevel((byte)(this.getPierceLevel() + 1));
    }

    public BurrowingArrowEntity(Level level, double x, double y, double z) {
        //? if >=1.21
        /*super(ACEntityRegistry.BURROWING_ARROW.get(), x, y, z, level, new ItemStack(ACItemRegistry.BURROWING_ARROW.get()), null);*/
        //? if >=1.20.3 && <1.21
        /*super(ACEntityRegistry.BURROWING_ARROW.get(), x, y, z, level, new ItemStack(ACItemRegistry.BURROWING_ARROW.get()));*/
        //? if <1.20.3
        super(ACEntityRegistry.BURROWING_ARROW.get(), x, y, z, level);
        this.setBaseDamage(3.5D);
    }


    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DUG_BLOCK_COUNT, 0);
        this.entityData.define(DIGGING, false);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return ACPlatform.getEntitySpawningPacket(this);
    }

    public void tick() {
        super.tick();
        this.prevDiggingProgress = diggingProgress;
        if (this.isDigging() && diggingProgress < 5F) {
            diggingProgress++;
        }
        if (!this.isDigging() && diggingProgress > 0F) {
            diggingProgress--;
        }
        if (this.inGround && hitPos != null && canMine(hitPos)) {
            this.setDigging(true);
            BlockState state = level().getBlockState(hitPos);
            int hardness = (int) (Math.max(state.getDestroySpeed(level(), hitPos), 0.2F) * 15F);
            int i = (int) ((float) this.miningTime / hardness * 10.0F);

            if (i != lastMineBlockBreakProgress) {
                this.level().destroyBlockProgress(this.getId(), hitPos, i);
                lastMineBlockBreakProgress = i;
            }
            if (miningTime % 8 == 0) {
                this.playSound(state.getSoundType().getHitSound());
            }
            Vec3 centerOf = Vec3.atCenterOf(hitPos).subtract(this.position());
            if (miningTime++ > hardness) {
                this.level().destroyBlock(hitPos, true);
                this.setDugBlockCount(this.getDugBlockCount() + 1);
                miningTime = 0;
                lastMineBlockBreakProgress = -1;
                this.setDeltaMovement(centerOf.normalize().scale(0.3F));
            } else {
                this.setDeltaMovement(centerOf.scale(0.2F));
            }
        } else {
            if (hitPos != null) {
                this.level().destroyBlockProgress(this.getId(), hitPos, -1);
                hitPos = null;
            }
            this.setDigging(false);
        }
        if(this.isDigging()){
            if(soundTime-- <= 0){
                soundTime = 38;
                this.playSound(ACSoundRegistry.CORRODENT_TEETH.get());
            }
        }
    }

    protected void tickDespawn() {
        if (!isDigging()) {

            super.tickDespawn();
        }
    }

    @Override
    public void startFalling() {
        // See DarkArrowEntity#startFalling — the field write has to be gated by hand.
        //? if >=1.21.2 {
        /*this.setInGround(false);
        *///?} else {
        this.inGround = false;
        //?}
    }

    @Override
    public void remove(Entity.RemovalReason removalReason) {
        if (hitPos != null) {
            this.level().destroyBlockProgress(this.getId(), hitPos, -1);
            hitPos = null;
        }
        super.remove(removalReason);
    }

    private boolean canMine(BlockPos hitPos) {
        Entity owner = this.getOwner();
        if(owner != null && !(owner instanceof Player)){
            return false;
        }
        BlockState state = level().getBlockState(hitPos);
        return !state.is(ACTagRegistry.UNMOVEABLE) && state.getFluidState().isEmpty() && state.getDestroySpeed(level(), hitPos) != -1.0F && this.getDugBlockCount() < 5;
    }

    @Override
    protected void onHitBlock(BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
        hitPos = blockHitResult.getBlockPos();
    }

    protected ItemStack getPickupItem() {
        return new ItemStack(ACItemRegistry.BURROWING_ARROW.get());
    }

    private int getDugBlockCount() {
        return this.entityData.get(DUG_BLOCK_COUNT);
    }

    private void setDugBlockCount(int count) {
        this.entityData.set(DUG_BLOCK_COUNT, count);
    }

    private boolean isDigging() {
        return this.entityData.get(DIGGING);
    }

    private void setDigging(boolean digging) {
        this.entityData.set(DIGGING, digging);
    }

    public float getDiggingAmount(float partialTicks) {
        return (prevDiggingProgress + (diggingProgress - prevDiggingProgress) * partialTicks) * 0.2F;
    }
}
