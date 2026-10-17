package com.github.alexmodguy.alexscaves.fabric.client;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gives this mod's 119 non-solid blocks their chunk render layer on Fabric.
 *
 * <p><b>The gap this closes.</b> A block model declares its layer with a top-level {@code
 * "render_type"} field, and 188 of this mod's models do. That field is a <b>Forge extension</b>:
 * it is read by {@code ExtendedBlockModelDeserializer} on Forge and NeoForge, and it appears in no
 * vanilla class on any version in this range — verified by scanning the merged jars of 1.20.1,
 * 1.21.1, 1.21.4, 1.21.8, 1.21.11, 26.1.2 and 26.2 for the literal. Fabric API never read it
 * either. So on Fabric every one of those blocks fell through to {@code ItemBlockRenderTypes}'
 * default, which is {@code solid} — the layer whose shader discards nothing — and every
 * transparent texel in a cutout texture drew as opaque black. Reported against the pewen branch;
 * it was true of all 119.
 *
 * <p><b>Why it is read at runtime rather than listed here.</b> The mapping is derived from the
 * mod's own {@code blockstates/} and {@code models/} JSON at client init, walking each block's
 * blockstate for the models it names and each model's {@code parent} chain for the first
 * {@code render_type}. That is the same data Forge's deserializer reads, so a new block or a
 * changed model needs no second edit here and the two loaders cannot drift apart. Roughly 350
 * small files out of this mod's own jar, parsed once.
 *
 * <p><b>Per-block, not per-model.</b> Forge resolves the field per baked model, so one block may
 * legitimately use two layers; Fabric's map is keyed by block. Where a block's models disagree the
 * most permissive wins — translucent over cutout over cutout_mipped — which affects exactly one
 * block here ({@code cycad}, whose trunk is cutout and whose top is cutout_mipped) and costs it
 * only mipmapping on the top.
 *
 * <p>⚠️ Not needed from 26, and impossible there: that version deleted {@code
 * ItemBlockRenderTypes} and Fabric API's {@code BlockRenderLayerMap} with it. A quad's layer is
 * baked into {@code BakedQuad$MaterialInfo} from its sprite's transparency instead, so every
 * loader gets the right layer with no registration at all. The call site is gated {@code <26}.
 */
public final class ACFabricRenderLayers {

    private static final String CUTOUT = "cutout";
    private static final String CUTOUT_MIPPED = "cutout_mipped";
    private static final String TRANSLUCENT = "translucent";

    private ACFabricRenderLayers() {
    }

