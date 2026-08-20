package com.github.alexmodguy.alexscaves.mixin.fabric.client;

import com.github.alexmodguy.alexscaves.fabric.forge.client.event.ViewportEvent;
import com.github.alexmodguy.alexscaves.fabric.forge.common.MinecraftForge;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric's producer for {@code ViewportEvent.ComputeCameraAngles} — by some distance the most
 * user-visible of the client events, since {@code ClientEvents#computeCameraAngles} is what does the
 * screen shake (tremorsaurus/tremorzilla/atlatitan footfalls, the nuke flash, a possession), what
 * forces first person while something else is wearing your eyes, and what pulls the third-person
 * camera back far enough to see a submarine, a gum worm or a tremorzilla you are riding.
 *
 * <p><b>Why TAIL of {@code Camera#setup} rather than Forge's own position.</b> Forge posts the event
 * from {@code GameRenderer#renderLevel}, one statement after {@code camera.setup(…)} returns. Every
 * one of the handler's effects reaches the camera through {@link
 * com.github.alexmodguy.alexscaves.client.ACClientCompat#cameraMove} / {@code cameraMaxZoom}, i.e.
 * it mutates the camera object rather than reading anything {@code renderLevel} owns — so posting at
 * the tail of {@code setup} itself puts the mutation at the same point in the frame, on a target
 * whose descriptor this tree already tracks. The three bands below are copied verbatim from the
 * loader-neutral {@link com.github.alexmodguy.alexscaves.mixin.client.CameraMixin}, which injects at
 * the same TAIL and is verified on all 58 nodes.
 *
 * <p>The {@code priority} is raised above the default 1000 so this mixin is applied <i>after</i>
 * {@code CameraMixin}, which makes the magnet-reorientation run before the shake — the order Forge
 * has, where the magnet code is also a {@code setup} tail hook and the event comes later.
 *
 * <p>⚠️ <b>{@code setRoll} is deliberately not consumed.</b> Forge applies the returned roll by
 * rotating {@code renderLevel}'s own pose stack about Z, and from 1.21.6 the world's view matrix is
 * built from {@code camera.rotation()} into a local {@code Matrix4f} with no stable anchor to modify
 * across this range. The only caller that sets it is the STUNNED effect's cosmetic camera tilt; the
 * effect's other behaviour is unaffected, and every other branch of the handler works. Posting with
 * a roll of zero and ignoring the result is therefore an honest, contained gap rather than a
 * multi-band mixin that could break the frame on a node it was not read against.
 */
@Mixin(value = Camera.class, priority = 1500)
public class CameraSetupMixin {

    //? if >=26 {
    /*@Inject(method = {"Lnet/minecraft/client/Camera;alignWithEntity(F)V"}, remap = true, at = @At(value = "TAIL"))
    public void ac_fabricComputeCameraAngles(float partialTicks, CallbackInfo ci) {
    *///?} elif >=1.21.11 {
    /*@Inject(method = {"Lnet/minecraft/client/Camera;setup(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;ZZF)V"}, remap = true, at = @At(value = "TAIL"))
    public void ac_fabricComputeCameraAngles(net.minecraft.world.level.Level level, net.minecraft.world.entity.Entity entity, boolean detached, boolean mirrored, float partialTicks, CallbackInfo ci) {
    *///?} else {
    @Inject(method = {"Lnet/minecraft/client/Camera;setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V"}, remap = true, at = @At(value = "TAIL"))
    public void ac_fabricComputeCameraAngles(net.minecraft.world.level.BlockGetter level, net.minecraft.world.entity.Entity entity, boolean detached, boolean mirrored, float partialTicks, CallbackInfo ci) {
    //?}
        MinecraftForge.EVENT_BUS.post(new ViewportEvent.ComputeCameraAngles(
                (Camera) (Object) this, partialTicks, 0.0F));
    }
}
