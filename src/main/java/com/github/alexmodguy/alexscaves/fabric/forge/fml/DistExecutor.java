package com.github.alexmodguy.alexscaves.fabric.forge.fml;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

import java.util.function.Supplier;

/**
 * Fabric stand-in for Forge's side-selecting helper, used at exactly two sites in this tree — the
 * {@code AlexsCaves} and {@code Citadel} proxy fields.
 *
 * <p><b>The doubled {@link Supplier} is the API's whole point and is reproduced exactly.</b> The
 * caller writes {@code runForDist(() -> ClientProxy::new, () -> CommonProxy::new)}: the method
 * reference lives inside the outer lambda's body, so the class it names is a constant-pool entry
 * that the JVM only resolves when that lambda is actually invoked. A dedicated server therefore
 * never loads the client proxy — which matters more here than on Forge, since Fabric has no
 * equivalent of {@code RuntimeDistCleaner} to catch the mistake if it did.
 */
public final class DistExecutor {

    private DistExecutor() {
    }

    public static <T> T runForDist(Supplier<Supplier<T>> clientTarget, Supplier<Supplier<T>> serverTarget) {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT
                ? clientTarget.get().get()
                : serverTarget.get().get();
    }
}
