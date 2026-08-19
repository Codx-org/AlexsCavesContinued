package com.github.alexmodguy.alexscaves.server.block.blockentity;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.server.misc.ACPlatform;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.client.particle.ACParticleRegistry;
import com.github.alexmodguy.alexscaves.server.block.ACBlockRegistry;
import com.github.alexmodguy.alexscaves.server.block.NuclearFurnaceBlock;
import com.github.alexmodguy.alexscaves.server.entity.ACEntityRegistry;
import com.github.alexmodguy.alexscaves.server.entity.item.NuclearExplosionEntity;
import com.github.alexmodguy.alexscaves.server.entity.util.FallingBlockEntityAccessor;
import com.github.alexmodguy.alexscaves.server.inventory.NuclearFurnaceMenu;
import com.github.alexmodguy.alexscaves.server.misc.ACAdvancementTriggerRegistry;
import com.github.alexmodguy.alexscaves.server.misc.ACTagRegistry;
import com.github.alexmodguy.alexscaves.server.potion.ACEffectRegistry;
import com.github.alexmodguy.alexscaves.server.recipe.ACRecipeRegistry;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//? if forge
import net.minecraftforge.common.util.LazyOptional;
//? if forge
import net.minecraftforge.items.IItemHandler;
//? if forge
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class NuclearFurnaceBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, ACUpdatePacketReceiver {

    private static final float SPEED_REDUCTION = 0.2F;
    public static int MAX_BARRELING_TIME = 100;
    public static int MAX_WASTE = 1000;

    public int age;
    private int barrelTime = 0;
    private int currentWaste = 0;
    private int fissionTime = 0;
    private int cookTime = 0;
    private int maxCookTime = 0;

    private static final int[] SLOTS_FOR_UP = new int[]{0};
    private static final int[] SLOTS_FOR_DOWN = new int[]{3, 4};
    private static final int[] SLOTS_FOR_LEFT = new int[]{2};
    private static final int[] SLOTS_FOR_RIGHT = new int[]{1};


    protected NonNullList<ItemStack> items = NonNullList.withSize(5, ItemStack.EMPTY);
    // 1.21 replaced the "any Container" recipe input with a typed RecipeInput; a furnace's is
    // SingleRecipeInput, which AbstractCookingRecipe is now parameterised on.
    //? if >=1.21
    /*private final RecipeManager.CachedCheck<net.minecraft.world.item.crafting.SingleRecipeInput, ? extends AbstractCookingRecipe> quickCheck = RecipeManager.createCheck(getRecipeType());*/
    //? if <1.21
    private final RecipeManager.CachedCheck<Container, ? extends AbstractCookingRecipe> quickCheck = RecipeManager.createCheck(getRecipeType());
    private final Object2IntOpenHashMap<ResourceLocation> recipesUsed = new Object2IntOpenHashMap<>();

    private UsedRecipe currentRecipe;

    /**
     * A recipe together with its id. Until 1.20.1 a {@code Recipe} carried its own id; 1.20.2
     * moved it out into a {@code RecipeHolder}, and this furnace needs the id to tally which
     * recipes it has run so it can pop the right experience when the result is taken out.
     */
    private record UsedRecipe(ResourceLocation id, AbstractCookingRecipe recipe) {
    }

    protected final ContainerData dataAccess = new ContainerData() {
        public int get(int type) {
            switch (type) {
                case 0:
                    return currentWaste;
                case 1:
                    return barrelTime;
                case 2:
                    return fissionTime;
                case 3:
                    return cookTime;
                case 4:
                    return maxCookTime;
            }
            return 0;
        }

        public void set(int type, int value) {
            switch (type) {
                case 0:
                    currentWaste = value;
                case 1:
                    barrelTime = value;
                case 2:
                    fissionTime = value;
                case 3:
                    cookTime = value;
                case 4:
                    maxCookTime = value;
            }
        }

        public int getCount() {
            return 5;
        }
    };
    private Player lastInteractedWithPlayer;

    public NuclearFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ACBlockEntityRegistry.NUCLEAR_FURNACE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos blockPos, BlockState state, NuclearFurnaceBlockEntity entity) {
        entity.age++;
        if (entity.getCriticality() >= 3) {
            Vec3 vec3 = entity.getExhaustPos();
            if (!level.isClientSide() && level.getRandom().nextFloat() < 0.2) {
                entity.spreadFire(level, 6);
            }
            level.addAlwaysVisibleParticle(level.getRandom().nextInt(3) == 0 ? ParticleTypes.LAVA : ACParticleRegistry.MUSHROOM_CLOUD_SMOKE.get(), true, vec3.x, vec3.y + 1, vec3.z, (level.getRandom().nextFloat() - 0.5F) * 0.2F, 0.1F + level.getRandom().nextFloat() * 0.2F, (level.getRandom().nextFloat() - 0.5F) * 0.2F);
        } else if (entity.getCriticality() == 2) {
            Vec3 vec3 = entity.getExhaustPos();
            if (!level.isClientSide() && level.getRandom().nextFloat() < 0.05) {
                entity.spreadFire(level, 2);
            }
            level.addAlwaysVisibleParticle(ParticleTypes.LARGE_SMOKE, true, vec3.x, vec3.y, vec3.z, (level.getRandom().nextFloat() - 0.5F) * 0.1F, level.getRandom().nextFloat() * 0.1F, (level.getRandom().nextFloat() - 0.5F) * 0.1F);
        } else if (entity.isUndergoingFission() && level.getRandom().nextFloat() < entity.getCriticality() * 0.35F + 0.15F) {
            Vec3 vec3 = entity.getExhaustPos();
            ParticleOptions particleOptions = ACParticleRegistry.HAZMAT_BREATHE.get();
            if (entity.getCriticality() == 1 && level.getRandom().nextFloat() < 0.1F) {
                particleOptions = ParticleTypes.LARGE_SMOKE;
            }
            level.addAlwaysVisibleParticle(particleOptions, true, vec3.x, vec3.y, vec3.z, (level.getRandom().nextFloat() - 0.5F) * 0.7F, level.getRandom().nextFloat() * 0.1F, (level.getRandom().nextFloat() - 0.5F) * 0.7F);
        }
        if (!level.isClientSide()) {
            boolean flag = false;
            ItemStack cookStack = entity.items.get(0);
            ItemStack rodStack = entity.items.get(1);
            ItemStack barrelStack = entity.items.get(2);
            if (!cookStack.isEmpty()) {
                if (entity.currentRecipe == null || !ACCompat.cookingIngredient(entity.currentRecipe.recipe()).test(cookStack)) {
                    entity.currentRecipe = entity.getRecipeFor(cookStack).orElse(null);
                } else {
                    ItemStack cookResult = ACCompat.recipeResult(entity.currentRecipe.recipe(), level);
                    entity.maxCookTime = Math.max((int) Math.ceil(ACCompat.cookingTime(entity.currentRecipe.recipe()) * getSpeedReduction()), 5);
                    if (entity.canFitInResultSlot(cookResult, 3)) {
                        if (entity.fissionTime <= 0) {
                            if (!rodStack.isEmpty() && rodStack.is(ACTagRegistry.NUCLEAR_FURNACE_RODS)) {
                                entity.fissionTime = getMaxFissionTime();
                                rodStack.shrink(1);
                                entity.currentWaste += getWastePerBarrel();
                            }
                            entity.resetCookTime();
                        } else if (entity.cookTime < entity.maxCookTime) {
                            flag = true;
                            entity.cookTime++;
                        } else {
                            entity.setRecipeUsed(entity.currentRecipe);
                            entity.resetCookTime();
                            cookStack.shrink(1);
                            if (ItemStack.isSameItem(entity.items.get(3), cookResult)) {
                                entity.items.get(3).grow(cookResult.getCount());
                            } else {
                                entity.setItem(3, cookResult.copy());
                            }
                            flag = true;
                        }
                    } else {
                        entity.resetCookTime();
                    }
                }
            } else {
                entity.currentRecipe = null;
                entity.resetCookTime();
            }
            if (entity.fissionTime > 0) {
                entity.fissionTime--;
                flag = true;
            }
            if (entity.currentWaste >= getWastePerBarrel() && barrelStack.is(ACTagRegistry.NUCLEAR_FURNACE_BARRELS) && entity.canFitInResultSlot(new ItemStack(ACBlockRegistry.WASTE_DRUM.get()), 4)) {
                flag = true;
                if (entity.barrelTime < MAX_BARRELING_TIME) {
                    entity.barrelTime++;
                } else {
                    ItemStack wasteDrum = new ItemStack(ACBlockRegistry.WASTE_DRUM.get());
                    entity.barrelTime = 0;
                    barrelStack.shrink(1);
                    float prevCriticality = entity.getCriticality();
                    entity.currentWaste -= getWastePerBarrel();
                    if(prevCriticality == 3 && entity.getCriticality() <= 2 && entity.lastInteractedWithPlayer != null){
                        ACAdvancementTriggerRegistry.STOP_NUCLEAR_FURNACE_MELTDOWN.triggerForEntity(entity.lastInteractedWithPlayer);
                    }
                    if (ItemStack.isSameItem(entity.items.get(4), wasteDrum)) {
                        entity.items.get(4).grow(1);
                    } else {
                        entity.setItem(4, wasteDrum);
                    }
                }
            } else {
                entity.barrelTime = 0;
                flag = true;
            }
            if (flag) {
                entity.syncWithClient();
            }
            if (entity.currentWaste >= MAX_WASTE) {
                entity.destroyWhileCritical(true);
            }
        }else if(entity.isUndergoingFission() && !entity.isRemoved()){
            AlexsCaves.PROXY.playWorldSound(entity, (byte)7);
        }
    }

    public void setRemoved() {
        AlexsCaves.PROXY.clearSoundCacheFor(this);
        super.setRemoved();
    }

    public void destroyWhileCritical(boolean nuke) {
        int wasteBlocks = MAX_WASTE / getWastePerBarrel();
        this.currentWaste = 0;
        Vec3 vec3 = this.getExhaustPos();
        BlockState waste = ACBlockRegistry.UNREFINED_WASTE.get().defaultBlockState();
        for (int i = 0; i <= 1; i++) {
            for (int j = 0; j <= 1; j++) {
                for (int k = 0; k <= 1; k++) {
                    level.destroyBlock(this.getBlockPos().offset(i, j, k), false);
                }
            }
        }
        for (int i = 0; i < wasteBlocks; i++) {
            FallingBlockEntity fallingblockentity = ACCompat.createEntity(EntityType.FALLING_BLOCK, level);
            if (fallingblockentity instanceof FallingBlockEntityAccessor accessor) {
                accessor.setBlockState(waste);
            }
            fallingblockentity.setPos(vec3.add(level.getRandom().nextFloat() * 6 - 3, level.getRandom().nextFloat() * 3, level.getRandom().nextFloat() * 6 - 3));
            fallingblockentity.setDeltaMovement(fallingblockentity.position().subtract(vec3).normalize().scale(0.75F));
            level.addFreshEntity(fallingblockentity);
        }
        if (nuke) {
            NuclearExplosionEntity explosion = ACCompat.createEntity(ACEntityRegistry.NUCLEAR_EXPLOSION.get(), level);
            explosion.setPos(vec3.add(0, -1.5F, 0));
            explosion.setSize(0.75F);
            level.addFreshEntity(explosion);
        } else {
            AreaEffectCloud areaeffectcloud = new AreaEffectCloud(level, vec3.x, vec3.y - 1F, vec3.z);
            areaeffectcloud.setParticle(ACParticleRegistry.GAMMAROACH.get());
            ACCompat.setCloudColor(areaeffectcloud, 0X77D60E);
            areaeffectcloud.addEffect(new MobEffectInstance(ACCompat.effect(ACEffectRegistry.IRRADIATED.get()), 9600, this.getCriticality()));
            areaeffectcloud.setRadius(8.0F);
            areaeffectcloud.setDuration(12000);
            areaeffectcloud.setWaitTime(3);
            areaeffectcloud.setRadiusPerTick(-areaeffectcloud.getRadius() / (float) areaeffectcloud.getDuration());
            level.addFreshEntity(areaeffectcloud);
        }
    }

    private void spreadFire(Level level, int range) {
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < 5; i++) {
            mutableBlockPos.set(this.getBlockPos().getX() + 1 + range - level.getRandom().nextInt(range * 2), this.getBlockPos().getY() + 1 + range - level.getRandom().nextInt(range * 2), this.getBlockPos().getZ() + 1 + range - level.getRandom().nextInt(range * 2));
            if (level.isEmptyBlock(mutableBlockPos)) {
                level.setBlockAndUpdate(mutableBlockPos, BaseFireBlock.getState(level, mutableBlockPos));
                break;
            }
        }
    }

    private void resetCookTime() {
        int prev = cookTime;
        cookTime = 0;
        if (prev != cookTime) {
            syncWithClient();
        }
    }

    private void syncWithClient() {
        this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 2);
    }

    private static int getWastePerBarrel() {
        return MAX_WASTE / 10;
    }

    private boolean canFitInResultSlot(ItemStack putIn, int resultSlot) {
        ItemStack currentlyInThere = items.get(resultSlot);
        if (currentlyInThere.isEmpty()) {
            return true;
        } else if (!ItemStack.isSameItem(currentlyInThere, putIn)) {
            return false;
        } else if (currentlyInThere.getCount() + putIn.getCount() <= currentlyInThere.getMaxStackSize() && currentlyInThere.getCount() + putIn.getCount() <= currentlyInThere.getMaxStackSize()) {
            return true;
        } else {
            return currentlyInThere.getCount() + putIn.getCount() <= putIn.getMaxStackSize();
        }
    }

    public Vec3 getExhaustPos() {
        return new Vec3(this.getBlockPos().getX() + 1F, this.getBlockPos().getY() + 1F, this.getBlockPos().getZ() + 1F);
    }


    public int[] getSlotsForFace(Direction direction) {
        if (direction == Direction.DOWN) {
            return SLOTS_FOR_DOWN;
        } else {
            if (this.getBlockState().getBlock() == ACBlockRegistry.NUCLEAR_FURNACE.get()) {
                Direction facing = this.getBlockState().getValue(NuclearFurnaceBlock.FACING);
                if (direction == facing.getClockWise()) {
                    return SLOTS_FOR_LEFT;
                }else if(direction == facing.getCounterClockWise()){
                    return SLOTS_FOR_RIGHT;
                }
            }
            return SLOTS_FOR_UP;
        }
    }

    public void onPlayerUse(Player player){
        this.lastInteractedWithPlayer = player;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    public AABB getRenderBoundingBox() {
        BlockPos pos = this.getBlockPos();
        return ACPlatform.encapsulating(pos.offset(-1, -1, -1), pos.offset(2, 2, 2));
    }

    public void load(CompoundTag compoundTag) {
        super.load(compoundTag);
        this.items.clear();
        ContainerHelper.loadAllItems(compoundTag, this.items);
        loadAdditional(compoundTag);
    }

    private void loadAdditional(CompoundTag compoundTag) {
        currentWaste = ACCompat.getInt(compoundTag, "Waste");
        cookTime = ACCompat.getInt(compoundTag, "CookTime");
        maxCookTime = ACCompat.getInt(compoundTag, "MaxCookTime");
        fissionTime = ACCompat.getInt(compoundTag, "FissionTime");
        barrelTime = ACCompat.getInt(compoundTag, "BarrelTime");
        CompoundTag compoundtag = ACCompat.getCompound(compoundTag, "RecipesUsed");
        for(String s : ACCompat.getAllKeys(compoundtag)) {
            this.recipesUsed.put(ResourceLocation.parse(s), ACCompat.getInt(compoundtag, s));
        }
    }

    protected void saveAdditional(CompoundTag compoundTag) {
        super.saveAdditional(compoundTag);
        ContainerHelper.saveAllItems(compoundTag, this.items, true);
        compoundTag.putInt("Waste", currentWaste);
        compoundTag.putInt("CookTime", cookTime);
        compoundTag.putInt("MaxCookTime", maxCookTime);
        compoundTag.putInt("FissionTime", fissionTime);
        compoundTag.putInt("BarrelTime", barrelTime);
        CompoundTag compoundtag = new CompoundTag();
        this.recipesUsed.forEach((resLoc, count) -> {
            compoundtag.putInt(resLoc.toString(), count);
        });
        compoundtag.put("RecipesUsed", compoundtag);
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
        if (packet != null && packet.getTag() != null) {
            this.loadAdditional(packet.getTag());
        }
    }

    public int getContainerSize() {
        return this.items.size();
    }

    public boolean isEmpty() {
        for (ItemStack itemstack : this.items) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    public ItemStack removeItem(int slot, int count) {
        return ContainerHelper.removeItem(this.items, slot, count);
    }

    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    public void setItem(int slot, ItemStack itemStack) {
        ItemStack itemstack = this.items.get(slot);
        boolean flag = !itemStack.isEmpty() && ACCompat.sameItemSameData(itemstack, itemStack);
        this.items.set(slot, itemStack);
        if (itemStack.getCount() > this.getMaxStackSize()) {
            itemStack.setCount(this.getMaxStackSize());
        }

        if (slot == 0 && !flag) {
            this.setChanged();
        }
    }

    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    public boolean canPlaceItem(int slot, ItemStack stack) {
        return (slot != 0 || getRecipeFor(stack).isPresent()) && slot != 3 && slot != 4;
    }

    private Optional<UsedRecipe> getRecipeFor(ItemStack itemStack) {
        // 1.20.2: the cached check answers with a RecipeHolder rather than the recipe itself.
        // 1.21: and it is asked with a SingleRecipeInput rather than a one-slot Container.
        // 1.21.2: the check is server-only (recipes are no longer sent to the client) and the
        //         holder's id is a ResourceKey, while this furnace tallies plain ids.
        //? if >=1.21.2
        /*return this.level instanceof ServerLevel serverLevel ? this.quickCheck.getRecipeFor(new net.minecraft.world.item.crafting.SingleRecipeInput(itemStack), serverLevel).map(holder -> new UsedRecipe(holder.id().location(), holder.value())) : Optional.empty();*/
        //? if >=1.21 && <1.21.2
        /*return this.quickCheck.getRecipeFor(new net.minecraft.world.item.crafting.SingleRecipeInput(itemStack), this.level).map(holder -> new UsedRecipe(holder.id(), holder.value()));*/
        //? if >=1.20.2 && <1.21
        /*return this.quickCheck.getRecipeFor(new SimpleContainer(itemStack), this.level).map(holder -> new UsedRecipe(holder.id(), holder.value()));*/
        //? if <1.20.2
        return this.quickCheck.getRecipeFor(new SimpleContainer(itemStack), this.level).map(recipe -> new UsedRecipe(recipe.getId(), recipe));
    }

    public void clearContent() {
        this.items.clear();
    }

    public boolean isUndergoingFission() {
        return fissionTime > 0;
    }

    public int getCriticality() {
        float f = getWasteScale();
        if (f >= 0.8F) {
            return 3;
        } else if (f >= 0.6F) {
            return 2;
        } else if (f >= 0.35F) {
            return 1;
        }
        return 0;
    }

    public float getWasteScale() {
        return currentWaste / (float) MAX_WASTE;
    }

    // See AbyssalAltarBlockEntity: getItems/setItems became abstract on BaseContainerBlockEntity in
    // 1.20.5, and this class already keeps the list they describe.
    //? if >=1.20.5 {
    /*@Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> list) {
        this.items = list;
    }
    *///?}

    protected Component getDefaultName() {
        return Component.translatable("block.alexscaves.nuclear_furnace");
    }


    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new NuclearFurnaceMenu(id, inventory, this, this.dataAccess);
    }

    public WorldlyContainer getContainerFor(BlockPos offsetPos) {
        return this;
    }

    // Forge exposes an inventory to hoppers/pipes by overriding getCapability and handing back a
    // LazyOptional; NeoForge deleted all three of those types and registers block-entity
    // capabilities up front instead — see AlexsCaves#registerCapabilities, which wires the same
    // SidedInvWrapper to the same block entity type.
    //? if forge {
    private LazyOptional<? extends IItemHandler>[] handlers = SidedInvWrapper.create(this, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);

    @Override
    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @javax.annotation.Nullable Direction facing) {
        if (!this.remove && facing != null && capability == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER) {
            return handlers[facing.ordinal()].cast();
        }
        return super.getCapability(capability, facing);
    }
    //?}


    private void setRecipeUsed(@javax.annotation.Nullable UsedRecipe recipe) {
        if (recipe != null) {
            this.recipesUsed.addTo(recipe.id(), 1);
        }
    }

    // Vanilla's AbstractFurnaceBlockEntity pair, copied. 1.20.2 moved the recipe id out of
    // Recipe into RecipeHolder and every player-facing award API moved with it — awardRecipes,
    // triggerRecipeCrafted and RecipeManager#byKey all changed element type — so the two eras are
    // written out rather than gated line by line.
    //? if >=1.20.2 {
    /*public void awardUsedRecipesAndPopExperience(ServerPlayer serverPlayer) {
        List<net.minecraft.world.item.crafting.RecipeHolder<?>> list = this.getRecipesToAwardAndPopExperience(serverPlayer.serverLevel(), serverPlayer.position());
        serverPlayer.awardRecipes(list);

        for (net.minecraft.world.item.crafting.RecipeHolder<?> holder : list) {
            if (holder != null) {
                serverPlayer.triggerRecipeCrafted(holder, this.items);
            }
        }

        this.recipesUsed.clear();
    }

    public List<net.minecraft.world.item.crafting.RecipeHolder<?>> getRecipesToAwardAndPopExperience(ServerLevel serverLevel, Vec3 vec3) {
        List<net.minecraft.world.item.crafting.RecipeHolder<?>> list = Lists.newArrayList();

        for (Object2IntMap.Entry<ResourceLocation> entry : this.recipesUsed.object2IntEntrySet()) {
            ACCompat.recipeById(serverLevel, entry.getKey()).ifPresent((holder) -> {
                list.add(holder);
                createExperience(serverLevel, vec3, entry.getIntValue(), ACCompat.cookingExperience((AbstractCookingRecipe) holder.value()));
            });
        }

        return list;
    }
    *///?}
    //? if <1.20.2 {
    public void awardUsedRecipesAndPopExperience(ServerPlayer serverPlayer) {
        List<Recipe<?>> list = this.getRecipesToAwardAndPopExperience(serverPlayer.serverLevel(), serverPlayer.position());
        serverPlayer.awardRecipes(list);

        for(Recipe<?> recipe : list) {
            if (recipe != null) {
                serverPlayer.triggerRecipeCrafted(recipe, this.items);
            }
        }

        this.recipesUsed.clear();
    }

    public List<Recipe<?>> getRecipesToAwardAndPopExperience(ServerLevel serverLevel, Vec3 vec3) {
        List<Recipe<?>> list = Lists.newArrayList();

        for(Object2IntMap.Entry<ResourceLocation> entry : this.recipesUsed.object2IntEntrySet()) {
            ACCompat.recipeById(serverLevel, entry.getKey()).ifPresent((p_155023_) -> {
                list.add(p_155023_);
                createExperience(serverLevel, vec3, entry.getIntValue(), ACCompat.cookingExperience((AbstractCookingRecipe)p_155023_));
            });
        }

        return list;
    }
    //?}

    private static void createExperience(ServerLevel serverLevel, Vec3 vec3, int i1, float scale) {
        int i = Mth.floor((float)i1 * scale);
        float f = Mth.frac((float)i1 * scale);
        if (f != 0.0F && Math.random() < (double)f) {
            ++i;
        }
        ExperienceOrb.award(serverLevel, vec3, i);
    }

    public static RecipeType<? extends AbstractCookingRecipe> getRecipeType(){
        if (AlexsCaves.COMMON_CONFIG.nuclearFurnaceCustomType.get()) {
            return ACRecipeRegistry.NUCLEAR_FURNACE_TYPE.get();
        }
        return AlexsCaves.COMMON_CONFIG.nuclearFurnaceBlastingOnly.get() ? RecipeType.BLASTING : RecipeType.SMELTING;
    }

    public static float getSpeedReduction(){
        if (AlexsCaves.COMMON_CONFIG.nuclearFurnaceCustomType.get()) {
            return SPEED_REDUCTION;
        }
        return AlexsCaves.COMMON_CONFIG.nuclearFurnaceBlastingOnly.get() ? SPEED_REDUCTION : SPEED_REDUCTION * 0.5F;
    }

    public static int getMaxFissionTime() {
        return (int) Math.ceil(100 * 64 * getSpeedReduction());
    }

}
