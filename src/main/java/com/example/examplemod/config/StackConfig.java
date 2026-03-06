package com.example.examplemod.config;

import com.example.examplemod.StackRender;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = StackRender.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class StackConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER.define("enabled", true);
    public static final ForgeConfigSpec.IntValue MINIMUM_STACK_SIZE = BUILDER.defineInRange("minimumStackSize", 3, 2, Integer.MAX_VALUE);
    public static final ForgeConfigSpec.BooleanValue SHOW_LABEL = BUILDER.define("showLabel", true);
    public static final ForgeConfigSpec.DoubleValue LABEL_SCALE = BUILDER.defineInRange("labelScale", 1.0D, 0.1D, 10.0D);
    public static final ForgeConfigSpec.BooleanValue GROUP_SHEEP_BY_COLOR = BUILDER.define("groupSheepByColor", true);
    public static final ForgeConfigSpec.BooleanValue SUPPRESS_DEATH_ANIMATIONS = BUILDER.define("suppressDeathAnimations", true);
    public static final ForgeConfigSpec.IntValue MAX_SCAN_DISTANCE = BUILDER.defineInRange("maxScanDistance", 64, 1, 512);
    public static final ForgeConfigSpec.BooleanValue STACK_PASSIVE_MOBS = BUILDER.define("stackPassiveMobs", true);
    public static final ForgeConfigSpec.BooleanValue STACK_HOSTILE_MOBS = BUILDER.define("stackHostileMobs", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean enabled;
    public static int minimumStackSize;
    public static boolean showLabel;
    public static float labelScale;
    public static boolean groupSheepByColor;
    public static boolean suppressDeathAnimations;
    public static int maxScanDistance;
    public static boolean stackPassiveMobs;
    public static boolean stackHostileMobs;

    private StackConfig() {
    }

    @SubscribeEvent
    public static void onLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        enabled = ENABLED.get();
        minimumStackSize = MINIMUM_STACK_SIZE.get();
        showLabel = SHOW_LABEL.get();
        labelScale = LABEL_SCALE.get().floatValue();
        groupSheepByColor = GROUP_SHEEP_BY_COLOR.get();
        suppressDeathAnimations = SUPPRESS_DEATH_ANIMATIONS.get();
        maxScanDistance = MAX_SCAN_DISTANCE.get();
        stackPassiveMobs = STACK_PASSIVE_MOBS.get();
        stackHostileMobs = STACK_HOSTILE_MOBS.get();
    }
}
