package com.github.alexmodguy.alexscaves.client.sound;

import com.github.alexmodguy.alexscaves.server.misc.ACSoundRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.Music;

public class ACMusics {

    private static Music luxtructosaurusBossMusic;

    /**
     * The boss music, built on first use.
     *
     * <p>This used to be a {@code static final} field holding a {@code Music} subclass that passed
     * {@code null} for the sound event and resolved it in an overridden {@code getEvent()}, because
     * the field initialises while {@code ACSoundRegistry} is still filling. 1.21.6 turned
     * {@code Music} into a record, so there is nothing left to subclass — but deferring the whole
     * object rather than just its accessor answers the same problem and needs no version gate at
     * all. Both call sites run while the fight is on screen, long after registration.
     *
     * <p>A fresh instance per call would also be correct — {@code MusicManager} compares music by
     * the sound event's id, never by identity — the field is only there to avoid the churn.
     */
    public static Music luxtructosaurusBossMusic() {
        if (luxtructosaurusBossMusic == null) {
            luxtructosaurusBossMusic = new Music(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(ACSoundRegistry.LUXTRUCTOSAURUS_BOSS_MUSIC.get()), 0, 0, true);
        }
        return luxtructosaurusBossMusic;
    }
}
