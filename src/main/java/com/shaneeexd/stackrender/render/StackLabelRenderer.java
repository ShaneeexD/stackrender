package com.shaneeexd.stackrender.render;

import com.shaneeexd.stackrender.config.StackConfig;
import com.shaneeexd.stackrender.manager.StackGroupManager;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;

public final class StackLabelRenderer {
    private StackLabelRenderer() {
    }

    public static void renderLabel(LivingEntity entity, PoseStack poseStack, MultiBufferSource bufferSource) {
        if (!StackConfig.enabled || !StackConfig.showLabel) {
            return;
        }

        int count = StackGroupManager.getInstance().getStackCount(entity);
        if (count < StackConfig.minimumStackSize) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            return;
        }
        Font font = minecraft.font;
        Component text = Component.literal("x" + count);
        float yOffset = entity.getBbHeight() + 0.5F;
        float distanceScale = Math.max(0.6F, (float) Math.sqrt(player.distanceToSqr(entity)) * 0.025F);
        float scale = 0.025F * StackConfig.labelScale * distanceScale;
        int color = getColor(count);

        poseStack.pushPose();
        poseStack.translate(0.0D, yOffset, 0.0D);
        poseStack.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-scale, -scale, scale);

        Matrix4f matrix = poseStack.last().pose();
        float x = -font.width(text) / 2.0F;
        int background = (int) (minecraft.options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
        font.drawInBatch(text, x, 0.0F, color, false, matrix, bufferSource, Font.DisplayMode.SEE_THROUGH, background, LightTexture.FULL_BRIGHT);
        font.drawInBatch(text, x, 0.0F, color, false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    private static int getColor(int count) {
        if (count >= 100) {
            return 0xFF3333;
        }
        if (count >= 30) {
            return 0xFF8800;
        }
        if (count >= 10) {
            return 0xFFFF00;
        }
        return 0xFFFFFF;
    }
}


