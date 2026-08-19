package com.github.alexmodguy.alexscaves.fabric.forge.fml.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Fabric stand-in for the annotation that declares a mod class on Forge, and for the nested one
 * that declares a class of static event handlers.
 *
 * <p><b>Both are inert here, and that is the whole point.</b> On Forge the first is what constructs
 * the mod class and the second is what scans a class for handlers; on Fabric the entrypoints in
 * {@code fabric.mod.json} do the first and {@code fabric/event/**} does the second, so nothing on
 * this loader ever reads either annotation. They are reproduced rather than gated out because the
 * two classes that carry them — {@code AlexsCaves} and {@code ACEntityRegistry} — already sit
 * under four- and five-armed gate chains for the three spellings the other two loaders want, and a
 * fifth arm buys nothing an empty annotation type does not. Retention is {@code SOURCE}: keeping
 * it out of the class file makes it impossible for anything at runtime to mistake it for the real
 * thing.
 *
 * <p>The nested {@code Bus} enum keeps both constants even though a Fabric build has neither bus,
 * for the same reason: the attribute is written at the call site and has to name something.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Mod {

    String value();

    /**
     * Fabric stand-in for Forge's static-handler-class marker. Never scanned on this loader — see
     * the outer type.
     */
    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.TYPE)
    @interface EventBusSubscriber {

        String modid() default "";

        Bus bus() default Bus.FORGE;

        /** The two buses Forge distinguishes. Fabric has neither; the attribute still needs a type. */
        enum Bus {
            FORGE,
            MOD,
        }
    }
}
