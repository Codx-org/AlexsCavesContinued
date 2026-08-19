package com.github.alexmodguy.alexscaves.fabric.client;

import com.github.alexmodguy.alexscaves.server.item.ACClientExtensionItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

/**
 * Hands this mod's 3D item renderers to Fabric, which is the one half of the client-extension
 * object nothing on this loader would otherwise ask for.
 *
 * <p>Below 1.21.4 an item that draws itself in 3D says so through {@code
 * IClientItemExtensions.getCustomRenderer()}, and both other loaders ask every item for one. Fabric
 * has no such hook — its equivalent is a registry keyed by item, filled ahead of time — so this
 * walks the item registry exactly the way {@code ClientProxy#registerClientExtensions} walks it on
 * NeoForge, calls each {@link ACClientExtensionItem}'s {@code initializeClient} and forwards
 * whatever renderer comes back. The 24 implementors and their extension objects are untouched.
 *
 * <p><b>Why the two sides meet at all.</b> Fabric API's {@code BuiltinItemRendererRegistry} is
 * dispatched from a mixin at the head of vanilla's {@code BlockEntityWithoutLevelRenderer
 * #renderByItem} — precisely the method vanilla's own item pipeline calls once a model reports
 * {@code isCustomRenderer()}, which all 23 of this mod's {@code builtin/entity} models do. So the
 * registered renderer is reached through the same vanilla path Forge's hook feeds, and a
 * {@code DynamicItemRenderer}'s single method has the same six parameters as {@code renderByItem}
 * — hence the method reference below rather than an adapter.
 *
 * <p><b>Only the renderer half is dispatched.</b> The other question the extension object answers,
 * {@code getHumanoidArmorModel}, is decorative here: this mod's six armour sets all implement
 * {@code CustomArmorPostRender}, and {@code mixin.client.HumanoidArmorLayerMixin} cancels vanilla's
 * draw and calls {@code ACArmorRenderProperties} directly on every loader. Wiring an armour-model
 * hook as well would give those items two paths to the same models, only one of which runs.
 *
 * <p>⚠️ Empty from 1.21.4, which deleted the ISTER mechanism outright. There the renderer is named
 * by the item's own model definition ({@code DataPackMigration.writeItemModelDefinitions} writes
 * {@code minecraft:special} for these 23) and reached through {@code ACItemSpecialRenderer}, which
 * is loader-neutral — so this class has nothing left to do and the call site stays ungated.
 */
public final class ACFabricItemRenderers {

    private ACFabricItemRenderers() {
    }

    public static void register() {
        //? if <1.21.4 {
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof ACClientExtensionItem extensionItem)) {
                continue;
            }
            // Consumer-shaped on purpose: initializeClient is the loaders' own signature, and the
            // item decides whether to accept anything at all. An item that accepts an extension
            // object with no renderer of its own — the armour sets do exactly that — registers
            // nothing, which is why the null check is here and not at the call site.
            extensionItem.initializeClient(extensions -> {
                net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer = extensions.getCustomRenderer();
                if (renderer != null) {
                    net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry.INSTANCE
                            .register(item, renderer::renderByItem);
                }
            });
        }
        //?}
    }
}
