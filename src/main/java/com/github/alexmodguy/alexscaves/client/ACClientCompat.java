package com.github.alexmodguy.alexscaves.client;

import com.mojang.blaze3d.vertex.PoseStack;
// Imported rather than spelled out inline, unlike the rest of the blaze3d names in this file:
// 26.2 lifts the nested Mode enum out to com.mojang.blaze3d.PrimitiveTopology and the rename rule
// that does it matches the plain substring "VertexFormat.Mode", so a fully-qualified occurrence
// would come out with a doubled package prefix.
import com.mojang.blaze3d.vertex.VertexFormat;
// Forge deleted RenderLevelStageEvent in 1.21.2, and from 1.21.6 NeoForge's copy is unusable to a
// mod that wants all six stages: the event became an abstract base with one concrete subclass per
// stage, its bus refuses a listener for an abstract event class outright, and the three opaque
// layers collapsed into a single AfterOpaqueBlocks that cannot tell cutout from cutout-mipped. So
// >=1.21.6 drives the stages from mixin.client.LevelRenderStageMixin on every loader — which is the
// Fabric path as well. See ACLevelRenderStage.
//? if !fabric && (!forge || <1.21.2) && <1.21.6
import net.minecraftforge.client.event.RenderLevelStageEvent;
// TriState moved into vanilla in 1.21.5 and NeoForge dropped its own copy. Two single-line
// import gates rather than a fully-qualified name in the body, because the one place that
// names it already sits inside a //? arm and gates cannot nest.
//? if neoforge && >=1.20.5 && <1.21.5
/*import net.neoforged.neoforge.common.util.TriState;*/
//? if >=1.21.5
/*import net.minecraft.util.TriState;*/

/**
 * The client-side twin of {@link com.github.alexmodguy.alexscaves.server.misc.ACCompat}.
 *
 * <p>Everything here exists for the same reason: a rendering API changed shape across this mod's
 * version range and the call sites should not have to know. It is a separate class from ACCompat
 * because these signatures name client-only types, and ACCompat is loaded on a dedicated server.
 */
public class ACClientCompat {

    /**
     * The level-render pose stack, as {@code RenderLevelStageEvent} used to hand it over.
     *
     * <p>1.20.5 stopped threading a {@code PoseStack} through the level renderer — the modelview
     * transform travels as a bare {@code Matrix4f} now — and Forge's event followed, so
     * {@code getPoseStack()} keeps its name but changes its return type. Everything in this mod
     * that renders from the event wants a stack it can push and pop, so one is built around the
     * matrix here rather than at seven call sites.
     *
     * <p>Only <em>Forge</em> followed vanilla here. NeoForge kept {@code getPoseStack()} returning a
     * {@code PoseStack} (it builds one around the modelview matrix inside the event), so its arm is
     * the same as the pre-1.20.5 one.
     *
     * <p>From Forge 1.21.2, and from 1.21.6 on every loader, there is no usable event at all and
     * this has no callers, so it is not declared — the three arms are flat rather than nested because
     * every other gate in this tree is, and a gated-out arm is a block comment that cannot contain
     * another.
     */
    //? if fabric || (forge && >=1.21.2) || >=1.21.6 {
    /*// No RenderLevelStageEvent on this node; mixin.client.LevelRenderStageMixin supplies the stages.
    *///?} elif forge && >=1.20.5 {
    /*public static PoseStack poseStack(RenderLevelStageEvent event) {
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(event.getPoseStack());
        return poseStack;
    }
    *///?} else {
    public static PoseStack poseStack(RenderLevelStageEvent event) {
        return event.getPoseStack();
    }
    //?}

    /**
     * Which of this mod's render stages the event is announcing, or null for one it does not draw in.
     *
     * @see ACLevelRenderStage
     */
    //? if fabric || (forge && >=1.21.2) || >=1.21.6 {
    /*// See above — no event to translate.
    *///?} else {
    public static ACLevelRenderStage stageOf(RenderLevelStageEvent event) {
        RenderLevelStageEvent.Stage stage = event.getStage();
        if (stage == RenderLevelStageEvent.Stage.AFTER_SKY) {
            return ACLevelRenderStage.AFTER_SKY;
        } else if (stage == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return ACLevelRenderStage.AFTER_ENTITIES;
        } else if (stage == RenderLevelStageEvent.Stage.AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS) {
            return ACLevelRenderStage.AFTER_CUTOUT_MIPPED_BLOCKS;
        } else if (stage == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            return ACLevelRenderStage.AFTER_CUTOUT_BLOCKS;
        } else if (stage == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return ACLevelRenderStage.AFTER_TRANSLUCENT_BLOCKS;
        } else if (stage == RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS) {
            return ACLevelRenderStage.AFTER_TRIPWIRE_BLOCKS;
        }
        return null;
    }
    //?}

    /**
     * How far the frame being rendered is between two ticks, as {@code RenderLevelStageEvent} used
     * to hand it over.
     *
     * <p>1.21 replaced the client's loose partial-tick floats with a {@code DeltaTracker}, and
     * NeoForge's event followed: {@code getPartialTick()} keeps its name and returns the tracker
     * instead. Forge left the event returning a {@code float}, so this only splits on NeoForge.
     *
     * <p>{@code getGameTimeDeltaPartialTick(false)} is the right question of the two the tracker
     * answers — it is the value vanilla's own level rendering uses, and it respects {@code /tick
     * freeze}, which is what the pre-1.21 event field did as well. That makes it the opposite
     * choice from {@link #frameTime()}, whose callers want the pause-aware residual.
     */
    //? if fabric || (forge && >=1.21.2) || >=1.21.6 {
    /*// See #poseStack — no event on this node.
    *///?} elif neoforge && >=1.21 {
    /*public static float partialTick(RenderLevelStageEvent event) {
        return event.getPartialTick().getGameTimeDeltaPartialTick(false);
    }
    *///?} else {
    public static float partialTick(RenderLevelStageEvent event) {
        return event.getPartialTick();
    }
    //?}

    /**
     * Whether a name tag should be drawn, after {@code RenderNameTagEvent} has been posted.
     *
     * <p>Two renderers in this mod take over {@code EntityRenderer#render} wholesale and so have to
     * fire that event themselves. The question they ask is always the same — a listener may force
     * the tag on, force it off, or leave the decision to the renderer.
     *
     * <p>NeoForge's EventBus 8 (1.20.5+) deleted {@code Event.Result}, and the event answers with a
     * {@code TriState} of its own instead. The three states line up exactly, so only this one body
     * differs.
     *
     * @param vanillaAnswer what the renderer would have decided on its own — {@code shouldShowName}
     */
    public static boolean shouldRenderNameTag(
            // NeoForge 1.21.2 split the event into an abstract base plus CanRender/DoRender; only the
            // former carries the question and the (possibly replaced) content. Forge did not follow —
            // it re-keyed the same flat event on the render state and kept Event.Result.
            //? if neoforge && >=1.21.2 {
            /*net.minecraftforge.client.event.RenderNameTagEvent.CanRender event,
            *///?} else {
            net.minecraftforge.client.event.RenderNameTagEvent event,
            //?}
            boolean vanillaAnswer) {
        // Forge 62 dropped Event.Result for the eventbus-7 Cancellable marker, which declares no
        // methods at all — isCanceled() is woven in by the class transformer and is invisible to
        // javac. So the verdict cannot be read off the event here; WatcherRenderer, the only caller
        // left on those nodes, takes it from post()'s boolean return instead. This arm exists purely
        // so the helper keeps one signature on all 58 nodes.
        //? if forge && >=26 {
        /*return vanillaAnswer;
        *///?} elif neoforge && >=1.20.5 {
        /*TriState canRender = event.canRender();
        return canRender == TriState.TRUE
                || (canRender == TriState.DEFAULT && vanillaAnswer);
        *///?} else {
        return event.getResult() != net.minecraftforge.eventbus.api.Event.Result.DENY
                && (event.getResult() == net.minecraftforge.eventbus.api.Event.Result.ALLOW || vanillaAnswer);
        //?}
    }

    // The vertex memory behind the open beginTesselator/drawImmediate pair on 26.2, where there is
    // no shared Tesselator to borrow one from. Only ever one is live, because a caller must draw
    // what it began before beginning again; closeImmediateScratch is the belt-and-braces for a
    // caller that throws between the two, which would otherwise leak the native allocation.
    //? if >=26.2 {
    /*private static com.mojang.blaze3d.vertex.ByteBufferBuilder immediateScratch;

    private static void closeImmediateScratch() {
        if (immediateScratch != null) {
            immediateScratch.close();
            immediateScratch = null;
        }
    }
    *///?}

    /**
     * Opens the shared tesselator for one immediate-mode draw.
     *
     * <p>Through 1.20.6 a {@code Tesselator} handed out its one long-lived {@code BufferBuilder} and
     * you called {@code begin} on that. 1.21 inverted it: {@code begin} lives on the tesselator and
     * <em>returns</em> the builder, which is now valid only until the draw. {@code getBuilder()} is
     * gone, so the two-line idiom cannot survive as written.
     *
     * <p>Pairing rule for callers: every {@code beginTesselator} needs exactly one
     * {@code drawTesselator} on the builder it returned, and the local holding it must not be
     * {@code final} where a method opens the tesselator more than once — the 1.21 builder is a new
     * object each time, not the same one re-armed.
     */
    public static com.mojang.blaze3d.vertex.BufferBuilder beginTesselator(
            VertexFormat.Mode mode, com.mojang.blaze3d.vertex.VertexFormat format) {
        //? if >=26.2 {
        /*// 26.2 deleted Tesselator outright — there is no shared scratch buffer any more, because
        // nothing in vanilla accumulates a mesh on the CPU and draws it immediately except the
        // handful of places that own a GpuBuffer for the whole run. A BufferBuilder is still the
        // way to fill vertices, it just needs its own ByteBufferBuilder handed to it.
        //
        // One is allocated per draw and freed in drawImmediate's finally rather than kept alive
        // as a shared scratch: the pairing rule already guarantees one begin per draw, and these
        // are a handful of overlay draws a frame, so the allocation is cheaper than getting the
        // Result lifetime wrong. immediateScratch holds it across the pair for that reason.
        closeImmediateScratch();
        immediateScratch = new com.mojang.blaze3d.vertex.ByteBufferBuilder(1536);
        return new com.mojang.blaze3d.vertex.BufferBuilder(immediateScratch, mode, format);
        *///?} elif >=1.21 {
        /*return com.mojang.blaze3d.vertex.Tesselator.getInstance().begin(mode, format);
        *///?} else {
        com.mojang.blaze3d.vertex.BufferBuilder buffer = com.mojang.blaze3d.vertex.Tesselator.getInstance().getBuilder();
        buffer.begin(mode, format);
        return buffer;
        //?}
    }

    /**
     * Draws and closes what {@link #beginTesselator} opened.
     *
     * <p>The pre-1.21 {@code Tesselator#end()} did exactly this pair — finish the buffer, hand the
     * result to {@code BufferUploader.drawWithShader} — so the two arms are the same call sequence
     * spelled at different levels. The builder argument is unused below 1.21, where the tesselator
     * still knows which buffer is open; it is taken on every version so the call sites read the same.
     *
     * <p>1.21.5 deleted {@code BufferUploader}, and with it the whole notion of drawing a mesh
     * against whatever shader and fixed-function state happens to be bound — see
     * {@link ImmediateDraw}, which is what every caller uses from that version. The one caller left
     * below 1.21.5 is {@link #drawImmediate}'s own arm, so the newest arm is unreachable rather
     * than merely unused; it throws instead of silently drawing nothing.
     */
    public static void drawTesselator(com.mojang.blaze3d.vertex.BufferBuilder buffer) {
        //? if >=1.21.5 {
        /*throw new UnsupportedOperationException("drawTesselator has no 1.21.5 form; draw through ImmediateDraw");
        *///?} elif >=1.21 {
        /*com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(buffer.buildOrThrow());
        *///?} else {
        com.mojang.blaze3d.vertex.Tesselator.getInstance().end();
        //?}
    }

    /**
     * The shapes of hand-rolled immediate-mode draw this mod does, and every piece of state each
     * one used to set around itself.
     *
     * <p>Through 1.21.4 such a draw was written out longhand at the call site: bind a core shader
     * and a texture, poke {@code RenderSystem} for blend/depth/cull, fill the tesselator, hand the
     * result to {@code BufferUploader}, poke {@code RenderSystem} back. 1.21.5 deleted all three
     * halves of that — the fixed-function setters, the core-shader getters and
     * {@code BufferUploader} — and left exactly one way to put a mesh on screen:
     * {@link net.minecraft.client.renderer.RenderType#draw}, whose {@code RenderPipeline} carries
     * the state instead. The two spellings have nothing in common, so rather than gate nineteen
     * call sites this enum names the shapes and {@link #beginImmediate}/{@link #drawImmediate}
     * own the difference.
     *
     * <p>The flags describe what the <em>old</em> call sites did, verbatim, so the pre-1.21.5 arm
     * is a behavioural no-op: {@code blend} enables blending and the default function beforehand,
     * {@code unblend} disables it again afterwards, {@code overlay} additionally turns the depth
     * test off and closes the depth mask for the duration, and {@code noCull} drops backface
     * culling. Kinds that set none of them ran on whatever state was ambient. From 1.21.5 the
     * matching pipeline in {@code ACInternalShaders} states the same thing declaratively.
     *
     * <p>The one deliberate divergence: the pathfinding overlay enabled blending only when the
     * requested alpha was not 255 and disabled it otherwise. Translucent blending at full alpha is
     * the identity, so {@code POSITION_COLOR_*} blends unconditionally on every version.
     */
    public enum ImmediateDraw {
        POSITION_COLOR_FAN(VertexFormat.Mode.TRIANGLE_FAN, com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR, true, true, false, false),
        POSITION_COLOR_LINES(VertexFormat.Mode.DEBUG_LINES, com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR, true, true, false, false),
        POSITION_TEX_FAN(VertexFormat.Mode.TRIANGLE_FAN, com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX, false, false, false, false),
        POSITION_TEX_TRIANGLES(VertexFormat.Mode.TRIANGLES, com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX, false, false, false, false),
        POSITION_TEX_QUADS(VertexFormat.Mode.QUADS, com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX, false, false, false, false),
        POSITION_TEX_QUADS_BLEND(VertexFormat.Mode.QUADS, com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX, true, false, false, false),
        SCREEN_OVERLAY_QUADS(VertexFormat.Mode.QUADS, com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX, true, false, true, false),
        PARTICLE_QUADS(VertexFormat.Mode.QUADS, com.mojang.blaze3d.vertex.DefaultVertexFormat.PARTICLE, true, false, false, true),
        POSITION_TEX_COLOR_QUADS(VertexFormat.Mode.QUADS, com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR_TEX, true, true, false, false);

