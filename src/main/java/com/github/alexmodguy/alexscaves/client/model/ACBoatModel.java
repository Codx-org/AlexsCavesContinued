package com.github.alexmodguy.alexscaves.client.model;

import com.github.alexmodguy.alexscaves.citadel.client.model.AdvancedEntityModel;
import com.github.alexmodguy.alexscaves.citadel.client.model.AdvancedModelBox;
import net.minecraft.util.Mth;
import com.github.alexmodguy.alexscaves.server.entity.util.AlexsCavesBoat;
import net.minecraft.world.entity.Entity;

public abstract class ACBoatModel extends AdvancedEntityModel<Entity> {

    public abstract AdvancedModelBox getWaterMask();

    public void setupPaddleAnims(AlexsCavesBoat boat, AdvancedModelBox leftPaddle, AdvancedModelBox rightPaddle, float partialTicks) {
        animatePaddle(boat, 0, leftPaddle, partialTicks);
        animatePaddle(boat, 1, rightPaddle, partialTicks);
    }

    private static void animatePaddle(AlexsCavesBoat boat, int i, AdvancedModelBox paddle, float partialTicks) {
        float f = boat.acGetRowingTime(i, partialTicks);
        float f1 = i == 1 ? -1 : 1;
        paddle.rotateAngleZ = -f1 * Mth.clampedLerp((-(float) Math.PI / 3F), -0.2617994F, (Mth.sin(-f) + 1.0F) / 2.0F);
        paddle.rotateAngleY = f1 * Mth.clampedLerp((-(float) Math.PI / 4F), ((float) Math.PI / 4F), (Mth.sin(-f + 1.0F) + 1.0F) / 2.0F);
    }
}
