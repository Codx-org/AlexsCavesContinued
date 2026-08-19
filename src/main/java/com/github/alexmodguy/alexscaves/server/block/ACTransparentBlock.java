package com.github.alexmodguy.alexscaves.server.block;

import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * A plain glass-like block: see-through, full brightness, lets skylight through.
 *
 * <p>1.20.3 renamed this corner of the block hierarchy — {@code AbstractGlassBlock} became
 * {@code TransparentBlock} (and the concrete {@code GlassBlock} that sat on top of it disappeared,
 * since it added nothing). Members are otherwise identical on both sides. Alex's Caves extends this
 * shim instead so the rename stays in one file: {@link SugarGlassBlock} and {@link DepthGlassBlock}
 * derive from it, and amber is a bare instance.
 */
//? if >=1.20.3 {
/*public class ACTransparentBlock extends net.minecraft.world.level.block.TransparentBlock {

    public ACTransparentBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
}
*///?} else {
public class ACTransparentBlock extends net.minecraft.world.level.block.AbstractGlassBlock {

    public ACTransparentBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
}
//?}
