package com.shaneeexd.stackrender.client;

import com.shaneeexd.stackrender.config.StackConfig;
import com.shaneeexd.stackrender.manager.StackGroupManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class StackDebugOverlay {
    @SubscribeEvent
    public void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!StackConfig.enabled || !StackConfig.debugOverlay) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null) {
            return;
        }

        StackGroupManager manager = StackGroupManager.getInstance();
        GuiGraphics guiGraphics = event.getGuiGraphics();
        int x = 6;
        int y = 6;
        int color = 0xFFFFFF;

        guiGraphics.drawString(minecraft.font, Component.literal("Entity Stack Renderer"), x, y, color, true);
        guiGraphics.drawString(minecraft.font, Component.literal("Candidates: " + manager.getLastCandidateCount()), x, y + 10, color, true);
        guiGraphics.drawString(minecraft.font, Component.literal("Groups: " + manager.getLastGroupCount()), x, y + 20, color, true);
        guiGraphics.drawString(minecraft.font, Component.literal("Suppressed: " + manager.getLastSuppressedCount()), x, y + 30, color, true);
        guiGraphics.drawString(minecraft.font, Component.literal(String.format("Rebuild: %.3f ms", manager.getLastRebuildMillis())), x, y + 40, color, true);
    }
}


