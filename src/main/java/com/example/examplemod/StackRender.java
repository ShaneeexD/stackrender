package com.example.examplemod;

import com.mojang.logging.LogUtils;
import com.example.examplemod.config.StackConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(StackRender.MOD_ID)
public final class StackRender {
    public static final String MOD_ID = "stackrender";
    public static final Logger LOGGER = LogUtils.getLogger();

    public StackRender(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.CLIENT, StackConfig.SPEC, "stackrender-client.toml");
        MinecraftForge.EVENT_BUS.register(new com.example.examplemod.event.ClientTickHandler());
    }
}
