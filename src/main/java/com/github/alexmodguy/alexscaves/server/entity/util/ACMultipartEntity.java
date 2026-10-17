package com.github.alexmodguy.alexscaves.server.entity.util;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.message.MultipartEntityMessage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.entity.PartEntity;

public abstract class ACMultipartEntity<T extends Entity> extends PartEntity<T> {

    public ACMultipartEntity(T parent) {
        super(parent);
        this.blocksBuilding = true;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        Entity parent = this.getParent();
        if (parent == null) {
            return InteractionResult.PASS;
        } else {
            if (player.level().isClientSide()) {
                AlexsCaves.sendMSGToServer(new MultipartEntityMessage(parent.getId(), player.getId(), 0, 0));
            }
            return parent.interact(player, hand);
        }
    }

    @Override
    public boolean save(CompoundTag tag) {
        return false;
    }


    @Override
    public boolean canBeCollidedWith() {
        Entity parent = this.getParent();
        return parent != null && parent.canBeCollidedWith();
    }


    @Override
    public boolean isPickable() {
        Entity parent = this.getParent();
        return parent != null && parent.isPickable();
    }


    // 1.21.2 renames this to hurtServer (the !mc2102-hurt-* rules) and makes it server-only, which
    // is exactly where the client round-trip below stops working: the whole point of the message is
    // that the client is the side that notices the hit. From that version it no longer has to be —
    // the server resolves a part entity from the interact packet (ServerLevel#getEntity falls
    // through to its part index) and calls Player#attack on it, so the parent can be hurt right
    // here, with the same generic damage of the same size MultipartEntityMessage's handler applied.
    // Its distance check goes with it; handleInteract has already range-checked the attack.
    //
    // this.isInvulnerableTo becomes isInvulnerableToBase for the same reason it does in every other
    // Entity subclass in this tree — see the !mc2102-invulnerable-* rules, which describe only the
    // LivingEntity half of that split and deliberately leave this half hand-written.
    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity parent = this.getParent();
        //? if >=1.21.2 {
        /*if (!this.isInvulnerableToBase(source) && parent != null && source.getEntity() != null) {
            parent.hurt(parent.damageSources().generic(), amount);
        }
        *///?} else {
        if (!this.isInvulnerableTo(source) && parent != null) {
            Entity player = source.getEntity();
            if (player != null && player.level().isClientSide()) {
                AlexsCaves.sendMSGToServer(new MultipartEntityMessage(parent.getId(), player.getId(), 1, amount));
            }
        }
        //?}
        return false;
    }

    @Override
    public boolean is(Entity entityIn) {
        return this == entityIn || this.getParent() == entityIn;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void defineSynchedData() {

    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {

    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {

    }

    public boolean shouldBeSaved() {
        return false;
    }
}
