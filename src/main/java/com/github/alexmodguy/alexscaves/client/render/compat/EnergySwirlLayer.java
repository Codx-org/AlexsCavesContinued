package com.github.alexmodguy.alexscaves.client.render.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Pre-1.21.2 {@code EnergySwirlLayer<T, M>} — the first parameter is the entity again, and the
 * "is it charged" test comes off the entity through {@link PowerableMob} rather than off a render
 * state.
 *
 * <p>The body is vanilla's, reproduced rather than delegated for the same reason the sibling
 * {@link RenderLayer#renderColoredModel} is: it has to end at the compat {@link EntityModel}'s own
 * eight-float {@code renderToBuffer}, since 1.21.2 made {@code Model}'s overloads {@code final} and
 * the root this hierarchy hands vanilla is empty.
 */
public abstract class EnergySwirlLayer<T extends Entity & PowerableMob, M extends EntityModel<?>> extends RenderLayer<T, M> {

	public EnergySwirlLayer(RenderLayerParent<ACRenderState, M> parent) {
		super(parent);
	}

	@Override
	public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T entity,
			float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
		if (entity.isPowered()) {
			float bob = entity.tickCount + partialTicks;
			EntityModel<T> swirl = this.model();
			swirl.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTicks);
			this.getParentModel().copyPropertiesTo(swirl);
			VertexConsumer consumer = bufferSource.getBuffer(
					RenderType.energySwirl(this.getTextureLocation(), this.xOffset(bob) % 1.0F, bob * 0.01F % 1.0F));
			swirl.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
			swirl.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, 0.5F, 0.5F, 0.5F, 1.0F);
		}
	}

	protected abstract float xOffset(float bob);

	protected abstract ResourceLocation getTextureLocation();

	protected abstract EntityModel<T> model();
}
