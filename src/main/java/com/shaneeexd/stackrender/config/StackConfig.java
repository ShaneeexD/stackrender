package com.shaneeexd.stackrender.config;

import com.shaneeexd.stackrender.StackRender;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = StackRender.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class StackConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER.define("enabled", true);
    public static final ForgeConfigSpec.IntValue MINIMUM_STACK_SIZE = BUILDER.defineInRange("minimumStackSize", 3, 2, Integer.MAX_VALUE);
    public static final ForgeConfigSpec.BooleanValue SHOW_LABEL = BUILDER.define("showLabel", true);
    public static final ForgeConfigSpec.DoubleValue LABEL_SCALE = BUILDER.defineInRange("labelScale", 1.0D, 0.1D, 10.0D);
    public static final ForgeConfigSpec.BooleanValue SUPPRESS_DEATH_ANIMATIONS = BUILDER.define("suppressDeathAnimations", true);
    public static final ForgeConfigSpec.IntValue MAX_SCAN_DISTANCE = BUILDER.defineInRange("maxScanDistance", 64, 1, 512);
    public static final ForgeConfigSpec.IntValue GROUPING_CELL_SIZE = BUILDER.defineInRange("groupingCellSize", 1, 1, 8);
    public static final ForgeConfigSpec.BooleanValue STACK_PASSIVE_MOBS = BUILDER.define("stackPassiveMobs", true);
    public static final ForgeConfigSpec.BooleanValue STACK_HOSTILE_MOBS = BUILDER.define("stackHostileMobs", true);
    public static final ForgeConfigSpec.BooleanValue GROUP_SHEEP_BY_COLOR = BUILDER.define("groupSheepByColor", false);
    public static final ForgeConfigSpec.BooleanValue GROUP_CAT_BY_VARIANT = BUILDER.define("groupCatByVariant", false);
    public static final ForgeConfigSpec.BooleanValue GROUP_HORSE_BY_VARIANT = BUILDER.define("groupHorseByVariant", false);
    public static final ForgeConfigSpec.BooleanValue GROUP_TROPICAL_FISH_BY_VARIANT = BUILDER.define("groupTropicalFishByVariant", false);
    public static final ForgeConfigSpec.BooleanValue DEBUG_OVERLAY = BUILDER.define("debugOverlay", false);
    public static final ForgeConfigSpec.BooleanValue DEBUG_LOGGING = BUILDER.define("debugLogging", false);
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> DISABLED_ENTITY_TYPES = BUILDER.defineListAllowEmpty(
            "disabledEntityTypes",
            List.of(),
            StackConfig::isValidEntityTypeId
    );

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean enabled;
    public static int minimumStackSize;
    public static boolean showLabel;
    public static float labelScale;
    public static boolean suppressDeathAnimations;
    public static int maxScanDistance;
    public static int groupingCellSize;
    public static boolean stackPassiveMobs;
    public static boolean stackHostileMobs;
    public static boolean groupSheepByColor;
    public static boolean groupCatByVariant;
    public static boolean groupHorseByVariant;
    public static boolean groupTropicalFishByVariant;
    public static boolean debugOverlay;
    public static boolean debugLogging;
    public static final Set<String> disabledEntityTypeIds = new LinkedHashSet<>();

    private static ModConfig activeConfig;

    private StackConfig() {
    }

    public static EditableValues snapshot() {
        EditableValues values = new EditableValues();
        values.enabled = enabled;
        values.minimumStackSize = minimumStackSize;
        values.showLabel = showLabel;
        values.labelScale = labelScale;
        values.suppressDeathAnimations = suppressDeathAnimations;
        values.maxScanDistance = maxScanDistance;
        values.groupingCellSize = groupingCellSize;
        values.stackPassiveMobs = stackPassiveMobs;
        values.stackHostileMobs = stackHostileMobs;
        values.groupSheepByColor = groupSheepByColor;
        values.groupCatByVariant = groupCatByVariant;
        values.groupHorseByVariant = groupHorseByVariant;
        values.groupTropicalFishByVariant = groupTropicalFishByVariant;
        values.debugOverlay = debugOverlay;
        values.debugLogging = debugLogging;
        values.disabledEntityTypeIds.addAll(disabledEntityTypeIds);
        return values;
    }

    public static EditableValues defaults() {
        EditableValues values = new EditableValues();
        values.enabled = true;
        values.minimumStackSize = 3;
        values.showLabel = true;
        values.labelScale = 1.0F;
        values.suppressDeathAnimations = true;
        values.maxScanDistance = 64;
        values.groupingCellSize = 1;
        values.stackPassiveMobs = true;
        values.stackHostileMobs = true;
        values.groupSheepByColor = false;
        values.groupCatByVariant = false;
        values.groupHorseByVariant = false;
        values.groupTropicalFishByVariant = false;
        values.debugOverlay = false;
        values.debugLogging = false;
        return values;
    }

    public static void apply(EditableValues values) {
        ENABLED.set(values.enabled);
        MINIMUM_STACK_SIZE.set(Math.max(2, values.minimumStackSize));
        SHOW_LABEL.set(values.showLabel);
        LABEL_SCALE.set((double) Math.max(0.1F, values.labelScale));
        SUPPRESS_DEATH_ANIMATIONS.set(values.suppressDeathAnimations);
        MAX_SCAN_DISTANCE.set(Math.max(1, values.maxScanDistance));
        GROUPING_CELL_SIZE.set(clamp(values.groupingCellSize, 1, 8));
        STACK_PASSIVE_MOBS.set(values.stackPassiveMobs);
        STACK_HOSTILE_MOBS.set(values.stackHostileMobs);
        GROUP_SHEEP_BY_COLOR.set(values.groupSheepByColor);
        GROUP_CAT_BY_VARIANT.set(values.groupCatByVariant);
        GROUP_HORSE_BY_VARIANT.set(values.groupHorseByVariant);
        GROUP_TROPICAL_FISH_BY_VARIANT.set(values.groupTropicalFishByVariant);
        DEBUG_OVERLAY.set(values.debugOverlay);
        DEBUG_LOGGING.set(values.debugLogging);
        DISABLED_ENTITY_TYPES.set(new ArrayList<>(values.disabledEntityTypeIds));
        bake();
        if (activeConfig != null) {
            activeConfig.save();
        }
    }

    public static boolean isEntityTypeEnabled(EntityType<?> entityType) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entityType);
        return key != null && !disabledEntityTypeIds.contains(key.toString());
    }

    @SubscribeEvent
    public static void onLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        activeConfig = event.getConfig();
        bake();
    }

    private static void bake() {
        enabled = ENABLED.get();
        minimumStackSize = MINIMUM_STACK_SIZE.get();
        showLabel = SHOW_LABEL.get();
        labelScale = LABEL_SCALE.get().floatValue();
        suppressDeathAnimations = SUPPRESS_DEATH_ANIMATIONS.get();
        maxScanDistance = MAX_SCAN_DISTANCE.get();
        groupingCellSize = GROUPING_CELL_SIZE.get();
        stackPassiveMobs = STACK_PASSIVE_MOBS.get();
        stackHostileMobs = STACK_HOSTILE_MOBS.get();
        groupSheepByColor = GROUP_SHEEP_BY_COLOR.get();
        groupCatByVariant = GROUP_CAT_BY_VARIANT.get();
        groupHorseByVariant = GROUP_HORSE_BY_VARIANT.get();
        groupTropicalFishByVariant = GROUP_TROPICAL_FISH_BY_VARIANT.get();
        debugOverlay = DEBUG_OVERLAY.get();
        debugLogging = DEBUG_LOGGING.get();
        disabledEntityTypeIds.clear();
        for (String id : DISABLED_ENTITY_TYPES.get()) {
            disabledEntityTypeIds.add(id);
        }
    }

    private static boolean isValidEntityTypeId(Object value) {
        if (!(value instanceof String id)) {
            return false;
        }
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key != null && ForgeRegistries.ENTITY_TYPES.containsKey(key);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static final class EditableValues {
        public boolean enabled;
        public int minimumStackSize;
        public boolean showLabel;
        public float labelScale;
        public boolean suppressDeathAnimations;
        public int maxScanDistance;
        public int groupingCellSize;
        public boolean stackPassiveMobs;
        public boolean stackHostileMobs;
        public boolean groupSheepByColor;
        public boolean groupCatByVariant;
        public boolean groupHorseByVariant;
        public boolean groupTropicalFishByVariant;
        public boolean debugOverlay;
        public boolean debugLogging;
        public final Set<String> disabledEntityTypeIds = new LinkedHashSet<>();
    }
}


