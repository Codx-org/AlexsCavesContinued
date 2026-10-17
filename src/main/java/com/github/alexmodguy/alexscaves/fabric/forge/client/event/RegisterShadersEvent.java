package com.github.alexmodguy.alexscaves.fabric.forge.client.event;

import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Event;

/**
 * Fabric stand-in for core-shader registration.
 *
 * <p>The phase has three lives across this tree's range and only the first two reach this loader.
 * Up to 1.21.1 a mod compiled its own program during the event and handed back a setter to keep it
 * in; 1.21.2 reduced that to declaring the program and letting the client compile, reload and supply
 * it; from 1.21.5 nothing registers anything at all and the declarations stand on their own, so the
 * listener and its handler are both gated away and this class is left empty.
 *
 * <p>The arms mirror {@code ClientProxy}'s exactly. Nothing is imported: the compiled-shader type is
 * gone above 1.21.1 and the declaration type does not exist below 1.21.2, so either import would be
 * a hard error on the nodes the other arm serves.
 *
 * <p>⚠️ Owed to the dispatcher, not to this class: Fabric registers a core shader through a callback
 * of its own rather than at a moment on a bus, and the callback supplies the resource provider the
 * pre-1.21.2 arm reads. Firing this event from inside that callback is what makes the sink below a
 * real destination rather than a place the eight programs are dropped.
 */
public class RegisterShadersEvent extends Event {

    //? if <1.21.2 {
    private final net.minecraft.server.packs.resources.ResourceProvider resourceProvider;
    private final java.util.function.BiConsumer<net.minecraft.client.renderer.ShaderInstance, java.util.function.Consumer<net.minecraft.client.renderer.ShaderInstance>> sink;

    public RegisterShadersEvent(net.minecraft.server.packs.resources.ResourceProvider resourceProvider,
                                java.util.function.BiConsumer<net.minecraft.client.renderer.ShaderInstance, java.util.function.Consumer<net.minecraft.client.renderer.ShaderInstance>> sink) {
        this.resourceProvider = resourceProvider;
        this.sink = sink;
    }

    public net.minecraft.server.packs.resources.ResourceProvider getResourceProvider() {
        return resourceProvider;
    }

    public void registerShader(net.minecraft.client.renderer.ShaderInstance shader,
                               java.util.function.Consumer<net.minecraft.client.renderer.ShaderInstance> setter) {
        sink.accept(shader, setter);
    }
    //?} elif <1.21.5 {
    /*private final java.util.function.Consumer<net.minecraft.client.renderer.ShaderProgram> sink;

    public RegisterShadersEvent(java.util.function.Consumer<net.minecraft.client.renderer.ShaderProgram> sink) {
        this.sink = sink;
    }

    public void registerShader(net.minecraft.client.renderer.ShaderProgram program) {
        sink.accept(program);
    }
    *///?}
}
