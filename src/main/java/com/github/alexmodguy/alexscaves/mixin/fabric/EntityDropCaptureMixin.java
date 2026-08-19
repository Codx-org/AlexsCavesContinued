package com.github.alexmodguy.alexscaves.mixin.fabric;

import com.github.alexmodguy.alexscaves.fabric.entity.ACDropCapture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collection;

/**
 * Supplies {@link ACDropCapture} on {@code Entity} — Fabric's stand-in for the loaders'
 * {@code captureDrops} patch.
 *
 * <p>Both loaders add a nullable {@code Collection<ItemEntity> captureDrops} field to {@code Entity}
 * plus a getter and a setter, and splice a test into the deepest {@code spawnAtLocation} overload:
 * where vanilla ends with {@code this.level().addFreshEntity(itemEntity)}, they write
 * {@code if (captureDrops() != null) captureDrops().add(itemEntity); else level().addFreshEntity(…)}.
 * Read out of the 1.20.1 merged jars: vanilla has exactly one {@code Level#addFreshEntity} call in
 * that method, at offset 58; Forge's copy branches on the field at 54 and keeps the vanilla call as
 * the {@code else} at 79. So one {@code @Redirect} on that single call reproduces the patch, and the
 * three shallower overloads need nothing — they all chain into this one.
 *
 * <p>The setter is Forge's contract verbatim (install {@code value}, return whatever was installed
 * before), which is what lets {@code GumWormEntity#dropAllDeathLoot} install a fresh list, run the
 * ordinary drop path, then take the list back and re-drop everything at the surface.
 *
 * <p>The field is deliberately <em>not</em> saved or synced, exactly as on the loaders: it is live
 * only for the duration of one {@code dropAllDeathLoot} call, on the server, and both loaders leave
 * it out of {@code addAdditionalSaveData} for the same reason.
 *
 * <p>⚠️ Gated {@code <1.21.2}, and the band above it is more than a descriptor change. 1.21.2
 * threads a {@code ServerLevel} through as the first parameter (the {@code !mc2102-spawnatlocation-*}
 * rules), and 26.2 goes further still: the {@code (ServerLevel, ItemStack, float)} overload becomes a
 * two-line delegate to a new {@code (ServerLevel, ItemStack, Vec3)} form, so the vanilla
 * {@code addFreshEntity} call — and therefore this injection — moves into a <em>different method</em>.
 * Re-derive the enclosing selector from the bytecode of each band rather than assuming the descriptor
 * is the only thing that moved. Note also that this hole is <em>silent</em>: with the redirect gated
 * away the capture list is still installed and simply never filled, so the gum worm would drop
 * nothing at all rather than crash.
 */
@Mixin(Entity.class)
public class EntityDropCaptureMixin implements ACDropCapture {

    @Unique
    private Collection<ItemEntity> ac_capturedDrops;

    @Override
    public Collection<ItemEntity> ac_captureDrops(Collection<ItemEntity> value) {
        Collection<ItemEntity> previous = this.ac_capturedDrops;
        this.ac_capturedDrops = value;
        return previous;
    }

    //? if <1.21.2 {
    @Redirect(
            method = "spawnAtLocation(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z")
    )
    private boolean ac_captureInsteadOfSpawning(Level level, Entity spawned) {
        if (this.ac_capturedDrops != null && spawned instanceof ItemEntity item) {
            return this.ac_capturedDrops.add(item);
        }
        return level.addFreshEntity(spawned);
    }
    //?}
}
