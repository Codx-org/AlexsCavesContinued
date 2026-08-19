package com.github.alexmodguy.alexscaves.client.render.compat;

import com.github.alexmodguy.alexscaves.client.ACClientCompat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Pre-1.21.2 {@code ItemInHandLayer<T, M>}.
 *
 * <p>Two things changed in 1.21.2 that the subclasses in this mod cannot absorb: the layer became
 * keyed on a render state, and {@code renderArmWithItem} gained a baked-model argument and lost the
 * entity. Both hooks are restored here in their old shapes, so the two inner {@code ItemLayer}
 * classes (gingerbread man, licowitch) keep their overrides verbatim.
 *
 * <p>The vanilla layer is also constructed directly in a few renderers with an
 * {@link ItemInHandRenderer} from the dispatcher — the same object on every version — which is why
 * this constructor keeps that parameter rather than 1.21.2's {@code ItemRenderer}.
 */
public class ItemInHandLayer<T extends LivingEntity, M extends EntityModel<?> & ArmedModel> extends RenderLayer<T, M> {

	private final ItemInHandRenderer itemInHandRenderer;

	public ItemInHandLayer(RenderLayerParent<ACRenderState, M> parent, ItemInHandRenderer itemInHandRenderer) {
		super(parent);
		this.itemInHandRenderer = itemInHandRenderer;
	}

	@Override
	public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T entity,
			float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
		boolean rightHanded = entity.getMainArm() == HumanoidArm.RIGHT;
		ItemStack inLeft = rightHanded ? entity.getOffhandItem() : entity.getMainHandItem();
		ItemStack inRight = rightHanded ? entity.getMainHandItem() : entity.getOffhandItem();
		if (!inLeft.isEmpty() || !inRight.isEmpty()) {
			poseStack.pushPose();
			if (this.getParentModel().young) {
				poseStack.translate(0.0F, 0.75F, 0.0F);
				poseStack.scale(0.5F, 0.5F, 0.5F);
			}
			this.renderArmWithItem(entity, inRight, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, HumanoidArm.RIGHT, poseStack, bufferSource, packedLight);
			this.renderArmWithItem(entity, inLeft, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, HumanoidArm.LEFT, poseStack, bufferSource, packedLight);
			poseStack.popPose();
		}
	}

	protected void renderArmWithItem(LivingEntity entity, ItemStack stack, ItemDisplayContext displayContext, HumanoidArm arm,
			PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		if (!stack.isEmpty()) {
			poseStack.pushPose();
			this.getParentModel().translateToHand(arm, poseStack);
			poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
			poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
			boolean left = arm == HumanoidArm.LEFT;
			poseStack.translate((left ? -1 : 1) / 16.0F, 0.125F, -0.625F);
			ACClientCompat.renderItemInHand(this.itemInHandRenderer, entity, stack, displayContext, left, poseStack, bufferSource, packedLight);
			poseStack.popPose();
		}
	}
}
