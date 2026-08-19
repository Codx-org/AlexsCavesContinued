package com.github.alexmodguy.alexscaves.citadel.server.generation;

/**
 * Implemented on {@code NoiseGeneratorSettings} by {@code mixin.citadel.NoiseGeneratorSettingsMixin}
 * so the merged surface rules can be swapped back out for the vanilla ones while level data is
 * being written.
 */
public interface NoiseGeneratorSettingsAccessor {

    void onSaveData(boolean saving);
}
