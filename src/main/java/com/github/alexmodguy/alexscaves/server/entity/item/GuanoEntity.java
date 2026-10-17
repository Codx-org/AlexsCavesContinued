package com.github.alexmodguy.alexscaves.server.entity.item;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;
import com.github.alexmodguy.alexscaves.server.entity.ACEntityRegistry;
import com.github.alexmodguy.alexscaves.server.item.ACItemRegistry;
import com.github.alexmodguy.alexscaves.server.misc.ACPlatform;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class GuanoEntity extends ThrowableItemProjectile {

    public GuanoEntity(EntityType entityType, Level level) {
        super(entityType, level);
    }


    public GuanoEntity(Level level, LivingEntity thrower) {
        // 1.21.2 folded the thrown stack into every ThrowableItemProjectile constructor: the item
        // slot used to be seeded from getDefaultItem() and left alone, and it is now a required
        // argument. It is the same stack either way — getDefaultItem() is exactly what vanilla
        // seeded — it just cannot be spelled that way here, an instance method being unavailable
        // before super() runs, so the registry entry it returns is named directly.
        //? if >=1.21.2 {
        /*super(ACEntityRegistry.GUANO.get(), thrower, level, new net.minecraft.world.item.ItemStack(ACItemRegistry.GUANO.get()));
        *///?} else {
        super(ACEntityRegistry.GUANO.get(), thrower, level);
        //?}
    }

    public GuanoEntity(Level level, double x, double y, double z) {
        //? if >=1.21.2 {
        /*super(ACEntityRegistry.GUANO.get(), x, y, z, level, new net.minecraft.world.item.ItemStack(ACItemRegistry.GUANO.get()));
        *///?} else {
        super(ACEntityRegistry.GUANO.get(), x, y, z, level);
        //?}
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return ACPlatform.getEntitySpawningPacket(this);
    }

    public void handleEntityEvent(byte message) {
        if (message == 3) {
            double d0 = 0.08D;
            for (int i = 0; i < 8; ++i) {
                this.level().addParticle(ACCompat.itemParticle(ParticleTypes.ITEM, this.getItem()), this.getX(), this.getY(), this.getZ(), ((double) this.random.nextFloat() - 0.5D) * 0.08D, ((double) this.random.nextFloat() - 0.5D) * 0.08D, ((double) this.random.nextFloat() - 0.5D) * 0.08D);
            }
        }
    }

    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        hitResult.getEntity().hurt(damageSources().thrown(this, this.getOwner()), 0F);
    }

    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (!this.level().isClientSide()) {
            this.level().broadcastEntityEvent(this, (byte) 3);
            this.discard();
        }

    }

    protected Item getDefaultItem() {
        return ACItemRegistry.GUANO.get();
    }
}