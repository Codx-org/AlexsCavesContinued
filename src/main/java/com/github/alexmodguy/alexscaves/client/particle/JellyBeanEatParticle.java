package com.github.alexmodguy.alexscaves.client.particle;

import com.github.alexmodguy.alexscaves.server.item.ACItemRegistry;
import com.github.alexmodguy.alexscaves.server.item.BiomeTreatItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BreakingItemParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class JellyBeanEatParticle extends BreakingItemParticle {

    protected JellyBeanEatParticle(ClientLevel clientLevel, double x, double y, double z, double xd, double yd, double zd, ItemStack stack) {
        // 1.21.4 resolves a stack into an ItemStackRenderState before a particle ever sees it — the
        // particle picks its sprite off the state, not the item. Vanilla's own item-particle provider
        // resolves with the GROUND context and no holder, which is what particleState mirrors.
        //? if >=1.21.9 {
        /*super(clientLevel, x, y, z, acParticleSprite(stack, clientLevel));
        *///?} elif >=1.21.4 {
        /*super(clientLevel, x, y, z, com.github.alexmodguy.alexscaves.client.render.item.ACItemRenderCompat.particleState(stack, clientLevel));
        *///?} else {
        super(clientLevel, x, y, z, stack);
        //?}
        this.xd *= (double)0.1F;
        this.yd *= (double)0.1F;
        this.zd *= (double)0.1F;
        this.xd += xd;
        this.yd += yd;
        this.zd += zd;
        // ItemColors is gone from 1.21.4: a tint is a positional ItemTintSource baked into the item
        // model definition, with no runtime lookup left to call. This particle is only ever spawned
        // for the two edibles below, so the mod answers for itself rather than reviving the registry.
        //
        // Below that, getItemColors() is a Forge accessor over a private field whose body is one
        // getfield, so the Fabric arm reads the field the access widener opens rather than inventing
        // a helper. (Gates do not nest — hence a third arm rather than one inside the else.)
        //? if >=1.21.4 {
        /*int colorizer = stack.is(ACItemRegistry.JELLY_BEAN.get())
                ? com.github.alexmodguy.alexscaves.server.item.JellyBeanItem.getBeanColor(stack)
                : -1;
        *///?} elif fabric {
        /*int colorizer = Minecraft.getInstance().itemColors.getColor(stack, 0);
        *///?} else {
        int colorizer = Minecraft.getInstance().getItemColors().getColor(stack, 0);
        //?}
        if(stack.getItem() == ACItemRegistry.BIOME_TREAT.get()){
            colorizer = BiomeTreatItem.getBiomeTreatColorOf(Minecraft.getInstance().level, stack);
        }
        if(colorizer != -1){
            float f = (float)(colorizer >> 16 & 255) / 255.0F;
            float f1 = (float)(colorizer >> 8 & 255) / 255.0F;
            float f2 = (float)(colorizer & 255) / 255.0F;
            this.setColor(f, f1, f2);
        }
    }

    // 1.21.9 moved the sprite pick out of the particle: BreakingItemParticle now takes a
    // TextureAtlasSprite where 1.21.4-1.21.8 took the whole ItemStackRenderState. This mirrors vanilla's
    // own ItemParticleProvider#getSprite exactly, missing-sprite fallback included.
    //
    // 26 gave the pick a return type — pickParticleMaterial hands back a Material$Baked (sprite plus a
    // force-translucent flag) instead of the bare sprite — so the whole method is repeated rather than
    // gated inside, since a Stonecutter arm cannot nest. Only the two marked lines differ.
    //? if >=26 {
    /*private static net.minecraft.client.renderer.texture.TextureAtlasSprite acParticleSprite(
            ItemStack stack, ClientLevel level) {
        net.minecraft.client.renderer.item.ItemStackRenderState state =
                new net.minecraft.client.renderer.item.ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver()
                .updateForTopItem(state, stack, net.minecraft.world.item.ItemDisplayContext.GROUND, level, null, 0);
        net.minecraft.client.resources.model.sprite.Material.Baked material = state.pickParticleMaterial(level.getRandom());
        return material != null
                ? material.sprite()
                : Minecraft.getInstance().getAtlasManager()
                        .getAtlasOrThrow(net.minecraft.data.AtlasIds.BLOCKS).missingSprite();
    }
    *///?} elif >=1.21.9 {
    /*private static net.minecraft.client.renderer.texture.TextureAtlasSprite acParticleSprite(
            ItemStack stack, ClientLevel level) {
        net.minecraft.client.renderer.item.ItemStackRenderState state =
                new net.minecraft.client.renderer.item.ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver()
                .updateForTopItem(state, stack, net.minecraft.world.item.ItemDisplayContext.GROUND, level, null, 0);
        net.minecraft.client.renderer.texture.TextureAtlasSprite sprite = state.pickParticleIcon(level.getRandom());
        return sprite != null
                ? sprite
                : Minecraft.getInstance().getAtlasManager()
                        .getAtlasOrThrow(net.minecraft.data.AtlasIds.BLOCKS).missingSprite();
    }
    *///?}

    @OnlyIn(Dist.CLIENT)
    public static class Factory implements ParticleProvider<ItemParticleOption> {
        public Particle createParticle(ItemParticleOption itemParticleOption, ClientLevel clientLevel, double x, double y, double z, double xd, double yd, double zd) {
            return new JellyBeanEatParticle(clientLevel, x, y, z, xd, yd, zd,
                    com.github.alexmodguy.alexscaves.server.misc.ACCompat.particleStack(itemParticleOption));
        }
    }

}
