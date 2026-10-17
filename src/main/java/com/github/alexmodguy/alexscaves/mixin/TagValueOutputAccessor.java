package com.github.alexmodguy.alexscaves.mixin;

import com.mojang.serialization.DynamicOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.storage.TagValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.util.ProblemReporter;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Opens {@code TagValueOutput}'s constructor, so the mod can hand it a tag of its own.
 *
 * <p>1.21.6 and up only, like {@link TagValueInputAccessor}.
 */
@Mixin(TagValueOutput.class)
public interface TagValueOutputAccessor {

    /**
     * A {@code TagValueOutput} writing into a tag the caller already has.
     *
     * <p>Both public factories mint their own empty {@code CompoundTag}, which is no use to the six
     * bucketable fish and the possession totem: each builds a tag, asks the entity to fill it and
     * then stores it somewhere of its own. The constructor does exactly what is wanted and is
     * merely package-private, so an invoker is the whole fix — see {@code ACCompat#asOutput}.
     */
    @Invoker("<init>")
    static TagValueOutput ac_new(ProblemReporter problemReporter, DynamicOps<Tag> ops, CompoundTag output) {
        throw new AssertionError();
    }
}
