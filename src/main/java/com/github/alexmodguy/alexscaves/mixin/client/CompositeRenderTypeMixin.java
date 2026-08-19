package com.github.alexmodguy.alexscaves.mixin.client;

import com.github.alexmodguy.alexscaves.client.ACClientCompat;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Puts {@link ACClientCompat#setImmediateTint} back on the map for 1.21.6.
 *
 * <p>Every hand-rolled immediate-mode draw this mod does used to tint itself with
 * {@code RenderSystem#setShaderColor}, a global that the bound core shader read as its
 * {@code ColorModulator}. 1.21.6 deleted it: the modulator is one member of the per-draw
 * {@code DynamicTransforms} uniform block, written into a ring buffer by whoever builds the render
 * pass. For a mod that is {@code RenderType#draw}, which hardcodes the modulator to white and takes
 * no colour argument — so the only ways to tint a mesh are to reimplement that whole method with one
 * constant changed, or to change the constant. This is the constant.
 *
 * <p>{@code CompositeRenderType} is package-private, hence {@code targets}. The handler runs on
 * vanilla's draws too, so it is deliberately nothing but a null check on a static that
 * {@link ACClientCompat#drawImmediate} clears in a {@code finally} — a tint is live for exactly the
 * one draw that asked for it.
 *
 * <p>Not applied below 1.21.6, where {@code setShaderColor} still exists and the source file is
 * excluded from the compile: see {@code ModPlatformPlugin}'s {@code vanishedMixins}.
 *
 * <p>1.21.11 folded the composite subclass away: {@code RenderType} moved to
 * {@code …renderer.rendertype} and became a single concrete class, so {@code draw(MeshData)} — and
 * the {@code new Vector4f(1, 1, 1, 1)} inside it — sit on the outer type now. Only the two strings
 * move; the handler is untouched. Note both are safe from the package's rename rules, which key on
 * a trailing space, comma or {@code >} and on the import line.
 *
 * <p>26.2 deleted {@code draw(MeshData)} outright — a render type does not own a draw call any more,
 * it hands out a {@code PreparedRenderType} for the caller to draw GPU buffers with (see
 * {@code ACClientCompat#drawImmediate}'s own 26.2 arm). The modulator moved with it, into the
 * {@code prepare()} path: the transforms slice is written by a private
 * {@code writeDynamicTransforms(Matrix4f)}, whose one statement calls
 * {@code DynamicUniforms#writeTransform(Matrix4f, Matrix4f)} — and that two-argument overload is
 * precisely the one that hardcodes the modulator, filling in the private {@code WHITE} and
 * {@code NO_OFFSET} constants before delegating. So there is no {@code new Vector4f} left to modify;
 * what there is instead is a choice of overload, which makes the hook a {@code @Redirect} that swaps
 * in the four-argument form {@code (modelView, colorModulator, modelOffset, textureMatrix)} whenever
 * a tint is pending. Both overloads build the same {@code Transform} record, so the untinted path is
 * byte-for-byte what vanilla would have done.
 */
//? if >=1.21.11 {
/*@Mixin(targets = "net.minecraft.client.renderer.rendertype.RenderType")
*///?} else {
@Mixin(targets = "net.minecraft.client.renderer.RenderType$CompositeRenderType")
//?}
public abstract class CompositeRenderTypeMixin {

    // The whole member is gated rather than just the annotation, because from 26.2 the handler is a
    // different shape: a @Redirect standing in for a two-argument call, not a @ModifyExpressionValue
    // on a constructed value. The two older arms therefore repeat the body verbatim — there is no
    // shared tail an arm chain can leave behind once the signatures disagree.
    //? if >=26.2 {
    /*@org.spongepowered.asm.mixin.injection.Redirect(
            method = "Lnet/minecraft/client/renderer/rendertype/RenderType;writeDynamicTransforms(Lorg/joml/Matrix4f;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/DynamicUniforms;writeTransform(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"
            ),
            remap = true
    )
    private com.mojang.blaze3d.buffers.GpuBufferSlice ac_immediateTint(net.minecraft.client.renderer.DynamicUniforms uniforms, org.joml.Matrix4f modelView, org.joml.Matrix4f textureMatrix) {
        Vector4f tint = ACClientCompat.immediateTint;
        return tint == null
                ? uniforms.writeTransform(modelView, textureMatrix)
                : uniforms.writeTransform(modelView, tint, new org.joml.Vector3f(), textureMatrix);
    }
    *///?} elif >=1.21.11 {
    /*@ModifyExpressionValue(
            method = "Lnet/minecraft/client/renderer/rendertype/RenderType;draw(Lcom/mojang/blaze3d/vertex/MeshData;)V",
            at = @At(value = "NEW", target = "(FFFF)Lorg/joml/Vector4f;"),
            remap = true
    )
    private Vector4f ac_immediateTint(Vector4f colorModulator) {
        Vector4f tint = ACClientCompat.immediateTint;
        return tint == null ? colorModulator : tint;
    }
    *///?} else {
    @ModifyExpressionValue(
            method = "Lnet/minecraft/client/renderer/RenderType$CompositeRenderType;draw(Lcom/mojang/blaze3d/vertex/MeshData;)V",
            at = @At(value = "NEW", target = "(FFFF)Lorg/joml/Vector4f;"),
            remap = true
    )
    private Vector4f ac_immediateTint(Vector4f colorModulator) {
        Vector4f tint = ACClientCompat.immediateTint;
        return tint == null ? colorModulator : tint;
    }
    //?}
}
