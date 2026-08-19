package com.github.alexmodguy.alexscaves.server.entity.item;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;
import com.github.alexmodguy.alexscaves.server.entity.ACEntityRegistry;
import com.github.alexmodguy.alexscaves.server.entity.util.AlexsCavesBoat;
import com.github.alexmodguy.alexscaves.server.misc.ACPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class AlexsCavesBoatEntity extends Boat implements AlexsCavesBoat {

    // 1.21.2 split the boat hierarchy — Boat/ChestBoat became thin subclasses of AbstractBoat/
    // AbstractChestBoat — and deleted the Boat.Type enum along with the DATA_ID_TYPE accessor that
    // carried it. This mod never used either: it has always kept its own AlexsCavesBoat.Type in
    // that same accessor and stubbed out the vanilla variant pair. So from 1.21.2 it simply owns
    // the accessor outright, under the same name, and every call site below is untouched.
    //? if >=1.21.2 {
    /*private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> DATA_ID_TYPE =
            net.minecraft.network.syncher.SynchedEntityData.defineId(AlexsCavesBoatEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ID_TYPE, 0);
    }
    *///?}

    public AlexsCavesBoatEntity(EntityType type, Level level) {
        //? if >=1.21.2 {
        /*this(type, level, new AlexsCavesBoatEntity[1]);
        *///?} else {
        super(type, level);
        this.blocksBuilding = true;
        //?}
    }

    /**
     * 1.21.2 took the item a boat drops out of {@code getDropItem()} — which is {@code final} on
     * {@code AbstractBoat} now, as is {@code getPickResult()} — and made it a {@code Supplier<Item>}
     * constructor argument. This boat's item depends on its {@link AlexsCavesBoat.Type}, which lives
     * in synched data and so is only readable once the entity exists.
     *
     * <p>Hence the one-element array: the supplier closes over the array rather than over
     * {@code this}, which an explicit constructor invocation may not reference, and the array is
     * filled in the moment {@code super} returns. Both consumers run long after that.
     */
    //? if >=1.21.2 {
    /*private AlexsCavesBoatEntity(EntityType type, Level level, AlexsCavesBoatEntity[] self) {
        super(type, level, dropItemOf(self));
        self[0] = this;
        this.blocksBuilding = true;
    }

    private static java.util.function.Supplier<Item> dropItemOf(AlexsCavesBoatEntity[] self) {
        return () -> self[0].getACBoatType().getDropSupplier().get();
    }
    *///?}


    public AlexsCavesBoatEntity(Level level, double x, double y, double z) {
        this(ACEntityRegistry.BOAT.get(), level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    public AlexsCavesBoatEntity(Level level, Vec3 location, AlexsCavesBoat.Type type) {
        this(level, location.x, location.y, location.z);
        this.setACBoatType(type);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return ACPlatform.getEntitySpawningPacket(this);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putString("ACBoatType", getACBoatType().getName());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        if (nbt.contains("ACBoatType")) {
            this.entityData.set(DATA_ID_TYPE, AlexsCavesBoat.Type.byName(ACCompat.getString(nbt, "ACBoatType")).ordinal());
        }
    }

    /**
     * Upstream copied vanilla's "a boat falling more than three blocks breaks into planks and
     * sticks" so it could drop <em>this</em> boat's planks instead of oak.
     *
     * <p>1.21.2 deleted that behaviour from vanilla: {@code AbstractBoat#checkFallDamage} now only
     * resets the fall distance, and the two fields this body reads ({@code status}, {@code lastYd})
     * are private to it. So from 1.21.2 the override is dropped entirely — inheriting vanilla's
     * method is exactly the intended behaviour, since there is no longer a drop to correct.
     */
    //? if <1.21.2 {
    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        this.lastYd = this.getDeltaMovement().y;
        if (!this.isPassenger()) {
            if (onGround) {
                if (this.fallDistance > 3.0F) {
                    if (this.status != Boat.Status.ON_LAND) {
                        this.resetFallDistance();
                        return;
                    }

                    this.causeFallDamage(this.fallDistance, 1.0F, this.damageSources().fall());
                    if (!this.level().isClientSide() && !this.isRemoved()) {
                        this.kill();
                        if (ACCompat.gameRule(this.level(), GameRules.RULE_DOENTITYDROPS, true)) {
                            for (int i = 0; i < 3; ++i) {
                                ACCompat.spawnAtLocation(this, this.getACBoatType().getPlankSupplier().get());
                            }

                            for (int j = 0; j < 2; ++j) {
                                ACCompat.spawnAtLocation(this, Items.STICK);
                            }
                        }
                    }
                }

                this.resetFallDistance();
            } else if (!this.level().getFluidState(this.blockPosition().below()).is(FluidTags.WATER) && y < 0.0D) {
                this.fallDistance -= (float) y;
            }
        }
    }

    //?}

    // From 1.21.2 this is final on AbstractBoat and answers from the constructor's supplier —
    // see dropItemOf above, which hands it the same item this returned.
    //? if <1.21.2 {
    @Override
    public Item getDropItem() {
        return getACBoatType().getDropSupplier().get();
    }
    //?}

    public void setACBoatType(AlexsCavesBoat.Type type) {
        this.entityData.set(DATA_ID_TYPE, type.ordinal());
    }

    public AlexsCavesBoat.Type getACBoatType() {
        return AlexsCavesBoat.Type.byId(this.entityData.get(DATA_ID_TYPE));
    }

    // The vanilla wood variant, stubbed out because this boat's appearance comes from its own type.
    // 1.21.2 removed the Boat.Type enum and both accessors, so the stubs go with them.
    //? if <1.21.2 {
    @Override
    public void setVariant(Boat.Type vanillaType) {
    }

    @Override
    public Boat.Type getVariant() {
        return Boat.Type.OAK;
    }
    //?}

}