package com.github.alexmodguy.alexscaves.citadel.client.rewards;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.citadel.server.entity.CitadelEntityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.*;

public class CitadelCapes {

    private static final List<Cape> CAPES = new ArrayList<>();
    private static final Map<UUID, Boolean> HAS_CAPES_ENABLED = new LinkedHashMap<>();

    public static void addCapeFor(List<UUID> uuids, String translationKey, ResourceLocation texture) {
        CAPES.add(new Cape(uuids, translationKey, texture));
    }

    public static List<Cape> getCapesFor(UUID uuid) {
        return CAPES.isEmpty() ? CAPES : CAPES.stream().filter(cape -> cape.isFor(uuid)).toList();
    }


    public static Cape getNextCape(String currentID, UUID playerUUID) {
        if (CAPES.isEmpty()) {
            return null;
        }
        int currentIndex = -1;
        for (int i = 0; i < CAPES.size(); i++) {
            if (CAPES.get(i).getCapeId().equals(currentID)) {
                currentIndex = i;
                break;
            }
        }
        for (int i = currentIndex + 1; i < CAPES.size(); i++) {
            if (CAPES.get(i).isFor(playerUUID)) {
                return CAPES.get(i);
            }
        }
        return null;
    }

    @Nullable
    public static Cape getById(String identifier) {
        for (int i = 0; i < CAPES.size(); i++) {
            if (CAPES.get(i).getCapeId().equals(identifier)) {
                return CAPES.get(i);
            }
        }
        return null;
    }

    @Nullable
    private static Cape getFirstApplicable(Player player) {
        for (int i = 0; i < CAPES.size(); i++) {
            if (CAPES.get(i).isFor(player.getUUID())) {
                return CAPES.get(i);
            }
        }
        return null;
    }

    public static Cape getCurrentCape(Player player) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(player);
        if (ACCompat.getBoolean(tag, "CitadelCapeDisabled")) {
            return null;
        }
        if (tag.contains("CitadelCapeType")) {
            if (ACCompat.getString(tag, "CitadelCapeType").isEmpty()) {
                return getFirstApplicable(player);
            } else {
                return CitadelCapes.getById(ACCompat.getString(tag, "CitadelCapeType"));
            }
        } else {
            // Upstream returned null here and relied on Citadel's cape-selection screen to write the
            // tag. That screen is not vendored, so with no tag nobody would ever get a cape — fall
            // back to the first cape the player is eligible for.
            return getFirstApplicable(player);
        }
    }

    public static class Cape {
        private final List<UUID> isFor;
        private final String identifier;
        private final ResourceLocation texture;

        public Cape(List<UUID> isFor, String identifier, ResourceLocation texture) {
            this.isFor = isFor;
            this.identifier = identifier;
            this.texture = texture;
        }

        public List<UUID> getIsFor() {
            return isFor;
        }

        public String getCapeId() {
            return identifier;
        }

        public ResourceLocation getTexture() {
            return texture;
        }

        public boolean isFor(UUID uuid) {
            return isFor.contains(uuid);
        }
    }
}
