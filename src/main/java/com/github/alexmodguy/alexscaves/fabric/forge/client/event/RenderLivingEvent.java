package com.github.alexmodguy.alexscaves.fabric.forge.client.event;

import com.github.alexmodguy.alexscaves.fabric.forge.event.entity.living.LivingEvent;
import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Cancelable;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Fabric stand-in for the loader's around-a-living-entity-render pair.
 *
 * <p>This mod is on both sides of it. It <i>receives</i> the event in {@code ClientEvents} — that is
 * where the possessed-player camera swap, the watcher's stare and the tremorsaurus' scale live — and
 * it <i>publishes</i> it from {@code WatcherRenderer}, which renders a second entity inside its own
 * draw and has to give every other listener the same chance at it the loader would. So the stub is
 * not a one-way adapter: the dispatcher constructs {@code Pre}/{@code Post} around vanilla's
 * {@code LivingEntityRenderer#render}, and the mod's own renderer constructs them too.
 *
 * <p><b>1.21.2 swapped the entity out of the first slot for the render state it was extracted
 * from</b>, exactly as both other loaders did, and this stub follows NeoForge's spelling of that
 * change rather than Forge's: {@code getRenderState()}, and {@code partialTick} kept on the event.
 * That is not a preference — the shared call sites in {@code ClientEvents}, {@code ACClientCompat}
 * and {@code WatcherRenderer} route Fabric into their {@code elif >=1.21.2} arm, which is the arm
 * written against NeoForge, so any other shape here would need a fourth arm at each of them.
 *
 * <p>The base class moves with it. Below 1.21.2 this is a {@link LivingEvent}, because the entity
 * is genuinely in scope and {@code getEntity()} is what the handlers read; from 1.21.2 there is no
 * entity to hand a {@code LivingEvent} constructor, so it extends the bus's root event instead and
 * the entity is recovered from the state through {@code ACStateAccess}. Extending {@code LivingEvent}
 * with a null entity would compile and would turn every stray {@code getEntity()} into a silent NPE.
 *
 * <p>{@code Pre} and {@code Post} gate only their declaration line, and only for the bound: their
 * constructors' shape is identical on both sides of 1.21.2, since the first parameter is {@code T}
 * either way and only what {@code T} may be changes.
 *
 * <p><b>The renderer is the RAW vanilla type on purpose.</b> The loader declares it as
 * {@code M extends EntityModel<T>}'s renderer, but {@code EntityModel} gained a render-state type
 * parameter at 1.21.2 and the model parameter is read by nobody in this tree — every call site
 * either passes {@code this} or relays {@link #getRenderer()} straight back into another
 * constructor. A raw declaration therefore compiles unchanged across the whole 1.20.1→26.2 range,
 * where a bounded one would need a gate per band. {@code M} survives as an unbounded parameter only
 * so the mod's existing explicitly-parameterised {@code new …Pre<WatcherEntity, WatcherModel>(…)}
 * call sites keep their spelling.
 *
 * <p><b>Never import the renderer type.</b> The {@code !mc2102-render-import-living} replacement
 * rewrites the whole import statement onto this mod's render shim from 1.21.2, which would silently
 * retype this field on 23 nodes. It is spelled out inline for that reason, which the rule cannot
 * reach. The same goes for the render-state type in the gated declaration below.
 */
//? if >=1.21.2 {
/*public class RenderLivingEvent<T extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState, M> extends com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Event {
*///?} else {
public class RenderLivingEvent<T extends LivingEntity, M> extends LivingEvent {
//?}

    private final net.minecraft.client.renderer.entity.LivingEntityRenderer renderer;
    private final float partialTick;
    private final PoseStack poseStack;
    private final MultiBufferSource bufferSource;
    private final int packedLight;

    //? if >=1.21.2 {
    /*private final T renderState;

    public RenderLivingEvent(T renderState, net.minecraft.client.renderer.entity.LivingEntityRenderer renderer,
                             float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        this.renderState = renderState;
        this.renderer = renderer;
        this.partialTick = partialTick;
        this.poseStack = poseStack;
        this.bufferSource = bufferSource;
        this.packedLight = packedLight;
    }

    public T getRenderState() {
        return renderState;
    }
    *///?} else {
    public RenderLivingEvent(T entity, net.minecraft.client.renderer.entity.LivingEntityRenderer renderer,
                             float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super(entity);
        this.renderer = renderer;
        this.partialTick = partialTick;
        this.poseStack = poseStack;
        this.bufferSource = bufferSource;
        this.packedLight = packedLight;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getEntity() {
        return (T) super.getEntity();
    }
    //?}

    public net.minecraft.client.renderer.entity.LivingEntityRenderer getRenderer() {
        return renderer;
    }

    public float getPartialTick() {
        return partialTick;
    }

    public PoseStack getPoseStack() {
        return poseStack;
    }

    public MultiBufferSource getMultiBufferSource() {
        return bufferSource;
    }

    public int getPackedLight() {
        return packedLight;
    }

    /** Cancelling this suppresses the whole vanilla draw; the dispatcher must honour that. */
    @Cancelable
    //? if >=1.21.2
    /*public static class Pre<T extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState, M> extends RenderLivingEvent<T, M> {*/
    //? if <1.21.2
    public static class Pre<T extends LivingEntity, M> extends RenderLivingEvent<T, M> {

        public Pre(T entity, net.minecraft.client.renderer.entity.LivingEntityRenderer renderer,
                   float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
            super(entity, renderer, partialTick, poseStack, bufferSource, packedLight);
        }
    }

    /** Fired after the draw, and only when {@link Pre} was not cancelled. */
    //? if >=1.21.2
    /*public static class Post<T extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState, M> extends RenderLivingEvent<T, M> {*/
    //? if <1.21.2
    public static class Post<T extends LivingEntity, M> extends RenderLivingEvent<T, M> {

        public Post(T entity, net.minecraft.client.renderer.entity.LivingEntityRenderer renderer,
                    float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
            super(entity, renderer, partialTick, poseStack, bufferSource, packedLight);
        }
    }
}
