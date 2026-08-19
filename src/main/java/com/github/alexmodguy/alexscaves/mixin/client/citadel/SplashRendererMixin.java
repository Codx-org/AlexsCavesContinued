package com.github.alexmodguy.alexscaves.mixin.client.citadel;

import com.github.alexmodguy.alexscaves.client.ACClientCompat;

import com.github.alexmodguy.alexscaves.citadel.CitadelEvent;
import com.github.alexmodguy.alexscaves.citadel.CitadelConstants;
import com.github.alexmodguy.alexscaves.citadel.client.event.EventRenderSplashText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SplashRenderer.class)
public class SplashRendererMixin {

    // 1.21.11 retyped the field: the splash is a Component now, and the yellow that used to be a
    // literal in render() lives in the style SplashManager gives it (DEFAULT_STYLE). A @Shadow is
    // matched by descriptor as well as by name, so the declaration has to move with it.
    //? if >=1.21.11 {
    /*@Mutable
    @Shadow
    @Final
    private net.minecraft.network.chat.Component splash;
    *///?} else {
    @Mutable
    @Shadow
    @Final
    private String splash;
    //?}

    private int splashTextColor = -1;

    // 1.21.6 rebuilt the GUI's transform stack: GuiGraphics#pose is a Matrix3x2fStack, so the splash's
    // tilt is a single rotate(F) rather than a quaternion mulPose, and render's trailing argument
    // became the fade alpha as a float — which moves the target descriptor as well as the anchor. The
    // three arms below carry annotation and signature only; the bodies are shared.
    //
    // The middle arm exists because 1.21.5 widened the rotation overload to the read-only joml
    // interface, which changes the descriptor the call site names even though the argument handed
    // over is the same object. (Said here rather than on the arm itself: a `//` line between an arm's
    // marker and its `/*` is stripped of its prefix when that arm activates — see DEVELOPMENT.md.)
    // 1.21.11 stopped drawing through the GuiGraphics transform stack altogether: it builds its own
    // local Matrix3x2f and hands it to the ActiveTextCollector, so the tilt is the same rotate(F)
    // call on a plain Matrix3x2f rather than on the stack.
    //? if >=1.21.11 {
    /*@Inject(
            method = {"Lnet/minecraft/client/gui/components/SplashRenderer;render(Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/client/gui/Font;F)V"},
            remap = CitadelConstants.REMAPREFS,
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/joml/Matrix3x2f;rotate(F)Lorg/joml/Matrix3x2f;",
                    shift = At.Shift.BEFORE
            ))
    protected void citadel_preRenderSplashText(GuiGraphics guiGraphics, int width, Font font, float loadProgress, CallbackInfo ci) {
    *///?} elif >=1.21.6 {
    /*@Inject(
            method = {"Lnet/minecraft/client/gui/components/SplashRenderer;render(Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/client/gui/Font;F)V"},
            remap = CitadelConstants.REMAPREFS,
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/joml/Matrix3x2fStack;rotate(F)Lorg/joml/Matrix3x2f;",
                    shift = At.Shift.BEFORE
            ))
    protected void citadel_preRenderSplashText(GuiGraphics guiGraphics, int width, Font font, float loadProgress, CallbackInfo ci) {
    *///?} elif >=1.21.5 {
    /*@Inject(
            method = {"Lnet/minecraft/client/gui/components/SplashRenderer;render(Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/client/gui/Font;I)V"},
            remap = CitadelConstants.REMAPREFS,
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionfc;)V",
                    shift = At.Shift.BEFORE
            ))
    protected void citadel_preRenderSplashText(GuiGraphics guiGraphics, int width, Font font, int loadProgress, CallbackInfo ci) {
    *///?} else {
    @Inject(
            method = {"Lnet/minecraft/client/gui/components/SplashRenderer;render(Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/client/gui/Font;I)V"},
            remap = CitadelConstants.REMAPREFS,
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionf;)V",
                    shift = At.Shift.BEFORE
            ))
    protected void citadel_preRenderSplashText(GuiGraphics guiGraphics, int width, Font font, int loadProgress, CallbackInfo ci) {
    //?}
        ACClientCompat.pushPose(guiGraphics);
        citadel_firePre(guiGraphics);
    }

    // Firing the event is what the field retype splits, not the injection, so it is hoisted here
    // rather than duplicated into a fourth annotation arm. On 1.21.11 the Citadel colour override
    // rides the Component's own style — that is where vanilla's yellow went — which is also why
    // that node needs no @ModifyConstant at all.
    //? if >=1.21.11 {
    /*@org.spongepowered.asm.mixin.Unique
    private void citadel_firePre(GuiGraphics guiGraphics) {
        EventRenderSplashText.Pre event = new EventRenderSplashText.Pre(splash.getString(), guiGraphics, ACClientCompat.partialTick(), 16776960);
        EventRenderSplashText.Pre.post(event);

        if (event.getCitadelResult() == CitadelEvent.Result.ALLOW) {
            splash = net.minecraft.network.chat.Component.literal(event.getSplashText())
                    .withStyle(net.minecraft.network.chat.Style.EMPTY.withColor(event.getSplashTextColor()));
        }
    }
    *///?} else {
    private void citadel_firePre(GuiGraphics guiGraphics) {
        EventRenderSplashText.Pre event = new EventRenderSplashText.Pre(splash, guiGraphics, ACClientCompat.partialTick(), 16776960);
        EventRenderSplashText.Pre.post(event);

        if (event.getCitadelResult() == CitadelEvent.Result.ALLOW) {
            splash = event.getSplashText();
            splashTextColor = event.getSplashTextColor();
        }
    }
    //?}

    // The draw itself moved too: 1.21.11 hands the finished Component to the frame's
    // ActiveTextCollector instead of calling GuiGraphics#drawCenteredString, so the post hook
    // follows the text to its new sink.
    //? if >=1.21.11 {
    /*@Inject(
            method = {"Lnet/minecraft/client/gui/components/SplashRenderer;render(Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/client/gui/Font;F)V"},
            remap = CitadelConstants.REMAPREFS,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/ActiveTextCollector;accept(Lnet/minecraft/client/gui/TextAlignment;IILnet/minecraft/client/gui/ActiveTextCollector$Parameters;Lnet/minecraft/network/chat/Component;)V",
                    shift = At.Shift.AFTER
            )
    )
    protected void citadel_postRenderSplashText(GuiGraphics guiGraphics, int width, Font font, float loadProgress, CallbackInfo ci) {
    *///?} elif >=1.21.6 {
    /*@Inject(
            method = {"Lnet/minecraft/client/gui/components/SplashRenderer;render(Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/client/gui/Font;F)V"},
            remap = CitadelConstants.REMAPREFS,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawCenteredString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V",
                    shift = At.Shift.AFTER
            )
    )
    protected void citadel_postRenderSplashText(GuiGraphics guiGraphics, int width, Font font, float loadProgress, CallbackInfo ci) {
    *///?} else {
    @Inject(
            method = {"Lnet/minecraft/client/gui/components/SplashRenderer;render(Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/client/gui/Font;I)V"},
            remap = CitadelConstants.REMAPREFS,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawCenteredString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V",
                    shift = At.Shift.AFTER
            )
    )
    protected void citadel_postRenderSplashText(GuiGraphics guiGraphics, int width, Font font, int loadProgress, CallbackInfo ci) {
    //?}
        citadel_firePost(guiGraphics);
        ACClientCompat.popPose(guiGraphics);
    }

    // Same split as citadel_firePre: the event is String-shaped on every version, the field is not.
    //? if >=1.21.11 {
    /*@org.spongepowered.asm.mixin.Unique
    private void citadel_firePost(GuiGraphics guiGraphics) {
        EventRenderSplashText.Post.post(new EventRenderSplashText.Post(splash.getString(), guiGraphics, ACClientCompat.partialTick()));
    }
    *///?} else {
    private void citadel_firePost(GuiGraphics guiGraphics) {
        EventRenderSplashText.Post.post(new EventRenderSplashText.Post(splash, guiGraphics, ACClientCompat.partialTick()));
    }
    //?}

    // The yellow moved with the alpha. Up to 1.21.5 vanilla OR'd 0x00FFFF00 with the alpha byte and
    // handed the result to drawCenteredString; 1.21.6 passes 0xFFFFFF00 to ARGB.color(float, int),
    // which masks the colour to 24 bits — so the event's RGB means exactly the same thing on both
    // sides and only the constant to match changes.
    // 1.21.11 has no colour constant in render() at all: the yellow is a Style on the Component,
    // applied by SplashManager, so there is nothing here to modify and citadel_firePre carries the
    // override on the Component instead. The arm is deliberately empty — the whole method goes away
    // on that band, which is also why splashTextColor is only written below 1.21.11.
    //? if >=1.21.11 {
    /*
    *///?} elif >=1.21.6 {
    /*@ModifyConstant(
            method = {"Lnet/minecraft/client/gui/components/SplashRenderer;render(Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/client/gui/Font;F)V"},
            remap = CitadelConstants.REMAPREFS,
            constant = @Constant(intValue = -256))
    private int citadel_splashTextColor(int value) {
        return splashTextColor == -1 ? value : splashTextColor;
    }
    *///?} else {
    @ModifyConstant(
            method = {"Lnet/minecraft/client/gui/components/SplashRenderer;render(Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/client/gui/Font;I)V"},
            remap = CitadelConstants.REMAPREFS,
            constant = @Constant(intValue = 16776960))
    private int citadel_splashTextColor(int value) {
        return splashTextColor == -1 ? value : splashTextColor;
    }
    //?}
}