        private final VertexFormat.Mode mode;
        private final com.mojang.blaze3d.vertex.VertexFormat format;
        private final boolean blend;
        private final boolean unblend;
        private final boolean overlay;
        private final boolean noCull;

        ImmediateDraw(VertexFormat.Mode mode, com.mojang.blaze3d.vertex.VertexFormat format,
                      boolean blend, boolean unblend, boolean overlay, boolean noCull) {
            this.mode = mode;
            this.format = format;
            this.blend = blend;
            this.unblend = unblend;
            this.overlay = overlay;
            this.noCull = noCull;
        }
    }

    /**
     * The tint the next {@link #drawImmediate} multiplies its texture by, or {@code null} for none.
     *
     * <p>Read from {@code RenderType$CompositeRenderType#draw} by {@code CompositeRenderTypeMixin},
     * which is why it is public; nothing else should touch it. It exists only from 1.21.6 in
     * practice, but is declared unconditionally so {@link #setImmediateTint} needs no field gate.
     */
    @javax.annotation.Nullable
    public static org.joml.Vector4f immediateTint;

    /**
     * Sets the colour every following vertex of the next {@link #drawImmediate} is multiplied by.
     *
     * <p>Through 1.21.5 this was {@code RenderSystem#setShaderColor}: one piece of global
     * fixed-function state, set before the draw and reset to white after. 1.21.6 deleted it. The
     * colour modulator is a member of the per-draw {@code DynamicTransforms} uniform block now, and
     * {@code RenderType#draw} — the only way a mod can put a mesh on screen — hardcodes it to white,
     * so there is no argument to pass and no state to set.
     *
     * <p>Rather than reimplement that fifty-line draw with one constant changed, the tint is parked
     * here and {@code CompositeRenderTypeMixin} substitutes it for vanilla's white
     * {@code Vector4f} — see {@link #drawImmediate}, which consumes and clears it. White is the
     * identity, so the call sites' trailing "reset to white" stays meaningful on every version.
     */
    public static void setImmediateTint(float red, float green, float blue, float alpha) {
        //? if >=1.21.6 {
        /*immediateTint = new org.joml.Vector4f(red, green, blue, alpha);
        *///?} else {
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(red, green, blue, alpha);
        //?}
    }

    /** {@link #beginImmediate(ImmediateDraw, net.minecraft.resources.ResourceLocation)} for the untextured shapes. */
    public static com.mojang.blaze3d.vertex.BufferBuilder beginImmediate(ImmediateDraw kind) {
        return beginImmediate(kind, null);
    }

