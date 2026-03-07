package com.example.examplemod;

import com.example.examplemod.client.StackDebugOverlay;
import com.example.examplemod.client.StackConfigScreen;
import com.mojang.logging.LogUtils;
import com.example.examplemod.config.StackConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(StackRender.MOD_ID)
public final class StackRender {
    public static final String MOD_ID = "stackrender";
    public static final Logger LOGGER = LogUtils.getLogger();

    public StackRender(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.CLIENT, StackConfig.SPEC, "stackrender-client.toml");
        MinecraftForge.EVENT_BUS.register(new com.example.examplemod.event.ClientTickHandler());
        MinecraftForge.EVENT_BUS.register(new StackDebugOverlay());
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ClientModEvents {
        private ClientModEvents() {
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            ModLoadingContext.get().registerExtensionPoint(
                    ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new StackConfigScreen(parent))
            );
        }
    }
}
