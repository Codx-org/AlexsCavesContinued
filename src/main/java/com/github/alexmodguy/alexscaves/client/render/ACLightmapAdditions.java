package com.github.alexmodguy.alexscaves.client.render;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.client.ACClientCompat;
import com.github.alexmodguy.alexscaves.client.ClientProxy;
import com.github.alexmodguy.alexscaves.server.entity.util.PossessesCamera;
import com.github.alexmodguy.alexscaves.server.misc.ACCompat;
import com.github.alexmodguy.alexscaves.server.misc.ACLoadedMods;
import com.github.alexmodguy.alexscaves.server.potion.ACEffectRegistry;
import com.github.alexmodguy.alexscaves.server.potion.DeepsightEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * The two things this mod adds to the vanilla lightmap, as plain client code: a brightness bonus and
 * a colour tint, both read straight off {@link ClientProxy}'s per-frame biome state.
 *
 * <p>It exists because the lightmap has been rebuilt three times over this version range and the
 * additions have to be expressed differently each time — a CPU pixel loop up to 1.21.1, two extra
 * uniforms on a copy of vanilla's fragment shader from 1.21.2, and mutations of a render state from
 * 26.1 — while <em>what</em> is added has never changed. Keeping it here means the three callers
 * (`LightTextureMixin`, `LightmapMixin`, `LightmapRenderStateExtractorMixin`) cannot drift apart,
 * which matters: {@code getBrightness} and the lightmap texture must agree or entities light
 * differently from the blocks they stand on.
 *
 * <p>No Stonecutter gates: every API named here is this mod's own or unchanged across 1.20.1-26.x.
 */
public class ACLightmapAdditions {

    /**
     * How much this mod adds to a raw light level, as one number: the biome's ambient light, floored
     * by darkness incarnate and possession, topped up by deepsight underwater, and pulled back down
     * while a primordial boss is active. Zero — the no-op — when the option is off.
     */
    public static float ambientBonus() {
        if (!AlexsCaves.CLIENT_CONFIG.biomeAmbientLight.get()) {
            return 0.0F;
        }
        float f = ClientProxy.lastBiomeAmbientLightAmountPrev + (ClientProxy.lastBiomeAmbientLightAmount - ClientProxy.lastBiomeAmbientLightAmountPrev) * ACClientCompat.frameTime();
        float primordialBossAmount = AlexsCaves.PROXY.getPrimordialBossActiveAmount(ACClientCompat.frameTime());
        if (Minecraft.getInstance().getCameraEntity() instanceof PossessesCamera || Minecraft.getInstance().getCameraEntity() instanceof LivingEntity afflicted && afflicted.hasEffect(ACCompat.effect(ACEffectRegistry.DARKNESS_INCARNATE.get()))) {
            f = Math.max(f, 0.35F);
        }
        if (Minecraft.getInstance().player.hasEffect(ACCompat.effect(ACEffectRegistry.DEEPSIGHT.get())) && Minecraft.getInstance().player.isUnderWater()) {
            f = Math.min(1.0F, f + 0.05F * DeepsightEffect.getIntensity(Minecraft.getInstance().player, ACClientCompat.frameTime()));
        }
        return primordialBossAmount > 0.0F ? f - primordialBossAmount * 0.06F : f;
    }

    /**
     * Whether the biome light tint applies at all. Distant Horizons is excluded because it renders
     * its own terrain against this same lightmap and the tint doubles up across the seam.
     */
    public static boolean tintEnabled() {
        return AlexsCaves.CLIENT_CONFIG.biomeAmbientLightColoring.get() && !ACLoadedMods.isDistantHorizonsLoaded();
    }

    /**
     * The biome light tint for this frame, as a multiplier. White ({@code (1,1,1)}) outside a
     * coloured biome, so a caller may apply it unconditionally.
     */
    public static Vec3 tintColor() {
        return ClientProxy.lastBiomeLightColorPrev.add(ClientProxy.lastBiomeLightColor.subtract(ClientProxy.lastBiomeLightColorPrev).scale(ACClientCompat.frameTime()));
    }
}