    /**
     * Opens the tesselator for one {@link ImmediateDraw}, with every piece of setup that shape
     * needs. Pair it with exactly one {@link #drawImmediate} passing the same kind and texture.
     *
     * <p>From 1.21.5 this is only the {@code begin} — the render type built in
     * {@code ACRenderTypes} binds the shader, the texture and the fixed-function state at draw
     * time, which is the whole point of a pipeline.
     */
    public static com.mojang.blaze3d.vertex.BufferBuilder beginImmediate(ImmediateDraw kind, @javax.annotation.Nullable net.minecraft.resources.ResourceLocation texture) {
        //? if <1.21.5 {
        if (kind.blend) {
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        }
        if (kind.overlay) {
            com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
            com.mojang.blaze3d.systems.RenderSystem.depthMask(false);
        }
        if (kind.noCull) {
            com.mojang.blaze3d.systems.RenderSystem.disableCull();
            com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        }
        if (texture != null) {
            bindTextureForSetup(texture);
            com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, texture);
        }
        switch (kind) {
            case POSITION_COLOR_FAN, POSITION_COLOR_LINES -> setPositionColorShader();
            case POSITION_TEX_COLOR_QUADS -> setPositionTexColorShader();
            default -> setPositionTexShader();
        }
        //?}
        return beginTesselator(kind.mode, kind.format);
    }

    /** Draws and closes what {@link #beginImmediate} opened, and undoes its setup. */
    public static void drawImmediate(ImmediateDraw kind, com.mojang.blaze3d.vertex.BufferBuilder buffer,
                                     @javax.annotation.Nullable net.minecraft.resources.ResourceLocation texture) {
        //? if >=26.2 {
        /*// 26.2 deleted RenderType#draw(MeshData) with the rest of immediate mode: a render type
        // no longer owns a draw call, it hands out a PreparedRenderType — pipeline, output target,
        // dynamic transforms, scissor and textures resolved for this frame — which draws from GPU
        // buffers. So the mesh has to reach the GPU first.
        //
        // The vertices become a one-shot USAGE_VERTEX buffer; the indices come from the shared
        // per-topology sequential buffer (0,1,2,… or the quad pattern), which is what every vanilla
        // caller of drawFromBuffer uses for an unsorted mesh — this mod's meshes are never sorted,
        // so MeshData#indexBuffer is null and there is nothing of its own to upload. baseVertex and
        // firstIndex are 0 because the buffer holds exactly this one draw.
        try (com.mojang.blaze3d.vertex.MeshData mesh = buffer.buildOrThrow()) {
            com.mojang.blaze3d.vertex.MeshData.DrawState drawState = mesh.drawState();
            com.mojang.blaze3d.systems.RenderSystem.AutoStorageIndexBuffer sequential =
                    com.mojang.blaze3d.systems.RenderSystem.getSequentialBuffer(drawState.primitiveTopology());
            com.mojang.blaze3d.buffers.GpuBuffer indices = sequential.getBuffer(drawState.indexCount());
            com.mojang.blaze3d.buffers.GpuBuffer vertices = com.mojang.blaze3d.systems.RenderSystem.getDevice()
                    .createBuffer(() -> "Alex's Caves immediate draw", com.mojang.blaze3d.buffers.GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
            try {
                com.github.alexmodguy.alexscaves.client.render.ACRenderTypes.getImmediate(kind, texture).prepare()
                        .drawFromBuffer(vertices, indices, sequential.type(), 0, 0, drawState.indexCount());
            } finally {
                vertices.close();
            }
        } finally {
            closeImmediateScratch();
            immediateTint = null;
        }
        *///?} elif >=1.21.5 {
        /*try {
            com.github.alexmodguy.alexscaves.client.render.ACRenderTypes.getImmediate(kind, texture).draw(buffer.buildOrThrow());
        } finally {
            // One draw, one tint: clearing here is what keeps a pending colour from leaking onto a
            // later draw that never asked for one — including a vanilla draw, since the mixin that
            // reads it sits on the shared render type rather than on anything of this mod's.
            immediateTint = null;
        }
        *///?} else {
        drawTesselator(buffer);
        if (kind.unblend) {
            com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        }
        if (kind.overlay) {
            com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
            com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        }
        //?}
    }

    /** {@link #drawImmediate(ImmediateDraw, com.mojang.blaze3d.vertex.BufferBuilder, net.minecraft.resources.ResourceLocation)} for the untextured shapes. */
    public static void drawImmediate(ImmediateDraw kind, com.mojang.blaze3d.vertex.BufferBuilder buffer) {
        drawImmediate(kind, buffer, null);
    }

    /**
     * How far the client is between the last tick and the next, for animation.
     *
     * <p>1.21 replaced the client's two partial-tick accessors with a {@code DeltaTracker}, whose
     * {@code getGameTimeDeltaPartialTick(boolean)} takes over from both. The boolean asks whether to
     * ignore a frozen game — {@code /tick freeze} — and passing {@code true} reproduces the older
     * pair exactly, because before 1.21 neither of them knew about freezing at all.
     *
     * <p>Two helpers rather than one because the originals were not the same method: this one is the
     * timer's raw residual and {@link #partialTick()} is the pause-aware one they differ on only
     * while the game is paused. On 1.21 the raw value is no longer reachable, so both arrive at the
     * pause-aware residual — which is the value vanilla itself renders a paused frame with.
     */
    public static float frameTime() {
        //? if >=1.21 {
        /*return net.minecraft.client.Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        *///?} else {
        return net.minecraft.client.Minecraft.getInstance().getFrameTime();
        //?}
    }

    /**
     * The partial tick a paused frame is rendered with. See {@link #frameTime()}.
     *
     * <p>Below 1.21 this is Forge's {@code getPartialTick()}, which reads a Forge-added
     * {@code realPartialTick} field — assigned, in Forge's own patch of the render loop, from the
     * very expression vanilla inlines at its one call site: {@code pause ? pausePartialTick :
     * timer.partialTick}. So the Fabric arm is not an approximation of it, it is the same value
     * spelled out, with {@link #frameTime()} standing in for the raw residual.
     */
    public static float partialTick() {
        //? if >=1.21 {
        /*return net.minecraft.client.Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        *///?} elif fabric {
        /*net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        return mc.isPaused() ? mc.pausePartialTick : mc.getFrameTime();
        *///?} else {
        return net.minecraft.client.Minecraft.getInstance().getPartialTick();
        //?}
    }

    /**
     * Nudges the camera along its own axes — the screen-shake and third-person pull-back this mod does.
     *
     * <p>1.21 retyped {@code Camera#move} and {@code Camera#getMaxZoom} from {@code double} to
     * {@code float}. Call sites keep speaking {@code double} (that is what the tremor maths produces)
     * and the narrowing happens here, once.
     *
     * @see com.github.alexmodguy.alexscaves.mixin.client.CameraAccessor for why this is an invoker
     */
    public static void cameraMove(net.minecraft.client.Camera camera, double forwards, double up, double side) {
        com.github.alexmodguy.alexscaves.mixin.client.CameraAccessor accessor =
                (com.github.alexmodguy.alexscaves.mixin.client.CameraAccessor) camera;
        //? if >=1.21 {
        /*accessor.ac$move((float) forwards, (float) up, (float) side);
        *///?} else {
        accessor.ac$move(forwards, up, side);
        //?}
    }

    /** How far back the camera may pull before it hits geometry. See {@link #cameraMove}. */
    public static double cameraMaxZoom(net.minecraft.client.Camera camera, double startingDistance) {
        com.github.alexmodguy.alexscaves.mixin.client.CameraAccessor accessor =
                (com.github.alexmodguy.alexscaves.mixin.client.CameraAccessor) camera;
        //? if >=1.21 {
        /*return accessor.ac$getMaxZoom((float) startingDistance);
        *///?} else {
        return accessor.ac$getMaxZoom(startingDistance);
        //?}
    }

    /**
     * The camera's near plane, used here to hang a trail's first vertex right in front of the eye.
     *
     * <p>26 made the field of view a parameter — {@code getNearPlane(float fovDegrees)} — where the
     * no-arg form read {@code Options#fov()} itself. The successor is the camera's own
     * {@code getFov()}, which is that same option after the fov modifier and the death/fluid warp
     * have been applied, i.e. the angle the frame is actually projected with; matching the plane to
     * the real projection is the point of the method, so it is also the better answer.
     */
    public static net.minecraft.client.Camera.NearPlane nearPlane(net.minecraft.client.Camera camera) {
        //? if >=26 {
        /*return camera.getNearPlane(camera.getFov());
        *///?} else {
        return camera.getNearPlane();
        //?}
    }

    // ── 1.21's packed vertex colour ─────────────────────────────────────────────
    // Everything that used to take four float channels — Model#renderToBuffer, ModelPart#render,
    // VertexConsumer#addVertex — takes one packed ARGB int from 1.21 on. Most of this mod's models
    // never see that, because Citadel's BasicEntityModel keeps the four-float shape and bridges;
    // these two helpers are for the call sites whose receiver is a vanilla type, which cannot.

    /**
     * Draws a model tinted by four channels.
     *
     * @see com.github.alexmodguy.alexscaves.server.misc.ACColors#argbF for the packing
     */
    public static void renderToBuffer(
            net.minecraft.client.model.Model model,
            PoseStack poseStack,
            com.mojang.blaze3d.vertex.VertexConsumer consumer,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue,
            float alpha) {
        //? if >=1.21.2 {
        /*// 1.21.2 made BOTH of Model#renderToBuffer's overloads final, and the compat EntityModel
        // hands vanilla an empty root — so going through the vanilla entry point with one of this
        // mod's own models emits no vertices at all. The hierarchy's real draw call is the compat
        // model's eight-float overload (compat/RenderLayer#renderColoredModel says the same thing
        // one level down).
        if (model instanceof com.github.alexmodguy.alexscaves.client.render.compat.EntityModel<?> acModel) {
            acModel.renderToBuffer(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        } else {
            model.renderToBuffer(poseStack, consumer, packedLight, packedOverlay,
                    com.github.alexmodguy.alexscaves.server.misc.ACColors.argbF(alpha, red, green, blue));
        }
        *///?} elif >=1.21 {
        /*model.renderToBuffer(poseStack, consumer, packedLight, packedOverlay,
                com.github.alexmodguy.alexscaves.server.misc.ACColors.argbF(alpha, red, green, blue));
        *///?} else {
        model.renderToBuffer(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        //?}
    }

    /**
     * Emits one fully-specified vertex, packing its tint the way {@link #renderToBuffer} does.
     *
     * <p>Named {@code emitVertex} and not {@code addVertex} on purpose: the {@code .vertex(} ->
     * {@code .addVertex(} replacement rule would otherwise rewrite every call to this helper, in
     * one direction or the other, on every node.
     */
    public static void emitVertex(
            com.mojang.blaze3d.vertex.VertexConsumer consumer,
            float x,
            float y,
            float z,
            float red,
            float green,
            float blue,
            float alpha,
            float u,
            float v,
            int packedOverlay,
            int packedLight,
            float normalX,
            float normalY,
            float normalZ) {
        //? if >=1.21 {
        /*consumer.addVertex(x, y, z, com.github.alexmodguy.alexscaves.server.misc.ACColors.argbF(alpha, red, green, blue),
                u, v, packedOverlay, packedLight, normalX, normalY, normalZ);
        *///?} else {
        consumer.vertex(x, y, z, red, green, blue, alpha, u, v, packedOverlay, packedLight, normalX, normalY, normalZ);
        //?}
    }

    /**
     * The buffer an armour-textured item renders into.
     *
     * <p>1.21 dropped {@code getArmorFoilBuffer}'s {@code noEntity} flag — the armour render type it
     * is handed already decides that — leaving only the glint flag every call site here varies.
     */
    public static com.mojang.blaze3d.vertex.VertexConsumer armorFoilBuffer(
            net.minecraft.client.renderer.MultiBufferSource bufferSource,
            net.minecraft.client.renderer.RenderType renderType,
            boolean withGlint) {
        //? if >=26.2 {
        /*// 26.2 took the foil helpers private, onto the frame's own ItemFeatureRenderer — there is
        // no static entry point left. ACDrawCollector reproduces the four-line choice vanilla makes
        // (see the comment there); this is that, over the buffer source the caller handed in, with
        // `sheeted = false` for the same reason the 1.21.9 arm passed `false`: armour is drawn on an
        // entity, never on the flat GUI sheet.
        if (!withGlint) {
            return bufferSource.getBuffer(renderType);
        }
        return com.github.alexmodguy.alexscaves.client.render.compat.ACDrawCollector.foilBuffer(
                bufferSource::getBuffer, renderType, false);
        *///?} elif >=1.21.9 {
        /*// 1.21.9 folded the armour and item foil buffers into one getFoilBuffer, whose third
        // argument picks the glint render type: `true` is the flat GUI sheet, `false` the entity
        // one. Armour is always drawn on an entity, so it takes the same branch getArmorFoilBuffer
        // always did.
        return net.minecraft.client.renderer.entity.ItemRenderer.getFoilBuffer(bufferSource, renderType, false, withGlint);
        *///?} elif >=1.21 {
        /*return net.minecraft.client.renderer.entity.ItemRenderer.getArmorFoilBuffer(bufferSource, renderType, withGlint);
        *///?} else {
        return net.minecraft.client.renderer.entity.ItemRenderer.getArmorFoilBuffer(bufferSource, renderType, false, withGlint);
        //?}
    }

    /**
     * The camera's orientation — what everything that billboards toward the viewer multiplies its
     * pose by.
     *
     * <p>1.21.9 deleted {@code EntityRenderDispatcher#cameraOrientation} together with the override
     * slot behind it; the render pipeline threads a {@code CameraRenderState} down to each submit
     * instead. The dispatcher still holds the live {@code Camera}, which is what the deleted method
     * read whenever nothing had overridden it — and nothing in this tree ever did, outside the one
     * inventory macro noted on {@link #overrideCameraOrientation}.
     */
    public static org.joml.Quaternionf cameraOrientation() {
        net.minecraft.client.renderer.entity.EntityRenderDispatcher dispatcher =
                net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher();
        //? if >=1.21.9 {
        /*return dispatcher.camera.rotation();
        *///?} else {
        return dispatcher.cameraOrientation();
        //?}
    }

    /**
     * Squared distance from the camera to a point. 1.21.9 left the dispatcher only the
     * {@code Entity} overload, so the coordinate one is spelled out against the camera position it
     * used to read.
     */
    public static double dispatcherDistanceToSqr(
            net.minecraft.client.renderer.entity.EntityRenderDispatcher dispatcher, double x, double y, double z) {
        //? if >=1.21.9 {
        /*return dispatcher.camera.getPosition().distanceToSqr(x, y, z);
        *///?} else {
        return dispatcher.distanceToSqr(x, y, z);
        //?}
    }

    /**
     * The two dispatcher-wide toggles Citadel's inventory-entity macro sets around a render.
     *
     * <p>Both are gone in 1.21.9: with rendering deferred through a {@code SubmitNodeCollector}
     * there is no per-dispatcher state left to flip — a shadow is a piece list on the render state
     * and the orientation rides the {@code CameraRenderState}. The one caller
     * ({@code UiRenderMacros#drawEntity}) is vendored Citadel debug UI that nothing in this mod
     * reaches, so both become no-ops there rather than growing a state-shaped reimplementation.
     */
    public static void overrideCameraOrientation(
            net.minecraft.client.renderer.entity.EntityRenderDispatcher dispatcher, org.joml.Quaternionf orientation) {
        //? if <1.21.9 {
        dispatcher.overrideCameraOrientation(orientation);
        //?}
    }

    /** @see #overrideCameraOrientation */
    public static void setDispatcherRenderShadow(
            net.minecraft.client.renderer.entity.EntityRenderDispatcher dispatcher, boolean renderShadow) {
        //? if <1.21.9 {
        dispatcher.setRenderShadow(renderShadow);
        //?}
    }

    /**
     * The player renderer for a skin model name, as the hologram projector asks for it.
     *
     * <p>1.21.9 replaced the dispatcher's string-keyed skin map with a {@code PlayerModelType}-keyed
     * one of {@code AvatarRenderer}s (still a {@code LivingEntityRenderer} subclass, so the call
     * site's {@code instanceof} guard is unaffected). The two keys are exactly the two enum
     * constants, and the mod only ever produces {@code "slim"} or {@code "default"} here.
     *
     * <p>Fabric needs an arm of its own on BOTH sides of that split, for the same reason each time:
     * the accessor the other two loaders call ({@code getSkinMap()} below 1.21.9,
     * {@code getPlayerRenderers()} from it) is a loader patch, and this loader reaches the field
     * directly through the access widener instead. Only the key type differs between the two Fabric
     * arms, so they are ordered ahead of the loader arms rather than nested inside them.
     */
    @SuppressWarnings("rawtypes")
    public static net.minecraft.client.renderer.entity.EntityRenderer playerRenderer(
            net.minecraft.client.renderer.entity.EntityRenderDispatcher dispatcher, String modelName) {
        //? if fabric && >=1.21.9 {
        /*return dispatcher.playerRenderers.get("slim".equals(modelName)
                ? net.minecraft.world.entity.player.PlayerModelType.SLIM
                : net.minecraft.world.entity.player.PlayerModelType.WIDE);
        *///?} elif >=1.21.9 {
        /*return dispatcher.getPlayerRenderers().get("slim".equals(modelName)
                ? net.minecraft.world.entity.player.PlayerModelType.SLIM
                : net.minecraft.world.entity.player.PlayerModelType.WIDE);
        *///?} elif fabric {
        /*return dispatcher.playerRenderers.get(modelName);
        *///?} else {
        return dispatcher.getSkinMap().get(modelName);
        //?}
    }

    /**
     * The {@code EntityModel} of whatever renderer the dispatcher has for this entity, or null if
     * it is not a living renderer.
     *
     * <p>From 1.21.2 {@code EntityRenderDispatcher#getRenderer} is parameterised on a render state
     * rather than the entity, so the declared return type no longer conforms to anything a call
     * site can name — hence the raw type here and nowhere else.
     */
    @SuppressWarnings("rawtypes")
    public static net.minecraft.client.model.EntityModel<?> rendererModel(net.minecraft.world.entity.Entity entity) {
        net.minecraft.client.renderer.entity.EntityRenderer render =
                net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        if (render instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer living) {
            return (net.minecraft.client.model.EntityModel<?>) living.getModel();
        }
        return null;
    }

    /**
     * {@code EntityRenderDispatcher#render}. 1.21.2 dropped its yaw parameter — the body rotation
     * comes off the render state now — so the argument is only forwarded on the older versions.
     */
    public static <E extends net.minecraft.world.entity.Entity> void dispatcherRender(
            net.minecraft.client.renderer.entity.EntityRenderDispatcher dispatcher,
            E entity,
            double x,
            double y,
            double z,
            float yaw,
            float partialTick,
            PoseStack poseStack,
            net.minecraft.client.renderer.MultiBufferSource buffers,
            int packedLight) {
        //? if >=1.21.9 {
        /*// The dispatcher's own entry point became state-shaped too, and lost its light for the same
        // reason renderEntity's did — extractEntity fills lightCoords in from the entity's block
        // position, so the caller's value is written back over it. The offset is still passed to
        // submit rather than folded into the pose stack, exactly as before.
        com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers submit =
                com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers.of(buffers);
        if (submit != null) {
            net.minecraft.client.renderer.entity.state.EntityRenderState state = dispatcher.extractEntity(entity, partialTick);
            state.lightCoords = packedLight;
            submit.flush();
            dispatcher.submit(state, submit.camera(), x, y, z, poseStack, submit.collector());
        }
        *///?} elif >=1.21.2 {
        /*dispatcher.render(entity, x, y, z, partialTick, poseStack, buffers, packedLight);
        *///?} else {
        dispatcher.render(entity, x, y, z, yaw, partialTick, poseStack, buffers, packedLight);
        //?}
    }

    /**
     * The texture the dispatcher's renderer would use for this entity, or null if it is not a
     * living renderer.
     *
     * <p>1.21.2 moved {@code getTextureLocation} off {@code EntityRenderer} and onto
     * {@code LivingEntityRenderer}, taking a render state rather than the entity.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static net.minecraft.resources.ResourceLocation rendererTexture(net.minecraft.world.entity.Entity entity, float partialTick) {
        net.minecraft.client.renderer.entity.EntityRenderer render =
                net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        if (render == null) {
            return null;
        }
        //? if >=1.21.2 {
        /*if (render instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer living
                // createRenderState is declared on the plain EntityRenderer, so it answers the base
                // state type; a living renderer's own state is always the living one.
                && render.createRenderState(entity, partialTick) instanceof net.minecraft.client.renderer.entity.state.LivingEntityRenderState state) {
            return living.getTextureLocation(state);
        }
        return null;
        *///?} else {
        return render.getTextureLocation(entity);
        //?}
    }

    /**
     * Poses a model that belongs to some other entity's renderer — the three places that draw a mob
     * with a render type of their own (amber monolith, notor hologram, the guide book's entity
     * widget) rather than handing the whole job to the renderer.
     *
     * <p>Up to 1.21.1 that is just the three public flags plus {@code setupAnim}. From 1.21.2 the
     * animation input is a render state, so the state is built through the renderer that owns the
     * model — that keeps a vanilla model working too, since state and model then match — and the
     * caller's neutral-pose arguments are written over it.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void setupAnim(
            net.minecraft.client.model.EntityModel model,
            net.minecraft.world.entity.LivingEntity living,
            boolean young,
            boolean riding,
            float attackTime,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            float partialTick) {
        //? if >=1.21.2 {
        /*net.minecraft.client.renderer.entity.EntityRenderer raw =
                net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(living);
        if (raw == null) {
            return;
        }
        net.minecraft.client.renderer.entity.state.EntityRenderState state = raw.createRenderState(living, partialTick);
        if (state instanceof net.minecraft.client.renderer.entity.state.LivingEntityRenderState livingState) {
            livingState.walkAnimationPos = limbSwing;
            livingState.walkAnimationSpeed = limbSwingAmount;
            livingState.ageInTicks = ageInTicks;
            livingState.yRot = netHeadYaw;
            livingState.xRot = headPitch;
            livingState.bodyRot = 0.0F;
            livingState.isBaby = young;
        }
        if (state instanceof com.github.alexmodguy.alexscaves.client.render.compat.ACRenderState acState) {
            acState.riding = riding;
            acState.attackTime = attackTime;
        }
        model.setupAnim(state);
        *///?} else {
        model.young = young;
        model.riding = riding;
        model.attackTime = attackTime;
        model.setupAnim(living, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        //?}
    }

    /**
     * Renders one entity inside another's renderer, at the pose stack's current position and with
     * the given body yaw. A dozen places do this (every rider/held-mob layer, the submarine's
     * passengers, the amber monolith and the hologram projector) and each carried its own copy of
     * the vanilla dispatcher's crash-report wrapping.
     *
     * <p>From 1.21.2 the renderer no longer takes the entity or a yaw: it extracts a render state
     * first and renders that, so the yaw is applied by overwriting the state's rotations. Building
     * the state here also runs {@code renderstate.EntityRendererMixin}, which is what keeps
     * {@code ACStateAccess} valid for anything these nested renders trigger.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <E extends net.minecraft.world.entity.Entity> void renderEntity(
            E entity,
            float yaw,
            float partialTick,
            PoseStack poseStack,
            net.minecraft.client.renderer.MultiBufferSource buffers,
            int packedLight) {
        Object render = null;
        try {
            render = net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
            if (render != null) {
                //? if >=1.21.9 {
                /*net.minecraft.client.renderer.entity.EntityRenderer raw = (net.minecraft.client.renderer.entity.EntityRenderer) render;
                net.minecraft.client.renderer.entity.state.EntityRenderState state = raw.createRenderState(entity, partialTick);
                if (state instanceof net.minecraft.client.renderer.entity.state.LivingEntityRenderState living) {
                    living.bodyRot = yaw;
                    living.yRot = yaw;
                }
                // 1.21.9's submit(state, pose, collector, camera) has NO light parameter: the light
                // travels in the state, where extractRenderState has just filled it in from the
                // entity's own block position. Every caller here passes a light of its own — the
                // enclosing renderer's for a nested in-world render, full bright for a GUI one — so
                // writing it back reproduces the pre-1.21.9 call exactly.
                state.lightCoords = packedLight;
                // A nested render has to reach the modern submit API; unwrap the collector out of the
                // recording buffer source the enclosing legacy body was handed.
                com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers submit =
                        com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers.of(buffers);
                if (submit != null) {
                    submit.flush();
                    raw.submit(state, poseStack, submit.collector(), submit.camera());
                }
                *///?} elif >=1.21.2 {
                /*net.minecraft.client.renderer.entity.EntityRenderer raw = (net.minecraft.client.renderer.entity.EntityRenderer) render;
                net.minecraft.client.renderer.entity.state.EntityRenderState state = raw.createRenderState(entity, partialTick);
                if (state instanceof net.minecraft.client.renderer.entity.state.LivingEntityRenderState living) {
                    living.bodyRot = yaw;
                    living.yRot = yaw;
                }
                raw.render(state, poseStack, buffers, packedLight);
                *///?} else {
                ((net.minecraft.client.renderer.entity.EntityRenderer) render).render(entity, yaw, partialTick, poseStack, buffers, packedLight);
                //?}
            }
        } catch (Throwable throwable) {
            net.minecraft.CrashReport crashreport = net.minecraft.CrashReport.forThrowable(throwable, "Rendering entity in world");
            entity.fillCrashReportCategory(crashreport.addCategory("Entity being rendered"));
            net.minecraft.CrashReportCategory category = crashreport.addCategory("Renderer details");
            category.setDetail("Assigned renderer", render);
            category.setDetail("Rotation", Float.valueOf(yaw));
            category.setDetail("Delta", Float.valueOf(partialTick));
            throw new net.minecraft.ReportedException(crashreport);
        }
    }

    /**
     * The three shapes of {@code GuiGraphics#blit} this mod uses, on every version.
     *
     * <p>1.21.2 rebuilt the family: the {@code int} u/v overloads are gone, the remaining ones take
     * a {@code Function<ResourceLocation, RenderType>} as their first argument, and the
     * {@code blitOffset} that the ten-argument shape carried has no successor — depth is the pose
     * stack's business now. The old default texture size of 256×256 is likewise no longer implied,
     * so the seven-argument shape states it.
     */
    public static void blit(net.minecraft.client.gui.GuiGraphics graphics, net.minecraft.resources.ResourceLocation texture, int x, int y, int u, int v, int width, int height) {
        blit(graphics, texture, x, y, u, v, width, height, 256, 256);
    }

    public static void blit(net.minecraft.client.gui.GuiGraphics graphics, net.minecraft.resources.ResourceLocation texture, int x, int y, int u, int v, int width, int height, int textureWidth, int textureHeight) {
        // 1.21.6 names the pipeline directly where 1.21.2 took a render-type factory: a GUI blit
        // has no per-call state left to build a RenderType around, so the argument is the pipeline
        // constant that factory used to wrap. Same arity, same order.
        //? if >=1.21.6 {
        /*graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture, x, y, (float) u, (float) v, width, height, textureWidth, textureHeight);
        *///?} elif >=1.21.2 {
        /*graphics.blit(net.minecraft.client.renderer.RenderType::guiTextured, texture, x, y, (float) u, (float) v, width, height, textureWidth, textureHeight);
        *///?} else {
        graphics.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
        //?}
    }

    /** The {@code blitOffset} shape; the offset is dropped from 1.21.2, where nothing consumes it. */
    public static void blit(net.minecraft.client.gui.GuiGraphics graphics, net.minecraft.resources.ResourceLocation texture, int x, int y, int blitOffset, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        //? if >=1.21.6 {
        /*graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, textureWidth, textureHeight);
        *///?} elif >=1.21.2 {
        /*graphics.blit(net.minecraft.client.renderer.RenderType::guiTextured, texture, x, y, u, v, width, height, textureWidth, textureHeight);
        *///?} else {
        graphics.blit(texture, x, y, blitOffset, u, v, width, height, textureWidth, textureHeight);
        //?}
    }

    /**
     * The entity a {@code RenderLivingEvent} is about.
     *
     * <p>1.21.2 made the event carry a {@code LivingEntityRenderState} instead — the render-state
     * split means the entity is no longer in scope when the event fires. The mixin in
     * {@code mixin.renderstate} stashes it on every state as it is extracted, so it is still
     * reachable, for vanilla entities as well as this mod's own; see {@code ACStateAccess}.
     *
     * <p>The parameter is deliberately the raw type: the event gained a third type parameter in
     * that version, and a raw parameter is the one spelling that is legal on both sides of it.
     *
     * <p>The two loaders named the new getter differently — NeoForge {@code getRenderState()}, Forge
     * {@code getState()} — so 1.21.2 and up is two arms rather than one.
     */
    @SuppressWarnings("rawtypes")
    public static net.minecraft.world.entity.LivingEntity renderedEntity(net.minecraftforge.client.event.RenderLivingEvent event) {
        //? if forge && >=1.21.2 {
        /*return (net.minecraft.world.entity.LivingEntity) com.github.alexmodguy.alexscaves.client.render.compat.ACStateAccess.entity(event.getState());
        *///?} elif >=1.21.2 {
        /*return (net.minecraft.world.entity.LivingEntity) com.github.alexmodguy.alexscaves.client.render.compat.ACStateAccess.entity(event.getRenderState());
        *///?} else {
        return event.getEntity();
        //?}
    }

    /**
     * How far the frame a {@code RenderLivingEvent} belongs to is between two ticks.
     *
     * <p>Forge dropped the value from the event in 1.21.2 — the render state is meant to carry
     * everything the renderer may read, and vanilla's own state does not carry this — so it comes
     * back off the state through {@code ACStateAccess}, stashed there at extraction time by the same
     * mixin that stashes the entity. NeoForge kept passing it through the event.
     */
    @SuppressWarnings("rawtypes")
    public static float renderPartialTick(net.minecraftforge.client.event.RenderLivingEvent event) {
        //? if forge && >=1.21.2 {
        /*return com.github.alexmodguy.alexscaves.client.render.compat.ACStateAccess.partialTick(event.getState());
        *///?} else {
        return event.getPartialTick();
        //?}
    }

    /**
     * Whether the post-processing shader currently selected is exactly {@code effect}.
     *
     * <p>1.21.2 replaced {@code GameRenderer#currentEffect()}, which handed back the live
     * {@code PostChain} so the caller could read its name, with {@code currentPostEffect()}, which
     * hands back the id directly — the chain itself is now resolved lazily, once per frame, out of
     * the shader manager.
     */
    public static boolean isPostEffect(net.minecraft.client.renderer.GameRenderer renderer, net.minecraft.resources.ResourceLocation effect) {
        //? if >=1.21.2 {
        /*return effect.equals(renderer.currentPostEffect());
        *///?} else {
        return renderer.currentEffect() != null && effect.toString().equals(renderer.currentEffect().getName());
        //?}
    }

    /**
     * Selects a post-processing shader, answering whether it actually resolved.
     *
     * <p>Below 1.21.2 {@code loadEffect} parsed the JSON there and then and left
     * {@code effectActive} false if it could not. From 1.21.2 {@code setPostEffect} only records
     * the id and sets the flag unconditionally, so the missing-shader question has to be asked of
     * the shader manager instead — which is also where the resolution happens each frame.
     *
     * <p>{@code setPostEffect} is private in vanilla; NeoForge widened it and neither Forge nor
     * Fabric did, hence the invoker on the arm those two share. An {@code @Invoker} reaches a private
     * method on any loader, so that arm needs no access widener of its own. See
     * {@code mixin.client.GameRendererAccessor}.
     */
    public static boolean loadPostEffect(net.minecraft.client.renderer.GameRenderer renderer, net.minecraft.resources.ResourceLocation effect) {
        //? if !neoforge && >=1.21.2 {
        /*((com.github.alexmodguy.alexscaves.mixin.client.GameRendererAccessor) renderer).ac$setPostEffect(effect);
        return net.minecraft.client.Minecraft.getInstance().getShaderManager()
                .getPostChain(effect, net.minecraft.client.renderer.LevelTargetBundle.MAIN_TARGETS) != null;
        *///?} elif >=1.21.2 {
        /*renderer.setPostEffect(effect);
        return net.minecraft.client.Minecraft.getInstance().getShaderManager()
                .getPostChain(effect, net.minecraft.client.renderer.LevelTargetBundle.MAIN_TARGETS) != null;
        *///?} else {
        renderer.loadEffect(effect);
        return renderer.effectActive;
        //?}
    }

    /**
     * The box to frustum-test a part entity against, for the six renderers that cull their parent's
     * parts by hand.
     *
     * <p>One body on every version: this mod's part entities all implement
     * {@code CullingBoundsEntity}, so the interface answers even below 1.21.2 where
     * {@code Entity#getBoundingBoxForCulling()} still existed and was what they overrode. Anything
     * else falls back to what vanilla's default returned.
     */
    public static net.minecraft.world.phys.AABB cullingBox(net.minecraft.world.entity.Entity entity) {
        return entity instanceof com.github.alexmodguy.alexscaves.server.entity.util.CullingBoundsEntity culled
                ? culled.getBoundingBoxForCulling()
                : entity.getBoundingBox();
    }

    // ---------------------------------------------------------------------------------------
    // Shader fog. Up to 1.21.1 the fog was four independent RenderSystem scalars (start, end,
    // shape, colour) with a getter and a setter each; 1.21.2 collapsed them into one immutable
    // FogParameters record behind getShaderFog()/setShaderFog(). Reading a single component is
    // therefore an accessor and writing one is a whole-record rebuild. Only the two components
    // this mod touches are wrapped — the acid/soda fog override reads both planes, and the
    // watcher particle pulls the far plane in to 40 for the duration of its own draw.
    //
    // 1.21.6 takes the fog off the CPU entirely: FogRenderer writes a std140 Fog block into a ring
    // buffer once a frame and RenderSystem's getter/setter trade in an opaque GpuBufferSlice, so
    // there is nothing left to read a plane out of and nothing to write one back into. Both call
    // sites move off these helpers on that version — the fog override reads the RenderFog event's
    // own getters (which is where the values live now) and the watcher particle drops its override —
    // so all three arms are unreachable rather than merely unused, and say so instead of returning
    // a made-up number. Same shape as drawTesselator above.
    // ---------------------------------------------------------------------------------------

    public static float getShaderFogStart() {
        //? if >=1.21.6 {
        /*throw new UnsupportedOperationException("shader fog is a GPU uniform block from 1.21.6; read ViewportEvent.RenderFog");
        *///?} elif >=1.21.2 {
        /*return com.mojang.blaze3d.systems.RenderSystem.getShaderFog().start();
        *///?} else {
        return com.mojang.blaze3d.systems.RenderSystem.getShaderFogStart();
        //?}
    }

    public static float getShaderFogEnd() {
        //? if >=1.21.6 {
        /*throw new UnsupportedOperationException("shader fog is a GPU uniform block from 1.21.6; read ViewportEvent.RenderFog");
        *///?} elif >=1.21.2 {
        /*return com.mojang.blaze3d.systems.RenderSystem.getShaderFog().end();
        *///?} else {
        return com.mojang.blaze3d.systems.RenderSystem.getShaderFogEnd();
        //?}
    }

    public static void setShaderFogEnd(float end) {
        //? if >=1.21.6 {
        /*throw new UnsupportedOperationException("shader fog is a GPU uniform block from 1.21.6 and cannot be overridden per draw");
        *///?} elif >=1.21.2 {
        /*net.minecraft.client.renderer.FogParameters previous = com.mojang.blaze3d.systems.RenderSystem.getShaderFog();
        com.mojang.blaze3d.systems.RenderSystem.setShaderFog(new net.minecraft.client.renderer.FogParameters(
                previous.start(), end, previous.shape(),
                previous.red(), previous.green(), previous.blue(), previous.alpha()));
        *///?} else {
        com.mojang.blaze3d.systems.RenderSystem.setShaderFogEnd(end);
        //?}
    }

    // ---------------------------------------------------------------------------------------
    // NativeImage pixels. 1.21.2 renamed setPixelRGBA/getPixelRGBA to setPixel/getPixel AND
    // flipped their byte order: the old pair took and returned the buffer's native little-endian
    // ABGR word, the new pair take and return ARGB and convert internally. setPixelABGR is
    // private from 1.21.2, so the conversion has to happen here. Every call site in this mod
    // packs ABGR (that is what ACColors.abgr and vanilla's own lightmap arithmetic produce), so
    // these keep that convention on both sides of the split and the call sites stay identical.
    // ---------------------------------------------------------------------------------------

    public static void setPixelABGR(com.mojang.blaze3d.platform.NativeImage image, int x, int y, int abgr) {
        //? if >=1.21.2 {
        /*image.setPixel(x, y, net.minecraft.util.ARGB.fromABGR(abgr));
        *///?} else {
        image.setPixelRGBA(x, y, abgr);
        //?}
    }

    /**
     * {@code RenderSystem.runAsFancy}, which 1.21.2 deleted. Six batched block/entity renderers in
     * this mod use it to force FANCY graphics for the duration of one draw, because their translucency
     * only sorts correctly under the shader-transparency framebuffers.
     *
     * <p>From 1.21.2 this is vanilla's own body, transplanted — every piece of it
     * ({@code useShaderTransparency}, the {@code graphicsMode} option instance) still exists, only the
     * wrapper went away.
     *
     * <p>1.21.11 deleted {@code GraphicsStatus} and {@code Options#graphicsMode} outright; the
     * FABULOUS distinction is now a boolean {@code Options#improvedTransparency()}, which is exactly
     * what {@code Minecraft.useShaderTransparency()} reads. <strong>Setting that option is not a
     * viable translation</strong>: every graphics {@code OptionInstance}'s update callback calls
     * {@code Options#setGraphicsPresetToCustom()}, so a per-draw flip would permanently rewrite the
     * player's graphics preset to CUSTOM in their own options screen.
     *
     * <p>So the flip moves to where its only observable effect ever was. Vanilla's own helper guarded
     * on {@code useShaderTransparency()} and restored the option afterwards — i.e. the whole point was
     * to make that one query answer {@code false} for the duration of the draw, and the thing that
     * asks it inside these six renderers is {@code ItemRenderer#useTransparentGlint}, which picks a
     * fabulous glint render type that does not sort outside the fabulous pass. {@link #isForcingFancy}
     * is read by {@code mixin.client.MinecraftMixin}, which answers {@code false} while it is up.
     */
    public static void runAsFancy(Runnable runnable) {
        //? if >=1.21.11 {
        /*forcingFancy = true;
        try {
            runnable.run();
        } finally {
            forcingFancy = false;
        }
        *///?} elif >=1.21.2 {
        /*if (!net.minecraft.client.Minecraft.useShaderTransparency()) {
            runnable.run();
        } else {
            net.minecraft.client.OptionInstance<net.minecraft.client.GraphicsStatus> option = net.minecraft.client.Minecraft.getInstance().options.graphicsMode();
            net.minecraft.client.GraphicsStatus previous = option.get();
            option.set(net.minecraft.client.GraphicsStatus.FANCY);
            runnable.run();
            option.set(previous);
        }
        *///?} else {
        com.mojang.blaze3d.systems.RenderSystem.runAsFancy(runnable);
        //?}
    }

    // Line comments only below: the arm's body is itself a block comment when the gate is false, and a
    // nested one would close it early.
    //? if >=1.21.11 {
    /*// Render-thread only, like the rest of this mod's draw-scoped flags, so a plain static is enough.
    private static boolean forcingFancy;

    // Whether a runAsFancy draw is in progress — see that method for why this replaced flipping the
    // graphics option itself on 1.21.11. Read by mixin.client.MinecraftMixin.
    public static boolean isForcingFancy() {
        return forcingFancy;
    }
    *///?}

    public static int getPixelABGR(com.mojang.blaze3d.platform.NativeImage image, int x, int y) {
        //? if >=1.21.2 {
        /*return net.minecraft.util.ARGB.toABGR(image.getPixel(x, y));
        *///?} else {
        return image.getPixelRGBA(x, y);
        //?}
    }

    /**
     * One pixel of an atlas sprite's first frame, in the same ABGR packing {@link #getPixelABGR}
     * returns. Only {@code BlockColorFinder} asks, and it averages a whole sprite to pick a cave-map
     * colour, reading red at {@code >>0} and blue at {@code >>16} — so the packing is load-bearing.
     *
     * <p>Both patched jars add a frame-indexed {@code TextureAtlasSprite#getPixelRGBA(frame, x, y)}
     * and nothing in vanilla replaces it: the pixels live on the sprite's {@code contents()}, whose
     * un-mipmapped source image is private, hence the access widener. Frame 0 is the whole of the
     * translation — {@code SpriteContents#width}/{@code #height} are one frame's, so the coordinates
     * {@code BlockColorFinder} iterates address the first frame directly either way.
     *
     * <p>The Fabric arm goes through {@link #getPixelABGR} rather than reading the image itself,
     * which is what keeps the 1.21.2 {@code getPixelRGBA} → {@code getPixel} rename and its
     * ARGB/ABGR flip in exactly one place.
     */
    public static int spritePixel(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite, int x, int y) {
        //? if fabric {
        /*return getPixelABGR(sprite.contents().originalImage, x, y);
        *///?} else {
        return sprite.getPixelRGBA(0, x, y);
        //?}
    }

    /**
     * The render types a baked item model draws in. NeoForge's model extension dropped the
     * {@code fabulous} flag in 1.21.2 — it had already been ignored for several versions, since the
     * fabulous item sheet stopped being a separate pass — and all three call sites passed
     * {@code false} anyway, so nothing is lost by not offering it. Forge still declares it.
     *
     * <p>From 1.21.4 there is no such thing to ask for: NeoForge narrowed the hook to a single
     * {@code getRenderType(ItemStack)} and Forge deleted it outright, because an item is no longer
     * drawn by walking a baked model at all. Every caller is gated to the older path, and 1.21.5
     * deleted {@code BakedModel} itself, so the method cannot even be declared there — hence the
     * whole thing, signature included, is gated off rather than left throwing.
     *
     * <p>Fabric has no such hook at all — the whole idea of a model answering with several render
     * types is a loader extension, added so a block or item model could span more than one chunk
     * layer. Vanilla's answer is one type per stack, from {@code ItemBlockRenderTypes}, so the two
     * Fabric arms hand back a singleton list of it. That is what both loaders reduce to for every
     * model in this mod anyway, none of which is multi-layer. Vanilla dropped the same
     * {@code fabulous} flag NeoForge did and in the same version, hence the arm split at 1.21.2.
     *
     * <p>The arms restate the signature rather than nesting a gate inside a {@code <1.21.4} one:
     * a disabled arm is a block comment, and Java has no nested block comments.
     */
    //? if >=1.21.4 {
    /*// Deleted from 1.21.4 — see the javadoc. BakedModel itself is gone from 1.21.5.
    *///?} elif fabric && >=1.21.2 {
    /*public static java.util.List<net.minecraft.client.renderer.RenderType> itemRenderTypes(net.minecraft.client.resources.model.BakedModel model, net.minecraft.world.item.ItemStack stack) {
        return java.util.List.of(net.minecraft.client.renderer.ItemBlockRenderTypes.getRenderType(stack));
    }
    *///?} elif fabric {
    /*public static java.util.List<net.minecraft.client.renderer.RenderType> itemRenderTypes(net.minecraft.client.resources.model.BakedModel model, net.minecraft.world.item.ItemStack stack) {
        return java.util.List.of(net.minecraft.client.renderer.ItemBlockRenderTypes.getRenderType(stack, false));
    }
    *///?} elif neoforge && >=1.21.2 {
    /*public static java.util.List<net.minecraft.client.renderer.RenderType> itemRenderTypes(net.minecraft.client.resources.model.BakedModel model, net.minecraft.world.item.ItemStack stack) {
        return model.getRenderTypes(stack);
    }
    *///?} else {
    public static java.util.List<net.minecraft.client.renderer.RenderType> itemRenderTypes(net.minecraft.client.resources.model.BakedModel model, net.minecraft.world.item.ItemStack stack) {
        return model.getRenderTypes(stack, false);
    }
    //?}

    /**
     * Puts a baked item model's display transform on the pose stack, answering the model that should
     * actually be drawn — a multipart model may substitute itself here. 1.20.5 folded Forge's
     * {@code ForgeHooksClient} hook back onto the model; 1.21.4 moved the whole call inside
     * {@code ItemStackRenderState$LayerRenderState#render}, so from there nobody applies it by hand
     * — and 1.21.5 deleted the {@code BakedModel} this is written in terms of, so like
     * {@link #itemRenderTypes} the declaration itself is gated away rather than left throwing.
     *
     * <p>The Fabric arm is vanilla's own two lines, which is exactly what both loader hooks do once
     * the substitution they exist for is declined: read the display transform off the model and
     * apply it. Nothing here is a multipart model, so answering with the model that came in is not a
     * simplification — it is the same answer the loaders give. One arm covers the whole band,
     * because the difference the 1.20.5 split is about is which loader class owns the hook, and
     * vanilla's spelling did not move.
     */
    //? if >=1.21.4 {
    /*// Deleted from 1.21.4 — see the javadoc. BakedModel itself is gone from 1.21.5.
    *///?} elif fabric {
    /*public static net.minecraft.client.resources.model.BakedModel applyItemTransform(net.minecraft.client.resources.model.BakedModel model, PoseStack poseStack, net.minecraft.world.item.ItemDisplayContext ctx) {
        model.getTransforms().getTransform(ctx).apply(false, poseStack);
        return model;
    }
    *///?} elif >=1.20.5 {
    /*public static net.minecraft.client.resources.model.BakedModel applyItemTransform(net.minecraft.client.resources.model.BakedModel model, PoseStack poseStack, net.minecraft.world.item.ItemDisplayContext ctx) {
        return model.applyTransform(ctx, poseStack, false);
    }
    *///?} else {
    public static net.minecraft.client.resources.model.BakedModel applyItemTransform(net.minecraft.client.resources.model.BakedModel model, PoseStack poseStack, net.minecraft.world.item.ItemDisplayContext ctx) {
        return net.minecraftforge.client.ForgeHooksClient.handleCameraTransforms(poseStack, model, ctx, false);
    }
    //?}

    /**
     * The chunk render types a baked <i>block</i> model draws in — the loader-patched
     * {@code BakedModel#getRenderTypes(BlockState, RandomSource, ModelData)}, whose answer is a
     * {@code ChunkRenderTypeSet} rather than a list, hence the {@code Iterable} return.
     *
     * <p>Vanilla assigns a block one chunk layer, statically, so the Fabric arm is a singleton of
     * {@code ItemBlockRenderTypes#getChunkRenderType} — the same call the {@code >=1.21.5} arm of
     * {@link #renderTintedBlock} already makes on every loader, which is what makes it the right
     * answer here rather than an approximation: from 1.21.5 the loaders agree with vanilla and a
     * model's render type is per-part again.
     *
     * <p>Gated {@code <1.21.5} because {@code BakedModel} is deleted there and the one caller —
     * {@link #renderTintedBlock}'s oldest arm — is gated the same way.
     */
    //? if >=1.21.5 {
    /*// Deleted from 1.21.5 along with BakedModel — see the javadoc.
    *///?} elif fabric {
    /*public static Iterable<net.minecraft.client.renderer.RenderType> modelRenderTypes(net.minecraft.client.resources.model.BakedModel model, net.minecraft.world.level.block.state.BlockState state, net.minecraft.util.RandomSource rand, net.minecraftforge.client.model.data.ModelData data) {
        return java.util.List.of(net.minecraft.client.renderer.ItemBlockRenderTypes.getChunkRenderType(state));
    }
    *///?} else {
    public static Iterable<net.minecraft.client.renderer.RenderType> modelRenderTypes(net.minecraft.client.resources.model.BakedModel model, net.minecraft.world.level.block.state.BlockState state, net.minecraft.util.RandomSource rand, net.minecraftforge.client.model.data.ModelData data) {
        return model.getRenderTypes(state, rand, data);
    }
    //?}

    /**
     * A baked model's quads for one face, through the loader-patched five-argument
     * {@code BakedModel#getQuads}. Eleven call sites, all of them gated below 1.21.4 — the version
     * that replaced walking a baked model with the render-state pipeline.
     *
     * <p>Vanilla has only the three-argument form, and the two extra arguments are the loaders'
     * whole reason for patching it: a {@code ModelData} the model may bake against, and the render
     * type being drawn so a multi-layer model can answer per layer. Neither reaches anything here —
     * every call site hands over {@link ModelData#EMPTY} (see that class), and no model in this mod
     * spans more than one layer — so dropping them on Fabric loses nothing, and the Fabric arm is
     * vanilla's own overload.
     *
     * <p>Note what this does <i>not</i> do: it takes the model as a parameter and calls it directly,
     * so a {@code BakedModelWrapper} subclass that overrides the wide overload is only reached
     * because that stand-in's narrow overload delegates into it. That dispatch lives in the wrapper,
     * not here — see {@code fabric.forge.client.model.BakedModelWrapper}.
     */
    //? if >=1.21.5 {
    /*// Deleted from 1.21.5 along with BakedModel — see the javadoc.
    *///?} elif fabric {
    /*public static java.util.List<net.minecraft.client.renderer.block.model.BakedQuad> modelQuads(net.minecraft.client.resources.model.BakedModel model, net.minecraft.world.level.block.state.BlockState state, net.minecraft.core.Direction side, net.minecraft.util.RandomSource rand, net.minecraftforge.client.model.data.ModelData data, net.minecraft.client.renderer.RenderType renderType) {
        return model.getQuads(state, side, rand);
    }
    *///?} else {
    public static java.util.List<net.minecraft.client.renderer.block.model.BakedQuad> modelQuads(net.minecraft.client.resources.model.BakedModel model, net.minecraft.world.level.block.state.BlockState state, net.minecraft.core.Direction side, net.minecraft.util.RandomSource rand, net.minecraftforge.client.model.data.ModelData data, net.minecraft.client.renderer.RenderType renderType) {
        return model.getQuads(state, side, rand, data, renderType);
    }
    //?}

    /**
     * Whether an item draws as a block rather than a flat sprite — the thing that decides how a stack
     * of them is fanned out on a pedestal.
     */
    public static boolean isItemGui3d(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.Level level) {
        //? if >=1.21.4 {
        /*return com.github.alexmodguy.alexscaves.client.render.item.ACItemRenderCompat.isGui3d(stack, level);
        *///?} else {
        return net.minecraft.client.Minecraft.getInstance().getItemRenderer().getModel(stack, level, null, 0).isGui3d();
        //?}
    }

    /**
     * Whether an item is drawn by a renderer of its own rather than by its baked model's quads. The
     * question exists to keep the cave book's sepia pass off such items, because their renderer draws
     * from its own textures and would come out painted with the block atlas.
     *
     * <p>1.21.4 deleted the flag along with the idiom: a special renderer is now one kind of layer
     * inside {@code ItemStackRenderState}, and the sepia pass swaps render types at the buffer lookup
     * instead — which leaves a special layer's own buffers alone by construction. So the answer is
     * {@code false} there, and the caller takes the sepia path for everything.
     */
    public static boolean isCustomItemRenderer(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.Level level) {
        //? if >=1.21.4 {
        /*return false;
        *///?} else {
        return net.minecraft.client.Minecraft.getInstance().getItemRenderer().getModel(stack, level, null, 0).isCustomRenderer();
        //?}
    }

    /**
     * {@code ItemRenderer#renderStatic} — "draw this stack, here, now" — which 1.21.9 deleted along with
     * the rest of the immediate-mode item path. The argument list is deliberately the old method's, so
     * the four call sites read the same on every node.
     *
     * <p>The replacement resolves the stack into an {@code ItemStackRenderState} and <em>submits</em> it,
     * which is what vanilla's own top-level item draw does now. Submitting normally defers the work to
     * the frame's feature-render pass, but every one of these call sites needs the vertices in the
     * buffer source it was handed — a book page's own {@code BufferSource}, a block entity's, a thrown
     * entity's — so the submission goes through {@link com.github.alexmodguy.alexscaves.client.render.compat.ACDrawCollector},
     * which draws each node straight back out. The trailing {@code 0} is the outline colour, i.e. none,
     * which is what {@code renderStatic} always produced.
     */
    public static void renderItemStatic(net.minecraft.world.item.ItemStack stack,
                                        net.minecraft.world.item.ItemDisplayContext ctx,
                                        int packedLight, int packedOverlay, PoseStack poseStack,
                                        net.minecraft.client.renderer.MultiBufferSource buffers,
                                        net.minecraft.world.level.Level level, int seed) {
        //? if >=1.21.9 {
        /*net.minecraft.client.renderer.item.ItemStackRenderState state =
                new net.minecraft.client.renderer.item.ItemStackRenderState();
        net.minecraft.client.Minecraft.getInstance().getItemModelResolver()
                .updateForTopItem(state, stack, ctx, level, null, seed);
        state.submit(poseStack,
                new com.github.alexmodguy.alexscaves.client.render.compat.ACDrawCollector(buffers::getBuffer),
                packedLight, packedOverlay, 0);
        *///?} else {
        net.minecraft.client.Minecraft.getInstance().getItemRenderer()
                .renderStatic(stack, ctx, packedLight, packedOverlay, poseStack, buffers, level, seed);
        //?}
    }

    /**
     * The entity-drawing counterpart of a chunk render type. 1.21.2 dropped the same {@code fabulous}
     * flag here, for the same reason; both call sites passed {@code false}.
     *
     * <p>1.21.6 gave the four chunk layers a type of their own — {@code ChunkSectionLayer}, an enum
     * carrying the pipeline, buffer size and target each layer draws with — so what used to be a
     * {@code RenderType} standing in for a layer now says so. Only the parameter changes: both call
     * sites hand this whatever {@code ItemBlockRenderTypes#getChunkRenderType} returns, which moved
     * to the new type with it.
     *
     * <p>Forge 62 (26) deleted {@code RenderTypeHelper} along with the rest of the immediate-mode
     * block-drawing surface, and the only two callers — both inside {@link #renderTintedBlock} — are
     * gated out there. The {@code >=26} arm exists purely so the method still compiles; nothing can
     * reach it, and there is no successor worth guessing at until something needs one.
     *
     * <p>Fabric spells the loader's two-line body out rather than getting a {@code RenderTypeHelper}
     * stand-in of its own: it is one conditional over two vanilla sheets, both of which exist
     * unchanged from 1.20.1 to 26, so a stand-in would be a file whose entire content is the
     * expression below. The third branch the loader has — {@code translucentCullBlockSheet()} for
     * {@code cull == true} — is deliberately absent, because that sheet was deleted before 1.21.5
     * and both call sites here have always passed {@code false}. The 1.21.6 split is a separate arm
     * rather than a rename rule because the comparison changes shape with the parameter, from a
     * method call to an enum constant.
     */
    //? if >=1.21.6 {
    /*public static net.minecraft.client.renderer.RenderType entityRenderType(net.minecraft.client.renderer.chunk.ChunkSectionLayer chunkRenderType) {
    *///?} else {
    public static net.minecraft.client.renderer.RenderType entityRenderType(net.minecraft.client.renderer.RenderType chunkRenderType) {
    //?}
        //? if >=26 {
        /*throw new UnsupportedOperationException("RenderTypeHelper was deleted in Forge 62; nothing calls this on 26+");
        *///?} elif fabric && >=1.21.6 {
        /*return chunkRenderType != net.minecraft.client.renderer.chunk.ChunkSectionLayer.TRANSLUCENT
                ? net.minecraft.client.renderer.Sheets.cutoutBlockSheet()
                : net.minecraft.client.renderer.Sheets.translucentItemSheet();
        *///?} elif fabric {
        /*return chunkRenderType != net.minecraft.client.renderer.RenderType.translucent()
                ? net.minecraft.client.renderer.Sheets.cutoutBlockSheet()
                : net.minecraft.client.renderer.Sheets.translucentItemSheet();
        *///?} elif >=1.21.2 {
        /*return net.minecraftforge.client.RenderTypeHelper.getEntityRenderType(chunkRenderType);
        *///?} else {
        return net.minecraftforge.client.RenderTypeHelper.getEntityRenderType(chunkRenderType, false);
        //?}
    }

    /**
     * An entity built purely to be drawn — a book-page preview, the arrow nocked on a rendered bow.
     * 1.21.2 made {@code EntityType#create} state why the entity is appearing; {@code LOAD} is the
     * inert answer (no spawn rules, no equipment roll, no event fired), which is what these two
     * throwaway client-side instances have always wanted.
     */
    @javax.annotation.Nullable
    public static net.minecraft.world.entity.Entity displayEntity(net.minecraft.world.entity.EntityType<?> type, net.minecraft.world.level.Level level) {
        //? if >=1.21.2 {
        /*return com.github.alexmodguy.alexscaves.server.misc.ACCompat.markDisplayEntity(type.create(level, net.minecraft.world.entity.EntitySpawnReason.LOAD));
        *///?} else {
        return com.github.alexmodguy.alexscaves.server.misc.ACCompat.markDisplayEntity(type.create(level));
        //?}
    }

    // Hands out the GUI's shared buffer source for a hand-rolled draw. 1.21.2 made
    // GuiGraphics#bufferSource private and replaced it with drawSpecial, which lends the buffer for
    // the duration of a callback and flushes it afterwards — the flush being the point, since a GUI
    // draw is batched behind render states from then on and would otherwise be reordered around.
    // Below 1.21.2 the caller simply gets the buffer, and the batch ends where it always did, at the
    // end of the GUI pass.
    //
    // 1.21.6 took the door away again: there is no buffer to borrow, because the GUI is recorded as
    // render states and rasterised afterwards. Both callers drew text through it, and both spell
    // that as an ordinary GuiGraphics text draw from 1.21.6, so the shim has no callers left there
    // and goes away rather than pretending to work. Model geometry, which cannot be expressed as a
    // render state at all, goes through the picture-in-picture path instead — see CaveBookPipRenderer.
    //? if >=1.21.6 {
    /*// gone — no callers, and nothing to hand out. See above.
    *///?} elif >=1.21.2 {
    /*public static void drawSpecial(net.minecraft.client.gui.GuiGraphics graphics, java.util.function.Consumer<net.minecraft.client.renderer.MultiBufferSource> drawer) {
        graphics.drawSpecial(drawer);
    }
    *///?} else {
    public static void drawSpecial(net.minecraft.client.gui.GuiGraphics graphics, java.util.function.Consumer<net.minecraft.client.renderer.MultiBufferSource> drawer) {
        drawer.accept(graphics.bufferSource());
    }
    //?}

    /**
     * The GUI's transform stack, saved and restored. 1.21.6 replaced {@code GuiGraphics#pose}'s
     * {@link com.mojang.blaze3d.vertex.PoseStack} with a {@code Matrix3x2fStack} — the GUI is a
     * plain 2D affine transform now, so the operation is spelled {@code pushMatrix}/{@code popMatrix}
     * and there is no third axis to translate along.
     *
     * <p>Every one of this mod's GUI transforms is a translate, so the three shims below cover them
     * all. The z argument the old call sites passed was {@code 0} everywhere; where a draw wanted a
     * depth it named one per quad instead (see {@link #blit}), which is the only thing 1.21.6 still
     * accepts.
     */
    public static void pushPose(net.minecraft.client.gui.GuiGraphics graphics) {
        //? if >=1.21.6 {
        /*graphics.pose().pushMatrix();
        *///?} else {
        graphics.pose().pushPose();
        //?}
    }

    /** @see #pushPose */
    public static void popPose(net.minecraft.client.gui.GuiGraphics graphics) {
        //? if >=1.21.6 {
        /*graphics.pose().popMatrix();
        *///?} else {
        graphics.pose().popPose();
        //?}
    }

    /** @see #pushPose */
    public static void translate(net.minecraft.client.gui.GuiGraphics graphics, float x, float y) {
        //? if >=1.21.6 {
        /*graphics.pose().translate(x, y);
        *///?} else {
        graphics.pose().translate(x, y, 0.0F);
        //?}
    }

    /**
     * A tooltip, from a list of lines plus the optional image component that sits under them.
     *
     * <p>1.21.6 renamed this to {@code setTooltipForNextFrame} and made it deferred: the tooltip is
     * recorded and drawn by {@code Screen#renderWithTooltip} once the rest of the screen is on the
     * GUI's render-state list, which is how a tooltip stays on top now that the GUI is batched rather
     * than drawn in call order. The public {@code renderTooltip} that remains takes already-laid-out
     * {@code ClientTooltipComponent}s and is not the successor to this call.
     *
     * <p>Only the first tooltip set in a frame survives, so the caller's own ordering decides which
     * one shows — every call site here is inside a mutually exclusive hover test.
     */
    public static void renderTooltip(net.minecraft.client.gui.GuiGraphics graphics, net.minecraft.client.gui.Font font,
                                     java.util.List<net.minecraft.network.chat.Component> lines,
                                     java.util.Optional<net.minecraft.world.inventory.tooltip.TooltipComponent> image,
                                     int x, int y) {
        //? if >=1.21.6 {
        /*graphics.setTooltipForNextFrame(font, lines, image, x, y);
        *///?} else {
        graphics.renderTooltip(font, lines, image, x, y);
        //?}
    }

    /**
     * The fixed-function light setup a GUI draw wants. 1.21.6 made {@code Lighting} an instance held
     * by the game renderer — it owns the uniform buffer the light directions are uploaded through —
     * so the three statics became one {@code setupFor(Lighting.Entry)} call on that instance.
     *
     * <p>{@code ITEMS_FLAT} / {@code ENTITY_IN_UI} / {@code ITEMS_3D} are the same three setups under
     * their new names, with the same directions; only where they live moved.
     */
    public static void setupForFlatItems() {
        //? if >=1.21.6 {
        /*net.minecraft.client.Minecraft.getInstance().gameRenderer.getLighting()
                .setupFor(com.mojang.blaze3d.platform.Lighting.Entry.ITEMS_FLAT);
        *///?} else {
        com.mojang.blaze3d.platform.Lighting.setupForFlatItems();
        //?}
    }

    /** @see #setupForFlatItems */
    public static void setupForEntityInInventory() {
        //? if >=1.21.6 {
        /*net.minecraft.client.Minecraft.getInstance().gameRenderer.getLighting()
                .setupFor(com.mojang.blaze3d.platform.Lighting.Entry.ENTITY_IN_UI);
        *///?} else {
        com.mojang.blaze3d.platform.Lighting.setupForEntityInInventory();
        //?}
    }

    /** @see #setupForFlatItems */
    public static void setupFor3DItems() {
        //? if >=1.21.6 {
        /*net.minecraft.client.Minecraft.getInstance().gameRenderer.getLighting()
                .setupFor(com.mojang.blaze3d.platform.Lighting.Entry.ITEMS_3D);
        *///?} else {
        com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
        //?}
    }

    /**
     * The first-person arm, drawn by the player renderer. 1.21.2 stopped letting the renderer look
     * the player up for itself: the skin texture and whether the sleeve overlay is worn are passed
     * in, because by then the renderer works off a render state rather than the entity. Both values
     * are read here exactly as {@code ItemInHandRenderer} reads them.
     *
     * <p>NeoForge kept a trailing player argument on its patched overload so its own hooks can still
     * see whose arm it is; vanilla's own signature — which is what Forge and Fabric both call —
     * takes the two values and nothing else. Hence NeoForge's own arm, and one shared by the rest.
     */
    public static void renderFirstPersonHand(
            net.minecraft.client.renderer.entity.player.PlayerRenderer renderer,
            com.mojang.blaze3d.vertex.PoseStack poseStack,
            net.minecraft.client.renderer.MultiBufferSource bufferSource,
            int packedLight,
            net.minecraft.client.player.AbstractClientPlayer player,
            net.minecraft.world.entity.HumanoidArm arm) {
        boolean right = arm == net.minecraft.world.entity.HumanoidArm.RIGHT;
        //? if >=1.21.9 {
        /*// Two changes at once, and they reunify the loaders: the hand renders submit rather than
        // draw, and Forge dropped the trailing player argument its 1.21.2 patch had added, so both
        // loaders now take vanilla's five. PlayerSkin's four bare ResourceLocations became
        // ClientAsset.Texture holders, hence body().texturePath() for what used to be texture().
        net.minecraft.resources.ResourceLocation skin = player.getSkin().body().texturePath();
        boolean sleeve = player.isModelPartShown(right
                ? net.minecraft.world.entity.player.PlayerModelPart.RIGHT_SLEEVE
                : net.minecraft.world.entity.player.PlayerModelPart.LEFT_SLEEVE);
        com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers submit =
                com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers.of(bufferSource);
        if (submit != null) {
            submit.flush();
            if (right) {
                renderer.renderRightHand(poseStack, submit.collector(), packedLight, skin, sleeve);
            } else {
                renderer.renderLeftHand(poseStack, submit.collector(), packedLight, skin, sleeve);
            }
        }
        *///?} elif !neoforge && >=1.21.2 {
        /*net.minecraft.resources.ResourceLocation skin = player.getSkin().texture();
        boolean sleeve = player.isModelPartShown(right
                ? net.minecraft.world.entity.player.PlayerModelPart.RIGHT_SLEEVE
                : net.minecraft.world.entity.player.PlayerModelPart.LEFT_SLEEVE);
        if (right) {
            renderer.renderRightHand(poseStack, bufferSource, packedLight, skin, sleeve);
        } else {
            renderer.renderLeftHand(poseStack, bufferSource, packedLight, skin, sleeve);
        }
        *///?} elif >=1.21.2 {
        /*net.minecraft.resources.ResourceLocation skin = player.getSkin().texture();
        boolean sleeve = player.isModelPartShown(right
                ? net.minecraft.world.entity.player.PlayerModelPart.RIGHT_SLEEVE
                : net.minecraft.world.entity.player.PlayerModelPart.LEFT_SLEEVE);
        if (right) {
            renderer.renderRightHand(poseStack, bufferSource, packedLight, skin, sleeve, player);
        } else {
            renderer.renderLeftHand(poseStack, bufferSource, packedLight, skin, sleeve, player);
        }
        *///?} else {
        if (right) {
            renderer.renderRightHand(poseStack, bufferSource, packedLight, player);
        } else {
            renderer.renderLeftHand(poseStack, bufferSource, packedLight, player);
        }
        //?}
    }

    /**
     * Renders a held item through the shared {@code ItemInHandRenderer}.
     *
     * <p>1.21.5 dropped the {@code leftHand} argument: the flag is derived from the display context
     * ({@code ItemDisplayContext#leftHand}), which already distinguishes the two hand contexts. Every
     * call site here is a {@code renderArmWithItem} override, where vanilla hands down the context
     * matching the arm, or passes {@code GROUND} with {@code false} — so the two are the same value
     * and dropping the argument changes nothing.
     */
    public static void renderItemInHand(
            net.minecraft.client.renderer.ItemInHandRenderer renderer,
            net.minecraft.world.entity.LivingEntity entity,
            net.minecraft.world.item.ItemStack stack,
            net.minecraft.world.item.ItemDisplayContext displayContext,
            boolean leftHand,
            com.mojang.blaze3d.vertex.PoseStack poseStack,
            net.minecraft.client.renderer.MultiBufferSource bufferSource,
            int packedLight) {
        // 1.21.9 defers the draw: the renderer records into the frame's SubmitNodeCollector rather
        // than writing vertices, so what arrives here as a MultiBufferSource has to be unwrapped
        // back to the collector it is recording for. Every call site is a renderArmWithItem
        // override reached through the compat renderer, so there always is one.
        //? if >=1.21.9 {
        /*net.minecraft.client.renderer.SubmitNodeCollector acCollector =
                com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers.collectorOf(bufferSource);
        if (acCollector != null) {
            renderer.renderItem(entity, stack, displayContext, poseStack, acCollector, packedLight);
        }
        *///?} elif >=1.21.5 {
        /*renderer.renderItem(entity, stack, displayContext, poseStack, bufferSource, packedLight);
        *///?} else {
        renderer.renderItem(entity, stack, displayContext, leftHand, poseStack, bufferSource, packedLight);
        //?}
    }

    /**
     * Makes the main render target the write target again, after something has drawn into another.
     *
     * <p>1.21.5 has no such call and needs none: a draw goes through a render pass that names its
     * own colour and depth attachments, so there is no ambient framebuffer binding left over to
     * restore. {@code RenderTarget#bindWrite} was deleted along with the rest of the fixed-function
     * state, and the four call sites here were only ever undoing a binding.
     *
     * @param setViewport what {@code bindWrite} took: whether to reset the GL viewport as well.
     *                    Unused from 1.21.5.
     */
    public static void bindMainRenderTargetForWrite(boolean setViewport) {
        //? if >=1.21.5 {
        /*// nothing to restore — see above
        *///?} else {
        net.minecraft.client.Minecraft.getInstance().getMainRenderTarget().bindWrite(setViewport);
        //?}
    }

    /**
     * Binds the {@code position_tex_color} core shader. 1.21.2 replaced the {@code GameRenderer}
     * getter-per-shader with the {@code CoreShaders} constant table, and {@code setShader} takes the
     * program itself rather than a supplier of the compiled instance.
     *
     * <p>1.21.5 deleted the table as well: a shader is named by the {@code RenderPipeline} a draw
     * goes through, never bound as ambient state. {@link ImmediateDraw} is the replacement and the
     * only caller left below it, so this arm is unreachable rather than merely unused.
     */
    public static void setPositionTexColorShader() {
        //? if >=1.21.5 {
        /*throw new UnsupportedOperationException("no 1.21.5 form; draw through ImmediateDraw");
        *///?} elif >=1.21.2 {
        /*com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.renderer.CoreShaders.POSITION_TEX_COLOR);
        *///?} else {
        com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexColorShader);
        //?}
    }

    /**
     * Binds the {@code position_color} core shader. Same three eras as
     * {@link #setPositionTexColorShader}.
     *
     * <p>This has to be a method rather than an inline {@code setShader} in {@link #beginImmediate}:
     * the getter-to-constant rename is a string replacement, and a call site that spells the getter
     * out with its package would come back with the package written twice. Keeping each spelling
     * inside its own arm means only the arm that is commented out gets rewritten, which is harmless.
     */
    public static void setPositionColorShader() {
        //? if >=1.21.5 {
        /*throw new UnsupportedOperationException("no 1.21.5 form; draw through ImmediateDraw");
        *///?} elif >=1.21.2 {
        /*com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.renderer.CoreShaders.POSITION_COLOR);
        *///?} else {
        com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);
        //?}
    }

    /** Binds the {@code position_tex} core shader. See {@link #setPositionColorShader}. */
    public static void setPositionTexShader() {
        //? if >=1.21.5 {
        /*throw new UnsupportedOperationException("no 1.21.5 form; draw through ImmediateDraw");
        *///?} elif >=1.21.2 {
        /*com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.renderer.CoreShaders.POSITION_TEX);
        *///?} else {
        com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
        //?}
    }

    /**
     * Binds a texture to the current GL texture unit ahead of an immediate-mode draw.
     *
     * <p>{@code TextureManager#bindForSetup} is gone in 1.21.2. It only ever resolved the texture
     * (loading it on first use) and called {@code bind()} on it, hopping to the render thread first
     * — and every caller here is already on the render thread, so the resolve-and-bind pair is the
     * whole of it.
     *
     * <p>1.21.5 dropped {@code bind()} too — a texture reaches a draw as a sampler on the
     * {@code RenderType}, so there is no unit to bind it to ahead of time. {@link ImmediateDraw}
     * takes the texture as an argument instead, and is the only caller left below that version.
     */
    public static void bindTextureForSetup(net.minecraft.resources.ResourceLocation texture) {
        //? if >=1.21.5 {
        /*throw new UnsupportedOperationException("no 1.21.5 form; pass the texture to ImmediateDraw");
        *///?} elif >=1.21.2 {
        /*net.minecraft.client.Minecraft.getInstance().getTextureManager().getTexture(texture).bind();
        *///?} else {
        net.minecraft.client.Minecraft.getInstance().getTextureManager().bindForSetup(texture);
        //?}
    }

    /**
     * Allocates a blank {@link net.minecraft.client.renderer.texture.DynamicTexture}.
     *
     * <p>1.21.5 moved texture allocation onto the {@code GpuDevice}, whose {@code createTexture}
     * takes a debug label — so the constructor gained a leading name argument. The name passed here
     * is the same one the texture is registered under by {@link #registerDynamicTexture}, which is
     * what a graphics debugger would want to see next to the allocation.
     *
     * @param useCalloc what the trailing flag has always meant: zero the backing image on allocation.
     */
    public static net.minecraft.client.renderer.texture.DynamicTexture newDynamicTexture(
            String name, int width, int height, boolean useCalloc) {
        //? if >=1.21.5 {
        /*return new net.minecraft.client.renderer.texture.DynamicTexture(name, width, height, useCalloc);
        *///?} else {
        return new net.minecraft.client.renderer.texture.DynamicTexture(width, height, useCalloc);
        //?}
    }

    /** Serial numbers for {@link #registerDynamicTexture}, mirroring the map vanilla used to keep. */
    private static final java.util.Map<String, Integer> DYNAMIC_TEXTURE_SERIALS = new java.util.HashMap<>();

    /**
     * Registers a generated texture under a unique id and returns it.
     *
     * <p>1.21.4 deleted {@code TextureManager#register(String, DynamicTexture)}, whose whole job was
     * to hand out {@code minecraft:dynamic/<name>_<n>} from a per-name counter and register under it.
     * The 1.21.4 arm does exactly that, so the ids a texture pack could target are unchanged.
     */
    public static net.minecraft.resources.ResourceLocation registerDynamicTexture(String name, net.minecraft.client.renderer.texture.DynamicTexture texture) {
        //? if >=1.21.4 {
        /*int serial = DYNAMIC_TEXTURE_SERIALS.merge(name, 1, Integer::sum);
        net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.withDefaultNamespace(
                String.format(java.util.Locale.ROOT, "dynamic/%s_%d", name, serial));
        net.minecraft.client.Minecraft.getInstance().getTextureManager().register(id, texture);
        return id;
        *///?} else {
        return net.minecraft.client.Minecraft.getInstance().getTextureManager().register(name, texture);
        //?}
    }

    /**
     * Forwards a line width to a delegate vertex consumer, or does nothing below 1.21.11.
     *
     * <p>1.21.11 made a line's width a per-vertex format element ({@code VertexFormatElement
     * .LINE_WIDTH}) and added {@code VertexConsumer#setLineWidth(float)} as a new <em>abstract</em>
     * method, which every hand-written consumer in this mod has to answer. Each of those three
     * classes lives inside a Stonecutter arm of its own, and an arm cannot hold a nested gate — so
     * they declare the method unconditionally (without {@code @Override}: it overrides nothing below
     * 1.21.11, where an extra public method is simply harmless) and route the version difference
     * through here.
     */
    public static void setLineWidth(com.mojang.blaze3d.vertex.VertexConsumer delegate, float width) {
        //? if >=1.21.11 {
        /*delegate.setLineWidth(width);
        *///?}
    }

    /**
     * Decomposes a packed ARGB colour and hands it to a consumer's four-channel {@code setColor}.
     *
     * <p>The same shape as {@link #setLineWidth}, and for the same reason: 1.21.11 promoted
     * {@code VertexConsumer#setColor(int)} from a default method to an <em>abstract</em> one, so the
     * three hand-written consumers in this mod (each inside a Stonecutter arm, which cannot nest a
     * gate) declare it unconditionally without {@code @Override} and delegate here.
     *
     * <p>Routing back through {@code setColor(int,int,int,int)} is what the deleted default did, and
     * is what keeps each class's own colour handling — {@code Tinted}'s multiply, {@code Recorded}'s
     * capture, the pathfinding consumer's fixed colour — applying to a packed call as well.
     *
     * <p>Plain bit math rather than {@code net.minecraft.util.ARGB}: that class only exists from
     * 1.21.2 and one of the three callers has a {@code >=1.21} arm.
     *
     * <p>⚠️ The call below is written in the <em>pre-1.21</em> four-channel spelling — deliberately
     * not named here, since a rule rewrites its own token in prose as readily as in code —
     * because that is the direction the {@code !mc21-vc-color} rename rule runs: this tree's source
     * is authored in the old DSL throughout and rewritten upwards on every node {@code >=1.21}.
     * Writing the modern spelling here compiled on 23 nodes and broke the five below 1.21 — a rule
     * group guarded by a Kotlin {@code if} is not registered at all on the versions it excludes, so
     * there is no reverse pass to undo it.
     */
    public static com.mojang.blaze3d.vertex.VertexConsumer setColorPacked(com.mojang.blaze3d.vertex.VertexConsumer target, int argb) {
        return target.color(argb >> 16 & 255, argb >> 8 & 255, argb & 255, argb >>> 24);
    }

    /**
     * Draws the wireframe of a box, the way {@code ShapeRenderer#renderLineBox} used to.
     *
     * <p>1.21.11 deleted every {@code renderLineBox} overload along with the rest of the CPU-side line
     * geometry: a line's width became a per-vertex format element, so the one survivor is
     * {@code renderShape(PoseStack, VertexConsumer, VoxelShape, double, double, double, int, float)},
     * which takes a shape, a packed colour and an explicit width. This is that call — the box wrapped
     * as a {@code VoxelShape} at no offset, the four floats packed, and vanilla's own width.
     *
     * <p>The width is {@code Window#getAppropriateLineWidth()}, which is what {@code LevelRenderer}
     * passes when it draws the block-hit outline; below 1.21.11 the identical value came from
     * {@code RenderType.lines()}'s {@code LineStateShard}, so the magnet's range boxes keep the
     * thickness they have always had.
     *
     * <p>26.2 deleted {@code ShapeRenderer} along with the rest of immediate mode: an outline is a
     * submission now — {@code SubmitNodeCollector#submitShapeOutline} queues a
     * {@code ShapeOutlineFeatureRenderer$Submit} that the frame draws later. So the {@code >=26.2}
     * arm takes the {@code MultiBufferSource} itself rather than a buffer pulled out of it, which is
     * what {@code ACSubmitBuffers} needs to recover the frame's collector; the rule that rewrites the
     * one call site emits that argument instead. Its trailing {@code false} is vanilla's
     * "is the thing being outlined translucent" flag, which picks the after-terrain phase over the
     * ordinary shape-outline one — the range box is not, and the ordinary phase is where the old
     * {@code RenderType.lines()} draw sat.
     */
    //? if >=26.2 {
    /*public static void renderLineBox(com.mojang.blaze3d.vertex.PoseStack poseStack,
                                     net.minecraft.client.renderer.MultiBufferSource source,
                                     net.minecraft.world.phys.AABB box, float red, float green, float blue, float alpha) {
        net.minecraft.client.renderer.SubmitNodeCollector collector =
                com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers.collectorOf(source);
        if (collector == null) {
            return;
        }
        collector.submitShapeOutline(poseStack,
                net.minecraft.world.phys.shapes.Shapes.create(box),
                net.minecraft.client.renderer.rendertype.RenderTypes.lines(),
                net.minecraft.util.ARGB.colorFromFloat(alpha, red, green, blue),
                net.minecraft.client.Minecraft.getInstance().getWindow().getAppropriateLineWidth(),
                false);
    }
    *///?} elif >=1.21.11 {
    /*public static void renderLineBox(com.mojang.blaze3d.vertex.PoseStack poseStack, com.mojang.blaze3d.vertex.VertexConsumer consumer,
                                     net.minecraft.world.phys.AABB box, float red, float green, float blue, float alpha) {
        net.minecraft.client.renderer.ShapeRenderer.renderShape(poseStack, consumer,
                net.minecraft.world.phys.shapes.Shapes.create(box), 0.0D, 0.0D, 0.0D,
                net.minecraft.util.ARGB.colorFromFloat(alpha, red, green, blue),
                net.minecraft.client.Minecraft.getInstance().getWindow().getAppropriateLineWidth());
    }
    *///?}

    // ── Block rendering ────────────────────────────────────────────────────────
    //
    // 26 deleted BlockRenderDispatcher outright — there is no `getBlockRenderer()` on Minecraft any
    // more, and with it went renderSingleBlock, getBlockModel, getBlockModelShaper and
    // renderBreakingTexture. Drawing a block is a *submission* now: a BlockModelResolver fills a
    // BlockModelRenderState, and that state submits itself to the frame's SubmitNodeCollector.
    // Everything this mod draws block-shaped goes through the three helpers below so that only one
    // place in the tree knows any of it.

    /**
     * The resolver + display context the {@code >=26} arms need, built on first use.
     *
     * <p>Both are cheap value objects with no reload state of their own — {@code BlockModelResolver}
     * holds the {@code ModelManager}, which is a {@code Minecraft} field that survives a resource
     * reload — so one instance for the process is correct, the same as vanilla's own renderers, each
     * of which keeps one it took from its {@code EntityRendererProvider.Context}. Built lazily
     * rather than in a static initialiser because this class is touched from places that run before
     * the model manager exists.
     */
    //? if >=26 {
    /*private static net.minecraft.client.renderer.block.BlockModelResolver acBlockModelResolver;
    private static net.minecraft.client.renderer.block.model.BlockDisplayContext acBlockDisplayContext;

    private static net.minecraft.client.renderer.block.BlockModelResolver blockModelResolver() {
        if (acBlockModelResolver == null) {
            acBlockDisplayContext = net.minecraft.client.renderer.block.model.BlockDisplayContext.create();
            acBlockModelResolver = new net.minecraft.client.renderer.block.BlockModelResolver(
                    net.minecraft.client.Minecraft.getInstance().getModelManager());
        }
        return acBlockModelResolver;
    }
    *///?}

    /**
     * Draws one block model at the current pose, the way {@code BlockRenderDispatcher
     * #renderSingleBlock} used to.
     *
     * <p>The {@code >=26} arm is vanilla's own path, read out of {@code CarriedBlockLayer}: resolve
     * the state into a {@code BlockModelRenderState} and submit it with the light, the overlay and
     * an outline colour of zero (nothing this mod draws this way is ever outlined — see the same
     * note on {@code ACSubmitBuffers}). It needs the frame's collector, which is what the
     * {@code MultiBufferSource} handed to a legacy render body already carries from 1.21.9; if it is
     * somehow not one of ours there is nothing to submit to, so the draw is skipped rather than
     * guessed at.
     */
    public static void renderSingleBlock(net.minecraft.world.level.block.state.BlockState state, PoseStack poseStack,
                                         net.minecraft.client.renderer.MultiBufferSource source, int light, int overlay) {
        //? if >=26 {
        /*net.minecraft.client.renderer.SubmitNodeCollector collector =
                com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers.collectorOf(source);
        if (collector == null) {
            return;
        }
        net.minecraft.client.renderer.block.BlockModelRenderState renderState =
                new net.minecraft.client.renderer.block.BlockModelRenderState();
        blockModelResolver().update(renderState, state, acBlockDisplayContext);
        renderState.submit(poseStack, collector, light, overlay, 0);
        *///?} else {
        net.minecraft.client.Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, poseStack, source, light, overlay);
        //?}
    }

    /**
     * Draws one block model tinted by an arbitrary RGB multiplier — the nuclear bomb flashing red as
     * it arms, the waste drum flashing as it bursts. Both are block-shaped entities whose model is a
     * single plain layer, which is why one render type serves the whole draw.
     *
     * <p>Three arms, because the way a model hands over its quads changed twice:
     * <ul>
     *   <li>below 1.21.5 a {@code BakedModel} answers {@code getQuads} per {@code Direction} per
     *       render type, so the walk is explicit and the tint is four floats on {@code putBulkData};
     *   <li>1.21.5 replaced it with {@code BlockStateModel} and moved the render type onto each
     *       part, so vanilla's static {@code ModelBlockRenderer#renderModel} — which is that walk,
     *       tint and all — replaces the whole body;
     *   <li>26 deleted that static too. A part's quads still come out as {@code BakedQuad}s, and
     *       {@code VertexConsumer#putBakedQuad} still takes them, but the per-vertex colour, light
     *       and overlay now travel together in a {@code QuadInstance}. So the explicit walk comes
     *       back, with the tint packed into one ARGB int.
     * </ul>
     *
     * <p>The tint is clamped before packing: callers pass values above 1 (the bomb's green channel
     * is {@code 1 + progress}), which the pre-26 arms clamped inside their quad loops and which
     * would otherwise wrap round in a colour byte.
     */
    public static void renderTintedBlock(net.minecraft.world.level.block.state.BlockState state, PoseStack poseStack,
                                         net.minecraft.client.renderer.MultiBufferSource source,
                                         float red, float green, float blue, int light, int overlay) {
        float r = net.minecraft.util.Mth.clamp(red, 0.0F, 1.0F);
        float g = net.minecraft.util.Mth.clamp(green, 0.0F, 1.0F);
        float b = net.minecraft.util.Mth.clamp(blue, 0.0F, 1.0F);
        //? if >=26 {
        /*net.minecraft.client.renderer.block.dispatch.BlockStateModel model =
                net.minecraft.client.Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
        java.util.List<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart> parts = new java.util.ArrayList<>();
        model.collectParts(net.minecraft.util.RandomSource.create(42L), parts);
        com.mojang.blaze3d.vertex.QuadInstance instance = new com.mojang.blaze3d.vertex.QuadInstance();
        instance.setColor(net.minecraft.util.ARGB.colorFromFloat(1.0F, r, g, b));
        instance.setLightCoords(light);
        instance.setOverlayCoords(overlay);
        com.mojang.blaze3d.vertex.VertexConsumer consumer = source.getBuffer(
                net.minecraft.client.renderer.rendertype.RenderTypes.entityCutoutCull(
                        net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS));
        for (net.minecraft.client.renderer.block.dispatch.BlockStateModelPart part : parts) {
            for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
                for (net.minecraft.client.resources.model.geometry.BakedQuad quad : part.getQuads(direction)) {
                    consumer.putBakedQuad(poseStack.last(), quad, instance);
                }
            }
            for (net.minecraft.client.resources.model.geometry.BakedQuad quad : part.getQuads(null)) {
                consumer.putBakedQuad(poseStack.last(), quad, instance);
            }
        }
        *///?} elif >=1.21.5 {
        /*net.minecraft.client.renderer.block.ModelBlockRenderer.renderModel(poseStack.last(),
                source.getBuffer(entityRenderType(net.minecraft.client.renderer.ItemBlockRenderTypes.getChunkRenderType(state))),
                net.minecraft.client.Minecraft.getInstance().getBlockRenderer().getBlockModel(state),
                r, g, b, light, overlay);
        *///?} else {
        net.minecraft.client.resources.model.BakedModel bakedModel =
                net.minecraft.client.Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        net.minecraft.util.RandomSource randomSource = net.minecraft.util.RandomSource.create();
        for (net.minecraft.client.renderer.RenderType renderType : modelRenderTypes(bakedModel, state, net.minecraft.util.RandomSource.create(42L), net.minecraftforge.client.model.data.ModelData.EMPTY)) {
            com.mojang.blaze3d.vertex.VertexConsumer consumer = source.getBuffer(entityRenderType(renderType));
            for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
                randomSource.setSeed(42L);
                for (net.minecraft.client.renderer.block.model.BakedQuad quad : modelQuads(bakedModel, state, direction, randomSource, net.minecraftforge.client.model.data.ModelData.EMPTY, renderType)) {
                    consumer.putBulkData(poseStack.last(), quad, r, g, b, light, overlay);
                }
            }
            randomSource.setSeed(42L);
            for (net.minecraft.client.renderer.block.model.BakedQuad quad : modelQuads(bakedModel, state, null, randomSource, net.minecraftforge.client.model.data.ModelData.EMPTY, renderType)) {
                consumer.putBulkData(poseStack.last(), quad, r, g, b, light, overlay);
            }
        }
        //?}
    }

    /**
     * The particle sprite of a block's model — what {@code getBlockModelShaper().getBlockModel(state)
     * .getParticleIcon()} answered before 26. Used to average a block's colour for the cave map.
     *
     * <p>26 hoisted it onto the model set itself as a {@code Material.Baked}, which is the sprite
     * plus a translucency flag; only the sprite is wanted here.
     */
    /**
     * {@code Font#drawInBatch(String, …)}, which 26.2 deleted along with the rest of immediate-mode
     * drawing. See {@link #submitTextTo} for what replaces it.
     */
    public static void drawInBatch(net.minecraft.client.gui.Font font, String text, float x, float y, int color,
                                   boolean dropShadow, org.joml.Matrix4f pose,
                                   net.minecraft.client.renderer.MultiBufferSource bufferSource,
                                   net.minecraft.client.gui.Font.DisplayMode displayMode,
                                   int backgroundColor, int packedLight) {
        //? if >=26.2 {
        /*submitTextTo(net.minecraft.util.FormattedCharSequence.forward(text, net.minecraft.network.chat.Style.EMPTY),
                x, y, color, dropShadow, pose, bufferSource, displayMode, backgroundColor, packedLight, 0);
        *///?} else {
        font.drawInBatch(text, x, y, color, dropShadow, pose, bufferSource, displayMode, backgroundColor, packedLight);
        //?}
    }

    /** {@code Font#drawInBatch(Component, …)}. See {@link #submitTextTo}. */
    public static void drawInBatch(net.minecraft.client.gui.Font font, net.minecraft.network.chat.Component text,
                                   float x, float y, int color,
                                   boolean dropShadow, org.joml.Matrix4f pose,
                                   net.minecraft.client.renderer.MultiBufferSource bufferSource,
                                   net.minecraft.client.gui.Font.DisplayMode displayMode,
                                   int backgroundColor, int packedLight) {
        //? if >=26.2 {
        /*submitTextTo(text.getVisualOrderText(), x, y, color, dropShadow, pose, bufferSource, displayMode,
                backgroundColor, packedLight, 0);
        *///?} else {
        font.drawInBatch(text, x, y, color, dropShadow, pose, bufferSource, displayMode, backgroundColor, packedLight);
        //?}
    }

    /**
     * {@code Font#drawInBatch8xOutline}. On 26.2 an outline is not a second draw but a non-zero
     * {@code outlineColor} on the one submitted node — {@code TextFeatureRenderer} branches to
     * {@code Font#prepare8xTextOutline} on exactly that, so the two spellings render identically.
     *
     * <p>⚠️ Which means an outline colour of 0 silently loses the outline. None of this mod's three
     * call sites can pass one: two are opaque constants and the crucible's is built by
     * {@code ACColors.argb} with an alpha floor of 4.
     */
    public static void drawInBatch8xOutline(net.minecraft.client.gui.Font font,
                                            net.minecraft.util.FormattedCharSequence text,
                                            float x, float y, int color, int outlineColor, org.joml.Matrix4f pose,
                                            net.minecraft.client.renderer.MultiBufferSource bufferSource,
                                            int packedLight) {
        //? if >=26.2 {
        /*submitTextTo(text, x, y, color, false, pose, bufferSource,
                net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, packedLight, outlineColor);
        *///?} else {
        font.drawInBatch8xOutline(text, x, y, color, outlineColor, pose, bufferSource, packedLight);
        //?}
    }

    // 26.2 draws text the way it draws everything else: one submitted node per string, replayed by
    // TextFeatureRenderer during the feature pass. OrderedSubmitNodeCollector#submitText takes the
    // same ten values drawInBatch did — plus the outline colour, which is how drawInBatch8xOutline
    // folds into it — so the translation is one for one and the callers keep their bodies.
    //
    // The pose arrives as the bare Matrix4f the callers already had off poseStack.last(), because
    // that is what every one of them was passing; submitText wants a PoseStack, and mulPose over a
    // fresh one reproduces it exactly (a glyph quad has no normal, so the normal matrix is unread).
    //
    // A null collector means the caller drew outside the frame's submission phase, which is the
    // same "nothing to draw into" case ACSubmitBuffers.flush() already tolerates.
    //? if >=26.2 {
    /*private static void submitTextTo(net.minecraft.util.FormattedCharSequence text, float x, float y, int color,
                                     boolean dropShadow, org.joml.Matrix4f pose,
                                     net.minecraft.client.renderer.MultiBufferSource bufferSource,
                                     net.minecraft.client.gui.Font.DisplayMode displayMode,
                                     int backgroundColor, int packedLight, int outlineColor) {
        net.minecraft.client.renderer.SubmitNodeCollector collector =
                com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers.collectorOf(bufferSource);
        if (collector == null) {
            return;
        }
        com.mojang.blaze3d.vertex.PoseStack stack = new com.mojang.blaze3d.vertex.PoseStack();
        stack.mulPose(pose);
        collector.submitText(stack, x, y, text, dropShadow, displayMode, packedLight, color, backgroundColor, outlineColor);
    }
    *///?}

    /**
     * Throws away every compiled chunk section so the whole visible world is re-meshed — what
     * {@code levelRenderer.allChanged()} did. Called when the camera entity changes, because the
     * "am I inside this block" tests baked into a section's geometry can answer differently for a
     * different viewer.
     *
     * <p>26.2 renamed it {@code invalidateCompiledGeometry} and made it take the four things it used
     * to read off a field: the level, the options, the camera and the block colours.
     */
    public static void invalidateChunkGeometry() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        //? if >=26.2 {
        /*mc.levelRenderer.invalidateCompiledGeometry(mc.level, mc.options, mc.gameRenderer.mainCamera(), mc.getBlockColors());
        *///?} else {
        mc.levelRenderer.allChanged();
        //?}
    }

    public static net.minecraft.client.renderer.texture.TextureAtlasSprite blockParticleSprite(net.minecraft.world.level.block.state.BlockState state) {
        //? if >=26 {
        /*return net.minecraft.client.Minecraft.getInstance().getModelManager().getBlockStateModelSet()
                .getParticleMaterial(state).sprite();
        *///?} else {
        return net.minecraft.client.Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getBlockModel(state).getParticleIcon();
        //?}
    }

    /**
     * Pushes a baked quad's vertices into a buffer with a per-quad ALPHA.
     *
     * <p>Vanilla's own {@code VertexConsumer#putBulkData} hardcodes alpha 1.0 up to 1.20.4 — the
     * alpha-carrying overload the four callers use is a Forge ADDITION there (the patched jar has
     * three overloads on 1.20.1/1.20.2/1.20.4, vanilla two), and vanilla then adopted the very same
     * shape: by 1.20.6 the patched jar is back to two overloads and both carry alpha. So only the
     * Fabric nodes below that need a body of their own.
     *
     * <p>⚠️ Neither 1.20.5 nor 1.20.3 has a cached jar to read, so the exact version vanilla adopted it
     * at is bracketed, not measured: it is 1.20.5 or 1.20.6. The gate says 1.20.5, which is the
     * spelling that fails LOUDLY (a "no suitable method" on the delegating arm) if it is wrong, rather
     * than silently keeping a redundant hand-rolled copy alive. Re-check when 1.20.5-fabric lands.
     *
     * <p>The Fabric body is a faithful transcription of vanilla's, minus the {@code MemoryStack}: the
     * {@code int[]} a quad exposes IS the packed {@code DefaultVertexFormat.BLOCK} vertex, eight ints
     * per vertex, which vanilla reads by aliasing it as a little-endian {@code ByteBuffer} — three
     * position floats, four colour bytes (so byte 0 of the word is red), then u and v. Only the
     * literal 1.0F alpha vanilla passes on becomes the argument.
     *
     * <p>No caller exists at or above 1.21.4, where the baked model is gone and an item render state
     * draws itself; hence the empty top arm, which also keeps this off 1.21.11, where the overload
     * drops its trailing boolean.
     */
    //? if >=1.21.4 {
    /*
    *///?} elif fabric && <1.20.5 {
    /*public static void putBulkData(com.mojang.blaze3d.vertex.VertexConsumer consumer, PoseStack.Pose pose,
            net.minecraft.client.renderer.block.model.BakedQuad quad, float[] brightness,
            float red, float green, float blue, float alpha, int[] lightmap, int packedOverlay,
            boolean readExistingColor) {
        int[] packed = quad.getVertices();
        net.minecraft.core.Vec3i face = quad.getDirection().getNormal();
        org.joml.Matrix4f matrix = pose.pose();
        org.joml.Vector3f normal = pose.normal().transform(
                new org.joml.Vector3f((float) face.getX(), (float) face.getY(), (float) face.getZ()));
        int stride = 8;
        int vertices = packed.length / stride;
        for (int i = 0; i < vertices; i++) {
            int o = i * stride;
            float x = Float.intBitsToFloat(packed[o]);
            float y = Float.intBitsToFloat(packed[o + 1]);
            float z = Float.intBitsToFloat(packed[o + 2]);
            float r;
            float g;
            float b;
            if (readExistingColor) {
                int color = packed[o + 3];
                r = (float) (color & 255) / 255.0F * brightness[i] * red;
                g = (float) (color >> 8 & 255) / 255.0F * brightness[i] * green;
                b = (float) (color >> 16 & 255) / 255.0F * brightness[i] * blue;
            } else {
                r = brightness[i] * red;
                g = brightness[i] * green;
                b = brightness[i] * blue;
            }
            float u = Float.intBitsToFloat(packed[o + 4]);
            float v = Float.intBitsToFloat(packed[o + 5]);
            org.joml.Vector4f position = matrix.transform(new org.joml.Vector4f(x, y, z, 1.0F));
            consumer.vertex(position.x(), position.y(), position.z(), r, g, b, alpha, u, v,
                    packedOverlay, lightmap[i], normal.x(), normal.y(), normal.z());
        }
    }
    *///?} else {
    public static void putBulkData(com.mojang.blaze3d.vertex.VertexConsumer consumer, PoseStack.Pose pose,
            net.minecraft.client.renderer.block.model.BakedQuad quad, float[] brightness,
            float red, float green, float blue, float alpha, int[] lightmap, int packedOverlay,
            boolean readExistingColor) {
        consumer.putBulkData(pose, quad, brightness, red, green, blue, alpha, lightmap, packedOverlay, readExistingColor);
    }
    //?}

    /**
     * The level renderer's client tick counter — what the render-stage dispatch hands its listeners
     * as the render tick.
     *
     * <p>⚠️ {@code LevelRenderer#getTicks()} is a LOADER PATCH, present on Forge and NeoForge for the
     * whole range (javap'd on Forge 1.20.1, NeoForge 1.20.6 and both loaders at 1.21.6) and on
     * neither vanilla nor Fabric, where the backing {@code ticks} field is private. Fabric therefore
     * reads the field through the access widener rather than calling a method that does not exist.
     *
     * <p>The two callers — {@code ChunkSectionsToRenderMixin} from 1.21.6 and
     * {@code LevelRenderStageMixin}'s <b>static</b> sky arm from 1.21.11 — are both places where the
     * mixin cannot simply {@code @Shadow} the field: the first targets a different class, the second
     * has no {@code this}. Every other dispatch site shadows it and never comes through here.
     *
     * <p>The {@code >=26.2} arm exists only so this compiles there. 26.2 deleted the field and the
     * getter in the same change, and both call sites are bounded below it, so nothing reaches it.
     */
    //? if >=26.2 {
    /*public static int levelRendererTicks(net.minecraft.client.renderer.LevelRenderer renderer) {
        return 0;
    }
    *///?} elif fabric {
    /*public static int levelRendererTicks(net.minecraft.client.renderer.LevelRenderer renderer) {
        return renderer.ticks;
    }
    *///?} else {
    public static int levelRendererTicks(net.minecraft.client.renderer.LevelRenderer renderer) {
        return renderer.getTicks();
    }
    //?}
}
