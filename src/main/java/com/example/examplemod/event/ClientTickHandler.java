package com.example.examplemod.event;

import com.example.examplemod.manager.StackGroupManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ClientTickHandler {
    private int ticksUntilRebuild;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            ticksUntilRebuild = 0;
            StackGroupManager.getInstance().clear();
            return;
        }
        if (ticksUntilRebuild > 0) {
            ticksUntilRebuild--;
            return;
        }
        ticksUntilRebuild = 4;
        StackGroupManager.getInstance().rebuild(minecraft.level);
    }

    @SubscribeEvent
    public void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ticksUntilRebuild = 0;
        StackGroupManager.getInstance().clear();
    }
}
