package net.rp.rpessentials.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.rp.rpessentials.client.LicenseCardRenderer;
import net.rp.rpessentials.items.LicenseItem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
@OnlyIn(Dist.CLIENT)
public abstract class MixinItemInHandRenderer {

    @Shadow @Final
    private EntityRenderDispatcher entityRenderDispatcher;

    @Inject(
            method = "renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/player/LocalPlayer;I)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void rpessentials$renderLicenseCard(float partialTick, PoseStack poseStack,
                                                MultiBufferSource.BufferSource bufferSource,
                                                LocalPlayer player, int combinedLight,
                                                CallbackInfo ci) {

        if (!(player.getMainHandItem().getItem() instanceof LicenseItem)) return;

        // 1. Head-bob
        float xBob = Mth.lerp(partialTick, player.xBobO, player.xBob);
        float yBob = Mth.lerp(partialTick, player.yBobO, player.yBob);
        poseStack.mulPose(Axis.XP.rotationDegrees((player.getViewXRot(partialTick) - xBob) * 0.1F));
        poseStack.mulPose(Axis.YP.rotationDegrees((player.getViewYRot(partialTick) - yBob) * 0.1F));

        // 2. Inclinaison selon le pitch
        float f = Mth.clamp(1.0F - player.getViewXRot(partialTick) / 45.0F + 0.1F, 0.0F, 1.0F);
        float tilt = -Mth.cos(f * (float) Math.PI) * 0.5F + 0.5F;

        poseStack.translate(0.0F, 0.04F + tilt * -0.5F, -0.72F);
        poseStack.mulPose(Axis.XP.rotationDegrees(tilt * -85.0F));

        // 3. Bras
        if (!player.isInvisible()) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            rpessentials$renderMapHand(poseStack, bufferSource, combinedLight, player, HumanoidArm.RIGHT);
            rpessentials$renderMapHand(poseStack, bufferSource, combinedLight, player, HumanoidArm.LEFT);
            poseStack.popPose();
        }

        LicenseCardRenderer.renderCard(poseStack, bufferSource, player.getMainHandItem(), combinedLight);
        bufferSource.endBatch();
        ci.cancel();
    }

    private void rpessentials$renderMapHand(PoseStack poseStack, MultiBufferSource buffer,
                                            int packedLight, LocalPlayer player, HumanoidArm side) {
        PlayerRenderer renderer = (PlayerRenderer) entityRenderDispatcher.getRenderer(player);
        float sign = side == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(92.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(sign * -41.0F));
        poseStack.translate(sign * 0.3F, -1.1F, 0.45F);
        if (side == HumanoidArm.RIGHT) {
            renderer.renderRightHand(poseStack, buffer, packedLight, player);
        } else {
            renderer.renderLeftHand(poseStack, buffer, packedLight, player);
        }
        poseStack.popPose();
    }
}