    public static void register() {
        Map<String, String> modelLayers = new HashMap<>();
        int registered = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (id == null || !AlexsCaves.MODID.equals(id.getNamespace())) {
                continue;
            }
            String layer = layerOf(id.getPath(), modelLayers);
            if (layer != null) {
                putBlock(block, layer);
                registered++;
            }
        }
        AlexsCaves.LOGGER.debug("Alex's Caves Continued: {} blocks given a non-solid chunk render layer", registered);
    }

    /**
     * The layer a block's blockstate asks for, or null when every model it names is solid.
     */
    private static String layerOf(String path, Map<String, String> modelLayers) {
        JsonObject blockstate = readJson("/assets/" + AlexsCaves.MODID + "/blockstates/" + path + ".json");
        if (blockstate == null) {
            return null;
        }
        List<String> models = new ArrayList<>();
        collectModels(blockstate, models);
        String best = null;
        for (String model : models) {
            best = moreTransparent(best, renderTypeOf(model, modelLayers, 0));
        }
        return best;
    }

    /**
     * Every {@code "model"} string anywhere in a blockstate file. Deliberately shape-agnostic:
     * {@code variants} and {@code multipart} nest their models differently, either can hold a
     * weighted list instead of one object, and this mod ships both shapes.
     */
    private static void collectModels(JsonElement element, List<String> out) {
        if (element instanceof JsonObject object) {
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                JsonElement value = entry.getValue();
                if ("model".equals(entry.getKey()) && value.isJsonPrimitive()) {
                    out.add(value.getAsString());
                } else {
                    collectModels(value, out);
                }
            }
        } else if (element instanceof JsonArray array) {
            for (JsonElement child : array) {
                collectModels(child, out);
            }
        }
    }

    /**
     * The first {@code render_type} on a model's parent chain, memoised. Only this mod's own
     * models are opened — a vanilla parent can never carry the field.
     */
    private static String renderTypeOf(String model, Map<String, String> cache, int depth) {
        if (depth > 8 || model == null) {
            return null;
        }
        if (cache.containsKey(model)) {
            return cache.get(model);
        }
        String namespace = AlexsCaves.MODID;
        String path = model;
        int colon = model.indexOf(':');
        if (colon >= 0) {
            namespace = model.substring(0, colon);
            path = model.substring(colon + 1);
        }
        String found = null;
        if (AlexsCaves.MODID.equals(namespace)) {
            JsonObject json = readJson("/assets/" + AlexsCaves.MODID + "/models/" + path + ".json");
            if (json != null) {
                if (json.has("render_type")) {
                    found = json.get("render_type").getAsString();
                } else if (json.has("parent")) {
                    found = renderTypeOf(json.get("parent").getAsString(), cache, depth + 1);
                }
            }
        }
        cache.put(model, found);
        return found;
    }

    private static String moreTransparent(String a, String b) {
        return rank(a) >= rank(b) ? a : b;
    }

    private static int rank(String layer) {
        if (TRANSLUCENT.equals(layer)) {
            return 3;
        } else if (CUTOUT.equals(layer)) {
            return 2;
        } else if (CUTOUT_MIPPED.equals(layer)) {
            return 1;
        }
        return 0;
    }

    private static JsonObject readJson(String path) {
        try (InputStream in = ACFabricRenderLayers.class.getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            JsonElement parsed = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        } catch (Exception e) {
            AlexsCaves.LOGGER.warn("Alex's Caves Continued: could not read {} while resolving render layers", path, e);
            return null;
        }
    }

    /**
     * The one call that differs per version. Fabric API moved the map from {@code
     * api.blockrenderlayer.v1} to {@code api.client.rendering.v1} at 1.21.6 and swapped its
     * {@code RenderType} argument for the {@code ChunkSectionLayer} vanilla introduced in the same
     * version; 1.21.11 then collapsed the two cutout constants into one, exactly as
     * {@code !mc2106-fluidlayer-cutout} records for the fluid registrations. From 26 there is no
     * map to write to and this is a no-op, though nothing calls it there.
     */
    private static void putBlock(Block block, String layer) {
        //? if >=26 {
        /*// 26 bakes the layer into the quad from its sprite; nothing to register.
        *///?} elif >=1.21.11 {
        /*net.minecraft.client.renderer.chunk.ChunkSectionLayer type = TRANSLUCENT.equals(layer)
                ? net.minecraft.client.renderer.chunk.ChunkSectionLayer.TRANSLUCENT
                : net.minecraft.client.renderer.chunk.ChunkSectionLayer.CUTOUT;
        net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap.putBlock(block, type);
        *///?} elif >=1.21.6 {
        /*net.minecraft.client.renderer.chunk.ChunkSectionLayer type = TRANSLUCENT.equals(layer)
                ? net.minecraft.client.renderer.chunk.ChunkSectionLayer.TRANSLUCENT
                : CUTOUT_MIPPED.equals(layer)
                        ? net.minecraft.client.renderer.chunk.ChunkSectionLayer.CUTOUT_MIPPED
                        : net.minecraft.client.renderer.chunk.ChunkSectionLayer.CUTOUT;
        net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap.putBlock(block, type);
        *///?} else {
        net.minecraft.client.renderer.RenderType type = TRANSLUCENT.equals(layer)
                ? net.minecraft.client.renderer.RenderType.translucent()
                : CUTOUT_MIPPED.equals(layer)
                        ? net.minecraft.client.renderer.RenderType.cutoutMipped()
                        : net.minecraft.client.renderer.RenderType.cutout();
        net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(block, type);
        //?}
    }
}
