package com.github.alexmodguy.alexscaves.server.misc;

// 1.20.5 deleted net.minecraft.core.dispenser.AbstractProjectileDispenseBehavior in favour of
// ProjectileDispenseBehavior(Item), which requires the dispensed *item* to implement ProjectileItem.
// ACItemRegistry registers six anonymous subclasses over items that do not implement it (the seeking
// and burrowing arrows, the cinder brick, both ink bombs and guano), so the old shape is kept here
// rather than reshaping six items. Same simple name, different package: the import line in
// ACItemRegistry is swapped by the !mc205-projdispense stonecutter replacement on >=1.20.5 nodes.
//
// Below 1.20.5 the whole file is commented out and vanilla's class is used unchanged.
//
// This is a verbatim re-creation of vanilla 1.20.4's class against the 1.20.6 API: BlockSource is a
// record there (level()/pos()/state()), and DefaultDispenseItemBehavior#execute is protected — this
// override widens it to public, which is legal and matches what the deleted class did.
//? if >=1.20.5 {
/*import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

public abstract class AbstractProjectileDispenseBehavior extends DefaultDispenseItemBehavior {

    public ItemStack execute(BlockSource source, ItemStack stack) {
        Level level = source.level();
        Position position = DispenserBlock.getDispensePosition(source);
        Direction direction = source.state().getValue(DispenserBlock.FACING);
        Projectile projectile = this.getProjectile(level, position, stack);
        projectile.shoot(direction.getStepX(), (float) direction.getStepY() + 0.1F, direction.getStepZ(), this.getPower(), this.getUncertainty());
        level.addFreshEntity(projectile);
        stack.shrink(1);
        return stack;
    }

    protected void playSound(BlockSource source) {
        source.level().levelEvent(1002, source.pos(), 0);
    }

    protected abstract Projectile getProjectile(Level level, Position position, ItemStack stack);

    protected float getUncertainty() {
        return 6.0F;
    }

    protected float getPower() {
        return 1.1F;
    }
}
*///?}
