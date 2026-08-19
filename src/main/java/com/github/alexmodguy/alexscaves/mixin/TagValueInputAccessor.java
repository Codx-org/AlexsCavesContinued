package com.github.alexmodguy.alexscaves.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.TagValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The CompoundTag behind a {@code ValueInput}.
 *
 * <p>1.21.6 put {@code ValueInput}/{@code ValueOutput} on every save and load signature in the
 * game. This mod's ~150 overrides keep working on their {@code CompoundTag} — the replacement rules
 * in {@code stonecutter.gradle.kts} rewrite only the method header and bind the original parameter
 * name to {@code ACCompat.tagOf} — which needs the backing tag back out of the input. Vanilla makes
 * the write side public ({@code TagValueOutput#buildResult}); the read side's field is private, so
 * it takes this.
 *
 * <p>1.21.6 and up only: the target class does not exist below it, so {@code ModPlatformPlugin}
 * excludes this file from the compile and prunes the entry back out of the mixin config there.
 */
@Mixin(TagValueInput.class)
public interface TagValueInputAccessor {

    @Accessor("input")
    CompoundTag ac_getInput();
}
